package com.qb.browser.manager

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.qb.browser.model.Bubble
import com.qb.browser.model.WebPage
import com.qb.browser.viewmodel.BubbleViewModel
import com.qb.browser.viewmodel.WebViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * BubbleManager is responsible for managing the lifecycle and operations of browser bubbles. It
 * handles creation, updating, and removal of bubbles, as well as their state management.
 */
class BubbleManager(
        private val context: Context,
        private val bubbleViewModel: BubbleViewModel,
        private val webViewModel: WebViewModel,
        private val lifecycleScope: LifecycleCoroutineScope
) {
    private val _bubbles = MutableStateFlow<Map<String, Bubble>>(emptyMap())
    val bubbles: StateFlow<Map<String, Bubble>> = _bubbles

    companion object {
        private const val TAG = "BubbleManager"
        private const val SINGLE_BUBBLE_ID = "single_bubble"
    }

    fun createOrUpdateBubbleWithNewUrl(url: String) {
        lifecycleScope.launch {
            Log.d(TAG, "createOrUpdateBubbleWithNewUrl | URL: $url")
            val currentBubbles = _bubbles.value.toMutableMap()
            
            // Generate a unique ID for the new bubble
            val bubbleId = "bubble_${System.currentTimeMillis()}"
            
            val timestamp = System.currentTimeMillis()
            val newWebPage = WebPage(
                url = url,
                title = url,
                timestamp = timestamp,
                content = "",
                isAvailableOffline = false,
                visitCount = 1
            )

            // Create new bubble with unique ID
            val newBubble = Bubble(
                id = bubbleId,
                url = url,
                title = url,
                tabs = listOf(newWebPage)
            )
            
            // Add to current bubbles map
            currentBubbles[bubbleId] = newBubble
            
            // Update ViewModel
            bubbleViewModel.addBubble(newBubble)
            
            // Update state
            _bubbles.value = currentBubbles
            
            // Load URL in WebView
            webViewModel.loadUrl(bubbleId, url)
            
            Log.d(TAG, "Created new bubble with ID: $bubbleId for URL: $url")
        }
    }

    fun removeBubble(bubbleId: String) {
        lifecycleScope.launch {
            // Only remove the specified bubble, not all bubbles
            val currentBubbles = _bubbles.value.toMutableMap()
            currentBubbles.remove(bubbleId)
            _bubbles.value = currentBubbles
            
            // Update ViewModel
            bubbleViewModel.removeBubble(bubbleId)
            
            // Close tab in WebView
            webViewModel.closeTab(bubbleId)
            
            Log.d(TAG, "Removed bubble with ID: $bubbleId")
        }
    }

    fun removeBubble() {
        // For backward compatibility
        removeBubble(SINGLE_BUBBLE_ID)
    }
    
    fun getAllBubbles(): List<Bubble> {
        return _bubbles.value.values.toList()
    }

    fun cleanup() {
        lifecycleScope.launch {
            // Get all bubble IDs
            val bubbleIds = _bubbles.value.keys.toList()
            
            // Remove each bubble
            bubbleIds.forEach { bubbleId ->
                removeBubble(bubbleId)
            }
            
            // Clear the bubbles map
            _bubbles.value = emptyMap()
            
            Log.d(TAG, "Cleaned up all bubbles")
        }
    }
}