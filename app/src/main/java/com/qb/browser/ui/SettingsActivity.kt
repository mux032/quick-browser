package com.qb.browser.ui

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.qb.browser.ui.base.BaseActivity
import com.qb.browser.R
import com.qb.browser.util.SettingsManager

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

    companion object {
        private const val TAG = "SettingsActivity"
        private const val THEME_MODE = "light" // Theme is always light
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
