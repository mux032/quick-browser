package com.quick.browser.ui.settings

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.switchmaterial.SwitchMaterial
import com.quick.browser.R
import com.quick.browser.ui.base.BaseActivity
import com.quick.browser.util.Logger

/**
 * Settings activity for the browser
 * Theme is now always light and theme settings are removed.
 */
class SettingsActivity : BaseActivity() {

    // UI Components
    private lateinit var switchJavaScript: SwitchMaterial
    private lateinit var switchBlockAds: SwitchMaterial
    private lateinit var switchSaveHistory: SwitchMaterial

    companion object {
        private const val TAG = "SettingsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        try {
            // Set up toolbar
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = getString(R.string.settings)

            // Ensure toolbar sits below the status bar on all devices
            ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
                val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                v.updatePadding(top = statusBarHeight)
                insets
            }

            // Initialize UI Components
            initializeViews()

            // Load settings
            loadSettings()

            // Setup listeners
            setupListeners()
        } catch (e: Exception) {
            Logger.e(TAG, "Error initializing settings activity", e)
            Toast.makeText(this, "Error loading settings", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializeViews() {
        try {
            switchJavaScript = findViewById(R.id.switch_javascript)
            switchBlockAds = findViewById(R.id.switch_block_ads)
            switchSaveHistory = findViewById(R.id.switch_save_history)
        } catch (e: Exception) {
            Logger.e(TAG, "Error initializing views", e)
            throw e
        }
    }

    private fun loadSettings() {
        try {
            // Load settings from SettingsManager
            switchJavaScript.isChecked = settingsManager.isJavaScriptEnabled()
            switchBlockAds.isChecked = settingsManager.isAdBlockEnabled()
            switchSaveHistory.isChecked = settingsManager.isSaveHistoryEnabled()
        } catch (e: Exception) {
            Logger.e(TAG, "Error loading settings", e)
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

        } catch (e: Exception) {
            Logger.e(TAG, "Error setting up listeners", e)
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
