package com.quick.browser.data

import androidx.room.*
import com.quick.browser.data.local.entity.SavedArticle
import kotlinx.coroutines.flow.Flow

/**
 * DAO for saved articles
 *
 * This interface provides methods for accessing and modifying saved articles in the database.
 * It includes operations for inserting, retrieving, and deleting saved articles.
 */
@Dao
interface SavedArticleDao {
    
    /**
     * Get all saved articles ordered by saved date (newest first)
     *
     * @return A Flow of lists of saved articles ordered by saved date
     */
    @Query("SELECT * FROM saved_articles ORDER BY savedDate DESC")
    fun getAllSavedArticles(): Flow<List<SavedArticle>>
    
    /**
     * Get a saved article by its URL
     *
     * @param url The URL of the article to retrieve
     * @return The saved article or null if not found
     */
    @Query("SELECT * FROM saved_articles WHERE url = :url")
    suspend fun getSavedArticleByUrl(url: String): SavedArticle?
    
    /**
     * Insert a saved article
     *
     * @param article The article to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedArticle(article: SavedArticle)
    
    /**
     * Delete a saved article
     *
     * @param article The article to delete
     */
    @Delete
    suspend fun deleteSavedArticle(article: SavedArticle)
    
    /**
     * Delete a saved article by its URL
     *
     * @param url The URL of the article to delete
     */
    @Query("DELETE FROM saved_articles WHERE url = :url")
    suspend fun deleteSavedArticleByUrl(url: String)
    
    /**
     * Check if an article is saved
     *
     * @param url The URL of the article to check
     * @return The count of matching articles (0 if not saved, 1 if saved)
     */
    @Query("SELECT COUNT(*) FROM saved_articles WHERE url = :url")
    suspend fun isArticleSaved(url: String): Int
}