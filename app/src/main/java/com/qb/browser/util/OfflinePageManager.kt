package com.qb.browser.util

import android.content.Context
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import android.webkit.WebResourceResponse
import android.webkit.WebView
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages saving and loading offline pages
 */
class OfflinePageManager(private val context: Context) {
    
    companion object {
        private const val TAG = "OfflinePageManager"
        private const val DIRECTORY_OFFLINE = "offline_pages"
        private const val DEFAULT_BUFFER_SIZE = 8192
        
        @Volatile
        private var instance: OfflinePageManager? = null
        
        fun getInstance(context: Context): OfflinePageManager {
            return instance ?: synchronized(this) {
                instance ?: OfflinePageManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * Data class representing metadata about an offline page
     */
    data class OfflinePage(
        val id: String,
        val url: String,
        val title: String,
        val timestamp: Long,
        val filePath: String,
        val thumbnailPath: String? = null,
        val size: Long = 0
    )
    
    /**
     * Interface for callbacks during the save operation
     */
    interface SaveCallback {
        fun onProgress(progress: Int, message: String = "")
        fun onComplete(pageId: String, filePath: String)
        fun onError(error: String)
    }
    
    // Cache of loaded resources
    private val resourceCache = ConcurrentHashMap<String, WebResourceResponse>()
    
    // Keep track of save operations
    private val saveOperations = ConcurrentHashMap<String, SaveOperation>()
    
    private class SaveOperation(val totalResources: AtomicInteger, val savedResources: AtomicInteger)
    
    /**
     * Get the base directory for offline pages
     */
    private fun getOfflineBaseDir(): File {
        val externalDir = context.getExternalFilesDir(DIRECTORY_OFFLINE)
            ?: context.filesDir.resolve(DIRECTORY_OFFLINE)
        
        if (!externalDir.exists()) {
            externalDir.mkdirs()
        }
        
        return externalDir
    }
    
    /**
     * Create a directory for a specific page
     */
    private fun createPageDirectory(pageId: String): File {
        val pageDir = getOfflineBaseDir().resolve(pageId)
        if (!pageDir.exists()) {
            pageDir.mkdirs()
        }
        return pageDir
    }
    
    /**
     * Get the directory for a specific page
     */
    fun getPageDirectory(pageId: String): File {
        return getOfflineBaseDir().resolve(pageId)
    }
    
    /**
     * Get the directory path for a specific offline page
     * Used by OfflineWebViewClient to resolve resource paths
     */
    fun getOfflinePageDir(pageId: String): String {
        return getPageDirectory(pageId).absolutePath
    }
    
    /**
     * Generate a unique ID for a page
     */
    private fun generatePageId(url: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val urlHash = url.hashCode().toString().replace("-", "0")
        return "page_${timestamp}_$urlHash"
    }
    
    /**
     * Save a web page for offline reading by directly using provided HTML content
     */
    suspend fun savePageWithHtmlContent(
        url: String,
        title: String,
        htmlContent: String,
        callback: SaveCallback? = null
    ): Result<String> = 
        kotlin.runCatching {
            withContext(Dispatchers.IO) {
                // Generate a unique ID for this page
                val pageId = generatePageId(url)
                
                // Create directory for this page
                val pageDir = createPageDirectory(pageId)
                
                // Initialize save operation tracking
                val saveOperation = SaveOperation(AtomicInteger(0), AtomicInteger(0))
                saveOperations[pageId] = saveOperation
                
                callback?.onProgress(0, "Preparing to save page...")
                
                callback?.onProgress(10, "Processing HTML content...")
                
                // Parse HTML to find resources
                val document = Jsoup.parse(htmlContent, url)
                
                // Download CSS, images, and other resources
                val resourcesDir = File(pageDir, "resources")
                if (!resourcesDir.exists()) {
                    resourcesDir.mkdirs()
                }
                
                // Process different types of resources
                val cssLinks: Elements = document.select("link[rel=stylesheet]")
                val images: Elements = document.select("img[src]")
                val scripts: Elements = document.select("script[src]")
                val favicons: Elements = document.select("link[rel=icon], link[rel=shortcut icon]")
                
                // Calculate total resources
                val totalResources = cssLinks.size + images.size + scripts.size + favicons.size
                saveOperation.totalResources.set(totalResources)
                
                callback?.onProgress(15, "Downloading resources...")
                
                // Download and update CSS files
                downloadAndReplaceResources(
                    document,
                    cssLinks,
                    "href",
                    resourcesDir,
                    pageId,
                    saveOperation,
                    callback
                )
                
                // Download and update images
                downloadAndReplaceResources(
                    document,
                    images,
                    "src",
                    resourcesDir,
                    pageId,
                    saveOperation,
                    callback
                )
                
                // Download and update JavaScript
                downloadAndReplaceResources(
                    document,
                    scripts,
                    "src",
                    resourcesDir,
                    pageId,
                    saveOperation,
                    callback
                )
                
                // Download and update favicons
                downloadAndReplaceResources(
                    document,
                    favicons,
                    "href",
                    resourcesDir,
                    pageId,
                    saveOperation,
                    callback
                )
                
                callback?.onProgress(90, "Saving final HTML...")
                
                // Save modified HTML
                val htmlFile = File(pageDir, "index.html")
                htmlFile.writeText(document.outerHtml())
                
                // Save metadata
                saveMetadata(
                    OfflinePage(
                        id = pageId,
                        url = url,
                        title = title,
                        timestamp = System.currentTimeMillis(),
                        filePath = htmlFile.absolutePath
                    )
                )
                
                callback?.onProgress(100, "Page saved successfully")
                callback?.onComplete(pageId, htmlFile.absolutePath)
                
                // Clean up
                saveOperations.remove(pageId)
                
                pageId
            }
        }.onError(tag = TAG) { e ->
            Log.e(TAG, "Error saving page with HTML content: ${e.message}", e)
            callback?.onError("Failed to save page: ${e.message}")
            "Failed to save page: ${e.message}"
        }

    /**
     * Save a web page for offline reading
     */
    suspend fun savePage(
        webView: WebView, 
        url: String, 
        title: String, 
        callback: SaveCallback? = null
    ): Result<String> = 
        kotlin.runCatching {
            withContext(Dispatchers.IO) {
                // Generate a unique ID for this page
                val pageId = generatePageId(url)
                
                // Create directory for this page
                val pageDir = createPageDirectory(pageId)
                
                // Initialize save operation tracking
                val saveOperation = SaveOperation(AtomicInteger(0), AtomicInteger(0))
                saveOperations[pageId] = saveOperation
                
                callback?.onProgress(0, "Preparing to save page...")
                
                // Get HTML content
                val htmlContent = withContext(Dispatchers.Main) {
                    webView.evaluateJavascript(
                        "(function() { return document.documentElement.outerHTML; })();",
                        null
                    )
                    
                    // Wait a bit for JavaScript to execute
                    kotlinx.coroutines.delay(500)
                    
                    // Create a callback to get the HTML content
                    var resultHtml = ""
                    webView.evaluateJavascript(
                        "(function() { return document.documentElement.outerHTML; })();",
                        { result -> 
                            // Store the raw HTML result
                            resultHtml = result
                        }
                    )
                    
                    // Give time for the JavaScript callback to execute
                    kotlinx.coroutines.delay(1000)
                    
                    // Remove quotes added by evaluateJavascript
                    if (resultHtml.length >= 2) {
                        resultHtml.substring(1, resultHtml.length - 1)
                            .replace("\\\"", "\"")
                            .replace("\\n", "\n")
                            .replace("\\r", "\r")
                            .replace("\\t", "\t")
                            .replace("\\\\", "\\")
                    } else {
                        // Fallback if JavaScript execution failed
                        "<html><body><p>Failed to extract content</p></body></html>"
                    }
                }
                
                callback?.onProgress(10, "Processing HTML content...")
                
                // Parse HTML to find resources
                val document = Jsoup.parse(htmlContent.toString(), url)
                
                // Download CSS, images, and other resources
                val resourcesDir = File(pageDir, "resources")
                if (!resourcesDir.exists()) {
                    resourcesDir.mkdirs()
                }
                
                // Process different types of resources
                val cssLinks: Elements = document.select("link[rel=stylesheet]")
                val images: Elements = document.select("img[src]")
                val scripts: Elements = document.select("script[src]")
                val favicons: Elements = document.select("link[rel=icon], link[rel=shortcut icon]")
                
                // Calculate total resources
                val totalResources = cssLinks.size + images.size + scripts.size + favicons.size
                saveOperation.totalResources.set(totalResources)
                
                callback?.onProgress(15, "Downloading resources...")
                
                // Download and update CSS files
                downloadAndReplaceResources(
                    document,
                    cssLinks,
                    "href",
                    resourcesDir,
                    pageId,
                    saveOperation,
                    callback
                )
                
                // Download and update images
                downloadAndReplaceResources(
                    document,
                    images,
                    "src",
                    resourcesDir,
                    pageId,
                    saveOperation,
                    callback
                )
                
                // Download and update JavaScript
                downloadAndReplaceResources(
                    document,
                    scripts,
                    "src",
                    resourcesDir,
                    pageId,
                    saveOperation,
                    callback
                )
                
                // Download and update favicons
                downloadAndReplaceResources(
                    document,
                    favicons,
                    "href",
                    resourcesDir,
                    pageId,
                    saveOperation,
                    callback
                )
                
                callback?.onProgress(90, "Saving final HTML...")
                
                // Save modified HTML
                val htmlFile = File(pageDir, "index.html")
                htmlFile.writeText(document.outerHtml())
                
                // Save metadata
                saveMetadata(
                    OfflinePage(
                        id = pageId,
                        url = url,
                        title = title,
                        timestamp = System.currentTimeMillis(),
                        filePath = htmlFile.absolutePath
                    )
                )
                
                callback?.onProgress(100, "Page saved successfully")
                callback?.onComplete(pageId, htmlFile.absolutePath)
                
                // Clean up
                saveOperations.remove(pageId)
                
                pageId
            }
        }.onError(tag = TAG) { e ->
            Log.e(TAG, "Error saving page: ${e.message}", e)
            callback?.onError("Failed to save page: ${e.message}")
            "Failed to save page: ${e.message}"
        }
    
    /**
     * Download and replace resources in the document
     */
    private suspend fun downloadAndReplaceResources(
        document: Document,
        elements: Elements,
        urlAttribute: String,
        resourcesDir: File,
        pageId: String,
        saveOperation: SaveOperation,
        callback: SaveCallback?
    ) {
        elements.forEach { element ->
            try {
                val resourceUrl = element.absUrl(urlAttribute)
                if (resourceUrl.isNotEmpty()) {
                    val resourceFilename = createResourceFilename(resourceUrl)
                    val resourceFile = File(resourcesDir, resourceFilename)
                    
                    // Download resource
                    val success = downloadResource(resourceUrl, resourceFile)
                    
                    if (success) {
                        // Update element to point to local file
                        val relativePath = "resources/$resourceFilename"
                        element.attr(urlAttribute, relativePath)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing resource: ${e.message}", e)
            } finally {
                // Update progress
                val saved = saveOperation.savedResources.incrementAndGet()
                val total = saveOperation.totalResources.get()
                val progressPercent = if (total > 0) ((saved.toFloat() / total) * 75 + 15).toInt() else 15
                
                callback?.onProgress(
                    progressPercent.coerceIn(15, 90),
                    "Downloading resources ($saved/$total)..."
                )
            }
        }
    }
    
    /**
     * Create a filename for a resource based on its URL
     */
    private fun createResourceFilename(url: String): String {
        val uri = URL(url)
        val path = uri.path
        val filename = path.substring(path.lastIndexOf('/') + 1)
        
        // If filename is empty or doesn't have an extension, create one
        if (filename.isEmpty() || !filename.contains('.')) {
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(
                getMimeTypeFromUrl(url)
            ) ?: "dat"
            
            return "${url.hashCode().toString().replace("-", "0")}.$extension"
        }
        
        // Replace illegal characters
        return filename.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
    
    /**
     * Get the mime type from a URL
     */
    private fun getMimeTypeFromUrl(url: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(url) ?: ""
        return if (extension.isNotEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
        } else {
            "application/octet-stream"
        }
    }
    
    /**
     * Download a resource to a file
     */
    private suspend fun downloadResource(url: String, outputFile: File): Boolean = 
        withErrorHandlingAndFallback(
            tag = TAG,
            errorMessage = "Error downloading resource $url",
            fallback = false
        ) {
            val connection = URL(url).openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 30000
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
            )
            
            val inputStream = BufferedInputStream(connection.getInputStream())
            
            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
                output.flush()
            }
            
            inputStream.close()
            true
        }
    
    /**
     * Save metadata for an offline page
     */
    private fun saveMetadata(page: OfflinePage) {
        val metadataFile = File(getPageDirectory(page.id), "metadata.json")
        val json = """
            {
              "id": "${page.id}",
              "url": "${page.url}",
              "title": "${page.title.replace("\"", "\\\"")}",
              "timestamp": ${page.timestamp},
              "filePath": "${page.filePath}",
              "thumbnailPath": ${if (page.thumbnailPath != null) "\"${page.thumbnailPath}\"" else "null"},
              "size": ${page.size}
            }
        """.trimIndent()
        
        metadataFile.writeText(json)
    }
    
    /**
     * Load an offline page
     */
    fun loadOfflinePage(pageId: String): File? {
        val pageDir = getPageDirectory(pageId)
        val htmlFile = File(pageDir, "index.html")
        
        return if (htmlFile.exists() && htmlFile.isFile) {
            htmlFile
        } else {
            null
        }
    }
    
    /**
     * Get an offline resource
     */
    fun getOfflineResource(pageId: String, resourcePath: String): WebResourceResponse? {
        val cacheKey = "$pageId/$resourcePath"
        
        // Check cache first
        resourceCache[cacheKey]?.let { return it }
        
        try {
            val pageDir = getPageDirectory(pageId)
            val resourceFile = File(pageDir, resourcePath)
            
            if (!resourceFile.exists() || !resourceFile.isFile) {
                return null
            }
            
            val mimeType = getMimeTypeFromExtension(resourceFile.extension)
            val encoding = if (mimeType.startsWith("text/")) "UTF-8" else null
            val inputStream = resourceFile.inputStream()
            
            val response = WebResourceResponse(mimeType, encoding, inputStream)
            
            // Cache the response
            resourceCache[cacheKey] = response
            
            return response
        } catch (e: Exception) {
            Log.e(TAG, "Error loading offline resource: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Get mime type from file extension
     */
    private fun getMimeTypeFromExtension(extension: String): String {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase(Locale.US))
            ?: when (extension.lowercase()) {
                "css" -> "text/css"
                "js" -> "application/javascript"
                "html", "htm" -> "text/html"
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "svg" -> "image/svg+xml"
                "webp" -> "image/webp"
                "ico" -> "image/x-icon"
                "json" -> "application/json"
                "xml" -> "application/xml"
                "ttf" -> "application/x-font-ttf"
                "otf" -> "application/x-font-opentype"
                "woff" -> "application/font-woff"
                "woff2" -> "application/font-woff2"
                "eot" -> "application/vnd.ms-fontobject"
                else -> "application/octet-stream"
            }
    }
    
    /**
     * Get a list of all saved offline pages
     */
    suspend fun getAllOfflinePages(): List<OfflinePage> = withContext(Dispatchers.IO) {
        val pages = mutableListOf<OfflinePage>()
        val baseDir = getOfflineBaseDir()
        
        if (!baseDir.exists()) {
            return@withContext pages
        }
        
        baseDir.listFiles()?.forEach { pageDir ->
            if (pageDir.isDirectory) {
                val metadataFile = File(pageDir, "metadata.json")
                if (metadataFile.exists()) {
                    try {
                        val json = metadataFile.readText()
                        val page = parseMetadata(json)
                        pages.add(page)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing metadata: ${e.message}", e)
                    }
                }
            }
        }
        
        // Sort by timestamp (newest first)
        pages.sortByDescending { it.timestamp }
        
        pages
    }
    
    /**
     * Parse metadata JSON to an OfflinePage object
     */
    private fun parseMetadata(json: String): OfflinePage {
        // Simple JSON parsing to extract values
        val idPattern = """"id"\s*:\s*"(.*?)"""".toRegex()
        val urlPattern = """"url"\s*:\s*"(.*?)"""".toRegex()
        val titlePattern = """"title"\s*:\s*"(.*?)"""".toRegex()
        val timestampPattern = """"timestamp"\s*:\s*(\d+)""".toRegex()
        val filePathPattern = """"filePath"\s*:\s*"(.*?)"""".toRegex()
        val thumbnailPathPattern = """"thumbnailPath"\s*:\s*"(.*?)"""".toRegex()
        val sizePattern = """"size"\s*:\s*(\d+)""".toRegex()
        
        val id = idPattern.find(json)?.groupValues?.get(1) ?: ""
        val url = urlPattern.find(json)?.groupValues?.get(1) ?: ""
        val title = titlePattern.find(json)?.groupValues?.get(1)?.replace("\\\"", "\"") ?: ""
        val timestamp = timestampPattern.find(json)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val filePath = filePathPattern.find(json)?.groupValues?.get(1) ?: ""
        val thumbnailPathMatch = thumbnailPathPattern.find(json)
        val thumbnailPath = if (thumbnailPathMatch != null) thumbnailPathMatch.groupValues[1] else null
        val size = sizePattern.find(json)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        
        return OfflinePage(
            id = id,
            url = url,
            title = title,
            timestamp = timestamp,
            filePath = filePath,
            thumbnailPath = thumbnailPath,
            size = size
        )
    }
    
    /**
     * Delete an offline page
     */
    suspend fun deletePage(pageId: String): Boolean = 
        withErrorHandlingAndFallback(
            tag = TAG,
            errorMessage = "Error deleting page: $pageId",
            fallback = false
        ) {
            val pageDir = getPageDirectory(pageId)
            if (pageDir.exists() && pageDir.isDirectory) {
                // Delete all files recursively
                pageDir.deleteRecursively()
                
                // Clear any cached resources for this page
                val keysToRemove = resourceCache.keys.filter { it.startsWith("$pageId/") }
                keysToRemove.forEach { resourceCache.remove(it) }
                
                true
            } else {
                false
            }
        }
    
    /**
     * Clear the resource cache
     */
    fun clearCache() {
        resourceCache.clear()
    }
}