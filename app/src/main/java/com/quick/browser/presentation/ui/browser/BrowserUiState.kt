package com.quick.browser.presentation.ui.browser

import com.quick.browser.domain.model.Bubble
import com.quick.browser.domain.model.Settings
import com.quick.browser.domain.model.WebPage

/**
 * UI state for the browser screen
 */
data class BrowserUiState(
    val bubbles: List<Bubble> = emptyList(),
    val webPages: Map<String, WebPage> = emptyMap(),
    val settings: Settings? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)