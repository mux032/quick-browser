package com.quick.browser.util

import android.util.Patterns
import java.net.URI
import java.net.URISyntaxException

/**
 * Utility class for input validation operations
 */
object InputValidator {
    
    /**
     * Validate if a URL is valid
     *
     * @param url The URL to validate
     * @return True if the URL is valid, false otherwise
     */
    fun isValidUrl(url: String): Boolean {
        if (url.isEmpty()) return false
        
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
     * Validate if a string is a valid domain
     *
     * @param domain The domain to validate
     * @return True if the domain is valid, false otherwise
     */
    fun isValidDomain(domain: String): Boolean {
        if (domain.isEmpty()) return false
        
        return try {
            val uri = URI("http://$domain")
            uri.host != null
        } catch (e: URISyntaxException) {
            false
        }
    }
    
    /**
     * Validate if a string is not empty and doesn't contain dangerous characters
     *
     * @param input The input to validate
     * @param maxLength The maximum length allowed (default: 1000)
     * @return True if the input is valid, false otherwise
     */
    fun isValidText(input: String, maxLength: Int = 1000): Boolean {
        if (input.isEmpty() || input.length > maxLength) return false
        
        // Check for dangerous characters that could be used in XSS or injection attacks
        val dangerousChars = setOf('<', '>', '&', '"', '\'', '/', '\\')
        return !input.any { it in dangerousChars }
    }
    
    /**
     * Validate if a string is a valid bubble ID
     *
     * @param bubbleId The bubble ID to validate
     * @return True if the bubble ID is valid, false otherwise
     */
    fun isValidBubbleId(bubbleId: String): Boolean {
        return bubbleId.isNotEmpty() && bubbleId.length <= 50
    }
    
    /**
     * Sanitize user input to prevent XSS attacks
     *
     * @param input The input to sanitize
     * @return The sanitized input
     */
    fun sanitizeInput(input: String): String {
        return input
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;")
    }
}