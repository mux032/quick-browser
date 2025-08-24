package com.quick.browser.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.quick.browser.data.local.dao.WebPageDao
import com.quick.browser.data.local.entity.WebPage
import com.quick.browser.domain.repository.HistoryRepository
import javax.inject.Inject

/**
 * Repository implementation for managing web page history
 */
class HistoryRepositoryImpl @Inject constructor(
    private val webPageDao: WebPageDao
) : HistoryRepository {
    
    override fun getAllPages(): LiveData<List<com.quick.browser.domain.model.WebPage>> {
        return webPageDao.getAllPages().map { list ->
            list.map { entityToDomain(it) }
        }
    }
    
    override suspend fun getPageByUrl(url: String): com.quick.browser.domain.model.WebPage? {
        val entity = webPageDao.getPageByUrl(url)
        return entity?.let { entityToDomain(it) }
    }
    
    override suspend fun savePage(page: com.quick.browser.domain.model.WebPage) {
        webPageDao.insertPage(domainToEntity(page))
    }
    
    override suspend fun deletePage(page: com.quick.browser.domain.model.WebPage) {
        webPageDao.deletePage(domainToEntity(page))
    }
    
    override suspend fun deleteAllPages() {
        webPageDao.deleteAllPages()
    }
    
    override fun searchPages(query: String): LiveData<List<com.quick.browser.domain.model.WebPage>> {
        return webPageDao.searchPages(query).map { list ->
            list.map { entityToDomain(it) }
        }
    }
    
    override fun getRecentPages(limit: Int): LiveData<List<com.quick.browser.domain.model.WebPage>> {
        return webPageDao.getRecentPages(limit).map { list ->
            list.map { entityToDomain(it) }
        }
    }
    
    override fun getMostVisitedPages(limit: Int): LiveData<List<com.quick.browser.domain.model.WebPage>> {
        return webPageDao.getMostVisitedPages(limit).map { list ->
            list.map { entityToDomain(it) }
        }
    }
    
    override suspend fun incrementVisitCount(url: String) {
        webPageDao.incrementVisitCount(url)
    }
    
    override suspend fun updateOfflineStatus(url: String, isAvailable: Boolean) {
        webPageDao.updateOfflineStatus(url, isAvailable)
    }
    
    private fun entityToDomain(entity: WebPage): com.quick.browser.domain.model.WebPage {
        return com.quick.browser.domain.model.WebPage(
            url = entity.url,
            title = entity.title,
            timestamp = entity.timestamp,
            content = entity.content,
            isAvailableOffline = entity.isAvailableOffline,
            visitCount = entity.visitCount,
            faviconUrl = entity.faviconUrl,
            previewImageUrl = entity.previewImageUrl
        )
    }
    
    private fun domainToEntity(domain: com.quick.browser.domain.model.WebPage): WebPage {
        return WebPage(
            url = domain.url,
            title = domain.title,
            timestamp = domain.timestamp,
            content = domain.content ?: "",
            isAvailableOffline = domain.isAvailableOffline,
            visitCount = domain.visitCount,
            favicon = null, // Not stored in domain model
            faviconUrl = domain.faviconUrl,
            previewImageUrl = domain.previewImageUrl
        )
    }
}