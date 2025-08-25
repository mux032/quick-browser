package com.quick.browser.presentation.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quick.browser.domain.model.SavedArticle
import com.quick.browser.domain.usecase.DeleteArticleUseCase
import com.quick.browser.domain.usecase.GetSavedArticlesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for saved articles
 */
@HiltViewModel
class SavedArticlesViewModel @Inject constructor(
    private val getSavedArticlesUseCase: GetSavedArticlesUseCase,
    private val deleteArticleUseCase: DeleteArticleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedArticlesUiState())
    val uiState: StateFlow<SavedArticlesUiState> = _uiState

    init {
        loadSavedArticles()
    }

    private fun loadSavedArticles() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                // Collect the flow of saved articles from the use case
                getSavedArticlesUseCase().collectLatest { articles ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        articles = articles
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load saved articles: ${e.message}"
                )
            }
        }
    }

    fun deleteArticle(article: SavedArticle) {
        viewModelScope.launch {
            when (val result = deleteArticleUseCase(article)) {
                is com.quick.browser.domain.result.Result.Success<*> -> {
                    // Update UI state after deletion
                    val currentArticles = _uiState.value.articles.toMutableList()
                    currentArticles.removeAll { it.url == article.url }
                    _uiState.value = _uiState.value.copy(articles = currentArticles)
                }
                is com.quick.browser.domain.result.Result.Failure<*> -> {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to delete article: ${result.error}"
                    )
                }
            }
        }
    }
}