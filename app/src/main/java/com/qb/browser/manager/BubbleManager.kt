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
            Log.e(TAG, "createOrUpdateBubbleWithNewUrl | URL: $url")
            val currentBubbles = _bubbles.value.toMutableMap()
            val existingBubble = currentBubbles[SINGLE_BUBBLE_ID]

            Log.e(TAG, "createOrUpdateBubbleWithNewUrl | Existing bubble: $existingBubble")

            val timestamp = System.currentTimeMillis()
            val newWebPage = WebPage(
                url = url,
                title = url,
                timestamp = timestamp,
                content = "",
                isAvailableOffline = false,
                visitCount = 1
            )

            if (existingBubble == null) {
                // Create new bubble
                val newBubble = Bubble(
                    id = SINGLE_BUBBLE_ID,
                    url = url,
                    title = url,
                    tabs = listOf(newWebPage)
                )
                currentBubbles[SINGLE_BUBBLE_ID] = newBubble
                bubbleViewModel.addBubble(newBubble)
            } else {
                // Update existing bubble with new tab
                val updatedTabs = existingBubble.tabs + newWebPage
                val updatedBubble = existingBubble.copy(
                    url = url,
                    title = url,
                    tabs = updatedTabs
                )
                currentBubbles[SINGLE_BUBBLE_ID] = updatedBubble
                bubbleViewModel.updateBubble(updatedBubble)
            }

            _bubbles.value = currentBubbles
            webViewModel.loadUrl(SINGLE_BUBBLE_ID, url)
        }
    }

    fun removeBubble(bubbleId: String) {
        lifecycleScope.launch {
            _bubbles.value = emptyMap()
            bubbleViewModel.removeBubble(bubbleId)
            webViewModel.closeTab(bubbleId)
        }
    }

    fun removeBubble() {
        removeBubble(SINGLE_BUBBLE_ID)
    }

    fun cleanup() {
        removeBubble()
    }
}