package com.quick.browser.domain.usecase

import com.quick.browser.domain.model.SavedArticle
import com.quick.browser.domain.repository.ArticleRepository
import javax.inject.Inject

/**
 * Use case for deleting a saved article
 */
class DeleteArticleUseCase @Inject constructor(
    private val articleRepository: ArticleRepository
) {
    suspend operator fun invoke(article: SavedArticle) {
        articleRepository.deleteArticle(article)
    }
}