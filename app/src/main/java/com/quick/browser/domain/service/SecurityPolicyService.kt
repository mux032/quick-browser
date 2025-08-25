package com.quick.browser.domain.service

import android.content.Context
import android.webkit.WebView
import com.quick.browser.data.security.SecurityPolicyManager
import java.security.cert.Certificate

/**
 * Service for handling security policy operations
 * This service provides a clean interface for security-related operations
 */
class SecurityPolicyService(context: Context) {
    
    private val securityPolicyManager = SecurityPolicyManager(context)
    
    /**
     * Apply strict security settings to a WebView
     *
     * @param webView The WebView to configure
     */
    fun applySecuritySettings(webView: WebView) {
        securityPolicyManager.applySecuritySettings(webView)
    }
    
    /**
     * Sanitize user input to prevent injection attacks
     *
     * @param input The input string to sanitize
     * @return The sanitized string
     */
    fun sanitizeInput(input: String): String {
        return securityPolicyManager.sanitizeInput(input)
    }
    
    /**
     * Validate and format URL for security
     *
     * @param url The URL to validate
     * @return The validated and formatted URL, or null if invalid
     */
    fun validateAndFormatUrl(url: String): String? {
        return securityPolicyManager.validateAndFormatUrl(url)
    }
    
    /**
     * Check if a URL is safe to load based on security policies
     *
     * @param url The URL to check
     * @return True if the URL is safe to load
     */
    fun isUrlSafeToLoad(url: String): Boolean {
        return securityPolicyManager.isUrlSafeToLoad(url)
    }
    
    /**
     * Generate a certificate fingerprint for pinning
     *
     * @param certificate The certificate to fingerprint
     * @return The SHA-256 fingerprint of the certificate
     */
    fun generateCertificateFingerprint(certificate: Certificate): String {
        return securityPolicyManager.generateCertificateFingerprint(certificate)
    }
}