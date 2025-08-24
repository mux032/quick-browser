package com.quick.browser.domain.repository

import androidx.lifecycle.LiveData
import com.quick.browser.domain.model.WebPage

/**
 * Repository interface for managing web page history
 */
interface HistoryRepository {
    fun getAllPages(): LiveData<List<WebPage>>
    suspend fun getPageByUrl(url: String): WebPage?
    suspend fun savePage(page: WebPage)
    suspend fun deletePage(page: WebPage)
    suspend fun deleteAllPages()
    fun searchPages(query: String): LiveData<List<WebPage>>
    fun getRecentPages(limit: Int): LiveData<List<WebPage>>
    fun getMostVisitedPages(limit: Int): LiveData<List<WebPage>>
    suspend fun incrementVisitCount(url: String)
    suspend fun updateOfflineStatus(url: String, isAvailable: Boolean)
}