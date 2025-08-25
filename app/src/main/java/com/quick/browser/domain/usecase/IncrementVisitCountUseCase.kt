package com.quick.browser.domain.usecase

import com.quick.browser.domain.error.DomainError
import com.quick.browser.domain.repository.HistoryRepository
import com.quick.browser.domain.result.Result
import javax.inject.Inject

/**
 * Use case for incrementing the visit count of a web page
 *
 * @param historyRepository The repository to increment the visit count in
 */
class IncrementVisitCountUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    /**
     * Increment the visit count of a web page
     *
     * @param url The URL of the web page
     * @return A Result indicating success or failure
     */
    suspend operator fun invoke(url: String): Result<Unit, DomainError> {
        return try {
            historyRepository.incrementVisitCount(url)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DomainError.DatabaseError("Failed to increment visit count", e))
        }
    }
}