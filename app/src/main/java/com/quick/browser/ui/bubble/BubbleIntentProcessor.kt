package com.quick.browser.ui.bubble

import android.content.Context
import android.content.Intent
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LifecycleCoroutineScope
import com.quick.browser.Constants
import com.quick.browser.data.WebPageDao
import com.quick.browser.manager.AuthenticationHandler
import com.quick.browser.manager.BubbleManager
import com.quick.browser.model.WebPage
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.regex.Pattern

/**
 * BubbleIntentProcessor handles the processing of intents received by the BubbleService. It
 * extracts URLs from various types of sharing intents and processes them accordingly.
 */
class BubbleIntentProcessor(
        private val context: Context,
        private val bubbleManager: BubbleManager,
        private val webPageDao: WebPageDao,
        private val lifecycleScope: LifecycleCoroutineScope
) {
    companion object {
        private const val TAG = "BubbleIntentProcessor"
    }
    
    fun processIntent(intent: Intent) {
        runCatching {
            Log.d(TAG, "processIntent | Received intent: ${intent.action}, data: ${intent.extras}")
            when (intent.action) {
                Constants.ACTION_CREATE_BUBBLE -> handleCreateBubble(intent)
                Constants.ACTION_OPEN_URL -> handleOpenUrl(intent)
                Constants.ACTION_CLOSE_BUBBLE -> handleCloseBubble(intent)
                Constants.ACTION_TOGGLE_BUBBLES -> handleToggleBubbles()
                Constants.ACTION_ACTIVATE_BUBBLE -> handleActivateBubble(intent)
                Intent.ACTION_SEND -> handleSharedContent(intent)
                Intent.ACTION_VIEW -> handleViewAction(intent)
                else -> Log.w(TAG, "Unsupported intent action: ${intent.action}")
            }
        }.onFailure { 
            Log.e(TAG, "Error processing intent: ${it.message}")
        }
    }

    private fun handleCreateBubble(intent: Intent) {
        val sharedUrl = intent.getStringExtra(Constants.EXTRA_URL)
        val url = sharedUrl?.let { text -> extractUrl(text) ?: text}
        Log.e(TAG, "handleCreateBubble | Received intent: ${intent.action}, sharedURL: $url")
        
        if (url != null && isValidUrl(url)) {
            Log.e(TAG, "Creating bubble with URL: $url")
            
            // Check if this is an authentication URL that should be handled with Custom Tabs
            if (AuthenticationHandler.isAuthenticationUrl(url)) {
                Log.d(TAG, "Authentication URL detected, opening in Custom Tab: $url")
                // Generate a bubble ID for this URL to track it when returning from authentication
                val authBubbleId = java.util.UUID.randomUUID().toString()
                AuthenticationHandler.openInCustomTab(context, url, authBubbleId)
                return
            }
            
            // Save to history
            saveToHistory(url)
            
            // Create a new bubble with the shared URL
            bubbleManager.createOrUpdateBubbleWithNewUrl(url, null)
        } else {
            Log.e(TAG, "No valid URL provided.")
        }
    }
    
    /**
     * Saves a URL to the browsing history
     */
    private fun saveToHistory(url: String) {
        lifecycleScope.launch {
            try {
                // Check if the page already exists in history
                val existingPage = webPageDao.getPageByUrl(url)
                
                if (existingPage != null) {
                    // Update existing page timestamp and increment visit count
                    val updatedPage = existingPage.copy(
                        timestamp = System.currentTimeMillis(),
                        visitCount = existingPage.visitCount + 1
                    )
                    webPageDao.updatePage(updatedPage)
                    webPageDao.incrementVisitCount(url)
                    Log.d(TAG, "Updated existing page in history: $url")
                } else {
                    // Try to get the title and images from HTML asynchronously
                    var title: String? = null
                    var previewImageUrl: String? = null
                    var faviconUrl: String? = null

                    try {
                        Log.d(TAG, "Attempting to fetch document for URL: $url")
                        val document = fetchDocument(url) // Helper to fetch Jsoup document
                        if (document != null) {
                            Log.d(TAG, "Successfully fetched document for URL: $url")
                            title = extractTitleFromDocument(document)
                            previewImageUrl = extractPreviewImageUrlFromDocument(document)
                            faviconUrl = extractFaviconUrlFromDocument(document, url)
                            Log.d(TAG, "Extracted data - Title: $title, Preview Image: $previewImageUrl, Favicon: $faviconUrl")
                        } else {
                            Log.d(TAG, "Document fetch returned null for URL: $url")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error extracting data from HTML for $url", e)
                    }

                    // Fallback title if HTML extraction fails
                    val finalTitle = title ?: extractTitleFromUrl(url)

                    val newPage = WebPage(
                        url = url,
                        title = finalTitle,
                        timestamp = System.currentTimeMillis(),
                        visitCount = 1,
                        faviconUrl = faviconUrl,
                        previewImageUrl = previewImageUrl
                    )
                    webPageDao.insertPage(newPage)
                    Log.d(TAG, "Added new page to history with title '$finalTitle', preview URL: $previewImageUrl, favicon URL: $faviconUrl: $url")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving to history for $url", e)
            }
        }
    }
    
    /**
     * Process a URL if it's valid, handling common operations
     * @param url The URL to process
     * @param handler The function to call with the valid URL
     */
    private fun processValidUrl(url: String?, handler: (String) -> Unit) {
        if (url != null && isValidUrl(url)) {
            // Check if this is an authentication URL that should be handled with Custom Tabs
            if (AuthenticationHandler.isAuthenticationUrl(url)) {
                Log.d(TAG, "Authentication URL detected, opening in Custom Tab: $url")
                // Generate a bubble ID for this URL to track it when returning from authentication
                val authBubbleId = java.util.UUID.randomUUID().toString()
                AuthenticationHandler.openInCustomTab(context, url, authBubbleId)
                return
            }
            
            saveToHistory(url)
            handler(url)
        } else {
            Log.w(TAG, "Invalid or missing URL for handleSharedContent")
        }
    }
    
    private fun handleOpenUrl(intent: Intent) {
        val url = intent.getStringExtra(Constants.EXTRA_URL)
        val bubbleId = intent.getStringExtra(Constants.EXTRA_BUBBLE_ID)
        
        if (url != null && isValidUrl(url)) {
            // Check if this is an authentication URL that should be handled with Custom Tabs
            if (AuthenticationHandler.isAuthenticationUrl(url)) {
                Log.d(TAG, "Authentication URL detected, opening in Custom Tab: $url")
                // Use the provided bubble ID or generate a new one
                val authBubbleId = bubbleId ?: java.util.UUID.randomUUID().toString()
                AuthenticationHandler.openInCustomTab(context, url, authBubbleId)
                return
            }
            
            // Save to history
            saveToHistory(url)
            
            bubbleManager.createOrUpdateBubbleWithNewUrl(url, bubbleId)
        } else {
            Log.w(TAG, "Invalid or missing URL for handleOpenUrl. URL: $url")
        }
    }

    private fun handleCloseBubble(intent: Intent) {
        val bubbleId = intent.getStringExtra(Constants.EXTRA_BUBBLE_ID)
        if (bubbleId != null) {
            bubbleManager.removeBubble(bubbleId)
        }
        Log.d(TAG, "Bubble closed via intent")
    }

    private fun handleActivateBubble(intent: Intent) {
        val bubbleId = intent.getStringExtra(Constants.EXTRA_BUBBLE_ID)
        if (bubbleId != null) {
            val currentBubbles = bubbleManager.bubbles.value
            val bubble = currentBubbles[bubbleId]
            
            if (bubble != null) {
                // The BubbleView now handles expansion directly
                // Just log that the bubble was activated
                Log.d(TAG, "Bubble activated with ID: $bubbleId, URL: ${bubble.url}")
            } else {
                Log.w(TAG, "No bubble found to activate with ID: $bubbleId")
            }
        } else {
            Log.w(TAG, "No bubble ID provided in handleActivateBubble")
        }
    }

    private fun handleToggleBubbles() {
        // Optional: implement show/hide behavior if needed. Here's a basic toggle logic idea.
        val bubbles = bubbleManager.bubbles.value
        if (bubbles.isNotEmpty()) {
            // We now have multiple bubbles, so we'll just log this action
            // The main bubble will show all bubbles in its expanded state
            Log.d(TAG, "Toggle bubbles requested with ${bubbles.size} bubbles")
        } else {
            Log.d(TAG, "Toggle bubbles requested, but no bubbles exist.")
        }
    }

    /**
     * Handles the ACTION_SEND intent to create or update a bubble with a new URL.
     * @param intent The incoming intent containing the shared content.
     */
    private fun handleSharedContent(intent: Intent) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        Log.d(TAG, "handleSharedContent | Received shared text: $sharedText")
        
        // Extract URL from shared text if possible
        val url = sharedText?.let { text ->
            extractUrl(text) ?: text
        }
        Log.d(TAG, "handleSharedContent | Received url: $url")
        processValidUrl(url) { validUrl ->
            bubbleManager.createOrUpdateBubbleWithNewUrl(validUrl, null)
        }
    }
    
    /**
     * Extracts a URL from text that might contain other content
     */
    private fun extractUrl(text: String): String? {
        // Check for Google App specific pattern first (search.app/*)
        val googleAppPattern = "(https?://)?search\\.app/\\S+"
        val googleAppMatcher = Pattern.compile(googleAppPattern).matcher(text)
        if (googleAppMatcher.find()) {
            Log.d(TAG, "Found Google App URL: ${googleAppMatcher.group()}")
            return googleAppMatcher.group()
        }
        
        // Standard URL extraction using regex
        val urlPattern = "(https?://(?:www\\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.\\S{2,}|www\\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.\\S{2,}|https?://(?:www\\.|(?!www))[a-zA-Z0-9]+\\.\\S{2,}|www\\.[a-zA-Z0-9]+\\.\\S{2,})"
        val pattern = Pattern.compile(urlPattern)
        val matcher = pattern.matcher(text)
        
        return if (matcher.find()) {
            matcher.group()
        } else {
            null
        }
    }

    /**
     * Handles the ACTION_VIEW intent to create or update a bubble with a new URL.
     * @param intent The incoming intent containing the URL.
     */
    private fun handleViewAction(intent: Intent) {
        val data = intent.data
        if (data != null && isValidUrl(data.toString())) {
            val url = data.toString()
            
            // Check if this is an authentication URL that should be handled with Custom Tabs
            if (AuthenticationHandler.isAuthenticationUrl(url)) {
                Log.d(TAG, "Authentication URL detected, opening in Custom Tab: $url")
                // Generate a bubble ID for this URL to track it when returning from authentication
                val authBubbleId = java.util.UUID.randomUUID().toString()
                AuthenticationHandler.openInCustomTab(context, url, authBubbleId)
                return
            }
            
            // Save to history
            saveToHistory(url)
            
            bubbleManager.createOrUpdateBubbleWithNewUrl(url, null)
        }
    }

    /**
     * Validates the URL format using Android's built-in Patterns class.
     * @param url The URL string to validate.
     * @return True if the URL is valid, false otherwise.
     */
    private fun isValidUrl(url: String): Boolean {
        // First try with Android's built-in pattern
        if (Patterns.WEB_URL.matcher(url).matches()) {
            return true
        }
        
        // If that fails, try a more lenient approach for URLs that might have special characters
        // or don't strictly match the pattern but are still valid URLs
        val lowerUrl = url.lowercase()
        return lowerUrl.startsWith("http://") || 
               lowerUrl.startsWith("https://") || 
               lowerUrl.startsWith("www.") ||
               lowerUrl.contains(".")
    }
    
    /**
     * Extracts a user-friendly title from a URL.
     * @param url The URL to extract a title from.
     * @return A user-friendly title based on the URL.
     */
    private fun extractTitleFromUrl(url: String): String {
        try {
            // First try to fetch the actual title from the HTML (synchronously)
            val htmlTitle = try {
                // This is a blocking call, but it's okay for this context
                // In a real app, you might want to use a more sophisticated approach
                val connection = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(3000) // Short timeout to avoid blocking too long
                    .followRedirects(true)
                val document = connection.get()
                document.title()
            } catch (e: Exception) {
                Log.d(TAG, "Failed to fetch HTML title, falling back to URL parsing: ${e.message}")
                null
            }
            
            // If we successfully got a title from HTML, use it
            if (!htmlTitle.isNullOrBlank()) {
                Log.d(TAG, "Using HTML title: $htmlTitle")
                return htmlTitle
            }
            
            // Otherwise fall back to extracting from URL
            Log.d(TAG, "Falling back to URL-based title extraction")
            
            // Remove protocol (http://, https://, etc.)
            var title = url.replace(Regex("^(https?://|www\\.)"), "")
            
            // Remove trailing slashes
            title = title.replace(Regex("/+$"), "")
            
            // Remove query parameters and fragments
            title = title.split("?")[0].split("#")[0]
            
            // Split by path segments and take the domain
            val parts = title.split("/")
            if (parts.isNotEmpty()) {
                // Use domain as base
                title = parts[0]
                
                // If there's a specific page path, add it for context
                if (parts.size > 1 && parts.last().isNotEmpty()) {
                    // Replace hyphens and underscores with spaces
                    val pageName = parts.last()
                        .replace("-", " ")
                        .replace("_", " ")
                        .split(".")
                        .first() // Remove file extension if present
                    
                    // Capitalize first letter of each word
                    val formattedPageName = pageName.split(" ")
                        .filter { it.isNotEmpty() }
                        .joinToString(" ") { word ->
                            word.replaceFirstChar { 
                                if (it.isLowerCase()) it.titlecase() else it.toString() 
                            }
                        }
                    
                    if (formattedPageName.isNotEmpty()) {
                        title = "$formattedPageName - $title"
                    }
                }
            }
            
            return title
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting title from URL: $url", e)
            return "Web Page" // Fallback title
        }
    }

    private suspend fun fetchDocument(url: String): Document? {
        return try {
            Log.d(TAG, "Fetching document for URL: $url")
            val document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(5000) // 5 seconds
                .followRedirects(true)
                .get()
            Log.d(TAG, "Successfully fetched document for URL: $url, title: ${document.title()}")
            document
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching document for $url: ${e.message}", e)
            null
        }
    }

    private fun extractTitleFromDocument(document: Document): String? {
        var title = document.title()
        if (title.isNotBlank()) return title

        title = document.select("meta[itemprop=name]").firstOrNull()?.attr("content") ?: ""
        if (title.isNotBlank()) return title

        title = document.select("meta[property=og:title]").firstOrNull()?.attr("content") ?: ""
        if (title.isNotBlank()) return title

        title = document.select("meta[name=twitter:title]").firstOrNull()?.attr("content") ?: ""
        if (title.isNotBlank()) return title

        title = document.select("h1").firstOrNull()?.text() ?: ""
        if (title.isNotBlank()) return title

        return null
    }

    private fun extractPreviewImageUrlFromDocument(document: Document): String? {
        Log.d(TAG, "Extracting preview image URL from document")
        
        // 1. Try OpenGraph image
        document.select("meta[property=og:image]").firstOrNull()?.absUrl("content")?.let {
            if (it.isNotBlank()) {
                Log.d(TAG, "Found OpenGraph image: $it")
                return it
            }
        }

        // 2. Try Twitter card image
        document.select("meta[name=twitter:image]").firstOrNull()?.absUrl("content")?.let {
            if (it.isNotBlank()) {
                Log.d(TAG, "Found Twitter card image: $it")
                return it
            }
        }

        // 3. Try <link rel="image_src">
        document.select("link[rel=image_src]").firstOrNull()?.absUrl("href")?.let {
            if (it.isNotBlank()) {
                Log.d(TAG, "Found image_src link: $it")
                return it
            }
        }

        // 4. Fallback: First large <img> element on page (with size filtering)
        document.select("img").firstOrNull { element ->
            val src = element.absUrl("src")
            src.isNotBlank() && isImageUrlValid(src) && isImageLargeEnough(element)
        }?.absUrl("src")?.let {
            Log.d(TAG, "Found large image element: $it")
            return it
        }

        // 5. Final fallback: favicon
        document.select("link[rel~=(?i)icon]").firstOrNull()?.absUrl("href")?.let {
            if (it.isNotBlank()) {
                Log.d(TAG, "Found favicon as fallback: $it")
                return it
            }
        }

        Log.d(TAG, "No preview image found in document")
        return null
    }

    private fun isImageUrlValid(url: String): Boolean {
        return url.startsWith("http") && !url.contains("data:image") && !url.contains("base64")
    }

    private fun isImageLargeEnough(element: org.jsoup.nodes.Element): Boolean {
        try {
            val width = element.attr("width").toIntOrNull() ?: 0
            val height = element.attr("height").toIntOrNull() ?: 0
            
            // Check if width and height attributes exist and are large enough
            if (width > 100 && height > 100) {
                return true
            }
            
            // If no width/height attributes, we'll assume it's large enough
            // In a production app, you might want to actually download the image to check dimensions
            return !element.attr("width").isBlank() || !element.attr("height").isBlank()
        } catch (e: Exception) {
            Log.d(TAG, "Error checking image size: ${e.message}")
            return true // Assume it's okay if we can't determine size
        }
    }

    private fun extractFaviconUrlFromDocument(document: Document, baseUrl: String): String? {
        // 1. Try Apple touch icon
        document.select("link[rel=apple-touch-icon]").firstOrNull()?.absUrl("href")?.let {
            if (it.isNotBlank()) return it
        }

        // 2. Try generic favicons (shortcut icon, icon, etc.)
        document.select("link[rel~=(?i)^(shortcut icon|icon|shortcut)$]").firstOrNull {
            it.attr("href").isNotBlank()
        }?.absUrl("href")?.let {
            if (it.isNotBlank()) return it
        }

        // 3. Fallback to default /favicon.ico
        return try {
            val uri = java.net.URI(baseUrl)
            val scheme = uri.scheme ?: "https"
            val domain = uri.host ?: return null
            "$scheme://$domain/favicon.ico"
        } catch (e: Exception) {
            Log.w(TAG, "Could not construct default favicon URL from $baseUrl", e)
            null
        }
    }

    /**
     * Asynchronously extracts the title from a webpage's HTML.
     * This method should be called from a coroutine context.
     *
     * @param url The URL to extract the title from
     * @return The title from the HTML or null if it couldn't be extracted
     */
    @Deprecated("Use fetchDocument and extractTitleFromDocument instead")
    suspend fun extractTitleFromHtmlAsync(url: String): String? {
        return try {
            Log.d(TAG, "Fetching HTML title for URL: $url")
            val document = fetchDocument(url)
            document?.let { extractTitleFromDocument(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting title from HTML: ${e.message}", e)
            null
        }
    }
}