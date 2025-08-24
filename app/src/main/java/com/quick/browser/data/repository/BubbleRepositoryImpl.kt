package com.quick.browser.data.repository

import com.quick.browser.domain.model.Bubble
import com.quick.browser.domain.repository.BubbleRepository
import javax.inject.Inject

/**
 * Repository implementation for managing browser bubbles
 */
class BubbleRepositoryImpl @Inject constructor() : BubbleRepository {
    
    // In a real implementation, this would use a database or other persistence mechanism
    // For now, we'll use an in-memory store to demonstrate the pattern
    private val bubbleStore = mutableMapOf<String, Bubble>()
    
    override suspend fun createBubble(bubble: Bubble): Bubble {
        bubbleStore[bubble.id] = bubble
        return bubble
    }
    
    override suspend fun updateBubble(bubble: Bubble): Bubble {
        bubbleStore[bubble.id] = bubble
        return bubble
    }
    
    override suspend fun getBubbleById(id: String): Bubble? {
        return bubbleStore[id]
    }
    
    override suspend fun deleteBubble(bubble: Bubble) {
        bubbleStore.remove(bubble.id)
    }
    
    override suspend fun getAllBubbles(): List<Bubble> {
        return bubbleStore.values.toList()
    }
}