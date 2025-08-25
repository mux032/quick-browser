package com.quick.browser.data.repository

import com.quick.browser.domain.model.Bubble
import com.quick.browser.domain.repository.BubbleRepository
import javax.inject.Inject

/**
 * Repository implementation for managing browser bubbles
 *
 * This class implements the BubbleRepository interface and provides concrete
 * implementations for accessing and modifying browser bubbles.
 * Note: This is a simplified in-memory implementation for demonstration purposes.
 * In a real application, this would use a database or other persistence mechanism.
 */
class BubbleRepositoryImpl @Inject constructor() : BubbleRepository {
    
    // In a real implementation, this would use a database or other persistence mechanism
    // For now, we'll use an in-memory store to demonstrate the pattern
    private val bubbleStore = mutableMapOf<String, Bubble>()
    
    /**
     * Create a new bubble
     *
     * @param bubble The bubble to create
     * @return The created bubble
     */
    override suspend fun createBubble(bubble: Bubble): Bubble {
        bubbleStore[bubble.id] = bubble
        return bubble
    }
    
    /**
     * Update an existing bubble
     *
     * @param bubble The bubble to update
     * @return The updated bubble
     */
    override suspend fun updateBubble(bubble: Bubble): Bubble {
        bubbleStore[bubble.id] = bubble
        return bubble
    }
    
    /**
     * Get a bubble by its ID
     *
     * @param id The ID of the bubble to retrieve
     * @return The bubble or null if not found
     */
    override suspend fun getBubbleById(id: String): Bubble? {
        return bubbleStore[id]
    }
    
    /**
     * Delete a bubble
     *
     * @param bubble The bubble to delete
     */
    override suspend fun deleteBubble(bubble: Bubble) {
        bubbleStore.remove(bubble.id)
    }
    
    /**
     * Get all bubbles
     *
     * @return A list of all bubbles
     */
    override suspend fun getAllBubbles(): List<Bubble> {
        return bubbleStore.values.toList()
    }
}