package com.quick.browser.domain.usecase

import com.quick.browser.domain.model.SavedArticle
import com.quick.browser.domain.repository.ArticleRepository
import javax.inject.Inject

/**
 * Use case for saving an article
 */
class SaveArticleUseCase @Inject constructor(
    private val articleRepository: ArticleRepository
) {
    suspend operator fun invoke(article: SavedArticle) {
        articleRepository.saveArticle(article)
    }
}