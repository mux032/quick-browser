package com.quick.browser.domain.usecase

import com.quick.browser.domain.model.SavedArticle
import com.quick.browser.domain.repository.ArticleTagRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting saved articles by tag ID
 */
class GetSavedArticlesByTagUseCase @Inject constructor(
    private val articleTagRepository: ArticleTagRepository
) {
    operator fun invoke(tagId: Long): Flow<List<SavedArticle>> {
        return articleTagRepository.getArticlesForTag(tagId)
    }
}