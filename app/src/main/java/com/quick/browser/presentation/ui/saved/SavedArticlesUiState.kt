package com.quick.browser.presentation.ui.saved

import com.quick.browser.domain.model.SavedArticle

/**
 * UI state for the saved articles screen
 */
data class SavedArticlesUiState(
    val articles: List<SavedArticle> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)