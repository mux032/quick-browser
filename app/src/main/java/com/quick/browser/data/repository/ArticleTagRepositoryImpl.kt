package com.quick.browser.data.repository

import com.quick.browser.data.local.dao.ArticleTagDao
import com.quick.browser.data.local.dao.SavedArticleDao
import com.quick.browser.data.local.dao.TagDao
import com.quick.browser.data.mapper.toDomain
import com.quick.browser.domain.model.SavedArticle
import com.quick.browser.domain.model.Tag
import com.quick.browser.domain.repository.ArticleTagRepository
import com.quick.browser.utils.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Repository implementation for managing the relationship between articles and tags
 *
 * This class implements the ArticleTagRepository interface and provides concrete
 * implementations for article-tag relationship operations.
 *
 * @param articleTagDao The DAO for accessing article-tag relationships in the database
 * @param savedArticleDao The DAO for accessing saved articles in the database
 * @param tagDao The DAO for accessing tags in the database
 */
class ArticleTagRepositoryImpl @Inject constructor(
    private val articleTagDao: ArticleTagDao,
    private val savedArticleDao: SavedArticleDao,
    private val tagDao: TagDao
) : ArticleTagRepository {

    /**
     * Get all tags for a specific article
     *
     * @param articleUrl The URL of the article
     * @return A Flow of lists of tags associated with the article
     */
    override fun getTagsForArticle(articleUrl: String): Flow<List<Tag>> {
        return articleTagDao.getTagIdsForArticle(articleUrl).map { tagIds ->
            if (tagIds.isEmpty()) {
                emptyList()
            } else {
                tagDao.getTagsByIds(tagIds).map { it.toDomain() }
            }
        }
    }

    /**
     * Get all articles for a specific tag
     *
     * @param tagId The ID of the tag
     * @return A Flow of lists of articles with the specified tag
     */
    override fun getArticlesForTag(tagId: Long): Flow<List<SavedArticle>> {
        return savedArticleDao.getSavedArticlesByTagId(tagId).map { list ->
            list.map { it.toDomain() }
        }
    }

    /**
     * Associate an article with a tag
     *
     * @param articleUrl The URL of the article
     * @param tagId The ID of the tag
     */
    override suspend fun addTagToArticle(articleUrl: String, tagId: Long) {
        try {
            val articleTagEntity = com.quick.browser.data.local.entity.ArticleTag(
                articleUrl = articleUrl,
                tagId = tagId
            )
            articleTagDao.insertArticleTag(articleTagEntity)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to add tag to article", e)
            throw e
        }
    }

    /**
     * Remove a tag from an article
     *
     * @param articleUrl The URL of the article
     * @param tagId The ID of the tag
     */
    override suspend fun removeTagFromArticle(articleUrl: String, tagId: Long) {
        try {
            val articleTagEntity = com.quick.browser.data.local.entity.ArticleTag(
                articleUrl = articleUrl,
                tagId = tagId
            )
            articleTagDao.deleteArticleTag(articleTagEntity)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to remove tag from article", e)
            throw e
        }
    }

    /**
     * Remove all tags from an article
     *
     * @param articleUrl The URL of the article
     */
    override suspend fun removeAllTagsFromArticle(articleUrl: String) {
        try {
            articleTagDao.deleteArticleTagsForArticle(articleUrl)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to remove all tags from article", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "ArticleTagRepositoryImpl"
    }
}