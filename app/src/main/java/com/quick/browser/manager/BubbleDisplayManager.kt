package com.quick.browser.manager

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.lifecycle.LifecycleCoroutineScope
import com.quick.browser.Constants
import com.quick.browser.model.Bubble
import com.quick.browser.service.BubbleService
import com.quick.browser.ui.bubble.BubbleView
import com.quick.browser.util.ErrorHandler
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * BubbleDisplayManager handles the display of bubbles on screen.
 * It observes the BubbleViewModel and creates, updates, or removes BubbleViews accordingly.
 */
class BubbleDisplayManager(
    private val context: Context,
    private val bubbleManager: BubbleManager,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val settingsManager: SettingsManager,
    private val adBlocker: AdBlocker,
    private val summarizationManager: SummarizationManager
) {
    private val TAG = "BubbleDisplayManager"
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val bubbleViews = mutableMapOf<String, BubbleView>()
    private var bubbles: List<Bubble> = emptyList()

    init {
        observeBubbles()
    }

    /**
     * Observes the bubbles StateFlow from BubbleViewModel and updates the UI accordingly.
     */
    private fun observeBubbles() {
        lifecycleScope.launch {
            bubbleManager.bubbles.collectLatest { bubbleMap ->
                val bubbles = bubbleMap.values.toList()
                Log.d(TAG, "Bubbles updated: ${bubbles.size}")
                updateBubbleViews(bubbles)
            }
        }
    }

    /**
     * Updates the bubble views based on the current list of bubbles.
     * Adds new bubbles, updates existing ones, and removes bubbles that are no longer present.
     */
    private fun updateBubbleViews(bubbles: List<Bubble>) {
        // Store the current list of bubbles for reference
        this.bubbles = bubbles

        // Track current bubble IDs
        val currentBubbleIds = bubbles.map { it.id }.toSet()

        // Remove bubbles that are no longer in the list
        val bubbleIdsToRemove = bubbleViews.keys.filter { it !in currentBubbleIds }
        bubbleIdsToRemove.forEach { removeBubbleView(it) }

        // Add or update bubbles
        bubbles.forEach { bubble ->
            if (bubble.id in bubbleViews) {
                // Update existing bubble
                updateBubbleView(bubble)
            } else {
                // Add new bubble
                addBubbleView(bubble)
            }
        }

        Log.d(TAG, "Updated bubble views: ${bubbleViews.size} views for ${bubbles.size} bubbles")
    }

    /**
     * Adds a new bubble view to the window.
     */
    private fun addBubbleView(bubble: Bubble) {
        ErrorHandler.handleExceptions(
            tag = TAG,
            errorMessage = "Error adding bubble view for ${bubble.id}",
            showError = true,
            context = context,
            block = {
                Log.d(TAG, "Adding bubble view: ${bubble.id} with URL: ${bubble.url}")

                // Create bubble view
                val bubbleView = BubbleView(
                    context = context,
                    bubbleId = bubble.id,
                    url = bubble.url,
                    settingsManager = settingsManager,
                    adBlocker = adBlocker,
                    summarizationManager = summarizationManager
                )

                // Set close listener
                bubbleView.setOnCloseListener {
                    // Remove the bubble view
                    removeBubbleView(bubble.id)

                    // Also notify the BubbleViewModel to remove the bubble
                    val intent = Intent(context, BubbleService::class.java).apply {
                        action = Constants.ACTION_CLOSE_BUBBLE
                        putExtra(Constants.EXTRA_BUBBLE_ID, bubble.id)
                    }
                    context.startService(intent)
                }

                // Create layout params
                val layoutParams = createLayoutParams()

                // Add view to window manager
                windowManager.addView(bubbleView, layoutParams)

                // Store reference to bubble view
                bubbleViews[bubble.id] = bubbleView

                Log.d(TAG, "Bubble view added successfully: ${bubble.id}")
            }
        )
    }

    /**
     * Updates an existing bubble view.
     */
    private fun updateBubbleView(bubble: Bubble) {
        val bubbleView = bubbleViews[bubble.id] ?: return

        // Update bubble view properties
        try {
            // Update favicon if available
            bubble.favicon?.let { bubbleView.updateFavicon(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating bubble view", e)
        }
    }

    /**
     * Removes a bubble view from the window.
     */
    private fun removeBubbleView(bubbleId: String) {
        try {
            val bubbleView = bubbleViews[bubbleId] ?: return

            // Remove view from window manager
            windowManager.removeView(bubbleView)

            // Remove reference to bubble view
            bubbleViews.remove(bubbleId)

            Log.d(TAG, "Bubble view removed: $bubbleId")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing bubble view", e)
        }
    }

    /**
     * Creates layout parameters for the bubble view.
     */
    private fun createLayoutParams(): WindowManager.LayoutParams {
        val type =
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }
    }

    /**
     * Cleans up all bubble views.
     */
    fun cleanup() {
        bubbleViews.keys.toList().forEach { removeBubbleView(it) }
    }
}