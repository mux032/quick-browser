package com.quick.browser.data.local.dao

import androidx.room.*
import com.quick.browser.data.local.entity.ArticleTag
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for ArticleTag entities
 *
 * This interface provides methods for accessing and modifying the relationship
 * between articles and tags in the database.
 */
@Dao
interface ArticleTagDao {

    /**
     * Get all tags for a specific article
     *
     * @param articleUrl The URL of the article
     * @return A Flow of lists of tag IDs associated with the article
     */
    @Query("SELECT tagId FROM article_tags WHERE articleUrl = :articleUrl")
    fun getTagIdsForArticle(articleUrl: String): Flow<List<Long>>

    /**
     * Get all articles for a specific tag
     *
     * @param tagId The ID of the tag
     * @return A Flow of lists of article URLs associated with the tag
     */
    @Query("SELECT articleUrl FROM article_tags WHERE tagId = :tagId")
    fun getArticleUrlsForTag(tagId: Long): Flow<List<String>>

    /**
     * Insert a new article-tag relationship
     *
     * @param articleTag The article-tag relationship to insert
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticleTag(articleTag: ArticleTag)

    /**
     * Delete an article-tag relationship
     *
     * @param articleTag The article-tag relationship to delete
     */
    @Delete
    suspend fun deleteArticleTag(articleTag: ArticleTag)

    /**
     * Delete all article-tag relationships for a specific article
     *
     * @param articleUrl The URL of the article
     */
    @Query("DELETE FROM article_tags WHERE articleUrl = :articleUrl")
    suspend fun deleteArticleTagsForArticle(articleUrl: String)

    /**
     * Delete all article-tag relationships for a specific tag
     *
     * @param tagId The ID of the tag
     */
    @Query("DELETE FROM article_tags WHERE tagId = :tagId")
    suspend fun deleteArticleTagsForTag(tagId: Long)
}