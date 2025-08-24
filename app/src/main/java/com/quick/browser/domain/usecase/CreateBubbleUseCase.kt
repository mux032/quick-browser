package com.quick.browser.domain.usecase

import com.quick.browser.domain.model.Bubble
import com.quick.browser.domain.repository.BubbleRepository
import javax.inject.Inject

/**
 * Use case for creating a new browser bubble
 */
class CreateBubbleUseCase @Inject constructor(
    private val bubbleRepository: BubbleRepository
) {
    suspend operator fun invoke(bubble: Bubble): Bubble {
        return bubbleRepository.createBubble(bubble)
    }
}