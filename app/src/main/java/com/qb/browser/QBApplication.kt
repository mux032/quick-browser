package com.qb.browser

import android.app.Application
import com.qb.browser.db.AppDatabase
import com.qb.browser.db.SettingsDao
import com.qb.browser.viewmodel.BubbleViewModel
import com.qb.browser.viewmodel.WebViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel


class QBApplication : Application() {

    lateinit var settingsDao: SettingsDao
    lateinit var bubbleViewModelFactory: BubbleViewModelFactory

    // Optional: global ViewModels (avoid using if navigation-scoped is preferred)
    lateinit var bubbleViewModel: BubbleViewModel
    lateinit var webViewModel: WebViewModel

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
    }
}

class BubbleViewModelFactory(
    private val settingsDao: SettingsDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BubbleViewModel(settingsDao) as T
    }
}
