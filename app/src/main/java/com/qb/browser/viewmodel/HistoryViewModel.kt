package com.qb.browser.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qb.browser.data.WebPageDao
import com.qb.browser.model.WebPage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val webPageDao: WebPageDao
) : ViewModel() {

    /**
     * Get all pages from history
     */
    fun getAllPages(): LiveData<List<WebPage>> {
        return webPageDao.getAllPages()
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
