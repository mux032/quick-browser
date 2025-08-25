package com.quick.browser.domain.usecase

import com.quick.browser.domain.repository.HistoryRepository
import javax.inject.Inject

/**
 * Use case for deleting all web pages from history
 */
class DeleteAllPagesUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke() {
        historyRepository.deleteAllPages()
    }
}