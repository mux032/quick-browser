package com.quick.browser.presentation.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel class that provides common functionality for all ViewModels
 */
abstract class BaseViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Success(Unit))
    val uiState: StateFlow<UiState<Unit>> = _uiState

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent: Flow<UiEvent> = _uiEvent.receiveAsFlow()

    protected fun <T> updateUiState(state: UiState<T>) {
        viewModelScope.launch {
            _uiState.value = state as UiState<Unit>
        }
    }

    protected fun sendUiEvent(event: UiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }
}