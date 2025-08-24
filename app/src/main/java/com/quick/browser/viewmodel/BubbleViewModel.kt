package com.quick.browser.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quick.browser.domain.model.Bubble
import com.quick.browser.domain.model.Settings
import com.quick.browser.domain.repository.SettingsRepository
import com.quick.browser.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BubbleViewModel @Inject constructor(private val settingsRepository: SettingsRepository) : ViewModel() {

    private val _bubbles = MutableStateFlow<List<Bubble>>(emptyList())
    val bubbles: StateFlow<List<Bubble>> = _bubbles

    private val _settings = MutableLiveData<Settings?>()
    val settings: LiveData<Settings?> = _settings

    fun loadSettings() {
        viewModelScope.launch { 
            val settings = settingsRepository.getSettings()
            _settings.postValue(settings)
        }
    }

    fun saveSettings(settings: Settings) {
        viewModelScope.launch { 
            settingsRepository.saveSettings(settings)
        }
    }

    fun addBubble(bubble: Bubble) {
        Logger.e("BubbleViewModel", "Adding bubble: $bubble")
        viewModelScope.launch {
            val currentList = _bubbles.value.toMutableList()
            currentList.add(bubble)
            _bubbles.value = currentList
        }
    }

    fun removeBubble(bubbleId: String) {
        viewModelScope.launch {
            val currentList = _bubbles.value.toMutableList()
            currentList.removeAll { it.id == bubbleId }
            _bubbles.value = currentList
        }
    }

    fun updateBubble(bubble: Bubble) {
        viewModelScope.launch {
            val currentList = _bubbles.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == bubble.id }
            if (index != -1) {
                currentList[index] = bubble
                _bubbles.value = currentList
            }
        }
    }
}
