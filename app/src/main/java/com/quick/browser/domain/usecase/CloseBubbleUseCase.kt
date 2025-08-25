package com.quick.browser.domain.usecase

import com.quick.browser.domain.model.Bubble
import com.quick.browser.domain.repository.BubbleRepository
import javax.inject.Inject

/**
 * Use case for closing/removing a browser bubble
 *
 * This use case handles the removal of browser bubbles by delegating
 * to the bubble repository.
 *
 * @param bubbleRepository The repository to delete bubbles from
 */
class CloseBubbleUseCase @Inject constructor(
    private val bubbleRepository: BubbleRepository
) {
    /**
     * Close/remove a browser bubble
     *
     * @param bubble The bubble to close/remove
     */
    suspend operator fun invoke(bubble: Bubble) {
        bubbleRepository.deleteBubble(bubble)
    }
}