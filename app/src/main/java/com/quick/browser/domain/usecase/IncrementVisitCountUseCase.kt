package com.quick.browser.domain.usecase

import com.quick.browser.domain.repository.HistoryRepository
import javax.inject.Inject

/**
 * Use case for incrementing the visit count of a web page
 */
class IncrementVisitCountUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(url: String) {
        historyRepository.incrementVisitCount(url)
    }
}