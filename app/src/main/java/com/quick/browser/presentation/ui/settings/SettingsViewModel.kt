package com.quick.browser.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quick.browser.domain.model.Settings
import com.quick.browser.domain.usecase.GetSettingsUseCase
import com.quick.browser.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for settings
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        loadSettings()
    }

    private fun loadSettings() {
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
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to load settings"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load settings"
                )
            }
        }
    }

    fun updateSettings(settings: Settings) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                when (val result = updateSettingsUseCase(settings)) {
                    is com.quick.browser.domain.result.Result.Success<*> -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            settings = settings
                        )
                    }
                    is com.quick.browser.domain.result.Result.Failure<*> -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to update settings"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to update settings"
                )
            }
        }
    }
}