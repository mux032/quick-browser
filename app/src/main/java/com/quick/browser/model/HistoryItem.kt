package com.quick.browser.model

/**
 * Represents different types of items in the history list
 */
sealed class HistoryItem {
    data class Header(val title: String) : HistoryItem()
    data class WebPageItem(val webPage: WebPage) : HistoryItem()
}