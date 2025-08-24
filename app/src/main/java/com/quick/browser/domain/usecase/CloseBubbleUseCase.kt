package com.quick.browser.domain.usecase

import com.quick.browser.domain.model.Bubble
import com.quick.browser.domain.repository.BubbleRepository
import javax.inject.Inject

/**
 * Use case for closing/removing a browser bubble
 */
class CloseBubbleUseCase @Inject constructor(
    private val bubbleRepository: BubbleRepository
) {
    suspend operator fun invoke(bubble: Bubble) {
        bubbleRepository.deleteBubble(bubble)
    }
}