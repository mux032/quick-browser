package com.quick.browser.presentation.ui.settings

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.switchmaterial.SwitchMaterial
import com.quick.browser.R
import com.quick.browser.presentation.ui.components.BaseActivity
import com.quick.browser.utils.Logger

/**
 * Settings activity for the browser
 * Theme is now always light and theme settings are removed.
 */
class SettingsActivity : BaseActivity() {

    // UI Components
    private lateinit var switchJavaScript: SwitchMaterial
    private lateinit var switchBlockAds: SwitchMaterial
    private lateinit var switchSaveHistory: SwitchMaterial
    private lateinit var switchShowUrlBar: SwitchMaterial
    private lateinit var switchAutoFontSize: SwitchMaterial
    private lateinit var seekbarFontSize: SeekBar
    private lateinit var textFontSizePreview: TextView
    private lateinit var textFontSizeSample: TextView
    private lateinit var layoutManualFontSize: android.widget.LinearLayout

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
            switchShowUrlBar = findViewById(R.id.switch_show_url_bar)
            switchAutoFontSize = findViewById(R.id.switch_auto_font_size)
            seekbarFontSize = findViewById(R.id.seekbar_font_size)
            textFontSizePreview = findViewById(R.id.text_font_size_preview)
            textFontSizeSample = findViewById(R.id.text_font_size_sample)
            layoutManualFontSize = findViewById(R.id.layout_manual_font_size)
        } catch (e: Exception) {
            Logger.e(TAG, "Error initializing views", e)
            throw e
        }
    }

    private fun loadSettings() {
        try {
            // Load settings from SettingsService
            switchJavaScript.isChecked = settingsService.isJavaScriptEnabled()
            switchBlockAds.isChecked = settingsService.isAdBlockEnabled()
            switchSaveHistory.isChecked = settingsService.isSaveHistoryEnabled()
            switchShowUrlBar.isChecked = settingsService.isUrlBarVisible()
            
            // Accessibility settings
            switchAutoFontSize.isChecked = settingsService.isAutoFontSizeEnabled()
            val manualFontSize = settingsService.getManualFontSize()
            seekbarFontSize.progress = manualFontSize
            textFontSizePreview.text = getString(R.string.font_size_preview, manualFontSize)
            textFontSizeSample.textSize = manualFontSize.toFloat()
            
            // Enable/disable manual font size controls based on auto font size setting
            seekbarFontSize.isEnabled = !switchAutoFontSize.isChecked
            textFontSizePreview.isEnabled = !switchAutoFontSize.isChecked
            textFontSizeSample.isEnabled = !switchAutoFontSize.isChecked
        } catch (e: Exception) {
            Logger.e(TAG, "Error loading settings", e)
            throw e
        }
    }

    private fun setupListeners() {
        try {
            // JavaScript setting
            switchJavaScript.setOnCheckedChangeListener { _, isChecked ->
                settingsService.setJavaScriptEnabled(isChecked)
            }

            // Ad blocking setting
            switchBlockAds.setOnCheckedChangeListener { _, isChecked ->
                settingsService.setAdBlockEnabled(isChecked)
            }

            // Save history setting
            switchSaveHistory.setOnCheckedChangeListener { _, isChecked ->
                settingsService.setSaveHistoryEnabled(isChecked)
            }

            // Show URL bar setting
            switchShowUrlBar.setOnCheckedChangeListener { _, isChecked ->
                settingsService.setUrlBarVisible(isChecked)
            }
            
            // Accessibility settings
            switchAutoFontSize.setOnCheckedChangeListener { _, isChecked ->
                settingsService.setAutoFontSizeEnabled(isChecked)
                // Enable/disable manual font size controls based on auto font size setting
                seekbarFontSize.isEnabled = !isChecked
                textFontSizePreview.isEnabled = !isChecked
                textFontSizeSample.isEnabled = !isChecked
            }
            
            seekbarFontSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (progress >= 8) {  // Minimum font size is 8
                        settingsService.setManualFontSize(progress)
                        textFontSizePreview.text = getString(R.string.font_size_preview, progress)
                        textFontSizeSample.textSize = progress.toFloat()
                    }
                }
                
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

        } catch (e: Exception) {
            Logger.e(TAG, "Error setting up listeners", e)
            throw e
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
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