package com.quick.browser.domain.usecase

import androidx.lifecycle.LiveData
import com.quick.browser.domain.model.WebPage
import com.quick.browser.domain.repository.HistoryRepository
import javax.inject.Inject

/**
 * Use case for getting most visited web pages
 */
class GetMostVisitedPagesUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    operator fun invoke(limit: Int): LiveData<List<WebPage>> {
        return historyRepository.getMostVisitedPages(limit)
    }
}