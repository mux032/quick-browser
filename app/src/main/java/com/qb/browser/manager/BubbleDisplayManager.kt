package com.qb.browser.manager

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.lifecycle.LifecycleCoroutineScope
import com.qb.browser.Constants
import com.qb.browser.model.Bubble
import com.qb.browser.service.BubbleService
import com.qb.browser.ui.BubbleView
import com.qb.browser.util.ErrorHandler
import com.qb.browser.viewmodel.BubbleViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * BubbleDisplayManager handles the display of bubbles on screen.
 * It observes the BubbleViewModel and creates, updates, or removes BubbleViews accordingly.
 */
class BubbleDisplayManager(
    private val context: Context,
    private val bubbleViewModel: BubbleViewModel,
    private val lifecycleScope: LifecycleCoroutineScope
) {
    private val TAG = "BubbleDisplayManager"
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val bubbleViews = mutableMapOf<String, BubbleView>()
    
    // Define window type constant based on API level
    private val WINDOW_TYPE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        WindowManager.LayoutParams.TYPE_PHONE
    }

    init {
        observeBubbles()
    }

    /**
     * Observes the bubbles StateFlow from BubbleViewModel and updates the UI accordingly.
     */
    private fun observeBubbles() {
        lifecycleScope.launch {
            bubbleViewModel.bubbles.collectLatest { bubbles ->
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
        // Track current bubble IDs
        val currentBubbleIds = bubbles.map { it.id }.toSet()
        
        // Remove bubbles that are no longer in the list
        bubbleViews.keys.toList().forEach { id ->
            if (id !in currentBubbleIds) removeBubbleView(id)
        }
        
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
    }

    /**
     * Adds a new bubble view to the window.
     */
    private fun addBubbleView(bubble: Bubble) {
        try {
            // Create bubble view
            val bubbleView = BubbleView(
                context = context,
                bubbleId = bubble.id,
                url = bubble.url
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
            
            // Load saved position if available
            bubbleView.loadSavedPosition()
        } catch (e: Exception) {
            Log.e(TAG, "Error adding bubble view for ${bubble.id}", e)
        }
    }

    /**
     * Updates the favicon of an existing bubble view.
     * Note: Currently only updates the favicon. Expand this method if more properties need updating.
     */
    private fun updateBubbleView(bubble: Bubble) {
        val bubbleView = bubbleViews[bubble.id] ?: return
        
        try {
            // Update favicon if available
            bubble.favicon?.let { bubbleView.updateFavicon(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating bubble favicon", e)
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
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WINDOW_TYPE,
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