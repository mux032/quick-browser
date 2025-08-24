package com.quick.browser.domain.repository

import com.quick.browser.domain.model.Bubble

/**
 * Repository interface for managing browser bubbles
 */
interface BubbleRepository {
    suspend fun createBubble(bubble: Bubble): Bubble
    suspend fun updateBubble(bubble: Bubble): Bubble
    suspend fun getBubbleById(id: String): Bubble?
    suspend fun deleteBubble(bubble: Bubble)
    suspend fun getAllBubbles(): List<Bubble>
}