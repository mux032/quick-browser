package com.qb.browser.service

import android.content.Intent
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.qb.browser.Constants
import com.qb.browser.QBApplication
import com.qb.browser.manager.BubbleManager
import com.qb.browser.manager.BubbleNotificationManager
import com.qb.browser.manager.BubblePositionManager
import com.qb.browser.util.BubbleIntentProcessor
import com.qb.browser.db.WebPageDao
import com.qb.browser.db.AppDatabase
import kotlinx.coroutines.SupervisorJob

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
    private lateinit var positionManager: BubblePositionManager
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

        // Intent extras
        const val EXTRA_URL = Constants.EXTRA_URL
        const val EXTRA_BUBBLE_ID = Constants.EXTRA_BUBBLE_ID

        fun isRunning(): Boolean = isServiceRunning
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BubbleService onCreate()")

        try {
            isServiceRunning = true

            // Initialize managers
            val app = application as QBApplication
            bubbleManager =
                    BubbleManager(
                            context = this,
                            bubbleViewModel = app.bubbleViewModel,
                            webViewModel = app.webViewModel,
                            lifecycleScope = lifecycleScope
                    )

            notificationManager = BubbleNotificationManager(this)
            positionManager = BubblePositionManager(this)
            val database = AppDatabase.getInstance(this)
            val webPageDao = database.webPageDao() // Inject WebPageDao from application context

            // Initialize BubbleIntentProcessor with necessary dependencies
            intentProcessor =
                    BubbleIntentProcessor(
                            context = this,
                            bubbleManager = bubbleManager,
                            webPageDao = webPageDao,
                            lifecycleScope = lifecycleScope
                    )
            // Start foreground service
            startForeground(
                    BubbleNotificationManager.NOTIFICATION_ID,
                    notificationManager.createNotification()
            )

            Log.d(TAG, "Service initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            isServiceRunning = false
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
        bubbleManager.cleanup()
        serviceJob.cancel()
    }
}
