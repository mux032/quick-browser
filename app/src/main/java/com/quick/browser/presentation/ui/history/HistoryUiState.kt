package com.quick.browser.presentation.ui.history

import com.quick.browser.domain.model.WebPage

/**
 * UI state for the history screen
 */
data class HistoryUiState(
    val webPages: List<WebPage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)