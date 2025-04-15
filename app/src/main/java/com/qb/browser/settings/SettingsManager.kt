package com.qb.browser.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.qb.browser.db.AppDatabase
import com.qb.browser.db.SettingsDao
import com.qb.browser.model.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages application settings and preferences
 */
class SettingsManager private constructor(context: Context) {
    // Initialize the DAO from your database.
    private val settingsDao: SettingsDao = AppDatabase.getInstance(context).settingsDao()
    // Settings will be loaded from the database.
    private var settings: Settings? = null
    // Create an internal scope to launch coroutines outside of a LifecycleOwner.
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        // Preference keys
        private const val KEY_BUBBLE_SAVE_POSITION = "bubble_save_position"
        private const val KEY_BUBBLE_POSITION_PREFIX = "bubble_position_"
        private const val KEY_BROWSER_UA = "browser_user_agent"
        private const val KEY_BROWSER_JS_ENABLED = "browser_js_enabled"
        private const val KEY_BUBBLE_SIZE = "bubble_size"
        private const val KEY_ANIMATION_SPEED = "animation_speed"
        private const val KEY_THEME = "theme"
        private const val KEY_THEME_DARK = "theme_dark"
        private const val KEY_DEFAULT_BUBBLE_COLOR = "default_bubble_color"
        private const val KEY_BLOCK_ADS = "block_ads"
        private const val KEY_TEXT_SIZE = "text_size"
        
        // Default values
        private const val DEFAULT_BUBBLE_SIZE = "medium"
        private const val DEFAULT_ANIMATION_SPEED = "medium"
        private const val DEFAULT_THEME = "system"
        private const val DEFAULT_TEXT_SIZE = "medium"
        private const val DEFAULT_BUBBLE_COLOR = "#2196F3" // Material Blue
        private const val DEFAULT_EXPANDED_BUBBLE_SIZE = 64
        
        @Volatile
        private var instance: SettingsManager? = null
        
        fun getInstance(context: Context): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    
    /**
     * Check if bubble position saving is enabled
     */
    fun isBubblePositionSavingEnabled(): Boolean {
        return preferences.getBoolean(KEY_BUBBLE_SAVE_POSITION, true)
    }
    
    /**
     * Save the position of a bubble for a specific URL
     */
    fun saveBubblePosition(url: String, x: Int, y: Int) {
        if (!isBubblePositionSavingEnabled()) return
        
        val urlHash = url.hashCode().toString()
        preferences.edit()
            .putString("${KEY_BUBBLE_POSITION_PREFIX}$urlHash", "$x,$y")
            .apply()
    }
    
    /**
     * Get the saved position for a bubble with specific URL
     * @return Pair of (x, y) coordinates or null if no position saved
     */
    fun getSavedBubblePosition(url: String): Pair<Int, Int>? {
        val urlHash = url.hashCode().toString()
        val positionStr = preferences.getString("${KEY_BUBBLE_POSITION_PREFIX}$urlHash", null) ?: return null
        
        try {
            val parts = positionStr.split(",")
            if (parts.size == 2) {
                val x = parts[0].toInt()
                val y = parts[1].toInt()
                return Pair(x, y)
            }
        } catch (e: Exception) {
            // If parsing fails, return null
        }
        
        return null
    }
    
    /**
     * Set whether to save bubble positions
     */
    fun setBubblePositionSaving(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_BUBBLE_SAVE_POSITION, enabled)
            .apply()
    }
    
    /**
     * Clear all saved bubble positions
     */
    fun clearSavedBubblePositions() {
        val editor = preferences.edit()
        preferences.all.keys
            .filter { it.startsWith(KEY_BUBBLE_POSITION_PREFIX) }
            .forEach { editor.remove(it) }
        editor.apply()
    }
    
    /**
     * Get the user agent setting
     */
    fun getUserAgent(): String? {
        return preferences.getString(KEY_BROWSER_UA, null)
    }
    
    /**
     * Set the user agent
     */
    fun setUserAgent(userAgent: String?) {
        preferences.edit()
            .putString(KEY_BROWSER_UA, userAgent)
            .apply()
    }
    
    /**
     * Check if JavaScript is enabled
     */
    fun isJavaScriptEnabled(): Boolean {
        return preferences.getBoolean(KEY_BROWSER_JS_ENABLED, true)
    }
    
    /**
     * Set JavaScript enabled state
     */
    fun setJavaScriptEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_BROWSER_JS_ENABLED, enabled)
            .apply()
    }
    
    /**
     * Get the bubble size setting
     */
    fun getBubbleSize(): String {
        return preferences.getString(KEY_BUBBLE_SIZE, DEFAULT_BUBBLE_SIZE) ?: DEFAULT_BUBBLE_SIZE
    }
    
    /**
     * Set the bubble size
     */
    fun setBubbleSize(size: String) {
        preferences.edit()
            .putString(KEY_BUBBLE_SIZE, size)
            .apply()
    }
    
    /**
     * Get the animation speed setting
     */
    fun getAnimationSpeed(): String {
        return preferences.getString(KEY_ANIMATION_SPEED, DEFAULT_ANIMATION_SPEED) ?: DEFAULT_ANIMATION_SPEED
    }
    
    /**
     * Set the animation speed
     */
    fun setAnimationSpeed(speed: String) {
        preferences.edit()
            .putString(KEY_ANIMATION_SPEED, speed)
            .apply()
    }
    
    /**
     * Get the theme setting
     */
    fun getTheme(): String {
        return preferences.getString(KEY_THEME, DEFAULT_THEME) ?: DEFAULT_THEME
    }
    
    /**
     * Set the theme
     */
    fun setTheme(theme: String) {
        preferences.edit()
            .putString(KEY_THEME, theme)
            .apply()
    }
    
    /**
     * Check if dark theme is enabled
     */
    fun isDarkThemeEnabled(): Boolean {
        return preferences.getBoolean(KEY_THEME_DARK, false)
    }
    
    /**
     * Set dark theme enabled state
     */
    fun setDarkThemeEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_THEME_DARK, enabled)
            .apply()
    }
    
    /**
     * Get the default bubble color
     */
    fun getDefaultBubbleColor(): String {
        return preferences.getString(KEY_DEFAULT_BUBBLE_COLOR, DEFAULT_BUBBLE_COLOR) ?: DEFAULT_BUBBLE_COLOR
    }
    
    /**
     * Set the default bubble color
     */
    fun setDefaultBubbleColor(color: String) {
        preferences.edit()
            .putString(KEY_DEFAULT_BUBBLE_COLOR, color)
            .apply()
    }
    
    /**
     * Check if ad blocking is enabled
     */
    fun isAdBlockEnabled(): Boolean {
        return preferences.getBoolean(KEY_BLOCK_ADS, true)
    }
    
    /**
     * Set ad blocking enabled state
     */
    fun setAdBlockEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_BLOCK_ADS, enabled)
            .apply()
    }
    
    /**
     * Get the text size setting for reading mode
     */
    fun getTextSize(): String {
        return preferences.getString(KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE) ?: DEFAULT_TEXT_SIZE
    }
    
    /**
     * Set the text size for reading mode
     */
    fun setTextSize(size: String) {
        preferences.edit()
            .putString(KEY_TEXT_SIZE, size)
            .apply()
    }

    fun getExpandedBubbleSize(): Int {
        return (settings?.expandedBubbleSize?.times(DEFAULT_EXPANDED_BUBBLE_SIZE))?.toInt() 
            ?: DEFAULT_EXPANDED_BUBBLE_SIZE
    }

    fun setExpandedBubbleSize(size: Int) {
        val normalizedSize = size.toFloat() / DEFAULT_EXPANDED_BUBBLE_SIZE
        scope.launch {
            settings?.let {
                val updatedSettings = it.copy(expandedBubbleSize = normalizedSize)
                settingsDao.updateSettings(updatedSettings)
                settings = updatedSettings
            }
        }
    }
}