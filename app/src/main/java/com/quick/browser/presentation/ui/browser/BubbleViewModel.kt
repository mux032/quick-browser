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

@HiltViewModel
class BubbleViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState

    fun loadSettings() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val settings = getSettingsUseCase()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    settings = settings
                )
            } catch (e: Exception) {
                Logger.e("BubbleViewModel", "Error loading settings", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load settings"
                )
            }
        }
    }

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

    fun addBubble(bubble: Bubble) {
        Logger.e("BubbleViewModel", "Adding bubble: $bubble")
        val currentList = _uiState.value.bubbles.toMutableList()
        currentList.add(bubble)
        _uiState.value = _uiState.value.copy(bubbles = currentList)
    }

    fun removeBubble(bubbleId: String) {
        val currentList = _uiState.value.bubbles.toMutableList()
        currentList.removeAll { it.id == bubbleId }
        _uiState.value = _uiState.value.copy(bubbles = currentList)
    }

    fun updateBubble(bubble: Bubble) {
        val currentList = _uiState.value.bubbles.toMutableList()
        val index = currentList.indexOfFirst { it.id == bubble.id }
        if (index != -1) {
            currentList[index] = bubble
            _uiState.value = _uiState.value.copy(bubbles = currentList)
        }
    }
}