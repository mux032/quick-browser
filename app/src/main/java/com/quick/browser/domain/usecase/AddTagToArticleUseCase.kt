package com.quick.browser.domain.usecase

import com.quick.browser.domain.repository.ArticleTagRepository
import javax.inject.Inject

/**
 * Use case for adding a tag to an article
 */
class AddTagToArticleUseCase @Inject constructor(
    private val articleTagRepository: ArticleTagRepository
) {
    suspend operator fun invoke(articleUrl: String, tagId: Long): Result<Boolean> {
        return try {
            val result = articleTagRepository.addTagToArticle(articleUrl, tagId)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}