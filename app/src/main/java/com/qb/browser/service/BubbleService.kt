package com.qb.browser.service

import android.content.Intent
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.qb.browser.Constants
import com.qb.browser.QBApplication
import com.qb.browser.manager.BubbleDisplayManager
import com.qb.browser.manager.BubbleManager
import com.qb.browser.manager.BubbleNotificationManager
import com.qb.browser.ui.bubble.BubbleIntentProcessor
import com.qb.browser.data.AppDatabase
import com.qb.browser.data.WebPageDao
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * BubbleService is the core service responsible for managing floating bubbles in the QB Browser. It
 * coordinates between different managers to handle bubble lifecycle, positioning, and user
 * interactions.
 *
 * Key responsibilities:
 * - Service lifecycle management
 * - Intent processing
 * - Manager coordination
 * - State preservation
 */
class BubbleService : LifecycleService() {

    private val serviceJob = SupervisorJob()
    private lateinit var bubbleManager: BubbleManager
    private lateinit var notificationManager: BubbleNotificationManager
    private lateinit var bubbleDisplayManager: BubbleDisplayManager
    private lateinit var intentProcessor: BubbleIntentProcessor
    private lateinit var webPageDao: WebPageDao


    companion object {
        private const val TAG = "BubbleService"
        @Volatile private var isServiceRunning = false

        // Action constants
        @JvmStatic val ACTION_START_BUBBLE = Constants.ACTION_CREATE_BUBBLE
        @JvmStatic val ACTION_OPEN_URL = Constants.ACTION_OPEN_URL
        @JvmStatic val ACTION_CLOSE_BUBBLE = Constants.ACTION_CLOSE_BUBBLE
        @JvmStatic val ACTION_TOGGLE_BUBBLES = Constants.ACTION_TOGGLE_BUBBLES
        @JvmStatic val ACTION_ACTIVATE_BUBBLE = Constants.ACTION_ACTIVATE_BUBBLE

        // Extra keys
        const val EXTRA_X = "extra_x"
        const val EXTRA_Y = "extra_y"

        fun isRunning(): Boolean = isServiceRunning
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BubbleService onCreate()")

        try {
            isServiceRunning = true

            // Initialize managers
            val app = application as QBApplication
            // Set reference to this service in the application
            app.bubbleService = this
            
            bubbleManager =
                    BubbleManager(
                            context = this,
                            bubbleViewModel = app.bubbleViewModel,
                            webViewModel = app.webViewModel,
                            lifecycleScope = lifecycleScope
                    )

            notificationManager = BubbleNotificationManager(this)
            val database = AppDatabase.getInstance(this)
            webPageDao = database.webPageDao() // Inject WebPageDao from application context

            // Initialize BubbleDisplayManager to handle UI
            bubbleDisplayManager = BubbleDisplayManager(
                context = this,
                bubbleViewModel = app.bubbleViewModel,
                lifecycleScope = lifecycleScope
            )

            // Initialize BubbleIntentProcessor with necessary dependencies
            intentProcessor =
                    BubbleIntentProcessor(
                            context = this,
                            bubbleManager = bubbleManager,
                            webPageDao = webPageDao,
                            lifecycleScope = lifecycleScope
                    )
            // Start foreground service with notification
            try {
                startForeground(
                        BubbleNotificationManager.NOTIFICATION_ID,
                        notificationManager.createNotification()
                )
            } catch (e: Exception) {
                // If notification fails (e.g., permission not granted), log but continue
                Log.e(TAG, "Could not show notification, but service will continue", e)
                // Service will still run, but might be killed by system in low memory situations
            }

            Log.d(TAG, "Service initialized successfully")

            // Observe theme changes
            observeThemeChanges()

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            isServiceRunning = false
        }
    }

    private fun observeThemeChanges() {
        lifecycleScope.launch {
            val qbApp = application as QBApplication
            var lastKnownThemeMode = qbApp.currentThemeMode.value // Get initial value

            qbApp.currentThemeMode
                // .drop(1) // Optional: drop initial value if it's considered already applied or to avoid immediate refresh
                .collectLatest { newThemeMode ->
                    if (newThemeMode != lastKnownThemeMode) {
                        Log.d(TAG, "Theme changed from $lastKnownThemeMode to $newThemeMode. Refreshing bubbles.")
                        Log.d(TAG, "BubbleService: applyThemeChange will be called for mode: $newThemeMode") // Added Log
                        lastKnownThemeMode = newThemeMode
                        // Ensure bubbleDisplayManager is initialized before calling
                        if (::bubbleDisplayManager.isInitialized) {
                            bubbleDisplayManager.applyThemeChange()
                        }
                    }
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand | Received intent: ${intent?.action}, data: ${intent?.extras}")
        intent?.let { intentProcessor.processIntent(it) }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        bubbleDisplayManager.cleanup()
        bubbleManager.cleanup()
        serviceJob.cancel()
        
        // Remove reference from application
        try {
            val app = application as QBApplication
            if (app.bubbleService === this) {
                app.bubbleService = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing service reference", e)
        }
        
        Log.d(TAG, "BubbleService onDestroy()")
    }
}
