package com.qb.browser.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.qb.browser.model.WebPage

/**
 * Data Access Object for WebPage entities
 */
@Dao
interface WebPageDao {
    
    /**
     * Insert a new page
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: WebPage)
    
    /**
     * Update an existing page
     */
    @Update
    suspend fun updatePage(page: WebPage)
    
    /**
     * Delete a page
     */
    @Delete
    suspend fun deletePage(page: WebPage)
    
    /**
     * Get all pages ordered by timestamp (newest first)
     */
    @Query("SELECT * FROM web_pages ORDER BY timestamp DESC")
    fun getAllPages(): LiveData<List<WebPage>>
    
    /**
     * Get a specific page by URL
     */
    @Query("SELECT * FROM web_pages WHERE url = :url LIMIT 1")
    suspend fun getPageByUrl(url: String): WebPage?
    
    /**
     * Delete all pages
     */
    @Query("DELETE FROM web_pages")
    suspend fun deleteAllPages()
    
    /**
     * Search pages by title or URL
     */
    @Query("SELECT * FROM web_pages WHERE title LIKE :query OR url LIKE :query ORDER BY timestamp DESC")
    fun searchPages(query: String): LiveData<List<WebPage>>
    
    /**
     * Get the most recent pages
     */
    @Query("SELECT * FROM web_pages ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentPages(limit: Int): LiveData<List<WebPage>>
    
    /**
     * Get the most visited pages
     */
    @Query("SELECT * FROM web_pages ORDER BY visitCount DESC LIMIT :limit")
    fun getMostVisitedPages(limit: Int): LiveData<List<WebPage>>
    
    /**
     * Increment visit count for a page
     */
    @Query("UPDATE web_pages SET visitCount = visitCount + 1 WHERE url = :url")
    suspend fun incrementVisitCount(url: String)
}
