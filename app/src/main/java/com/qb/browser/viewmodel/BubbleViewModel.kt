package com.qb.browser.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.qb.browser.settings.SettingsManager

/**
 * ViewModel for managing bubble settings
 */
class BubbleViewModel(application: Application) : AndroidViewModel(application) {
    
    private val settingsManager = SettingsManager.getInstance(application)
    
    // Data class to represent bubble settings
    data class BubbleSettings(
        val size: String = "medium",
        val animationSpeed: String = "medium",
        val savePositions: Boolean = true,
        val blockAds: Boolean = true,
        val defaultColor: String = "#2196F3",
        val javascriptEnabled: Boolean = true,
        val darkTheme: Boolean = false
    )
    
    private val _settings = MutableLiveData<BubbleSettings>()
    
    /**
     * Get current bubble settings as LiveData
     */
    fun getSettings(): LiveData<BubbleSettings> {
        if (_settings.value == null) {
            // Load settings from SettingsManager
            val settings = BubbleSettings(
                size = settingsManager.getBubbleSize(),
                animationSpeed = settingsManager.getAnimationSpeed(),
                savePositions = settingsManager.isBubblePositionSavingEnabled(),
                blockAds = settingsManager.isAdBlockEnabled(),
                defaultColor = settingsManager.getDefaultBubbleColor(),
                javascriptEnabled = settingsManager.isJavaScriptEnabled(),
                darkTheme = settingsManager.isDarkThemeEnabled()
            )
            _settings.value = settings
        }
        return _settings
    }
    
    /**
     * Save bubble settings
     */
    fun saveSettings(settings: BubbleSettings) {
        // Update settings in manager
        settingsManager.setBubbleSize(settings.size)
        settingsManager.setAnimationSpeed(settings.animationSpeed)
        settingsManager.setBubblePositionSaving(settings.savePositions)
        settingsManager.setAdBlockEnabled(settings.blockAds)
        settingsManager.setDefaultBubbleColor(settings.defaultColor)
        settingsManager.setJavaScriptEnabled(settings.javascriptEnabled)
        settingsManager.setDarkThemeEnabled(settings.darkTheme)
        
        // Update LiveData
        _settings.value = settings
    }
    
    /**
     * Clear all saved bubble positions
     */
    fun clearBubblePositions() {
        settingsManager.clearSavedBubblePositions()
    }

    /**
     * Get the title of a bubble by its ID
     */
    fun getTitle(bubbleId: String): LiveData<String> {
        val title = MutableLiveData<String>()
        title.value = "Untitled" // Default title, replace with actual logic if needed
        return title
    }
}