package com.qb.browser.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.qb.browser.db.AppDatabase
import com.qb.browser.model.WebPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getInstance(application)
    private val webPageDao = database.webPageDao()
    
    /**
     * Get all pages from history
     */
    fun getAllPages(): LiveData<List<WebPage>> {
        return webPageDao.getAllPages()
    }
    
    /**
     * Get all offline pages
     */
    fun getOfflinePages(): LiveData<List<WebPage>> {
        return webPageDao.getOfflinePages()
    }
    
    /**
     * Delete a page from history
     */
    fun deletePage(page: WebPage) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                webPageDao.deletePage(page)
            }
        }
    }
    
    /**
     * Clear all history (except saved offline pages)
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                webPageDao.deleteAllNonOfflinePages()
            }
        }
    }
    
    /**
     * Clear all data including offline pages
     */
    fun clearAllData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                webPageDao.deleteAllPages()
            }
        }
    }
    
    /**
     * Search history by query
     */
    fun searchHistory(query: String): LiveData<List<WebPage>> {
        return webPageDao.searchPages("%$query%")
    }
    
    /**
     * Get recent pages
     */
    fun getRecentPages(limit: Int = 10): LiveData<List<WebPage>> {
        return webPageDao.getRecentPages(limit)
    }
    
    /**
     * Get most visited pages
     */
    fun getMostVisitedPages(limit: Int = 10): LiveData<List<WebPage>> {
        return webPageDao.getMostVisitedPages(limit)
    }
}
