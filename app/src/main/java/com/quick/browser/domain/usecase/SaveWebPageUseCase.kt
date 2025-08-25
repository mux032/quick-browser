package com.quick.browser.domain.usecase

import com.quick.browser.domain.model.WebPage
import com.quick.browser.domain.repository.HistoryRepository
import javax.inject.Inject

/**
 * Use case for saving a web page to history
 */
class SaveWebPageUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(webPage: WebPage) {
        historyRepository.savePage(webPage)
    }
}