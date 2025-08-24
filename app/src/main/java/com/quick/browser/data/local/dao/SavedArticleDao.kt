package com.quick.browser.data

import androidx.room.*
import com.quick.browser.data.local.entity.SavedArticle
import kotlinx.coroutines.flow.Flow

/**
 * DAO for saved articles
 */
@Dao
interface SavedArticleDao {
    
    @Query("SELECT * FROM saved_articles ORDER BY savedDate DESC")
    fun getAllSavedArticles(): Flow<List<SavedArticle>>
    
    @Query("SELECT * FROM saved_articles WHERE url = :url")
    suspend fun getSavedArticleByUrl(url: String): SavedArticle?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedArticle(article: SavedArticle)
    
    @Delete
    suspend fun deleteSavedArticle(article: SavedArticle)
    
    @Query("DELETE FROM saved_articles WHERE url = :url")
    suspend fun deleteSavedArticleByUrl(url: String)
    
    @Query("SELECT COUNT(*) FROM saved_articles WHERE url = :url")
    suspend fun isArticleSaved(url: String): Int
}