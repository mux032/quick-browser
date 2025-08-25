package com.quick.browser.domain.repository

import androidx.lifecycle.LiveData
import com.quick.browser.domain.model.WebPage

/**
 * Repository interface for managing web page history
 *
 * This interface defines the contract for accessing and modifying web browsing history.
 * It provides methods to retrieve, save, and delete web pages from history, as well as
 * search and sort functionality.
 */
interface HistoryRepository {
    /**
     * Get all web pages in history
     *
     * @return LiveData containing a list of all web pages in history
     */
    fun getAllPages(): LiveData<List<WebPage>>

    /**
     * Get a web page by its URL
     *
     * @param url The URL of the web page to retrieve
     * @return The web page or null if not found
     */
    suspend fun getPageByUrl(url: String): WebPage?

    /**
     * Save a web page to history
     *
     * @param page The web page to save
     */
    suspend fun savePage(page: WebPage)

    /**
     * Delete a web page from history
     *
     * @param page The web page to delete
     */
    suspend fun deletePage(page: WebPage)

    /**
     * Delete all web pages from history
     */
    suspend fun deleteAllPages()

    /**
     * Search web pages by query
     *
     * @param query The search query
     * @return LiveData containing a list of matching web pages
     */
    fun searchPages(query: String): LiveData<List<WebPage>>

    /**
     * Get recent web pages
     *
     * @param limit The maximum number of pages to return
     * @return LiveData containing a list of recent web pages
     */
    fun getRecentPages(limit: Int): LiveData<List<WebPage>>

    /**
     * Get most visited web pages
     *
     * @param limit The maximum number of pages to return
     * @return LiveData containing a list of most visited web pages
     */
    fun getMostVisitedPages(limit: Int): LiveData<List<WebPage>>

    /**
     * Increment the visit count for a web page
     *
     * @param url The URL of the web page to increment the visit count for
     */
    suspend fun incrementVisitCount(url: String)

    /**
     * Update the offline availability status of a web page
     *
     * @param url The URL of the web page
     * @param isAvailable Whether the page is available offline
     */
    suspend fun updateOfflineStatus(url: String, isAvailable: Boolean)
}