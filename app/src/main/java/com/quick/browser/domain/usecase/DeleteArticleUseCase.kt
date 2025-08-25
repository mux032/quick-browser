package com.quick.browser.domain.usecase

import com.quick.browser.domain.error.DomainError
import com.quick.browser.domain.model.SavedArticle
import com.quick.browser.domain.repository.ArticleRepository
import com.quick.browser.domain.result.Result
import javax.inject.Inject

/**
 * Use case for deleting a saved article
 *
 * @param articleRepository The repository to delete the article from
 */
class DeleteArticleUseCase @Inject constructor(
    private val articleRepository: ArticleRepository
) {
    /**
     * Delete a saved article
     *
     * @param article The article to delete
     * @return A Result indicating success or failure
     */
    suspend operator fun invoke(article: SavedArticle): Result<Unit, DomainError> {
        return try {
            articleRepository.deleteArticle(article)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DomainError.DatabaseError("Failed to delete article", e))
        }
    }
}