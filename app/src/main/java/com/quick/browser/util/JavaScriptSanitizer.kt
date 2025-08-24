package com.quick.browser.util

/**
 * Utility class for sanitizing JavaScript code to prevent XSS and other injection attacks
 */
object JavaScriptSanitizer {
    
    private const val TAG = "JavaScriptSanitizer"
    
    // Patterns for potentially dangerous JavaScript constructs
    private val DANGEROUS_PATTERNS = listOf(
        "eval\\s*\\(",
        "setTimeout\\s*\\([^,]+,\\s*0\\s*\\)",
        "setInterval\\s*\\([^,]+,\\s*0\\s*\\)",
        "Function\\s*\\(",
        "document\\.write\\s*\\(",
        "document\\.writeln\\s*\\(",
        "innerHTML\\s*=",
        "outerHTML\\s*=",
        "insertAdjacentHTML\\s*\\(",
        "execCommand\\s*\\("
    )
    
    // Allowed global functions
    private val ALLOWED_GLOBALS = setOf(
        "console", "Math", "JSON", "Array", "Object", "String", "Number", 
        "Boolean", "Date", "RegExp", "Error", "encodeURIComponent", 
        "decodeURIComponent", "parseInt", "parseFloat"
    )
    
    /**
     * Sanitize JavaScript code by removing potentially dangerous constructs
     *
     * @param script The JavaScript code to sanitize
     * @return The sanitized JavaScript code
     */
    fun sanitizeJavaScript(script: String): String {
        var sanitized = script
        
        try {
            // Remove dangerous patterns
            for (pattern in DANGEROUS_PATTERNS) {
                sanitized = sanitized.replace(Regex(pattern, setOf(RegexOption.IGNORE_CASE)), "")
            }
            
            // Remove access to dangerous global properties
            sanitized = sanitized.replace(Regex("window\\.(?!(${ALLOWED_GLOBALS.joinToString("|")})\\b)[a-zA-Z_\\$][a-zA-Z0-9_\\$]*"), "")
            sanitized = sanitized.replace(Regex("document\\.(?!(${ALLOWED_GLOBALS.joinToString("|")})\\b)[a-zA-Z_\\$][a-zA-Z0-9_\\$]*"), "")
            
            // Remove dangerous DOM manipulation
            sanitized = sanitized.replace(Regex("\\.createElement\\s*\\([^)]*script[^)]*\\)", setOf(RegexOption.IGNORE_CASE)), "")
            
            // Remove data URLs that could contain scripts
            sanitized = sanitized.replace(Regex("data:text/javascript[^'\"]*", setOf(RegexOption.IGNORE_CASE)), "")
            
        } catch (e: Exception) {
            Logger.e(TAG, "Error sanitizing JavaScript", e)
            // In case of error, return empty string to prevent execution of potentially dangerous code
            sanitized = ""
        }
        
        return sanitized
    }
    
    /**
     * Validate that JavaScript code is safe to execute
     *
     * @param script The JavaScript code to validate
     * @return True if the script is considered safe
     */
    fun isJavaScriptSafe(script: String): Boolean {
        // Check for dangerous patterns
        for (pattern in DANGEROUS_PATTERNS) {
            if (Regex(pattern, setOf(RegexOption.IGNORE_CASE)).containsMatchIn(script)) {
                return false
            }
        }
        
        // Check for dangerous global access
        val windowAccessPattern = "window\\.(?!(${ALLOWED_GLOBALS.joinToString("|")})\\b)[a-zA-Z_\\$][a-zA-Z0-9_\\$]*"
        val documentAccessPattern = "document\\.(?!(${ALLOWED_GLOBALS.joinToString("|")})\\b)[a-zA-Z_\\$][a-zA-Z0-9_\\$]*"
        
        if (Regex(windowAccessPattern).containsMatchIn(script) || Regex(documentAccessPattern).containsMatchIn(script)) {
            return false
        }
        
        // Check for dangerous DOM manipulation
        val createElementPattern = "\\.createElement\\s*\\([^)]*script[^)]*\\)"
        if (Regex(createElementPattern, setOf(RegexOption.IGNORE_CASE)).containsMatchIn(script)) {
            return false
        }
        
        // Check for data URLs
        val dataUrlPattern = "data:text/javascript[^'\"]*"
        if (Regex(dataUrlPattern, setOf(RegexOption.IGNORE_CASE)).containsMatchIn(script)) {
            return false
        }
        
        return true
    }
    
    /**
     * Generate Content Security Policy header
     *
     * @return CSP header string
     */
    fun generateCSPHeader(): String {
        return "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: https:; " +
                "font-src 'self' data:; " +
                "connect-src 'self'; " +
                "media-src 'self'; " +
                "object-src 'none'; " +
                "child-src 'none'; " +
                "frame-ancestors 'none'; " +
                "base-uri 'self'; " +
                "form-action 'self';"
    }
}