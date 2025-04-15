package com.qb.browser.model

/**
 * Data class representing an offline saved web page
 */
data class OfflinePage(
    /**
     * Unique identifier for the offline page
     */
    val id: String,
    
    /**
     * Original URL of the page
     */
    val url: String,
    
    /**
     * Page title
     */
    val title: String,
    
    /**
     * Timestamp when the page was saved (milliseconds since epoch)
     */
    val timestamp: Long,
    
    /**
     * Path to the saved HTML file
     */
    val filePath: String,
    
    /**
     * Path to the thumbnail image (optional)
     */
    val thumbnailPath: String? = null,
    
    /**
     * Size of the saved page in bytes
     */
    val size: Long = 0
)