package com.quick.browser.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.quick.browser.domain.model.SavedArticle
import com.quick.browser.domain.repository.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for saved articles
 */
@HiltViewModel
class SavedArticlesViewModel @Inject constructor(
    private val repository: ArticleRepository
) : ViewModel() {
    
    val savedArticles = repository.getAllSavedArticles().asLiveData()
    
    fun deleteArticle(article: SavedArticle) {
        viewModelScope.launch {
            repository.deleteArticle(article)
        }
    }
}