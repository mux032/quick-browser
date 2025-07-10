package com.qb.browser

import android.app.Application
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.qb.browser.manager.SettingsManager
import com.qb.browser.service.BubbleService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class QBApplication : Application() {

    @Inject
    lateinit var settingsManager: SettingsManager

    // Reference to the BubbleService
    var bubbleService: BubbleService? = null

    override fun onCreate() {
        super.onCreate()

        // Apply dynamic colors if enabled
        applyThemeSettings()
    }

    /**
     * Apply theme settings including dynamic colors and night mode
     */
    fun applyThemeSettings() {
        // Apply dynamic colors if enabled and available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && settingsManager.isDynamicColorEnabled()) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }

        // Apply night mode setting
        val nightMode = if (settingsManager.isNightModeEnabled()) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}
