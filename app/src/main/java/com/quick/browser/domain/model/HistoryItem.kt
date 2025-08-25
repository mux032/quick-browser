package com.quick.browser.domain.model

/**
 * Domain model for a history item
 *
 * This sealed class represents different types of items that can appear in the history,
 * such as headers for date grouping and web page items.
 */
sealed class HistoryItem {
    /**
     * Represents a header item in the history, typically used for date grouping
     *
     * @property title The title of the header (e.g., "Today", "Yesterday", "Last Week")
     */
    data class Header(val title: String) : HistoryItem()

    /**
     * Represents a web page item in the history
     *
     * @property webPage The web page associated with this history item
     */
    data class WebPageItem(val webPage: WebPage) : HistoryItem()
}