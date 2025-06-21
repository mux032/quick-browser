package com.qb.browser

import android.app.Application
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.color.DynamicColors
import com.qb.browser.data.AppDatabase
import com.qb.browser.data.SettingsDao
import com.qb.browser.manager.SettingsManager
import com.qb.browser.service.BubbleService
import com.qb.browser.viewmodel.BubbleViewModel
import com.qb.browser.viewmodel.WebViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class QBApplication : Application() {

    lateinit var settingsDao: SettingsDao
    lateinit var bubbleViewModelFactory: BubbleViewModelFactory

    // Optional: global ViewModels (avoid using if navigation-scoped is preferred)
    lateinit var bubbleViewModel: BubbleViewModel
    lateinit var webViewModel: WebViewModel
    
    // Reference to the BubbleService
    var bubbleService: BubbleService? = null

    // Theme mode state flow
    private val _currentThemeMode = MutableStateFlow<Int>(AppCompatDelegate.getDefaultNightMode()) // Initialize with system default or last known
    val currentThemeMode: StateFlow<Int> = _currentThemeMode.asStateFlow()


    override fun onCreate() {
        super.onCreate()

        // Room DB and DAO
        val database = AppDatabase.getInstance(this)
        settingsDao = database.settingsDao()

        // Factory
        bubbleViewModelFactory = BubbleViewModelFactory(settingsDao)

        // Optional global ViewModels (can be removed in favor of using ViewModelProviders)
        bubbleViewModel = BubbleViewModel(settingsDao)
        webViewModel = WebViewModel()
        
        // Apply dynamic colors if enabled
        applyThemeSettings()
    }
    
    /**
     * Apply theme settings including dynamic colors and night mode
     */
    fun applyThemeSettings() {
        val settingsManager = SettingsManager.getInstance(this)
        
        // Apply dynamic colors if enabled and available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && settingsManager.isDynamicColorEnabled()) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
        
        // Apply night mode setting using the new AppThemeMode
        val appThemeMode = settingsManager.getAppThemeMode()
        AppCompatDelegate.setDefaultNightMode(appThemeMode)
        _currentThemeMode.value = appThemeMode // Update the StateFlow
    }
}

class BubbleViewModelFactory(
    private val settingsDao: SettingsDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BubbleViewModel(settingsDao) as T
    }
}
