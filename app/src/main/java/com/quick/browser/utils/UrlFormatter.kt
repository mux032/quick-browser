package com.quick.browser.utils

import android.util.Patterns
import java.net.URI

/**
 * Utility class for URL formatting and validation operations
 */
object UrlFormatter {
    
    /**
     * Format URL to ensure it has proper protocol
     *
     * @param url The URL to format
     * @return The formatted URL with appropriate protocol
     */
    fun formatUrl(url: String): String {
        return when {
            url.startsWith("http://", ignoreCase = true) || 
            url.startsWith("https://", ignoreCase = true) -> url
            url.startsWith("file://", ignoreCase = true) -> url
            url.startsWith("data:", ignoreCase = true) -> url
            url.startsWith("javascript:", ignoreCase = true) -> url
            url.startsWith("about:", ignoreCase = true) -> url
            url.contains("://") -> url // Already has a protocol
            url.isEmpty() -> ""
            else -> "https://$url" // Default to HTTPS
        }
    }
    
    /**
     * Validate if a URL is valid
     *
     * @param url The URL to validate
     * @return True if the URL is valid, false otherwise
     */
    fun isValidUrl(url: String): Boolean {
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
     * Extract domain from URL
     *
     * @param url The URL to extract domain from
     * @return The domain or empty string if invalid
     */
    fun extractDomain(url: String): String {
        return try {
            val uri = URI(url)
            val host = uri.host ?: return ""
            if (host.startsWith("www.")) host.substring(4) else host
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Check if URL is HTTPS
     *
     * @param url The URL to check
     * @return True if URL is HTTPS
     */
    fun isHttpsUrl(url: String): Boolean {
        return url.startsWith("https://", ignoreCase = true)
    }
    
    /**
     * Get URL without query parameters
     *
     * @param url The URL to process
     * @return URL without query parameters
     */
    fun getUrlWithoutQuery(url: String): String {
        return try {
            val uri = URI(url)
            val scheme = uri.scheme
            val host = uri.host
            val port = uri.port
            val path = uri.path
            
            val portPart = if (port != -1) ":$port" else ""
            val pathPart = path ?: ""
            
            "$scheme://$host$portPart$pathPart"
        } catch (e: Exception) {
            url
        }
    }
}

/**
 * Extension function to format URL
 */
fun String.formatUrl(): String = UrlFormatter.formatUrl(this)

/**
 * Extension function to validate URL
 */
fun String.isValidUrl(): Boolean = UrlFormatter.isValidUrl(this)

/**
 * Extension function to extract domain from URL
 */
fun String.extractDomain(): String = UrlFormatter.extractDomain(this)

/**
 * Extension function to check if URL is HTTPS
 */
fun String.isHttpsUrl(): Boolean = UrlFormatter.isHttpsUrl(this)

/**
 * Extension function to get URL without query parameters
 */
fun String.getUrlWithoutQuery(): String = UrlFormatter.getUrlWithoutQuery(this)