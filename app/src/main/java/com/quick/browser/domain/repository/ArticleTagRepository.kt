package com.quick.browser.domain.repository

import com.quick.browser.domain.model.SavedArticle
import com.quick.browser.domain.model.Tag
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing the relationship between articles and tags
 *
 * This interface defines the contract for article-tag relationship operations.
 * It provides methods for associating articles with tags and retrieving articles by tag.
 */
interface ArticleTagRepository {

    /**
     * Get all tags for a specific article
     *
     * @param articleUrl The URL of the article
     * @return A Flow of lists of tags associated with the article
     */
    fun getTagsForArticle(articleUrl: String): Flow<List<Tag>>

    /**
     * Get all articles for a specific tag
     *
     * @param tagId The ID of the tag
     * @return A Flow of lists of articles with the specified tag
     */
    fun getArticlesForTag(tagId: Long): Flow<List<SavedArticle>>

    /**
     * Associate an article with a tag
     *
     * @param articleUrl The URL of the article
     * @param tagId The ID of the tag
     * @return True if the association was created successfully, false otherwise
     */
    suspend fun addTagToArticle(articleUrl: String, tagId: Long): Boolean

    /**
     * Remove a tag from an article
     *
     * @param articleUrl The URL of the article
     * @param tagId The ID of the tag
     * @return True if the association was removed successfully, false otherwise
     */
    suspend fun removeTagFromArticle(articleUrl: String, tagId: Long): Boolean

    /**
     * Remove all tags from an article
     *
     * @param articleUrl The URL of the article
     * @return True if all associations were removed successfully, false otherwise
     */
    suspend fun removeAllTagsFromArticle(articleUrl: String): Boolean
}