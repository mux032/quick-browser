package com.quick.browser.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.quick.browser.data.local.entity.WebPage

/**
 * Data Access Object for WebPage entities
 *
 * This interface provides methods for accessing and modifying web pages in the database.
 * It includes operations for inserting, updating, deleting, and querying web pages
 * with various criteria such as recency, visit count, and time periods.
 */
@Dao
interface WebPageDao {

    /**
     * Insert a new page
     *
     * @param page The page to insert
     */
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertPage(page: WebPage)

    /**
     * Update an existing page
     *
     * @param page The page to update
     */
    @Update
    suspend fun updatePage(page: WebPage)

    /**
     * Delete a page
     *
     * @param page The page to delete
     */
    @Delete
    suspend fun deletePage(page: WebPage)

    /**
     * Get all pages ordered by timestamp (newest first)
     *
     * @return LiveData containing a list of all pages ordered by timestamp
     */
    @Query("SELECT * FROM web_pages ORDER BY timestamp DESC")
    fun getAllPages(): LiveData<List<WebPage>>

    /**
     * Get a specific page by URL
     *
     * @param url The URL of the page to retrieve
     * @return The page or null if not found
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
     *
     * @param query The search query
     * @return LiveData containing a list of matching pages
     */
    @Query("SELECT * FROM web_pages WHERE title LIKE :query OR url LIKE :query ORDER BY timestamp DESC")
    fun searchPages(query: String): LiveData<List<WebPage>>

    /**
     * Search pages by title or URL without BLOB data for efficient loading
     *
     * @param query The search query
     * @return LiveData containing a list of matching pages without BLOB data
     */
    @Query("SELECT url, title, timestamp, content, isAvailableOffline, visitCount, faviconUrl, previewImageUrl FROM web_pages WHERE title LIKE :query OR url LIKE :query ORDER BY timestamp DESC")
    @SuppressWarnings(RoomWarnings.Companion.QUERY_MISMATCH)
    fun searchPagesWithoutBlobs(query: String): LiveData<List<WebPage>>

    /**
     * Get the most recent pages
     *
     * @param limit The maximum number of pages to return
     * @return LiveData containing a list of recent pages
     */
    @Query("SELECT * FROM web_pages ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentPages(limit: Int): LiveData<List<WebPage>>

    /**
     * Get the most recent pages without BLOB data for efficient loading in lists
     *
     * @param limit The maximum number of pages to return
     * @return LiveData containing a list of recent pages without BLOB data
     */
    @Query("SELECT url, title, timestamp, content, isAvailableOffline, visitCount, faviconUrl, previewImageUrl FROM web_pages ORDER BY timestamp DESC LIMIT :limit")
    @SuppressWarnings(RoomWarnings.Companion.QUERY_MISMATCH)
    fun getRecentPagesWithoutBlobs(limit: Int): LiveData<List<WebPage>>

    /**
     * Get the most visited pages
     *
     * @param limit The maximum number of pages to return
     * @return LiveData containing a list of most visited pages
     */
    @Query("SELECT * FROM web_pages ORDER BY visitCount DESC LIMIT :limit")
    fun getMostVisitedPages(limit: Int): LiveData<List<WebPage>>

    /**
     * Increment visit count for a page
     *
     * @param url The URL of the page to increment the visit count for
     */
    @Query("UPDATE web_pages SET visitCount = visitCount + 1 WHERE url = :url")
    suspend fun incrementVisitCount(url: String)

    /**
     * Update the isAvailableOffline status for a page
     *
     * @param url The URL of the page
     * @param isAvailable Whether the page is available offline
     */
    @Query("UPDATE web_pages SET isAvailableOffline = :isAvailable WHERE url = :url")
    suspend fun updateOfflineStatus(url: String, isAvailable: Boolean)

    /**
     * Delete pages from today (since midnight)
     *
     * @param startOfDay The timestamp representing the start of today
     */
    @Query("DELETE FROM web_pages WHERE timestamp >= :startOfDay")
    suspend fun deleteTodayPages(startOfDay: Long)

    /**
     * Delete pages from last month (last 30 days)
     *
     * @param thirtyDaysAgo The timestamp representing 30 days ago
     */
    @Query("DELETE FROM web_pages WHERE timestamp >= :thirtyDaysAgo")
    suspend fun deleteLastMonthPages(thirtyDaysAgo: Long)

    /**
     * Get pages from today (since midnight)
     *
     * @param startOfDay The timestamp representing the start of today
     * @return LiveData containing a list of today's pages
     */
    @Query("SELECT * FROM web_pages WHERE timestamp >= :startOfDay ORDER BY timestamp DESC")
    fun getTodayPages(startOfDay: Long): LiveData<List<WebPage>>

    /**
     * Get pages from this week (between start of week and start of today)
     *
     * @param startOfWeek The timestamp representing the start of the week
     * @param startOfDay The timestamp representing the start of today
     * @return LiveData containing a list of this week's pages
     */
    @Query("SELECT * FROM web_pages WHERE timestamp >= :startOfWeek AND timestamp < :startOfDay ORDER BY timestamp DESC")
    fun getThisWeekPages(startOfWeek: Long, startOfDay: Long): LiveData<List<WebPage>>

    /**
     * Get older pages (before start of week)
     *
     * @param startOfWeek The timestamp representing the start of the week
     * @return LiveData containing a list of older pages
     */
    @Query("SELECT * FROM web_pages WHERE timestamp < :startOfWeek ORDER BY timestamp DESC")
    fun getOlderPages(startOfWeek: Long): LiveData<List<WebPage>>
}