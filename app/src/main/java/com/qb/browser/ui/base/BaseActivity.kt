package com.qb.browser.ui.base

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.qb.browser.R
import com.qb.browser.ui.theme.ThemeColor
import com.qb.browser.manager.SettingsManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Base activity class that applies consistent theming across all activities
 */
@AndroidEntryPoint
open class BaseActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply theme after dependency injection
        applyAppTheme()
    }

    /**
     * Apply the app theme based on settings
     */
    protected fun applyAppTheme() {
        // Use the injected SettingsManager instance
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