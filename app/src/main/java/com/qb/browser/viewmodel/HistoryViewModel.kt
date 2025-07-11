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
import java.util.Calendar
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
    
    /**
     * Get today's pages
     */
    fun getTodayPages(): LiveData<List<WebPage>> {
        return webPageDao.getTodayPages(getStartOfDay())
    }
    
    /**
     * Get this week's pages
     */
    fun getThisWeekPages(): LiveData<List<WebPage>> {
        return webPageDao.getThisWeekPages(getStartOfWeek(), getStartOfDay())
    }
    
    /**
     * Get older pages
     */
    fun getOlderPages(): LiveData<List<WebPage>> {
        return webPageDao.getOlderPages(getStartOfWeek())
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
     * Get 30 days ago timestamp
     */
    private fun getThirtyDaysAgo(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -30)
        return calendar.timeInMillis
    }
    
    /**
     * Delete today's history (since midnight)
     */
    fun deleteTodayHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val startOfDay = getStartOfDay()
                webPageDao.deleteTodayPages(startOfDay)
            }
        }
    }
    
    /**
     * Delete last month's history (last 30 days)
     */
    fun deleteLastMonthHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val thirtyDaysAgo = getThirtyDaysAgo()
                webPageDao.deleteLastMonthPages(thirtyDaysAgo)
            }
        }
    }
}
