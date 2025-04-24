package com.qb.browser.util

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import androidx.preference.PreferenceManager

/**
 * Manages application settings and user preferences
 */
class SettingsManager private constructor(context: Context) {
    
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val appContext = context.applicationContext
    
    companion object {
        // Keys for preferences
        private const val KEY_DARK_THEME = "pref_dark_theme"
        private const val KEY_SEPIA_THEME = "pref_sepia_theme"
        private const val KEY_TEXT_SIZE = "pref_text_size"
        private const val KEY_FONT_FAMILY = "pref_font_family"
        private const val KEY_BUBBLE_SIZE = "pref_bubble_size"
        private const val KEY_BUBBLE_OPACITY = "pref_bubble_opacity"
        private const val KEY_SAVE_POSITION = "pref_save_bubble_position"
        private const val KEY_AD_BLOCKING = "pref_ad_blocking"
        private const val KEY_JAVASCRIPT = "pref_javascript"
        private const val KEY_SAVE_HISTORY = "pref_save_history"
        private const val KEY_ENCRYPT_PAGES = "pref_encrypt_pages"
        private const val KEY_PAGE_SNAPSHOT = "pref_page_snapshot"
        private const val KEY_BACKGROUND_SYNC = "pref_background_sync"
        private const val KEY_TTS_SPEECH_RATE = "pref_tts_speech_rate"
        private const val KEY_TTS_PITCH = "pref_tts_pitch"
        private const val KEY_BUBBLE_SAVE_POSITION = "pref_bubble_save_position"
        private const val KEY_BUBBLE_POSITION_PREFIX = "bubble_position_"
        private const val PREFS_NAME = "bubble_settings"
        private const val KEY_EXPANDED_BUBBLE_SIZE = "expanded_bubble_size"
        private const val KEY_MAIN_BUBBLE_X = "main_bubble_x"
        private const val KEY_MAIN_BUBBLE_Y = "main_bubble_y"
        private const val KEY_THEME_COLOR = "pref_theme_color"
        
        // Default values
        private const val DEFAULT_TEXT_SIZE = 16
        private const val DEFAULT_BUBBLE_SIZE = 100 // Percentage of default size
        private const val DEFAULT_BUBBLE_OPACITY = 90 // Percentage of opacity
        private const val DEFAULT_EXPANDED_BUBBLE_SIZE = 64
        private const val DEFAULT_THEME_COLOR = "Blue"
        
        // Font family options
        const val FONT_FAMILY_DEFAULT = "default"
        const val FONT_FAMILY_SERIF = "serif"
        const val FONT_FAMILY_SANS_SERIF = "sans-serif"
        const val FONT_FAMILY_MONOSPACE = "monospace"
        
        // Theme options
        const val THEME_LIGHT = 0
        const val THEME_DARK = 1
        const val THEME_SEPIA = 2
        
        @Volatile
        private var instance: SettingsManager? = null
        
        fun getInstance(context: Context): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * Dark theme settings
     */
    fun isDarkThemeEnabled(): Boolean {
        return preferences.getBoolean(KEY_DARK_THEME, false)
    }
    
    fun setDarkThemeEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_DARK_THEME, enabled).apply()
        
        // If dark theme is enabled, make sure sepia is disabled
        if (enabled && isSepiaThemeEnabled()) {
            setSepiaThemeEnabled(false)
        }
    }
    
    /**
     * Sepia theme settings
     */
    fun isSepiaThemeEnabled(): Boolean {
        return preferences.getBoolean(KEY_SEPIA_THEME, false)
    }
    
    fun setSepiaThemeEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SEPIA_THEME, enabled).apply()
        
        // If sepia theme is enabled, make sure dark is disabled
        if (enabled && isDarkThemeEnabled()) {
            setDarkThemeEnabled(false)
        }
    }
    
    /**
     * Get the current theme as an integer value
     */
    fun getCurrentTheme(): Int {
        return when {
            isDarkThemeEnabled() -> THEME_DARK
            isSepiaThemeEnabled() -> THEME_SEPIA
            else -> THEME_LIGHT
        }
    }
    
    /**
     * Set the current theme using an integer value
     */
    fun setCurrentTheme(theme: Int) {
        when (theme) {
            THEME_DARK -> {
                setDarkThemeEnabled(true)
                setSepiaThemeEnabled(false)
            }
            THEME_SEPIA -> {
                setDarkThemeEnabled(false)
                setSepiaThemeEnabled(true)
            }
            else -> {
                setDarkThemeEnabled(false)
                setSepiaThemeEnabled(false)
            }
        }
    }
    
    /**
     * Text size settings
     */
    fun getTextSize(): Int {
        return preferences.getInt(KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE)
    }
    
    fun setTextSize(size: Int) {
        preferences.edit().putInt(KEY_TEXT_SIZE, size).apply()
    }
    
    /**
     * Font family settings
     */
    fun getFontFamily(): String {
        return preferences.getString(KEY_FONT_FAMILY, FONT_FAMILY_DEFAULT) ?: FONT_FAMILY_DEFAULT
    }
    
    fun setFontFamily(fontFamily: String) {
        preferences.edit().putString(KEY_FONT_FAMILY, fontFamily).apply()
    }
    
    /**
     * Get the Typeface for the current font family
     */
    fun getTypeface(): Typeface {
        return when (getFontFamily()) {
            FONT_FAMILY_SERIF -> Typeface.SERIF
            FONT_FAMILY_SANS_SERIF -> Typeface.SANS_SERIF
            FONT_FAMILY_MONOSPACE -> Typeface.MONOSPACE
            else -> Typeface.DEFAULT
        }
    }
    
    /**
     * Bubble size settings
     */
    fun getBubbleSize(): Float {
        return preferences.getInt(KEY_BUBBLE_SIZE, DEFAULT_BUBBLE_SIZE).toFloat() / 100f
    }
    
    fun setBubbleSize(size: Float) {
        val sizeInt = (size * 100).toInt()
        preferences.edit().putInt(KEY_BUBBLE_SIZE, sizeInt).apply()
    }
    
    /**
     * Bubble opacity settings
     */
    fun getBubbleOpacity(): Int {
        return preferences.getInt(KEY_BUBBLE_OPACITY, DEFAULT_BUBBLE_OPACITY)
    }
    
    fun setBubbleOpacity(opacity: Int) {
        preferences.edit().putInt(KEY_BUBBLE_OPACITY, opacity).apply()
    }
    
    /**
     * Save bubble position settings
     */
    fun getSavePosition(): Boolean {
        return preferences.getBoolean(KEY_SAVE_POSITION, true)
    }
    
    fun setSavePosition(save: Boolean) {
        preferences.edit().putBoolean(KEY_SAVE_POSITION, save).apply()
    }
    
    /**
     * Ad blocking settings
     */
    fun isAdBlockEnabled(): Boolean {
        return preferences.getBoolean(KEY_AD_BLOCKING, true)
    }
    
    fun setAdBlockEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AD_BLOCKING, enabled).apply()
    }
    
    /**
     * JavaScript settings
     */
    fun isJavaScriptEnabled(): Boolean {
        return preferences.getBoolean(KEY_JAVASCRIPT, true)
    }
    
    fun setJavaScriptEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_JAVASCRIPT, enabled).apply()
    }
    
    /**
     * Save browsing history settings
     */
    fun isSaveHistoryEnabled(): Boolean {
        return preferences.getBoolean(KEY_SAVE_HISTORY, true)
    }
    
    fun setSaveHistoryEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SAVE_HISTORY, enabled).apply()
    }
    
    /**
     * Encrypt saved pages settings
     */
    fun isEncryptionEnabled(): Boolean {
        return preferences.getBoolean(KEY_ENCRYPT_PAGES, false)
    }
    
    fun setEncryptionEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ENCRYPT_PAGES, enabled).apply()
    }
    
    /**
     * Page snapshot settings
     */
    fun isPageSnapshotEnabled(): Boolean {
        return preferences.getBoolean(KEY_PAGE_SNAPSHOT, true)
    }
    
    fun setPageSnapshotEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_PAGE_SNAPSHOT, enabled).apply()
    }
    
    /**
     * Background sync settings
     */
    fun isBackgroundSyncEnabled(): Boolean {
        return preferences.getBoolean(KEY_BACKGROUND_SYNC, false)
    }
    
    fun setBackgroundSyncEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_BACKGROUND_SYNC, enabled).apply()
    }
    
    /**
     * TTS speech rate settings
     */
    fun getTtsSpeechRate(): Float {
        return preferences.getFloat(KEY_TTS_SPEECH_RATE, TextToSpeechManager.DEFAULT_SPEECH_RATE)
    }
    
    fun setTtsSpeechRate(rate: Float) {
        val boundedRate = rate.coerceIn(
            TextToSpeechManager.MIN_SPEECH_RATE, 
            TextToSpeechManager.MAX_SPEECH_RATE
        )
        preferences.edit().putFloat(KEY_TTS_SPEECH_RATE, boundedRate).apply()
    }
    
    /**
     * TTS pitch settings
     */
    fun getTtsPitch(): Float {
        return preferences.getFloat(KEY_TTS_PITCH, TextToSpeechManager.DEFAULT_PITCH)
    }
    
    fun setTtsPitch(pitch: Float) {
        val boundedPitch = pitch.coerceIn(
            TextToSpeechManager.MIN_PITCH, 
            TextToSpeechManager.MAX_PITCH
        )
        preferences.edit().putFloat(KEY_TTS_PITCH, boundedPitch).apply()
    }

    /**
     * Animation speed settings
     */
    fun getAnimationSpeed(): Float {
        return preferences.getFloat("animation_speed", 1.0f)
    }
    
    fun setAnimationSpeed(speed: Float) {
        preferences.edit().putFloat("animation_speed", speed).apply()
    }
    
    /**
     * Check if bubble animations are enabled
     */
    fun areBubbleAnimationsEnabled(): Boolean {
        return preferences.getBoolean("bubble_animations_enabled", true)
    }

    /**
     * Check if bubble position saving is enabled
     */
    fun isBubblePositionSavingEnabled(): Boolean {
        return preferences.getBoolean(KEY_BUBBLE_SAVE_POSITION, true)
    }

    /**
     * Save bubble position for a specific URL
     */
    fun saveBubblePosition(url: String, x: Int, y: Int) {
        if (!isBubblePositionSavingEnabled()) return
        val urlHash = url.hashCode().toString()
        preferences.edit()
            .putString("${KEY_BUBBLE_POSITION_PREFIX}$urlHash", "$x,$y")
            .apply()
    }

    /**
     * Get saved position for a bubble
     */
    fun getSavedBubblePosition(url: String): Pair<Int, Int>? {
        val urlHash = url.hashCode().toString()
        val positionStr = preferences.getString("${KEY_BUBBLE_POSITION_PREFIX}$urlHash", null) ?: return null
        try {
            val parts = positionStr.split(",")
            if (parts.size == 2) {
                return Pair(parts[0].toInt(), parts[1].toInt())
            }
        } catch (e: Exception) {
            // If parsing fails, return null
        }
        return null
    }

    fun getExpandedBubbleSize(): Int {
        return prefs.getInt(KEY_EXPANDED_BUBBLE_SIZE, DEFAULT_EXPANDED_BUBBLE_SIZE)
    }

    fun setExpandedBubbleSize(size: Int) {
        prefs.edit().putInt(KEY_EXPANDED_BUBBLE_SIZE, size).apply()
    }

    fun saveMainBubblePosition(x: Int, y: Int) {
        prefs.edit()
            .putInt(KEY_MAIN_BUBBLE_X, x)
            .putInt(KEY_MAIN_BUBBLE_Y, y)
            .apply()
    }

    fun getMainBubblePosition(): Pair<Int, Int> {
        val x = prefs.getInt(KEY_MAIN_BUBBLE_X, -1)
        val y = prefs.getInt(KEY_MAIN_BUBBLE_Y, -1)
        return Pair(x, y)
    }

    fun setLastBubblePosition(x: Int, y: Int) {
        // Implementation to save the position
        val editor = prefs.edit()
        editor.putInt(KEY_MAIN_BUBBLE_X, x)
        editor.putInt(KEY_MAIN_BUBBLE_Y, y)
        editor.apply()
    }
    
    /**
     * Bubble position (right or left side of screen)
     */
    fun isBubblePositionRight(): Boolean {
        return preferences.getBoolean("bubble_position_right", false)
    }
    
    fun setBubblePositionRight(isRight: Boolean) {
        preferences.edit().putBoolean("bubble_position_right", isRight).apply()
    }

    /**
     * Theme color settings
     */
    fun getThemeColor(): String {
        return preferences.getString(KEY_THEME_COLOR, DEFAULT_THEME_COLOR) ?: DEFAULT_THEME_COLOR
    }
    
    fun setThemeColor(colorName: String) {
        preferences.edit().putString(KEY_THEME_COLOR, colorName).apply()
    }
    
    /**
     * Get the primary color resource ID for the current theme
     */
    fun getCurrentThemePrimaryColorResId(): Int {
        val themeColor = com.qb.browser.ui.theme.ThemeColor.fromName(getThemeColor())
        return themeColor.primaryColorRes
    }
    
    /**
     * Get the primary dark color resource ID for the current theme
     */
    fun getCurrentThemePrimaryDarkColorResId(): Int {
        val themeColor = com.qb.browser.ui.theme.ThemeColor.fromName(getThemeColor())
        return themeColor.primaryDarkColorRes
    }
    
    /**
     * Get the accent color resource ID for the current theme
     */
    fun getCurrentThemeAccentColorResId(): Int {
        val themeColor = com.qb.browser.ui.theme.ThemeColor.fromName(getThemeColor())
        return themeColor.accentColorRes
    }
}