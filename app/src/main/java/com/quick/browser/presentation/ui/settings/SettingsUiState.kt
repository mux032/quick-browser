package com.quick.browser.presentation.ui.settings

import com.quick.browser.domain.model.Settings

/**
 * UI state for the settings screen
 */
data class SettingsUiState(
    val settings: Settings? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)