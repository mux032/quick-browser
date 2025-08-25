package com.quick.browser.domain.repository

import com.quick.browser.domain.model.Bubble

/**
 * Repository interface for managing browser bubbles
 *
 * This interface defines the contract for accessing and modifying browser bubbles.
 * It provides methods to create, retrieve, update, and delete bubbles.
 */
interface BubbleRepository {
    /**
     * Create a new bubble
     *
     * @param bubble The bubble to create
     * @return The created bubble
     */
    suspend fun createBubble(bubble: Bubble): Bubble

    /**
     * Update an existing bubble
     *
     * @param bubble The bubble to update
     * @return The updated bubble
     */
    suspend fun updateBubble(bubble: Bubble): Bubble

    /**
     * Get a bubble by its ID
     *
     * @param id The ID of the bubble to retrieve
     * @return The bubble or null if not found
     */
    suspend fun getBubbleById(id: String): Bubble?

    /**
     * Delete a bubble
     *
     * @param bubble The bubble to delete
     */
    suspend fun deleteBubble(bubble: Bubble)

    /**
     * Get all bubbles
     *
     * @return A list of all bubbles
     */
    suspend fun getAllBubbles(): List<Bubble>
}