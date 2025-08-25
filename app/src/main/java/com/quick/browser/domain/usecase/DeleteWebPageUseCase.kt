package com.quick.browser.domain.usecase

import com.quick.browser.domain.model.WebPage
import com.quick.browser.domain.repository.HistoryRepository
import javax.inject.Inject

/**
 * Use case for deleting a web page from history
 */
class DeleteWebPageUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(webPage: WebPage) {
        historyRepository.deletePage(webPage)
    }
}