package com.quick.browser.data.repository

import com.quick.browser.data.local.dao.ArticleTagDao
import com.quick.browser.data.local.dao.SavedArticleDao
import com.quick.browser.data.local.dao.TagDao
import com.quick.browser.domain.model.SavedArticle
import com.quick.browser.domain.model.Tag
import com.quick.browser.domain.repository.ArticleTagRepository
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
            tagIds.mapNotNull { tagId ->
                val tagEntity = tagDao.getTagById(tagId)
                tagEntity?.let { entityToDomain(it) }
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
            list.map { entityToDomain(it) }
        }
    }

    /**
     * Associate an article with a tag
     *
     * @param articleUrl The URL of the article
     * @param tagId The ID of the tag
     * @return True if the association was created successfully, false otherwise
     */
    override suspend fun addTagToArticle(articleUrl: String, tagId: Long): Boolean {
        return try {
            val articleTagEntity = com.quick.browser.data.local.entity.ArticleTag(
                articleUrl = articleUrl,
                tagId = tagId
            )
            articleTagDao.insertArticleTag(articleTagEntity)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Remove a tag from an article
     *
     * @param articleUrl The URL of the article
     * @param tagId The ID of the tag
     * @return True if the association was removed successfully, false otherwise
     */
    override suspend fun removeTagFromArticle(articleUrl: String, tagId: Long): Boolean {
        return try {
            val articleTagEntity = com.quick.browser.data.local.entity.ArticleTag(
                articleUrl = articleUrl,
                tagId = tagId
            )
            articleTagDao.deleteArticleTag(articleTagEntity)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Remove all tags from an article
     *
     * @param articleUrl The URL of the article
     * @return True if all associations were removed successfully, false otherwise
     */
    override suspend fun removeAllTagsFromArticle(articleUrl: String): Boolean {
        return try {
            articleTagDao.deleteArticleTagsForArticle(articleUrl)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Convert a saved article entity to a domain model
     *
     * @param entity The entity to convert
     * @return The domain model representation of the saved article
     */
    private fun entityToDomain(entity: com.quick.browser.data.local.entity.SavedArticle): SavedArticle {
        return SavedArticle(
            url = entity.url,
            title = entity.title,
            content = entity.content,
            author = entity.byline,
            siteName = entity.siteName,
            publishDate = entity.publishDate,
            savedDate = entity.savedDate,
            excerpt = entity.excerpt
        )
    }

    /**
     * Convert a tag entity to a domain model
     *
     * @param entity The entity to convert
     * @return The domain model representation of the tag
     */
    private fun entityToDomain(entity: com.quick.browser.data.local.entity.Tag): Tag {
        return Tag(
            id = entity.id,
            name = entity.name,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}