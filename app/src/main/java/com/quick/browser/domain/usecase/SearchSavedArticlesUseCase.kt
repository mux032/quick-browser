package com.quick.browser.domain.usecase

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.quick.browser.domain.model.SavedArticle
import com.quick.browser.domain.repository.ArticleRepository
import javax.inject.Inject

/**
 * Use case for searching saved articles
 */
class SearchSavedArticlesUseCase @Inject constructor(
    private val articleRepository: ArticleRepository
) {
    operator fun invoke(query: String): LiveData<List<SavedArticle>> {
        return articleRepository.searchSavedArticles(query).asLiveData()
    }
}