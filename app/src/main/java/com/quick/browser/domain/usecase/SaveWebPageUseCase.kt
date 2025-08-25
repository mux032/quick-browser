package com.quick.browser.domain.usecase

import com.quick.browser.domain.error.DomainError
import com.quick.browser.domain.model.WebPage
import com.quick.browser.domain.repository.HistoryRepository
import com.quick.browser.domain.result.Result
import javax.inject.Inject

/**
 * Use case for saving a web page to history
 *
 * @param historyRepository The repository to save the web page to
 */
class SaveWebPageUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    /**
     * Save a web page to history
     *
     * @param webPage The web page to save
     * @return A Result indicating success or failure
     */
    suspend operator fun invoke(webPage: WebPage): Result<Unit, DomainError> {
        return try {
            historyRepository.savePage(webPage)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DomainError.DatabaseError("Failed to save web page", e))
        }
    }
}