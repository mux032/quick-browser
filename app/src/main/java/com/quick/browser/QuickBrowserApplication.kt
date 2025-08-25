package com.quick.browser

import android.app.Application
import android.os.Build
import android.os.StrictMode
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.quick.browser.service.BubbleService
import com.quick.browser.utils.managers.SettingsManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class QuickBrowserApplication : Application() {

    @Inject
    lateinit var settingsManager: SettingsManager

    // Reference to the BubbleService
    var bubbleService: BubbleService? = null

    companion object {
        private var isDebugBuild: Boolean? = null

        /**
         * Checks if the application is running in debug mode
         * @return true if running in debug mode, false otherwise
         */
        fun isDebugBuild(): Boolean {
            if (isDebugBuild == null) {
                try {
                    // Try to get the BuildConfig.DEBUG value through reflection
                    val buildConfigClass = Class.forName("com.quick.browser.BuildConfig")
                    val debugField = buildConfigClass.getField("DEBUG")
                    isDebugBuild = debugField.getBoolean(null)
                } catch (e: Exception) {
                    // Default to false if we can't determine
                    isDebugBuild = false
                }
            }
            return isDebugBuild!!
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Configure StrictMode for detecting violations during development
        configureStrictMode()

        // Apply dynamic colors if enabled
        applyThemeSettings()
    }

    /**
     * Configure StrictMode to detect potential performance issues during development
     */
    private fun configureStrictMode() {
        if (isDebugBuild()) {
            // Thread policy for detecting network and disk operations on main thread
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build()
            )

            // VM policy for detecting leaked resources
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }
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
