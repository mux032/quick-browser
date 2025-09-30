package com.quick.browser.domain.service

import com.quick.browser.domain.model.Tag

interface ISettingsService {
    fun isDarkThemeEnabled(): Boolean
    fun setDarkThemeEnabled(enabled: Boolean)
    fun isSepiaThemeEnabled(): Boolean
    fun setSepiaThemeEnabled(enabled: Boolean)
    fun getCurrentTheme(): Int
    fun setCurrentTheme(theme: Int)
    fun getTextSize(): Int
    fun setTextSize(size: Int)
    fun getFontFamily(): String
    fun setFontFamily(fontFamily: String)
    fun getTypeface(): android.graphics.Typeface
    fun getBubbleSize(): Float
    fun setBubbleSize(size: Float)
    fun getBubbleOpacity(): Int
    fun setBubbleOpacity(opacity: Int)
    fun getSavePosition(): Boolean
    fun setSavePosition(save: Boolean)
    fun isAdBlockEnabled(): Boolean
    fun setAdBlockEnabled(enabled: Boolean)
    fun isJavaScriptEnabled(): Boolean
    fun setJavaScriptEnabled(enabled: Boolean)
    fun isSaveHistoryEnabled(): Boolean
    fun setSaveHistoryEnabled(enabled: Boolean)
    fun isEncryptionEnabled(): Boolean
    fun setEncryptionEnabled(enabled: Boolean)
    fun isSecureMode(): Boolean
    fun isPageSnapshotEnabled(): Boolean
    fun setPageSnapshotEnabled(enabled: Boolean)
    fun isBackgroundSyncEnabled(): Boolean
    fun setBackgroundSyncEnabled(enabled: Boolean)
    fun getThemeColor(): String
    fun setThemeColor(colorName: String)
    fun getCurrentThemePrimaryColorResId(): Int
    fun getCurrentThemePrimaryDarkColorResId(): Int
    fun getCurrentThemeAccentColorResId(): Int
    fun isDynamicColorEnabled(): Boolean
    fun setDynamicColorEnabled(enabled: Boolean)
    fun isNightModeEnabled(): Boolean
    fun setNightModeEnabled(enabled: Boolean)
    fun getSavedArticlesViewStyle(): String
    fun setSavedArticlesViewStyle(style: String)
    fun saveLastSharedUrl(url: String)
    fun getLastSharedUrl(): String?
    fun clearLastSharedUrl()
    fun getReaderFontSize(): Int
    fun setReaderFontSize(size: Int)
    fun getReaderBackground(): String
    fun setReaderBackground(background: String)
    fun getReaderTextAlign(): String
    fun setReaderTextAlign(alignment: String)
    fun isAutoFontSizeEnabled(): Boolean
    fun setAutoFontSizeEnabled(enabled: Boolean)
    fun getManualFontSize(): Int
    fun setManualFontSize(size: Int)
    fun isUrlBarVisible(): Boolean
    fun setUrlBarVisible(visible: Boolean)
}