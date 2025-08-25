package com.quick.browser.domain.usecase

import androidx.lifecycle.LiveData
import com.quick.browser.domain.model.WebPage
import com.quick.browser.domain.repository.HistoryRepository
import javax.inject.Inject

/**
 * Use case for searching web page history
 */
class SearchHistoryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    operator fun invoke(query: String): LiveData<List<WebPage>> {
        return historyRepository.searchPages(query)
    }
}