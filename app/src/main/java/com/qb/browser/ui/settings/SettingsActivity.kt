package com.qb.browser.ui.settings

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import android.os.Build // Import Build
import com.qb.browser.ui.base.BaseActivity
import com.qb.browser.R
import com.qb.browser.manager.SettingsManager

/**
 * Settings activity for the browser
 * Theme is now always light and theme settings are removed.
 */
class SettingsActivity : BaseActivity() {

    // UI Components
    private lateinit var switchJavaScript: Switch
    private lateinit var switchBlockAds: Switch
    private lateinit var seekBarBubbleSize: SeekBar
    private lateinit var seekBarAnimSpeed: SeekBar
    private lateinit var expandedBubbleSizeSlider: SeekBar
    private lateinit var textViewBubbleSize: TextView
    private lateinit var textViewAnimSpeed: TextView
    private lateinit var switchSaveHistory: Switch
    private lateinit var switchPositionRight: Switch
    private lateinit var spinnerTheme: android.widget.Spinner
    private lateinit var dynamicColorsLayout: android.widget.LinearLayout // Added for dynamic colors
    private lateinit var switchDynamicColors: com.google.android.material.switchmaterial.SwitchMaterial // Added for dynamic colors

    companion object {
        private const val TAG = "SettingsActivity"
        // private const val THEME_MODE = "light" // Theme is always light - This is being changed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        try {
            // Set up toolbar
            setSupportActionBar(findViewById(R.id.toolbar))
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = getString(R.string.settings)

            // Initialize UI Components
            initializeViews()

            // Load settings
            loadSettings()

            // Setup listeners
            setupListeners()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing settings activity", e)
            Toast.makeText(this, "Error loading settings", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializeViews() {
        try {
            switchJavaScript = findViewById(R.id.switch_javascript)
            switchBlockAds = findViewById(R.id.switch_block_ads)
            switchSaveHistory = findViewById(R.id.switch_save_history)
            spinnerTheme = findViewById(R.id.spinner_theme)
            dynamicColorsLayout = findViewById(R.id.dynamic_colors_layout)
            switchDynamicColors = findViewById(R.id.switch_dynamic_colors)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views", e)
            throw e
        }
    }

    private fun loadSettings() {
        try {
            // Load settings from SettingsManager
            switchJavaScript.isChecked = settingsManager.isJavaScriptEnabled()
            switchBlockAds.isChecked = settingsManager.isAdBlockEnabled()
            switchSaveHistory.isChecked = settingsManager.isSaveHistoryEnabled()

            // Setup Theme Spinner
            val themeEntries = resources.getStringArray(R.array.theme_options_entries)
            val themeValues = resources.getStringArray(R.array.theme_options_values)
            val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, themeEntries)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerTheme.adapter = adapter

            val currentThemeMode = settingsManager.getAppThemeMode()
            val currentThemeValue = when (currentThemeMode) {
                SettingsManager.APP_THEME_MODE_LIGHT -> "mode_light"
                SettingsManager.APP_THEME_MODE_DARK -> "mode_dark"
                SettingsManager.APP_THEME_MODE_SYSTEM -> "mode_system"
                else -> "mode_system" // Default to system
            }
            spinnerTheme.setSelection(themeValues.indexOf(currentThemeValue).coerceAtLeast(0))

            // Setup Dynamic Colors Switch
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicColorsLayout.visibility = android.view.View.VISIBLE
                switchDynamicColors.isChecked = settingsManager.isDynamicColorEnabled()
            } else {
                dynamicColorsLayout.visibility = android.view.View.GONE
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error loading settings", e)
            throw e
        }
    }

    private fun setupListeners() {
        try {
            // JavaScript setting
            switchJavaScript.setOnCheckedChangeListener { _, isChecked ->
                settingsManager.setJavaScriptEnabled(isChecked)
            }

            // Ad blocking setting
            switchBlockAds.setOnCheckedChangeListener { _, isChecked ->
                settingsManager.setAdBlockEnabled(isChecked)
            }

            // Save history setting
            switchSaveHistory.setOnCheckedChangeListener { _, isChecked ->
                settingsManager.setSaveHistoryEnabled(isChecked)
            }

            // Theme Spinner listener
            spinnerTheme.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    val themeValues = resources.getStringArray(R.array.theme_options_values)
                    val selectedThemeValue = themeValues[position]

                    val newMode = when (selectedThemeValue) {
                        "mode_light" -> SettingsManager.APP_THEME_MODE_LIGHT
                        "mode_dark" -> SettingsManager.APP_THEME_MODE_DARK
                        "mode_system" -> SettingsManager.APP_THEME_MODE_SYSTEM
                        else -> SettingsManager.APP_THEME_MODE_SYSTEM
                    }

                    if (settingsManager.getAppThemeMode() != newMode) {
                        settingsManager.setAppThemeMode(newMode)
                        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(newMode)
                        recreate() // Apply theme change immediately
                    }
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                    // No action needed
                }
            }

            // Dynamic Colors Switch listener
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                switchDynamicColors.setOnCheckedChangeListener { _, isChecked ->
                    settingsManager.setDynamicColorEnabled(isChecked)
                    Toast.makeText(this, "App restart required to apply dynamic color changes.", Toast.LENGTH_LONG).show()
                    // Note: For the change to take full effect, QBApplication.applyThemeSettings() needs to be
                    // re-evaluated, which typically happens on app restart.
                    // A recreate() here will only re-apply the current activity's theme,
                    // but won't trigger DynamicColors.applyToActivitiesIfAvailable() again.
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up listeners", e)
            throw e
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
