package com.quick.browser.presentation.ui.settings

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.quick.browser.R
import com.quick.browser.databinding.ActivitySettingsBinding
import com.quick.browser.domain.service.ISettingsService
import com.quick.browser.utils.Logger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Settings activity for the browser
 * Theme is now always light and theme settings are removed.
 */
@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsService: ISettingsService

    private lateinit var binding: ActivitySettingsBinding

    companion object {
        private const val TAG = "SettingsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_QBrowser)
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimary)

        try {
            // Initialize toolbar
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(false) // Hide back button
            supportActionBar?.title = getString(R.string.settings)

            // Ensure toolbar sits below the status bar on all devices
            ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { v, insets ->
                val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                v.updatePadding(top = statusBarHeight)
                insets
            }

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

    private fun loadSettings() {
        try {
            // Load settings from SettingsService
            binding.switchJavascript.isChecked = settingsService.isJavaScriptEnabled()
            binding.switchBlockAds.isChecked = settingsService.isAdBlockEnabled()
            binding.switchSaveHistory.isChecked = settingsService.isSaveHistoryEnabled()
            binding.switchShowUrlBar.isChecked = settingsService.isUrlBarVisible()

            // Accessibility settings
            binding.switchAutoFontSize.isChecked = settingsService.isAutoFontSizeEnabled()
            val manualFontSize = settingsService.getManualFontSize()
            binding.seekbarFontSize.progress = manualFontSize
            binding.textFontSizePreview.text = getString(R.string.font_size_preview, manualFontSize)
            binding.textFontSizeSample.textSize = manualFontSize.toFloat()

            // Enable/disable manual font size controls based on auto font size setting
            binding.seekbarFontSize.isEnabled = !binding.switchAutoFontSize.isChecked
            binding.textFontSizePreview.isEnabled = !binding.switchAutoFontSize.isChecked
            binding.textFontSizeSample.isEnabled = !binding.switchAutoFontSize.isChecked
        } catch (e: Exception) {
            Logger.e(TAG, "Error loading settings", e)
            throw e
        }
    }

    private fun setupListeners() {
        try {
            // JavaScript setting
            binding.switchJavascript.setOnCheckedChangeListener { _, isChecked ->
                settingsService.setJavaScriptEnabled(isChecked)
            }

            // Ad blocking setting
            binding.switchBlockAds.setOnCheckedChangeListener { _, isChecked ->
                settingsService.setAdBlockEnabled(isChecked)
            }

            // Save history setting
            binding.switchSaveHistory.setOnCheckedChangeListener { _, isChecked ->
                settingsService.setSaveHistoryEnabled(isChecked)
            }

            // Show URL bar setting
            binding.switchShowUrlBar.setOnCheckedChangeListener { _, isChecked ->
                settingsService.setUrlBarVisible(isChecked)
            }

            // Accessibility settings
            binding.switchAutoFontSize.setOnCheckedChangeListener { _, isChecked ->
                settingsService.setAutoFontSizeEnabled(isChecked)
                // Enable/disable manual font size controls based on auto font size setting
                binding.seekbarFontSize.isEnabled = !isChecked
                binding.textFontSizePreview.isEnabled = !isChecked
                binding.textFontSizeSample.isEnabled = !isChecked
            }

            binding.seekbarFontSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (progress >= 8) {  // Minimum font size is 8
                        settingsService.setManualFontSize(progress)
                        binding.textFontSizePreview.text = getString(R.string.font_size_preview, progress)
                        binding.textFontSizeSample.textSize = progress.toFloat()
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