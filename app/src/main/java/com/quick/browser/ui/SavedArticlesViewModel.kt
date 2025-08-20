package com.quick.browser.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.quick.browser.data.SavedArticleRepository
import com.quick.browser.model.SavedArticle
import kotlinx.coroutines.launch

/**
 * ViewModel for saved articles
 */
class SavedArticlesViewModel(
    private val repository: SavedArticleRepository
) : ViewModel() {
    
    val savedArticles = repository.getAllSavedArticles().asLiveData()
    
    fun deleteArticle(article: SavedArticle) {
        viewModelScope.launch {
            repository.deleteSavedArticle(article)
        }
    }
}

/**
 * Factory for SavedArticlesViewModel
 */
class SavedArticlesViewModelFactory(
    private val repository: SavedArticleRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavedArticlesViewModel::class.java)) {
            return SavedArticlesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}