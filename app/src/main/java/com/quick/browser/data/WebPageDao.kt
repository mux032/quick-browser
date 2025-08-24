package com.quick.browser.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.quick.browser.data.local.entity.WebPage

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
     * Search pages by title or URL without BLOB data for efficient loading
     */
    @Query("SELECT url, title, timestamp, content, isAvailableOffline, visitCount, faviconUrl, previewImageUrl FROM web_pages WHERE title LIKE :query OR url LIKE :query ORDER BY timestamp DESC")
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun searchPagesWithoutBlobs(query: String): LiveData<List<WebPage>>
    
    /**
     * Get the most recent pages
     */
    @Query("SELECT * FROM web_pages ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentPages(limit: Int): LiveData<List<WebPage>>
    
    /**
     * Get the most recent pages without BLOB data for efficient loading in lists
     */
    @Query("SELECT url, title, timestamp, content, isAvailableOffline, visitCount, faviconUrl, previewImageUrl FROM web_pages ORDER BY timestamp DESC LIMIT :limit")
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    fun getRecentPagesWithoutBlobs(limit: Int): LiveData<List<WebPage>>
    
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
    
    /**
     * Update the isAvailableOffline status for a page
     */
    @Query("UPDATE web_pages SET isAvailableOffline = :isAvailable WHERE url = :url")
    suspend fun updateOfflineStatus(url: String, isAvailable: Boolean)
    
    /**
     * Delete pages from today (since midnight)
     */
    @Query("DELETE FROM web_pages WHERE timestamp >= :startOfDay")
    suspend fun deleteTodayPages(startOfDay: Long)
    
    /**
     * Delete pages from last month (last 30 days)
     */
    @Query("DELETE FROM web_pages WHERE timestamp >= :thirtyDaysAgo")
    suspend fun deleteLastMonthPages(thirtyDaysAgo: Long)
    
    /**
     * Get pages grouped by time periods
     */
    @Query("SELECT * FROM web_pages WHERE timestamp >= :startOfDay ORDER BY timestamp DESC")
    fun getTodayPages(startOfDay: Long): LiveData<List<WebPage>>
    
    @Query("SELECT * FROM web_pages WHERE timestamp >= :startOfWeek AND timestamp < :startOfDay ORDER BY timestamp DESC")
    fun getThisWeekPages(startOfWeek: Long, startOfDay: Long): LiveData<List<WebPage>>
    
    @Query("SELECT * FROM web_pages WHERE timestamp < :startOfWeek ORDER BY timestamp DESC")
    fun getOlderPages(startOfWeek: Long): LiveData<List<WebPage>>
}
