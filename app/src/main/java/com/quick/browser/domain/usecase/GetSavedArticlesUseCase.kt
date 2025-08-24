package com.quick.browser.domain.usecase

import com.quick.browser.domain.model.SavedArticle
import com.quick.browser.domain.repository.ArticleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting all saved articles
 */
class GetSavedArticlesUseCase @Inject constructor(
    private val articleRepository: ArticleRepository
) {
    operator fun invoke(): Flow<List<SavedArticle>> {
        return articleRepository.getAllSavedArticles()
    }
}