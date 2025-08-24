package com.quick.browser.util

import android.util.Patterns

/**
 * Utility class for URL formatting and validation operations
 */
object UrlUtils {
    
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
            val uri = java.net.URI(url)
            val host = uri.host ?: return ""
            if (host.startsWith("www.")) host.substring(4) else host
        } catch (e: Exception) {
            ""
        }
    }
}