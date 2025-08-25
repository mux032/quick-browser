package com.quick.browser.domain.usecase

import com.quick.browser.domain.model.Bubble
import com.quick.browser.domain.repository.BubbleRepository
import javax.inject.Inject

/**
 * Use case for creating a new browser bubble
 *
 * This use case handles the creation of new browser bubbles by delegating
 * to the bubble repository.
 *
 * @param bubbleRepository The repository to create bubbles in
 */
class CreateBubbleUseCase @Inject constructor(
    private val bubbleRepository: BubbleRepository
) {
    /**
     * Create a new browser bubble
     *
     * @param bubble The bubble to create
     * @return The created bubble
     */
    suspend operator fun invoke(bubble: Bubble): Bubble {
        return bubbleRepository.createBubble(bubble)
    }
}