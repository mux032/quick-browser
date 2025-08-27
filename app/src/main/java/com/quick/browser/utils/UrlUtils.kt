package com.quick.browser.utils

import android.util.Patterns
import java.net.URI
import java.util.regex.Pattern

/**
 * Utility class for URL formatting, validation, and manipulation operations
 *
 * This object provides methods to format URLs, validate their structure,
 * extract components such as domains and paths, and manipulate URLs.
 */
object UrlUtils {
    
    /**
     * Format URL to ensure it has proper protocol
     *
     * This method adds a protocol prefix (https://) to URLs that don't have one,
     * unless they already have a recognized protocol or are empty.
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
     * This method uses Android's built-in URL pattern matcher for strict validation,
     * and falls back to a more lenient approach for URLs that might not strictly
     * match the pattern but are still valid.
     *
     * @param url The URL to validate
     * @return True if the URL is valid, false otherwise
     */
    fun isValidUrl(url: String): Boolean {
        // First try with Android's built-in pattern
        try {
            if (Patterns.WEB_URL.matcher(url).matches()) {
                return true
            }
        } catch (e: Exception) {
            // If we can't use Android's pattern matcher, fall back to our own validation
        }
        
        // If that fails, try a more lenient approach for URLs that might have special characters
        // or don't strictly match the pattern but are still valid URLs
        val lowerUrl = url.lowercase()
        return (lowerUrl.startsWith("http://") || 
               lowerUrl.startsWith("https://") || 
               lowerUrl.startsWith("www.") ||
               lowerUrl.contains("."))
    }
    
    /**
     * Extract domain from URL
     *
     * This method parses a URL and extracts the domain name, removing the
     * "www." prefix if present.
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
     * This method removes query parameters and fragments from a URL,
     * keeping only the scheme, host, port, and path components.
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
    
    /**
     * Extracts a URL from text that might contain other content
     */
    fun extractUrl(text: String): String? {
        // Check for Google App specific pattern first (search.app/*)
        val googleAppPattern = "(https?://)?search\\.app/\\S+"
        val googleAppMatcher = Pattern.compile(googleAppPattern).matcher(text)
        if (googleAppMatcher.find()) {
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
}

/**
 * Extension function to format URL
 *
 * @return The formatted URL with appropriate protocol
 */
fun String.formatUrl(): String = UrlUtils.formatUrl(this)

/**
 * Extension function to validate URL
 *
 * @return True if the URL is valid, false otherwise
 */
fun String.isValidUrl(): Boolean = UrlUtils.isValidUrl(this)

/**
 * Extension function to extract domain from URL
 *
 * @return The domain or empty string if invalid
 */
fun String.extractDomain(): String = UrlUtils.extractDomain(this)

/**
 * Extension function to check if URL is HTTPS
 *
 * @return True if URL is HTTPS
 */
fun String.isHttpsUrl(): Boolean = UrlUtils.isHttpsUrl(this)

/**
 * Extension function to get URL without query parameters
 *
 * @return URL without query parameters
 */
fun String.getUrlWithoutQuery(): String = UrlUtils.getUrlWithoutQuery(this)

/**
 * Extension function to extract URL from text
 *
 * @return The extracted URL or null if none found
 */
fun String.extractUrl(): String? = UrlUtils.extractUrl(this)