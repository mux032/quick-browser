package com.quick.browser.domain.usecase

import com.quick.browser.domain.error.DomainError
import com.quick.browser.domain.model.SavedArticle
import com.quick.browser.domain.repository.ArticleRepository
import com.quick.browser.domain.result.Result
import javax.inject.Inject

/**
 * Use case for saving an article
 *
 * @param articleRepository The repository to save the article to
 */
class SaveArticleUseCase @Inject constructor(
    private val articleRepository: ArticleRepository
) {
    /**
     * Save an article
     *
     * @param article The article to save
     * @return A Result indicating success or failure
     */
    suspend operator fun invoke(article: SavedArticle): Result<Unit, DomainError> {
        return try {
            articleRepository.saveArticle(article)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DomainError.DatabaseError("Failed to save article", e))
        }
    }
}