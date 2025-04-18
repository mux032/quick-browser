package com.qb.browser.util

import android.content.Context
import android.content.Intent
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LifecycleCoroutineScope
import com.qb.browser.Constants
import com.qb.browser.db.WebPageDao
import com.qb.browser.manager.BubbleManager
import com.qb.browser.model.WebPage
import com.qb.browser.service.BubbleService
import kotlinx.coroutines.launch

/**
 * BubbleIntentProcessor handles the processing of intents received by the BubbleService. It
 * extracts URLs from various types of sharing intents and processes them accordingly.
 */
class BubbleIntentProcessor(
        private val context: Context,
        private val bubbleManager: BubbleManager,
        private val webPageDao: WebPageDao,
        private val lifecycleScope: LifecycleCoroutineScope
) {
    companion object {
        private const val TAG = "BubbleIntentProcessor"
    }

    fun processIntent(intent: Intent) {
        try {
            Log.e(TAG, "processIntent | Received intent: ${intent.action}, data: ${intent.extras}")
            when (intent.action) {
                Constants.ACTION_CREATE_BUBBLE -> handleCreateBubble(intent)
                Constants.ACTION_OPEN_URL -> handleOpenUrl(intent)
                Constants.ACTION_CLOSE_BUBBLE -> handleCloseBubble(intent)
                Constants.ACTION_TOGGLE_BUBBLES -> handleToggleBubbles()
                Constants.ACTION_ACTIVATE_BUBBLE -> handleActivateBubble(intent)
                Constants.ACTION_SAVE_OFFLINE -> handleSaveOffline(intent)
                Constants.ACTION_SAVE_POSITION -> handleSavePosition(intent)
                Intent.ACTION_SEND -> handleSharedContent(intent)
                Intent.ACTION_VIEW -> handleViewAction(intent)
                else -> Log.w(TAG, "Unsupported intent action: ${intent.action}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing intent", e)
        }
    }

    private fun handleCreateBubble(intent: Intent) {
        val sharedUrl = intent.getStringExtra(Constants.EXTRA_URL)
        Log.e(TAG, "handleCreateBubble | Received intent: ${intent.action}, sharedURL: ${sharedUrl}")
        if (sharedUrl != null && isValidUrl(sharedUrl)) {
            Log.e(TAG, "Creating buuble, Received intent: ${intent.action}, data: ${intent.extras}")
            bubbleManager.createOrUpdateBubbleWithNewUrl(sharedUrl)
        }
    }
    
    private fun handleOpenUrl(intent: Intent) {
        val url = intent.getStringExtra(Constants.EXTRA_URL)
        if (url != null && isValidUrl(url)) {
            bubbleManager.createOrUpdateBubbleWithNewUrl(url)
        } else {
            Log.w(TAG, "Invalid or missing URL for handleOpenUrl")
        }
    }

    private fun handleCloseBubble(intent: Intent) {
        val bubbleId = intent.getStringExtra(BubbleService.EXTRA_BUBBLE_ID)
        if (bubbleId != null) {
            bubbleManager.removeBubble(bubbleId)
        }
        Log.d(TAG, "Bubble closed via intent")
    }

    private fun handleActivateBubble(intent: Intent) {
        // Since there's only one bubble, we can simulate activation by re-loading the current URL.
        val currentBubbles = bubbleManager.bubbles.value
        val bubble = currentBubbles["single_bubble"]
        if (bubble != null) {
            bubbleManager.createOrUpdateBubbleWithNewUrl(bubble.url)
            Log.d(TAG, "Bubble activated with URL: ${bubble.url}")
        } else {
            Log.w(TAG, "No bubble found to activate")
        }
    }

    private fun handleToggleBubbles() {
        // Optional: implement show/hide behavior if needed. Here’s a basic toggle logic idea.
        val bubbles = bubbleManager.bubbles.value
        if (bubbles.isNotEmpty()) {
            bubbleManager.removeBubble()
            Log.d(TAG, "Bubbles hidden")
        } else {
            // Recreate last URL? Or just show empty state?
            Log.d(TAG, "Toggle bubbles requested, but no previous state to restore.")
        }
    }

    private fun handleSaveOffline(intent: Intent) {
        val url = intent.getStringExtra(Constants.EXTRA_URL)
        if (url != null && isValidUrl(url)) {
            // Save to DB for offline availability
            lifecycleScope.launch {
                val existingPage = webPageDao.getPageByUrl(url)
                val pageToSave =
                        existingPage?.copy(isAvailableOffline = true)
                                ?: WebPage(
                                        url = url,
                                        title = url,
                                        timestamp = System.currentTimeMillis(),
                                        content = "", // Can be filled with offline HTML content
                                        isAvailableOffline = true,
                                        visitCount = 1
                                )
                webPageDao.insertPage(pageToSave)
                Log.d(TAG, "Page saved for offline: $url")
            }
        } else {
            Log.w(TAG, "Invalid URL or null passed to handleSaveOffline")
        }
    }

    private fun handleSavePosition(intent: Intent) {
        val x = intent.getIntExtra(BubbleService.EXTRA_X, 0)
        val y = intent.getIntExtra(BubbleService.EXTRA_Y, 0)
        val settingsManager = SettingsManager.getInstance(context)
        settingsManager.setLastBubblePosition(x, y)
        Log.d(TAG, "Saved bubble position: x=$x, y=$y")
    }

    /**
     * Handles the ACTION_SEND intent to create or update a bubble with a new URL.
     * @param intent The incoming intent containing the shared content.
     */
    private fun handleSharedContent(intent: Intent) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        Log.d(TAG, "handleSharedContent | Received shared text: $sharedText")
        if (sharedText != null && isValidUrl(sharedText)) {
            bubbleManager.createOrUpdateBubbleWithNewUrl(sharedText)
            Log.d(TAG, "Opened shared URL in bubble: $sharedText")
        } else {
            Log.w(TAG, "Invalid or missing shared text for handleSharedContent")
        }
    }

    /**
     * Handles the ACTION_VIEW intent to create or update a bubble with a new URL.
     * @param intent The incoming intent containing the URL.
     */
    private fun handleViewAction(intent: Intent) {
        val data = intent.data
        if (data != null && isValidUrl(data.toString())) {
            bubbleManager.createOrUpdateBubbleWithNewUrl(data.toString())
        }
    }

    /**
     * Validates the URL format using Android's built-in Patterns class.
     * @param url The URL string to validate.
     * @return True if the URL is valid, false otherwise.
     */
    private fun isValidUrl(url: String): Boolean {
        // First try with Android's built-in pattern
        if (Patterns.WEB_URL.matcher(url).matches()) {
            return true
        }
        
        // If that fails, try a more lenient approach for URLs that might have special characters
        // or don't strictly match the pattern but are still valid URLs
        val lowerUrl = url.lowercase()
        return lowerUrl.startsWith("http://") || 
               lowerUrl.startsWith("https://") || 
               lowerUrl.startsWith("www.") ||
               lowerUrl.contains(".")
    }
}
