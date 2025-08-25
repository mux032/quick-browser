package com.quick.browser.domain.usecase

import com.quick.browser.domain.repository.HistoryRepository
import javax.inject.Inject

/**
 * Use case for updating the offline status of a web page
 */
class UpdateOfflineStatusUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(url: String, isAvailable: Boolean) {
        historyRepository.updateOfflineStatus(url, isAvailable)
    }
}