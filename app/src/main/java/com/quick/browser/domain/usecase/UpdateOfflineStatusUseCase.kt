package com.quick.browser.domain.usecase

import com.quick.browser.domain.error.DomainError
import com.quick.browser.domain.repository.HistoryRepository
import com.quick.browser.domain.result.Result
import javax.inject.Inject

/**
 * Use case for updating the offline status of a web page
 *
 * @param historyRepository The repository to update the offline status in
 */
class UpdateOfflineStatusUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    /**
     * Update the offline status of a web page
     *
     * @param url The URL of the web page
     * @param isAvailable Whether the page is available offline
     * @return A Result indicating success or failure
     */
    suspend operator fun invoke(url: String, isAvailable: Boolean): Result<Unit, DomainError> {
        return try {
            historyRepository.updateOfflineStatus(url, isAvailable)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DomainError.DatabaseError("Failed to update offline status", e))
        }
    }
}