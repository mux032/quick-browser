package com.quick.browser.domain.model

/**
 * Domain model for a history item
 */
sealed class HistoryItem {
    data class Header(val title: String) : HistoryItem()
    data class WebPageItem(val webPage: WebPage) : HistoryItem()
}