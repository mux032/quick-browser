package com.qb.browser.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qb.browser.data.SettingsDao
import com.qb.browser.model.Bubble
import com.qb.browser.model.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class BubbleViewModel(private val settingsDao: SettingsDao) : ViewModel() {

    private val _bubbles = MutableStateFlow<List<Bubble>>(emptyList())
    val bubbles: StateFlow<List<Bubble>> = _bubbles

    private val _settings = MutableLiveData<Settings?>()
    val settings: LiveData<Settings?> = _settings

    fun loadSettings() {
        viewModelScope.launch { _settings.postValue(settingsDao.getSettings()) }
    }

    fun saveSettings(settings: Settings) {
        viewModelScope.launch { settingsDao.insertSettings(settings) }
    }

    fun addBubble(bubble: Bubble) {
        Log.e("BubbleViewModel", "Adding bubble: $bubble")
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
