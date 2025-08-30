package com.quick.browser.presentation.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quick.browser.domain.model.Settings
import com.quick.browser.domain.usecase.GetSettingsUseCase
import com.quick.browser.domain.usecase.UpdateSettingsUseCase
import com.quick.browser.presentation.ui.base.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the main activity
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<MainUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<MainUiState>> = _uiState

    private val _pendingUrl = MutableStateFlow<String?>(null)
    val pendingUrl: StateFlow<String?> = _pendingUrl.asStateFlow()

    init {
        loadSettings()
    }

    fun setPendingUrl(url: String?) {
        _pendingUrl.value = url
    }

    /**
     * Load app settings
     */
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                when (val result = getSettingsUseCase()) {
                    is com.quick.browser.domain.result.Result.Success -> {
                        _uiState.value = UiState.Success(
                            MainUiState(
                                settings = result.data
                            )
                        )
                    }
                    is com.quick.browser.domain.result.Result.Failure -> {
                        _uiState.value = UiState.Error("Failed to load settings: ${result.error}")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to load settings: ${e.message}")
            }
        }
    }

    /**
     * Update app settings
     */
    fun updateSettings(settings: Settings) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                updateSettingsUseCase(settings)
                _uiState.value = UiState.Success(
                    MainUiState(
                        settings = settings
                    )
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to update settings: ${e.message}")
            }
        }
    }
}