package com.quick.browser.presentation.ui.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quick.browser.domain.model.Bubble
import com.quick.browser.domain.model.Settings
import com.quick.browser.domain.usecase.GetSettingsUseCase
import com.quick.browser.domain.usecase.UpdateSettingsUseCase
import com.quick.browser.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the browser bubble UI
 *
 * This ViewModel manages the state for browser bubbles and handles
 * interactions with use cases for settings management.
 *
 * @param getSettingsUseCase The use case for retrieving app settings
 * @param updateSettingsUseCase The use case for updating app settings
 */
@HiltViewModel
class BubbleViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState

    /**
     * Load application settings
     *
     * This method retrieves the current application settings using the
     * GetSettingsUseCase and updates the UI state accordingly.
     */
    fun loadSettings() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                when (val result = getSettingsUseCase()) {
                    is com.quick.browser.domain.result.Result.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            settings = result.data
                        )
                    }
                    is com.quick.browser.domain.result.Result.Failure -> {
                        Logger.e("BubbleViewModel", "Error loading settings: ${result.error}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to load settings"
                        )
                    }
                }
            } catch (e: Exception) {
                Logger.e("BubbleViewModel", "Error loading settings", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load settings"
                )
            }
        }
    }

    /**
     * Save application settings
     *
     * This method saves the provided settings using the UpdateSettingsUseCase
     * and updates the UI state accordingly.
     *
     * @param settings The settings to save
     */
    fun saveSettings(settings: Settings) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                updateSettingsUseCase(settings)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    settings = settings
                )
            } catch (e: Exception) {
                Logger.e("BubbleViewModel", "Error saving settings", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to save settings"
                )
            }
        }
    }

    /**
     * Add a bubble to the UI state
     *
     * @param bubble The bubble to add
     */
    fun addBubble(bubble: Bubble) {
        Logger.e("BubbleViewModel", "Adding bubble: $bubble")
        val currentList = _uiState.value.bubbles.toMutableList()
        currentList.add(bubble)
        _uiState.value = _uiState.value.copy(bubbles = currentList)
    }

    /**
     * Remove a bubble from the UI state
     *
     * @param bubbleId The ID of the bubble to remove
     */
    fun removeBubble(bubbleId: String) {
        val currentList = _uiState.value.bubbles.toMutableList()
        currentList.removeAll { it.id == bubbleId }
        _uiState.value = _uiState.value.copy(bubbles = currentList)
    }

    /**
     * Update a bubble in the UI state
     *
     * @param bubble The updated bubble
     */
    fun updateBubble(bubble: Bubble) {
        val currentList = _uiState.value.bubbles.toMutableList()
        val index = currentList.indexOfFirst { it.id == bubble.id }
        if (index != -1) {
            currentList[index] = bubble
            _uiState.value = _uiState.value.copy(bubbles = currentList)
        }
    }
}