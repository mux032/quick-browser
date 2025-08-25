package com.quick.browser.presentation.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quick.browser.domain.model.WebPage
import com.quick.browser.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistoryUseCase: GetHistoryUseCase,
    private val deleteWebPageUseCase: DeleteWebPageUseCase,
    private val deleteAllPagesUseCase: DeleteAllPagesUseCase,
    private val searchHistoryUseCase: SearchHistoryUseCase,
    private val getRecentPagesUseCase: GetRecentPagesUseCase,
    private val getMostVisitedPagesUseCase: GetMostVisitedPagesUseCase,
    private val updateOfflineStatusUseCase: UpdateOfflineStatusUseCase,
    private val incrementVisitCountUseCase: IncrementVisitCountUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState

    /**
     * Get all pages from history
     */
    fun getAllPages() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                // Note: We would need to convert LiveData to StateFlow for full reactive UI
                // For now, we'll keep this as a placeholder
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load history"
                )
            }
        }
    }

    /**
     * Delete a page from history
     */
    fun deletePage(page: WebPage) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    deleteWebPageUseCase(page)
                }
                // Update UI state after deletion
                val currentPages = _uiState.value.webPages.toMutableList()
                currentPages.removeAll { it.url == page.url }
                _uiState.value = _uiState.value.copy(webPages = currentPages)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete page"
                )
            }
        }
    }

    /**
     * Clear all data including offline pages
     */
    fun clearAllData() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    deleteAllPagesUseCase()
                }
                _uiState.value = _uiState.value.copy(webPages = emptyList())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to clear history"
                )
            }
        }
    }

    /**
     * Search history by query
     */
    fun searchHistory(query: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    searchQuery = query,
                    isLoading = true
                )
                // Note: We would need to convert LiveData to StateFlow for full reactive UI
                // For now, we'll keep this as a placeholder
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to search history"
                )
            }
        }
    }

    /**
     * Get recent pages
     */
    fun getRecentPages(limit: Int = 10) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                // Note: We would need to convert LiveData to StateFlow for full reactive UI
                // For now, we'll keep this as a placeholder
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load recent pages"
                )
            }
        }
    }

    /**
     * Get most visited pages
     */
    fun getMostVisitedPages(limit: Int = 10) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                // Note: We would need to convert LiveData to StateFlow for full reactive UI
                // For now, we'll keep this as a placeholder
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load most visited pages"
                )
            }
        }
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
            try {
                withContext(Dispatchers.IO) {
                    incrementVisitCountUseCase(url)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update visit count"
                )
            }
        }
    }

    /**
     * Update offline status for a page
     */
    fun updateOfflineStatus(url: String, isAvailable: Boolean) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    updateOfflineStatusUseCase(url, isAvailable)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update offline status"
                )
            }
        }
    }
}