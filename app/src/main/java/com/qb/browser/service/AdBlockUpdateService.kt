package com.qb.browser.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.qb.browser.manager.AdBlocker
import com.qb.browser.manager.SettingsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Service to handle updating ad blocking rules in the background
 */
@AndroidEntryPoint
class AdBlockUpdateService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    @Inject
    lateinit var adBlocker: AdBlocker

    @Inject
    lateinit var settingsManager: SettingsManager

    companion object {
        private const val TAG = "AdBlockUpdateService"
        private const val UPDATE_INTERVAL = 7 * 24 * 60 * 60 * 1000L // 7 days in milliseconds

        // URLs for ad block filter lists
        private const val EASYLIST_URL = "https://easylist.to/easylist/easylist.txt"
        private const val EASYPRIVACY_URL = "https://easylist.to/easylist/easyprivacy.txt"
    }

    override fun onCreate() {
        super.onCreate()
        // AdBlocker and SettingsManager are already injected
        // Start periodic update check
        scheduleUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle manual update request
        if (intent?.action == "com.qb.browser.UPDATE_FILTERS") {
            serviceScope.launch {
                updateFilters()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * Schedule periodic updates of ad blocking rules
     */
    private fun scheduleUpdates() {
        serviceScope.launch {
            while (true) {
                if (settingsManager.isAdBlockEnabled()) {
                    updateFilters()
                }
                delay(UPDATE_INTERVAL)
            }
        }
    }

    /**
     * Update ad blocking filters from online sources
     */
    private suspend fun updateFilters() {
        try {
            Log.d(TAG, "Updating ad blocking filters...")

            // Try to update from EasyList
            val success = adBlocker.updateRulesFromUrl(EASYLIST_URL)

            if (success) {
                Log.d(TAG, "Successfully updated ad blocking filters")
            } else {
                Log.e(TAG, "Failed to update ad blocking filters")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating ad blocking filters", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}