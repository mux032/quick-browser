package com.quick.browser.domain.model

import android.graphics.Bitmap

/**
 * Domain model for web pages
 *
 * @property url The URL of the web page
 * @property title The title of the web page
 * @property timestamp The timestamp when the page was visited
 * @property content The content of the web page, if available
 * @property isAvailableOffline Whether the page is available offline
 * @property visitCount The number of times the page has been visited
 * @property faviconUrl The URL of the favicon, if available
 * @property previewImageUrl The URL of the preview image, if available
 * @property favicon The favicon bitmap, if available
 * @property summary A list of summary points for the page
 * @property parentBubbleId The ID of the bubble that this page belongs to
 */
data class WebPage(
    val url: String,
    val title: String,
    val timestamp: Long,
    val content: String? = null,
    val isAvailableOffline: Boolean = false,
    var visitCount: Int = 1,
    val faviconUrl: String? = null,
    val previewImageUrl: String? = null,
    val favicon: Bitmap? = null,
    val summary: List<String> = emptyList(),
    val parentBubbleId: String? = null
) {
    /**
     * Copy transient fields from another WebPage instance
     *
     * @param webPage The WebPage to copy fields from
     * @return A new WebPage with the transient fields copied
     */
    fun copyTransientFields(webPage: WebPage): WebPage {
        return webPage.copy(
            summary = this.summary,
            parentBubbleId = this.parentBubbleId
        )
    }
}