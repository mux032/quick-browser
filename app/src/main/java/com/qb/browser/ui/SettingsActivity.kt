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
            seekBarBubbleSize = findViewById(R.id.seekbar_bubble_size)
            seekBarAnimSpeed = findViewById(R.id.seekbar_anim_speed)
            expandedBubbleSizeSlider = findViewById(R.id.expanded_bubble_size_slider)
            textViewBubbleSize = findViewById(R.id.text_bubble_size_value)
            textViewAnimSpeed = findViewById(R.id.text_anim_speed_value)
            switchSaveHistory = findViewById(R.id.switch_save_history)
            switchPositionRight = findViewById(R.id.switch_position_right)
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

            // Set bubble size
            val bubbleSize = settingsManager.getBubbleSize()
            seekBarBubbleSize.progress = ((bubbleSize - 0.5f) * 100).toInt()
            updateBubbleSizeText(bubbleSize)

            // Set animation speed
            val animSpeed = settingsManager.getAnimationSpeed()
            seekBarAnimSpeed.progress = ((animSpeed - 0.5f) * 100).toInt()
            updateAnimSpeedText(animSpeed)

            // Set expanded bubble size
            expandedBubbleSizeSlider.progress = settingsManager.getExpandedBubbleSize()

            // Set other settings
            switchSaveHistory.isChecked = settingsManager.isSaveHistoryEnabled()
            switchPositionRight.isChecked = settingsManager.isBubblePositionRight()
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

            // Bubble size setting
            seekBarBubbleSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val size = 0.5f + (progress / 100f)
                        updateBubbleSizeText(size)
                        settingsManager.setBubbleSize(size)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            // Animation speed setting
            seekBarAnimSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val speed = 0.5f + (progress / 100f)
                        updateAnimSpeedText(speed)
                        settingsManager.setAnimationSpeed(speed)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            // Expanded bubble size setting
            expandedBubbleSizeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        settingsManager.setExpandedBubbleSize(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            // Save history setting
            switchSaveHistory.setOnCheckedChangeListener { _, isChecked ->
                settingsManager.setSaveHistoryEnabled(isChecked)
            }

            // Bubble position setting
            switchPositionRight.setOnCheckedChangeListener { _, isChecked ->
                settingsManager.setBubblePositionRight(isChecked)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up listeners", e)
            throw e
        }
    }

    private fun updateBubbleSizeText(size: Float) {
        val sizeText = when {
            size < 0.7f -> getString(R.string.size_small)
            size < 0.9f -> getString(R.string.size_medium)
            size < 1.1f -> getString(R.string.size_normal)
            size < 1.3f -> getString(R.string.size_large)
            else -> getString(R.string.size_extra_large)
        }
        textViewBubbleSize.text = sizeText
    }

    private fun updateAnimSpeedText(speed: Float) {
        val speedText = when {
            speed < 0.7f -> getString(R.string.speed_slow)
            speed < 0.9f -> getString(R.string.speed_medium)
            speed < 1.1f -> getString(R.string.speed_normal)
            speed < 1.3f -> getString(R.string.speed_fast)
            else -> getString(R.string.speed_very_fast)
        }
        textViewAnimSpeed.text = speedText
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
