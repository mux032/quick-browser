package com.quick.browser.presentation.ui.saved

import com.quick.browser.domain.model.SavedArticle
import com.quick.browser.domain.model.Tag

/**
 * UI state for the saved articles screen
 */
data class SavedArticlesUiState(
    val articles: List<SavedArticle> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentTag: Tag? = null
)