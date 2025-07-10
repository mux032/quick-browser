package com.qb.browser.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Manages downloading and caching of images for offline viewing
 */
class ImageDownloadManager private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: ImageDownloadManager? = null
        
        fun getInstance(context: Context): ImageDownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ImageDownloadManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        private const val TAG = "ImageDownloadManager"
        private const val CACHE_SIZE = 20 * 1024 * 1024 // 20MB memory cache
        private const val DISK_CACHE_DIR = "image_cache"
        private const val CONNECTION_TIMEOUT = 10000
        private const val READ_TIMEOUT = 15000
    }
    
    // Memory cache for quick access
    private val memoryCache: LruCache<String, Bitmap> = LruCache(CACHE_SIZE)
    
    // Disk cache directory
    private val diskCacheDir: File by lazy {
        File(context.cacheDir, DISK_CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Download and cache an image from URL
     */
    suspend fun downloadAndCacheImage(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val cacheKey = generateCacheKey(url)
            
            // Check memory cache first
            memoryCache.get(cacheKey)?.let { return@withContext it }
            
            // Check disk cache
            getCachedImageFromDisk(cacheKey)?.let { bitmap ->
                // Add to memory cache
                memoryCache.put(cacheKey, bitmap)
                return@withContext bitmap
            }
            
            // Download from network
            val bitmap = downloadImageFromNetwork(url)
            if (bitmap != null) {
                // Cache in memory
                memoryCache.put(cacheKey, bitmap)
                // Cache on disk
                saveImageToDisk(cacheKey, bitmap)
            }
            
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading image: $url", e)
            null
        }
    }
    
    /**
     * Download image from network
     */
    private suspend fun downloadImageFromNetwork(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.apply {
                connectTimeout = CONNECTION_TIMEOUT
                readTimeout = READ_TIMEOUT
                doInput = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
            }
            
            connection.connect()
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream: InputStream = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                connection.disconnect()
                bitmap
            } else {
                Log.w(TAG, "HTTP error ${connection.responseCode} for URL: $url")
                connection.disconnect()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error downloading image: $url", e)
            null
        }
    }
    
    /**
     * Get cached image from disk
     */
    private fun getCachedImageFromDisk(cacheKey: String): Bitmap? {
        return try {
            val cacheFile = File(diskCacheDir, cacheKey)
            if (cacheFile.exists()) {
                BitmapFactory.decodeFile(cacheFile.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading cached image", e)
            null
        }
    }
    
    /**
     * Save image to disk cache
     */
    private fun saveImageToDisk(cacheKey: String, bitmap: Bitmap) {
        try {
            val cacheFile = File(diskCacheDir, cacheKey)
            val outputStream = FileOutputStream(cacheFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image to disk", e)
        }
    }
    
    /**
     * Generate cache key from URL
     */
    private fun generateCacheKey(url: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(url.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Clear memory cache
     */
    fun clearMemoryCache() {
        memoryCache.evictAll()
    }
    
    /**
     * Clear disk cache
     */
    fun clearDiskCache() {
        try {
            diskCacheDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing disk cache", e)
        }
    }
    
    /**
     * Get cache size info
     */
    fun getCacheInfo(): Pair<Int, Long> {
        val memorySize = memoryCache.size()
        val diskSize = try {
            diskCacheDir.listFiles()?.sumOf { it.length() } ?: 0L
        } catch (e: Exception) {
            0L
        }
        return memorySize to diskSize
    }
}