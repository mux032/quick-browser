package com.quick.browser.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quick.browser.domain.model.WebPage
import com.quick.browser.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    /**
     * Get all pages from history
     */
    fun getAllPages(): LiveData<List<WebPage>> {
        return historyRepository.getAllPages()
    }

    /**
     * Delete a page from history
     */
    fun deletePage(page: WebPage) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                historyRepository.deletePage(page)
            }
        }
    }

    /**
     * Clear all data including offline pages
     */
    fun clearAllData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                historyRepository.deleteAllPages()
            }
        }
    }

    /**
     * Search history by query
     */
    fun searchHistory(query: String): LiveData<List<WebPage>> {
        // Note: We're using the regular searchPages method since we've updated the repository
        // to return domain models instead of entities
        return historyRepository.searchPages("%$query%")
    }

    /**
     * Get recent pages
     */
    fun getRecentPages(limit: Int = 10): LiveData<List<WebPage>> {
        return historyRepository.getRecentPages(limit)
    }

    /**
     * Get most visited pages
     */
    fun getMostVisitedPages(limit: Int = 10): LiveData<List<WebPage>> {
        return historyRepository.getMostVisitedPages(limit)
    }
    
    /**
     * Get start of today (midnight)
     */
    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * Get start of this week (Monday)
     */
    private fun getStartOfWeek(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * Increment visit count for a page
     */
    fun incrementVisitCount(url: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                historyRepository.incrementVisitCount(url)
            }
        }
    }
    
    /**
     * Update offline status for a page
     */
    fun updateOfflineStatus(url: String, isAvailable: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                historyRepository.updateOfflineStatus(url, isAvailable)
            }
        }
    }
}
