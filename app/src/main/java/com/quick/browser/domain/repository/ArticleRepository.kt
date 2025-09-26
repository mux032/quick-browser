package com.quick.browser.domain.repository

import com.quick.browser.domain.model.SavedArticle
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing saved articles
 *
 * This interface defines the contract for accessing and modifying saved articles.
 * It provides methods to retrieve, save, and delete articles from a data source.
 */
interface ArticleRepository {
    /**
     * Get all saved articles as a flow
     *
     * @return A flow of lists of saved articles
     */
    fun getAllSavedArticles(): Flow<List<SavedArticle>>

    /**
     * Get saved articles by tag ID as a flow
     *
     * @param tagId The ID of the tag to retrieve articles from
     * @return A flow of lists of saved articles with the specified tag
     */
    fun getSavedArticlesByFolderId(tagId: Long): Flow<List<SavedArticle>>

    /**
     * Search saved articles by title or content
     *
     * @param query The search query
     * @return A flow of lists of saved articles matching the query
     */
    fun searchSavedArticles(query: String): Flow<List<SavedArticle>>

    /**
     * Get a saved article by its URL
     *
     * @param url The URL of the article to retrieve
     * @return The saved article or null if not found
     */
    suspend fun getSavedArticleByUrl(url: String): SavedArticle?

    /**
     * Save an article
     *
     * @param article The article to save
     */
    suspend fun saveArticle(article: SavedArticle)

    /**
     * Save an article by its URL
     *
     * @param url The URL of the article to save
     * @param tagId The ID of the tag to save the article to (0 for no tag)
     * @return True if the article was saved successfully, false otherwise
     */
    suspend fun saveArticleByUrl(url: String, tagId: Long = 0): Boolean

    /**
     * Save an original page as an article
     *
     * @param url The URL of the page
     * @param title The title of the page
     * @param content The content of the page
     * @return True if the article was saved successfully, false otherwise
     */
    suspend fun saveOriginalPageAsArticle(url: String, title: String, content: String): Boolean

    /**
     * Delete a saved article
     *
     * @param article The article to delete
     */
    suspend fun deleteArticle(article: SavedArticle)

    /**
     * Delete a saved article by its URL
     *
     * @param url The URL of the article to delete
     */
    suspend fun deleteArticleByUrl(url: String)

    /**
     * Check if an article is saved
     *
     * @param url The URL of the article to check
     * @return True if the article is saved, false otherwise
     */
    suspend fun isArticleSaved(url: String): Boolean
}