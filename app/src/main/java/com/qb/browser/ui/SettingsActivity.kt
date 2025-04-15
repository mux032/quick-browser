package com.qb.browser.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import com.qb.browser.R
import com.qb.browser.model.Settings
import com.qb.browser.viewmodel.BubbleViewModel
import com.qb.browser.settings.SettingsManager  // Add this import

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var bubbleViewModel: BubbleViewModel
    private lateinit var settings: BubbleViewModel.BubbleSettings  // Changed from Settings to BubbleViewModel.BubbleSettings
    private lateinit var settingsManager: SettingsManager
    
    // UI Components
    private lateinit var switchJavaScript: Switch
    private lateinit var switchBlockAds: Switch
    private lateinit var switchNightMode: Switch
    private lateinit var seekBarBubbleSize: SeekBar
    private lateinit var seekBarAnimSpeed: SeekBar
    private lateinit var textViewBubbleSize: TextView
    private lateinit var textViewAnimSpeed: TextView
    private lateinit var switchSaveHistory: Switch
    private lateinit var switchEncryptData: Switch
    private lateinit var switchPositionRight: Switch
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)
        
        // Initialize ViewModel
        bubbleViewModel = ViewModelProvider(this)[BubbleViewModel::class.java]
        
        settingsManager = SettingsManager.getInstance(this)
        
        // Initialize UI Components
        initializeViews()
        
        // Load settings
        loadSettings()
        
        // Setup listeners
        setupListeners()
        
        // Setup expanded bubble size slider
        val expandedBubbleSizeSlider = findViewById<SeekBar>(R.id.expanded_bubble_size_slider)
        expandedBubbleSizeSlider.progress = settingsManager.getExpandedBubbleSize()
        expandedBubbleSizeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    settingsManager.setExpandedBubbleSize(progress)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    private fun initializeViews() {
        switchJavaScript = findViewById(R.id.switch_javascript)
        switchBlockAds = findViewById(R.id.switch_block_ads)
        switchNightMode = findViewById(R.id.switch_night_mode)
        seekBarBubbleSize = findViewById(R.id.seekbar_bubble_size)
        seekBarAnimSpeed = findViewById(R.id.seekbar_anim_speed)
        textViewBubbleSize = findViewById(R.id.text_bubble_size_value)
        textViewAnimSpeed = findViewById(R.id.text_anim_speed_value)
        switchSaveHistory = findViewById(R.id.switch_save_history)
        switchEncryptData = findViewById(R.id.switch_encrypt_data)
        switchPositionRight = findViewById(R.id.switch_position_right)
    }
    
    private fun loadSettings() {
        bubbleViewModel.getSettings().observe(this) { savedSettings ->
            if (savedSettings != null) {
                settings = savedSettings
                
                // Update UI with saved settings
                switchJavaScript.isChecked = settings.javascriptEnabled
                switchBlockAds.isChecked = settings.blockAds
                switchNightMode.isChecked = settings.darkTheme
                
                // Convert string sizes to progress values
                val sizeValue = when(settings.size) {
                    "small" -> 0.5f
                    "medium" -> 0.75f
                    "large" -> 1.0f
                    "extra_large" -> 1.25f
                    else -> 0.75f
                }
                seekBarBubbleSize.progress = ((sizeValue - 0.5f) * 100).toInt()
                
                val speedValue = when(settings.animationSpeed) {
                    "slow" -> 0.5f
                    "medium" -> 0.75f
                    "fast" -> 1.0f
                    "very_fast" -> 1.25f
                    else -> 0.75f
                }
                seekBarAnimSpeed.progress = ((speedValue - 0.5f) * 100).toInt()
                
                switchSaveHistory.isChecked = settings.savePositions
                
                // Apply night mode setting
                applyNightMode(settings.darkTheme)
            } else {
                // Create default settings if none exist
                settings = BubbleViewModel.BubbleSettings()
                bubbleViewModel.saveSettings(settings)
            }
        }
    }

    private fun updateSettings(
        size: String = settings.size,
        animationSpeed: String = settings.animationSpeed,
        savePositions: Boolean = settings.savePositions,
        blockAds: Boolean = settings.blockAds,
        defaultColor: String = settings.defaultColor,
        javascriptEnabled: Boolean = settings.javascriptEnabled,
        darkTheme: Boolean = settings.darkTheme
    ) {
        val updatedSettings = BubbleViewModel.BubbleSettings(
            size = size,
            animationSpeed = animationSpeed,
            savePositions = savePositions,
            blockAds = blockAds,
            defaultColor = defaultColor,
            javascriptEnabled = javascriptEnabled,
            darkTheme = darkTheme
        )
        settings = updatedSettings
        bubbleViewModel.saveSettings(updatedSettings)
    }

    private fun setupListeners() {
        switchJavaScript.setOnCheckedChangeListener { _, isChecked ->
            updateSettings(javascriptEnabled = isChecked)
        }
        
        switchBlockAds.setOnCheckedChangeListener { _, isChecked ->
            updateSettings(blockAds = isChecked)
        }
        
        switchNightMode.setOnCheckedChangeListener { _, isChecked ->
            updateSettings(darkTheme = isChecked)
            applyNightMode(isChecked)
        }
        
        seekBarBubbleSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val size = 0.5f + (progress / 100f)
                    updateBubbleSizeText(size)
                    val sizeString = when {
                        size < 0.7f -> "small"
                        size < 0.9f -> "medium"
                        size < 1.1f -> "large"
                        else -> "extra_large"
                    }
                    updateSettings(size = sizeString)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        seekBarAnimSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val speed = 0.5f + (progress / 100f)
                    updateAnimSpeedText(speed)
                    val speedString = when {
                        speed < 0.7f -> "slow"
                        speed < 0.9f -> "medium"
                        speed < 1.1f -> "fast"
                        else -> "very_fast"
                    }
                    updateSettings(animationSpeed = speedString)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        switchSaveHistory.setOnCheckedChangeListener { _, isChecked ->
            updateSettings(savePositions = isChecked)
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
    
    private fun applyNightMode(isNightMode: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (isNightMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
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
