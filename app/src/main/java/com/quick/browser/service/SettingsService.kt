package com.quick.browser.service

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import androidx.preference.PreferenceManager
import com.quick.browser.domain.service.IEncryptedPreferencesService
import com.quick.browser.domain.service.ISettingsService
import com.quick.browser.presentation.ui.theme.ThemeColor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Manages application settings and user preferences
 */
class SettingsService @Inject constructor(
    @ApplicationContext context: Context, 
    private val encryptedPrefs: IEncryptedPreferencesService
) : ISettingsService {

    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

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
        private const val KEY_THEME_COLOR = "pref_theme_color"
        private const val KEY_DYNAMIC_COLOR = "pref_dynamic_color"
        private const val KEY_NIGHT_MODE = "pref_night_mode"
        private const val KEY_LAST_SHARED_URL = "last_shared_url"
        private const val KEY_SHOW_URL_BAR = "pref_show_url_bar"
        private const val KEY_SAVED_ARTICLES_VIEW_STYLE = "pref_saved_articles_view_style"

        // Reader mode settings keys
        private const val KEY_READER_FONT_SIZE = "pref_reader_font_size"
        private const val KEY_READER_BACKGROUND = "pref_reader_background"
        private const val KEY_READER_TEXT_ALIGN = "pref_reader_text_align"

        // Default values
        private const val DEFAULT_TEXT_SIZE = 16
        private const val DEFAULT_BUBBLE_SIZE = 100 // Percentage of default size
        private const val DEFAULT_BUBBLE_OPACITY = 90 // Percentage of opacity
        private const val DEFAULT_EXPANDED_BUBBLE_SIZE = 64
        private const val DEFAULT_THEME_COLOR = "Blue"
        private const val DEFAULT_DYNAMIC_COLOR = true
        private const val DEFAULT_SHOW_URL_BAR = true
        private const val DEFAULT_SAVED_ARTICLES_VIEW_STYLE = "CARD"

        // Reader mode default values
        private const val DEFAULT_READER_FONT_SIZE = 18
        private const val DEFAULT_READER_BACKGROUND = "white"
        private const val DEFAULT_READER_TEXT_ALIGN = "left"

        // Font family options
        const val FONT_FAMILY_DEFAULT = "default"
        const val FONT_FAMILY_SERIF = "serif"
        const val FONT_FAMILY_SANS_SERIF = "sans-serif"
        const val FONT_FAMILY_MONOSPACE = "monospace"

        // Theme options
        const val THEME_LIGHT = 0
        const val THEME_DARK = 1
        const val THEME_SEPIA = 2

        // Reader mode background options
        const val READER_BG_WHITE = "white"
        const val READER_BG_SEPIA = "sepia"
        const val READER_BG_DARK = "dark"

        // Reader mode text alignment options
        const val READER_ALIGN_LEFT = "left"
        const val READER_ALIGN_CENTER = "center"
        const val READER_ALIGN_RIGHT = "right"
        const val READER_ALIGN_JUSTIFY = "justify"

        // Accessibility settings
        private const val KEY_AUTO_FONT_SIZE = "pref_auto_font_size"
        private const val KEY_MANUAL_FONT_SIZE = "pref_manual_font_size"
        private const val DEFAULT_MANUAL_FONT_SIZE = 16


    }

    /**
     * Dark theme settings
     */
    override fun isDarkThemeEnabled(): Boolean {
        return preferences.getBoolean(KEY_DARK_THEME, false)
    }

    override fun setDarkThemeEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_DARK_THEME, enabled).apply()

        // If dark theme is enabled, make sure sepia is disabled
        if (enabled && isSepiaThemeEnabled()) {
            setSepiaThemeEnabled(false)
        }
    }

    /**
     * Sepia theme settings
     */
    override fun isSepiaThemeEnabled(): Boolean {
        return preferences.getBoolean(KEY_SEPIA_THEME, false)
    }

    override fun setSepiaThemeEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SEPIA_THEME, enabled).apply()

        // If sepia theme is enabled, make sure dark is disabled
        if (enabled && isDarkThemeEnabled()) {
            setDarkThemeEnabled(false)
        }
    }

    /**
     * Get the current theme as an integer value
     */
    override fun getCurrentTheme(): Int {
        return when {
            isDarkThemeEnabled() -> THEME_DARK
            isSepiaThemeEnabled() -> THEME_SEPIA
            else -> THEME_LIGHT
        }
    }

    /**
     * Set the current theme using an integer value
     */
    override fun setCurrentTheme(theme: Int) {
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
    override fun getTextSize(): Int {
        return preferences.getInt(KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE)
    }

    override fun setTextSize(size: Int) {
        preferences.edit().putInt(KEY_TEXT_SIZE, size).apply()
    }

    /**
     * Font family settings
     */
    override fun getFontFamily(): String {
        return preferences.getString(KEY_FONT_FAMILY, FONT_FAMILY_DEFAULT) ?: FONT_FAMILY_DEFAULT
    }

    override fun setFontFamily(fontFamily: String) {
        preferences.edit().putString(KEY_FONT_FAMILY, fontFamily).apply()
    }

    /**
     * Get the Typeface for the current font family
     */
    override fun getTypeface(): Typeface {
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
    override fun getBubbleSize(): Float {
        return preferences.getInt(KEY_BUBBLE_SIZE, DEFAULT_BUBBLE_SIZE).toFloat() / 100f
    }

    override fun setBubbleSize(size: Float) {
        val sizeInt = (size * 100).toInt()
        preferences.edit().putInt(KEY_BUBBLE_SIZE, sizeInt).apply()
    }

    /**
     * Bubble opacity settings
     */
    override fun getBubbleOpacity(): Int {
        return preferences.getInt(KEY_BUBBLE_OPACITY, DEFAULT_BUBBLE_OPACITY)
    }

    override fun setBubbleOpacity(opacity: Int) {
        preferences.edit().putInt(KEY_BUBBLE_OPACITY, opacity).apply()
    }

    /**
     * Save bubble position settings
     */
    override fun getSavePosition(): Boolean {
        return preferences.getBoolean(KEY_SAVE_POSITION, true)
    }

    override fun setSavePosition(save: Boolean) {
        preferences.edit().putBoolean(KEY_SAVE_POSITION, save).apply()
    }

    /**
     * Ad blocking settings
     */
    override fun isAdBlockEnabled(): Boolean {
        return preferences.getBoolean(KEY_AD_BLOCKING, true)
    }

    override fun setAdBlockEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AD_BLOCKING, enabled).apply()
    }

    /**
     * JavaScript settings
     */
    override fun isJavaScriptEnabled(): Boolean {
        return preferences.getBoolean(KEY_JAVASCRIPT, true)
    }

    override fun setJavaScriptEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_JAVASCRIPT, enabled).apply()
    }

    /**
     * Save browsing history settings
     */
    override fun isSaveHistoryEnabled(): Boolean {
        return preferences.getBoolean(KEY_SAVE_HISTORY, true)
    }

    override fun setSaveHistoryEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SAVE_HISTORY, enabled).apply()
    }

    /**
     * Encrypt saved pages settings
     */
    override fun isEncryptionEnabled(): Boolean {
        return preferences.getBoolean(KEY_ENCRYPT_PAGES, false)
    }

    override fun setEncryptionEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ENCRYPT_PAGES, enabled).apply()
    }

    /**
     * Check if secure mode is enabled
     *
     * Secure mode combines several security settings to provide enhanced protection
     *
     * @return Boolean indicating if secure mode is enabled
     */
    override fun isSecureMode(): Boolean {
        // Consider secure mode enabled if encryption is enabled or JavaScript is disabled
        return isEncryptionEnabled() || !isJavaScriptEnabled()
    }

    /**
     * Page snapshot settings
     */
    override fun isPageSnapshotEnabled(): Boolean {
        return preferences.getBoolean(KEY_PAGE_SNAPSHOT, true)
    }

    override fun setPageSnapshotEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_PAGE_SNAPSHOT, enabled).apply()
    }

    /**
     * Background sync settings
     */
    override fun isBackgroundSyncEnabled(): Boolean {
        return preferences.getBoolean(KEY_BACKGROUND_SYNC, false)
    }

    override fun setBackgroundSyncEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_BACKGROUND_SYNC, enabled).apply()
    }

    /**
     * Theme color settings
     */
    override fun getThemeColor(): String {
        return preferences.getString(KEY_THEME_COLOR, DEFAULT_THEME_COLOR) ?: DEFAULT_THEME_COLOR
    }

    override fun setThemeColor(colorName: String) {
        preferences.edit().putString(KEY_THEME_COLOR, colorName).apply()
    }

    /**
     * Get the primary color resource ID for the current theme
     */
    override fun getCurrentThemePrimaryColorResId(): Int {
        val themeColor = ThemeColor.fromName(getThemeColor())
        return themeColor.primaryColorRes
    }

    /**
     * Get the primary dark color resource ID for the current theme
     */
    override fun getCurrentThemePrimaryDarkColorResId(): Int {
        val themeColor = ThemeColor.fromName(getThemeColor())
        return themeColor.primaryDarkColorRes
    }

    /**
     * Get the accent color resource ID for the current theme
     */
    override fun getCurrentThemeAccentColorResId(): Int {
        val themeColor = ThemeColor.fromName(getThemeColor())
        return themeColor.accentColorRes
    }

    /**
     * Dynamic color settings
     */
    override fun isDynamicColorEnabled(): Boolean {
        return preferences.getBoolean(KEY_DYNAMIC_COLOR, DEFAULT_DYNAMIC_COLOR)
    }

    override fun setDynamicColorEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
    }

    /**
     * Night mode settings
     */
    override fun isNightModeEnabled(): Boolean {
        return preferences.getBoolean(KEY_NIGHT_MODE, false)
    }

    override fun setNightModeEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_NIGHT_MODE, enabled).apply()
    }

    /**
     * Get the saved articles view style
     */
    override fun getSavedArticlesViewStyle(): String {
        return preferences.getString(KEY_SAVED_ARTICLES_VIEW_STYLE, DEFAULT_SAVED_ARTICLES_VIEW_STYLE) ?: DEFAULT_SAVED_ARTICLES_VIEW_STYLE
    }

    /**
     * Set the saved articles view style
     */
    override fun setSavedArticlesViewStyle(style: String) {
        preferences.edit().putString(KEY_SAVED_ARTICLES_VIEW_STYLE, style).apply()
    }

    /**
     * Save the last shared URL (used when permission is requested during link sharing)
     */
    override fun saveLastSharedUrl(url: String) {
        encryptedPrefs.putString(KEY_LAST_SHARED_URL, url)
    }

    /**
     * Get the last shared URL
     */
    override fun getLastSharedUrl(): String? {
        return encryptedPrefs.getString(KEY_LAST_SHARED_URL, null)
    }

    /**
     * Clear the last shared URL
     */
    override fun clearLastSharedUrl() {
        encryptedPrefs.remove(KEY_LAST_SHARED_URL)
    }

    // ============== READER MODE SETTINGS ==============

    /**
     * Reader mode font size settings
     */
    override fun getReaderFontSize(): Int {
        return preferences.getInt(KEY_READER_FONT_SIZE, DEFAULT_READER_FONT_SIZE)
    }

    override fun setReaderFontSize(size: Int) {
        preferences.edit().putInt(KEY_READER_FONT_SIZE, size).apply()
    }

    /**
     * Reader mode background color settings
     */
    override fun getReaderBackground(): String {
        return preferences.getString(KEY_READER_BACKGROUND, DEFAULT_READER_BACKGROUND) ?: DEFAULT_READER_BACKGROUND
    }

    override fun setReaderBackground(background: String) {
        preferences.edit().putString(KEY_READER_BACKGROUND, background).apply()
    }

    /**
     * Reader mode text alignment settings
     */
    override fun getReaderTextAlign(): String {
        return preferences.getString(KEY_READER_TEXT_ALIGN, DEFAULT_READER_TEXT_ALIGN) ?: DEFAULT_READER_TEXT_ALIGN
    }

    override fun setReaderTextAlign(alignment: String) {
        preferences.edit().putString(KEY_READER_TEXT_ALIGN, alignment).apply()
    }

    // ============== ACCESSIBILITY SETTINGS ==============

    /**
     * Automatic font size settings
     */
    override fun isAutoFontSizeEnabled(): Boolean {
        return preferences.getBoolean(KEY_AUTO_FONT_SIZE, true)
    }

    override fun setAutoFontSizeEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_FONT_SIZE, enabled).apply()
    }

    /**
     * Manual font size settings
     */
    override fun getManualFontSize(): Int {
        return preferences.getInt(KEY_MANUAL_FONT_SIZE, DEFAULT_MANUAL_FONT_SIZE)
    }

    override fun setManualFontSize(size: Int) {
        preferences.edit().putInt(KEY_MANUAL_FONT_SIZE, size).apply()
    }

    /**
     * Check if URL bar should be visible
     */
    override fun isUrlBarVisible(): Boolean {
        return preferences.getBoolean(KEY_SHOW_URL_BAR, DEFAULT_SHOW_URL_BAR)
    }

    /**
     * Set URL bar visibility
     */
    override fun setUrlBarVisible(visible: Boolean) {
        preferences.edit().putBoolean(KEY_SHOW_URL_BAR, visible).apply()
    }
}