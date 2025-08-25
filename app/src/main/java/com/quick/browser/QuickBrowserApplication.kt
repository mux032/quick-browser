package com.quick.browser

import android.app.Application
import android.os.Build
import android.os.StrictMode
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.quick.browser.service.BubbleService
import com.quick.browser.service.SettingsService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Main application class for the Quick Browser
 *
 * This class serves as the entry point for the application and handles
 * global initialization tasks such as dependency injection setup,
 * theme configuration, and debug mode configuration.
 *
 * It also maintains a reference to the BubbleService for easy access
 * from other parts of the application.
 */
@HiltAndroidApp
class QuickBrowserApplication : Application() {

    @Inject
    lateinit var settingsService: SettingsService

    /**
     * Reference to the BubbleService
     *
     * This property holds a reference to the active BubbleService instance,
     * allowing other components to interact with the bubble functionality.
     */
    var bubbleService: BubbleService? = null

    companion object {
        private var isDebugBuild: Boolean? = null

        /**
         * Checks if the application is running in debug mode
         *
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

    /**
     * Called when the application is starting
     *
     * This method performs global initialization tasks including:
     * - Configuring StrictMode for debug builds
     * - Applying theme settings
     */
    override fun onCreate() {
        super.onCreate()

        // Configure StrictMode for detecting violations during development
        configureStrictMode()

        // Apply dynamic colors if enabled
        applyThemeSettings()
    }

    /**
     * Configure StrictMode to detect potential performance issues during development
     *
     * This method sets up StrictMode policies to help identify accidental disk or
     * network operations on the main thread, which can cause UI jank or ANRs.
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
     *
     * This method configures the application's theme based on user preferences,
     * including dynamic color support (Android 12+) and night mode settings.
     */
    fun applyThemeSettings() {
        // Apply dynamic colors if enabled and available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && settingsService.isDynamicColorEnabled()) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }

        // Apply night mode setting
        val nightMode = if (settingsService.isNightModeEnabled()) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}
