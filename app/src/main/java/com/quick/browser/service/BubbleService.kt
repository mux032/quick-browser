package com.quick.browser.service

import android.content.Intent
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.quick.browser.QuickBrowserApplication
import com.quick.browser.presentation.ui.browser.BubbleIntentProcessor
import com.quick.browser.presentation.ui.browser.OfflineArticleSaver
import com.quick.browser.utils.Constants
import com.quick.browser.utils.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

/**
 * BubbleService is the core service responsible for managing floating bubbles in the Quick Browser.
 *
 * This service coordinates between different managers to handle bubble lifecycle, positioning,
 * and user interactions. It runs in the foreground to ensure bubbles remain visible and responsive.
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
    lateinit var historyRepository: com.quick.browser.domain.repository.HistoryRepository

    @Inject
    lateinit var tagRepository: com.quick.browser.domain.repository.TagRepository

    @Inject
    lateinit var settingsService: SettingsService

    @Inject
    lateinit var adBlockingService: AdBlockingService

    @Inject
    lateinit var summarizationService: SummarizationService

    @Inject
    lateinit var offlineArticleSaver: OfflineArticleSaver

    /**
     * Create or update a bubble with a new URL
     *
     * @param url The URL to load in the bubble
     * @param existingBubbleId The ID of an existing bubble to update, or null to create a new bubble
     */
    fun createOrUpdateBubbleWithNewUrl(url: String, existingBubbleId: String? = null) {
        bubbleManager.createOrUpdateBubbleWithNewUrl(url, existingBubbleId)
    }

    /**
     * Remove a bubble
     *
     * @param bubbleId The ID of the bubble to remove
     */
    fun removeBubble(bubbleId: String) {
        bubbleManager.removeBubble(bubbleId)
    }

    /**
     * Get all bubbles
     *
     * @return A list of all bubbles
     */
    fun getAllBubbles() = bubbleManager.getAllBubbles()
    
    /**
     * Get a flow of all bubbles
     *
     * @return A flow of lists of bubbles
     */
    fun getBubblesFlow() = bubbleManager.bubbles

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

        /**
         * Check if the service is running
         *
         * @return True if the service is running, false otherwise
         */
        fun isRunning(): Boolean = isServiceRunning
    }

    /**
     * Called when the service is created
     */
    override fun onCreate() {
        super.onCreate()
        Logger.d(TAG, "BubbleService onCreate()")

        try {
            isServiceRunning = true

            // Initialize managers
            val app = application as QuickBrowserApplication
            app.bubbleService = this

            bubbleManager =
                BubbleManager(
                    context = this,
                    lifecycleScope = lifecycleScope
                )

            notificationManager = BubbleNotificationManager(this)

            bubbleDisplayManager = BubbleDisplayManager(
                context = this,
                bubbleService = this,
                lifecycleScope = lifecycleScope,
                settingsService = settingsService,
                adBlockingService = adBlockingService,
                summarizationService = summarizationService,
                offlineArticleSaver = offlineArticleSaver,
                tagRepository = tagRepository
            )

            intentProcessor =
                BubbleIntentProcessor(
                    context = this,
                    bubbleService = this,
                    historyRepository = historyRepository,
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

    /**
     * Called when the service receives a start command
     *
     * @param intent The intent that started the service
     * @param flags Additional data about the start request
     * @param startId A unique integer ID for this start request
     * @return The restart behavior of the service
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Logger.d(TAG, "onStartCommand | Received intent: ${intent?.action}, data: ${intent?.extras}")
        intent?.let { intentProcessor.processIntent(it) }
        return START_STICKY
    }

    /**
     * Called when the service is destroyed
     */
    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        bubbleDisplayManager.cleanup()
        bubbleManager.cleanup()
        serviceJob.cancel()

        // Remove reference from application
        try {
            val app = application as QuickBrowserApplication
            if (app.bubbleService === this) {
                app.bubbleService = null
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error clearing service reference", e)
        }

        Logger.d(TAG, "BubbleService onDestroy()")
    }
}
