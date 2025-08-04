package com.quick.browser.manager

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.quick.browser.model.Bubble
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * BubbleManager is responsible for managing the lifecycle and operations of browser bubbles. It
 * handles creation, updating, and removal of bubbles, as well as their state management.
 */
class BubbleManager(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope
) {
    private val _bubbles = MutableStateFlow<Map<String, Bubble>>(emptyMap())
    val bubbles: StateFlow<Map<String, Bubble>> = _bubbles

    companion object {
        private const val TAG = "BubbleManager"
    }

    fun createOrUpdateBubbleWithNewUrl(url: String, existingBubbleId: String? = null) {
        lifecycleScope.launch {
            Log.d(TAG, "Creating new bubble for URL: $url, existing bubble ID: $existingBubbleId")

            try {
                val currentBubbles = _bubbles.value.toMutableMap()

                // Use the provided bubble ID or generate a new UUID
                val bubbleId = existingBubbleId ?: UUID.randomUUID().toString()

                // Create new bubble with unique ID
                val newBubble = Bubble(
                    id = bubbleId,
                    url = url,
                    title = url,
                )

                // Add to current bubbles map
                currentBubbles[bubbleId] = newBubble

                // Update state
                _bubbles.value = currentBubbles

                Log.d(TAG, "Successfully created new bubble with ID: $bubbleId for URL: $url")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating bubble for URL: $url", e)
            }
        }
    }

    fun removeBubble(bubbleId: String) {
        lifecycleScope.launch {
            // Only remove the specified bubble, not all bubbles
            val currentBubbles = _bubbles.value.toMutableMap()
            currentBubbles.remove(bubbleId)
            _bubbles.value = currentBubbles

            Log.d(TAG, "Removed bubble with ID: $bubbleId")
        }
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