package com.qb.browser.ui.base

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.qb.browser.R
import com.qb.browser.ui.theme.ThemeColor
import com.qb.browser.manager.SettingsManager

/**
 * Base activity class that applies consistent theming across all activities
 */
open class BaseActivity : AppCompatActivity() {

    protected lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before super.onCreate
        applyAppTheme()
        
        super.onCreate(savedInstanceState)
        
        // Initialize settings manager
        settingsManager = SettingsManager.getInstance(this)
    }
    
    /**
     * Apply the app theme based on settings
     */
    protected fun applyAppTheme() {
        val settingsManager = SettingsManager.getInstance(this)
        
        // Apply theme color
        val themeColor = ThemeColor.fromName(settingsManager.getThemeColor())
        
        // We'll use the system's night mode setting instead of a separate theme
        // The Theme.QBrowser will automatically use the night variant when in dark mode
        val themeResId = R.style.Theme_QBrowser
        
        // Apply the theme
        setTheme(themeResId)
        
        // Apply status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, themeColor.primaryDarkColorRes)
        }
    }
}