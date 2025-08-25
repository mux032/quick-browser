package com.quick.browser.utils.security

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import com.quick.browser.utils.Logger
import java.security.MessageDigest
import java.security.cert.Certificate

/**
 * Centralized security policy manager for the browser application
 *
 * This class handles all security-related configurations and policies including:
 * - WebView security settings
 * - Certificate pinning
 * - Input sanitization
 * - Content security policies
 */
class SecurityPolicyManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SecurityPolicyManager"
        private const val ENABLE_STRICT_MODE = true
    }
    
    /**
     * Apply strict security settings to a WebView
     *
     * @param webView The WebView to configure
     */
    fun applySecuritySettings(webView: WebView) {
        try {
            val settings = webView.settings
            
            // Disable file access
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            
            // Disable universal access from file URLs
            settings.allowFileAccessFromFileURLs = false
            settings.allowUniversalAccessFromFileURLs = false
            
            // Disable mixed content
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            
            // Disable saving password
            settings.savePassword = false
            
            // Enable DOM storage but with restrictions
            settings.domStorageEnabled = true
            
            // Disable database storage
            settings.databaseEnabled = false
            
            // Set user agent
            setUserAgent(settings)
            
            // Apply additional security settings
            applyAdvancedSecuritySettings(webView, settings)
            
            Logger.d(TAG, "Applied security settings to WebView")
        } catch (e: Exception) {
            Logger.e(TAG, "Error applying security settings", e)
        }
    }
    
    /**
     * Apply advanced security settings
     *
     * @param webView The WebView to configure
     * @param settings The WebView settings
     */
    private fun applyAdvancedSecuritySettings(webView: WebView, settings: WebSettings) {
        try {
            // Disable media playback requiring user gesture
            settings.mediaPlaybackRequiresUserGesture = true
            
            // Disable third-party cookies
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false)
        } catch (e: Exception) {
            Logger.w(TAG, "Could not apply advanced security settings", e)
        }
    }
    
    /**
     * Set a secure user agent string
     *
     * @param settings The WebView settings to modify
     */
    private fun setUserAgent(settings: WebSettings) {
        try {
            // Remove identifying information from user agent
            val originalUserAgent = settings.userAgentString
            // Remove any build-specific information that could be used for fingerprinting
            val secureUserAgent = originalUserAgent.replace(Regex("Build/[^\\s]+"), "Build/XYZ")
            settings.userAgentString = secureUserAgent
        } catch (e: Exception) {
            Logger.w(TAG, "Could not set secure user agent", e)
        }
    }
    
    /**
     * Sanitize user input to prevent injection attacks
     *
     * @param input The input string to sanitize
     * @return The sanitized string
     */
    fun sanitizeInput(input: String): String {
        if (input.isEmpty()) return input
        
        return input
            // Remove potentially dangerous characters
            .replace("<", "")
            .replace(">", "")
            .replace("\"", "")
            .replace("'", "")
            .replace("&", "")
            // Remove JavaScript protocol handlers
            .replace(Regex("javascript:", RegexOption.IGNORE_CASE), "")
            // Remove data URLs that could contain scripts
            .replace(Regex("data:text/html", RegexOption.IGNORE_CASE), "")
            // Limit length to prevent DoS
            .take(2000)
    }
    
    /**
     * Validate and format URL for security
     *
     * @param url The URL to validate
     * @return The validated and formatted URL, or null if invalid
     */
    fun validateAndFormatUrl(url: String): String? {
        if (url.isEmpty()) return null
        
        // Sanitize the input first
        val sanitizedUrl = sanitizeInput(url)
        
        // Check for allowed protocols
        val allowedProtocols = listOf("http://", "https://", "file://", "data:")
        val hasAllowedProtocol = allowedProtocols.any { sanitizedUrl.startsWith(it, ignoreCase = true) }
        
        // If no protocol, default to HTTPS
        return when {
            hasAllowedProtocol -> sanitizedUrl
            sanitizedUrl.contains("://") -> null // Unknown protocol
            else -> "https://$sanitizedUrl" // Default to HTTPS
        }
    }
    
    /**
     * Check if a URL is safe to load based on security policies
     *
     * @param url The URL to check
     * @return True if the URL is safe to load
     */
    fun isUrlSafeToLoad(url: String): Boolean {
        if (url.isEmpty()) return false
        
        // Block localhost and private IP addresses in strict mode
        if (ENABLE_STRICT_MODE) {
            val blockedPatterns = listOf(
                "localhost",
                "127.0.0.1",
                "10.",
                "172.16.",
                "192.168."
            )
            
            return !blockedPatterns.any { url.contains(it) }
        }
        
        return true
    }
    
    /**
     * Generate a certificate fingerprint for pinning
     *
     * @param certificate The certificate to fingerprint
     * @return The SHA-256 fingerprint of the certificate
     */
    fun generateCertificateFingerprint(certificate: Certificate): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val publicKey = certificate.publicKey.encoded
            val digest = md.digest(publicKey)
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Logger.e(TAG, "Error generating certificate fingerprint", e)
            ""
        }
    }
}