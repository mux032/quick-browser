package com.quick.browser.presentation.ui.base

/**
 * Base UI event class for all screens
 */
sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class ShowSnackbar(val message: String) : UiEvent()
    data class NavigateTo(val route: String) : UiEvent()
    object NavigateBack : UiEvent()
}