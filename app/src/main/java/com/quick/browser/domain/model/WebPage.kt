package com.quick.browser.domain.model

import android.graphics.Bitmap

/**
 * Domain model for web pages
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
    fun copyTransientFields(webPage: WebPage): WebPage {
        return webPage.copy(
            summary = this.summary,
            parentBubbleId = this.parentBubbleId
        )
    }
}