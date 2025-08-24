package com.quick.browser.service

import android.content.Intent
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.quick.browser.Constants
import com.quick.browser.QBApplication
import com.quick.browser.manager.*
import com.quick.browser.ui.bubble.BubbleIntentProcessor
import com.quick.browser.util.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

/**
 * BubbleService is the core service responsible for managing floating bubbles in the Quick Browser. It
 * coordinates between different managers to handle bubble lifecycle, positioning, and user
 * interactions.
 *
 * Key responsibilities:
 * - Service lifecycle management
 * - Intent processing
 * - Manager coordination
 * - State preservation
 */
@AndroidEntryPoint
class BubbleService : LifecycleService() {

    private val serviceJob = SupervisorJob()
    private lateinit var bubbleManager: BubbleManager
    private lateinit var notificationManager: BubbleNotificationManager
    private lateinit var bubbleDisplayManager: BubbleDisplayManager
    private lateinit var intentProcessor: BubbleIntentProcessor

    // Remove ViewModel usage from Service
    // private lateinit var bubbleViewModel: BubbleViewModel
    // private lateinit var webViewModel: WebViewModel

    @Inject
    lateinit var webPageDao: com.quick.browser.data.WebPageDao

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var adBlocker: AdBlocker

    @Inject
    lateinit var summarizationManager: SummarizationManager

    companion object {
        private const val TAG = "BubbleService"
        @Volatile
        private var isServiceRunning = false

        // Action constants
        @JvmStatic
        val ACTION_START_BUBBLE = Constants.ACTION_CREATE_BUBBLE
        @JvmStatic
        val ACTION_OPEN_URL = Constants.ACTION_OPEN_URL
        @JvmStatic
        val ACTION_CLOSE_BUBBLE = Constants.ACTION_CLOSE_BUBBLE
        @JvmStatic
        val ACTION_TOGGLE_BUBBLES = Constants.ACTION_TOGGLE_BUBBLES
        @JvmStatic
        val ACTION_ACTIVATE_BUBBLE = Constants.ACTION_ACTIVATE_BUBBLE

        // Extra keys
        const val EXTRA_X = "extra_x"
        const val EXTRA_Y = "extra_y"

        fun isRunning(): Boolean = isServiceRunning
    }

    override fun onCreate() {
        super.onCreate()
        Logger.d(TAG, "BubbleService onCreate()")

        try {
            isServiceRunning = true

            // Initialize managers
            val app = application as QBApplication
            app.bubbleService = this

            bubbleManager =
                BubbleManager(
                    context = this,
                    lifecycleScope = lifecycleScope
                )

            notificationManager = BubbleNotificationManager(this)

            bubbleDisplayManager = BubbleDisplayManager(
                context = this,
                bubbleManager = bubbleManager,
                lifecycleScope = lifecycleScope,
                settingsManager = settingsManager,
                adBlocker = adBlocker,
                summarizationManager = summarizationManager
            )

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
                Logger.e(TAG, "Could not show notification, but service will continue", e)
                // Service will still run, but might be killed by system in low memory situations
            }

            Logger.d(TAG, "Service initialized successfully")
        } catch (e: Exception) {
            Logger.e(TAG, "Error in onCreate", e)
            isServiceRunning = false
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Logger.d(TAG, "onStartCommand | Received intent: ${intent?.action}, data: ${intent?.extras}")
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
            Logger.e(TAG, "Error clearing service reference", e)
        }

        Logger.d(TAG, "BubbleService onDestroy()")
    }
}
