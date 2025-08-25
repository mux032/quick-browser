package com.quick.browser.domain.usecase

import androidx.lifecycle.LiveData
import com.quick.browser.domain.model.WebPage
import com.quick.browser.domain.repository.HistoryRepository
import javax.inject.Inject

/**
 * Use case for getting browsing history
 *
 * This use case provides access to the complete browsing history by delegating
 * to the history repository.
 *
 * @param historyRepository The repository to get history from
 */
class GetHistoryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    /**
     * Get all web pages in browsing history
     *
     * @return LiveData containing a list of all web pages in history
     */
    operator fun invoke(): LiveData<List<WebPage>> {
        return historyRepository.getAllPages()
    }
}