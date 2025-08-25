package com.quick.browser.domain.usecase

import androidx.lifecycle.LiveData
import com.quick.browser.domain.model.WebPage
import com.quick.browser.domain.repository.HistoryRepository
import javax.inject.Inject

/**
 * Use case for getting recent web pages
 */
class GetRecentPagesUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    operator fun invoke(limit: Int): LiveData<List<WebPage>> {
        return historyRepository.getRecentPages(limit)
    }
}