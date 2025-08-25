package com.quick.browser.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.quick.browser.data.local.dao.WebPageDao
import com.quick.browser.data.local.entity.WebPage
import com.quick.browser.domain.repository.HistoryRepository
import javax.inject.Inject

/**
 * Repository implementation for managing web page history
 *
 * This class implements the HistoryRepository interface and provides concrete
 * implementations for accessing and modifying web page history in the database.
 *
 * @param webPageDao The DAO for accessing web pages in the database
 */
class HistoryRepositoryImpl @Inject constructor(
    private val webPageDao: WebPageDao
) : HistoryRepository {
    
    /**
     * Get all web pages in history
     *
     * @return LiveData containing a list of all web pages in history
     */
    override fun getAllPages(): LiveData<List<com.quick.browser.domain.model.WebPage>> {
        return webPageDao.getAllPages().map { list ->
            list.map { entityToDomain(it) }
        }
    }
    
    /**
     * Get a web page by its URL
     *
     * @param url The URL of the web page to retrieve
     * @return The web page or null if not found
     */
    override suspend fun getPageByUrl(url: String): com.quick.browser.domain.model.WebPage? {
        val entity = webPageDao.getPageByUrl(url)
        return entity?.let { entityToDomain(it) }
    }
    
    /**
     * Save a web page to history
     *
     * @param page The web page to save
     */
    override suspend fun savePage(page: com.quick.browser.domain.model.WebPage) {
        webPageDao.insertPage(domainToEntity(page))
    }
    
    /**
     * Delete a web page from history
     *
     * @param page The web page to delete
     */
    override suspend fun deletePage(page: com.quick.browser.domain.model.WebPage) {
        webPageDao.deletePage(domainToEntity(page))
    }
    
    /**
     * Delete all web pages from history
     */
    override suspend fun deleteAllPages() {
        webPageDao.deleteAllPages()
    }
    
    /**
     * Search web pages by query
     *
     * @param query The search query
     * @return LiveData containing a list of matching web pages
     */
    override fun searchPages(query: String): LiveData<List<com.quick.browser.domain.model.WebPage>> {
        return webPageDao.searchPages(query).map { list ->
            list.map { entityToDomain(it) }
        }
    }
    
    /**
     * Get recent web pages
     *
     * @param limit The maximum number of pages to return
     * @return LiveData containing a list of recent web pages
     */
    override fun getRecentPages(limit: Int): LiveData<List<com.quick.browser.domain.model.WebPage>> {
        return webPageDao.getRecentPages(limit).map { list ->
            list.map { entityToDomain(it) }
        }
    }
    
    /**
     * Get most visited web pages
     *
     * @param limit The maximum number of pages to return
     * @return LiveData containing a list of most visited web pages
     */
    override fun getMostVisitedPages(limit: Int): LiveData<List<com.quick.browser.domain.model.WebPage>> {
        return webPageDao.getMostVisitedPages(limit).map { list ->
            list.map { entityToDomain(it) }
        }
    }
    
    /**
     * Increment the visit count for a web page
     *
     * @param url The URL of the web page to increment the visit count for
     */
    override suspend fun incrementVisitCount(url: String) {
        webPageDao.incrementVisitCount(url)
    }
    
    /**
     * Update the offline availability status of a web page
     *
     * @param url The URL of the web page
     * @param isAvailable Whether the page is available offline
     */
    override suspend fun updateOfflineStatus(url: String, isAvailable: Boolean) {
        webPageDao.updateOfflineStatus(url, isAvailable)
    }
    
    /**
     * Convert a web page entity to a domain model
     *
     * @param entity The web page entity to convert
     * @return The domain model representation of the web page
     */
    private fun entityToDomain(entity: WebPage): com.quick.browser.domain.model.WebPage {
        return com.quick.browser.domain.model.WebPage(
            url = entity.url,
            title = entity.title,
            timestamp = entity.timestamp,
            content = entity.content,
            isAvailableOffline = entity.isAvailableOffline,
            visitCount = entity.visitCount,
            faviconUrl = entity.faviconUrl,
            previewImageUrl = entity.previewImageUrl,
            favicon = entity.favicon,
            summary = entity.summary,
            parentBubbleId = entity.parentBubbleId
        )
    }
    
    /**
     * Convert a domain model to a web page entity
     *
     * @param domain The domain model to convert
     * @return The entity representation of the web page
     */
    private fun domainToEntity(domain: com.quick.browser.domain.model.WebPage): WebPage {
        val entity = WebPage(
            url = domain.url,
            title = domain.title,
            timestamp = domain.timestamp,
            content = domain.content ?: "",
            isAvailableOffline = domain.isAvailableOffline,
            visitCount = domain.visitCount,
            faviconUrl = domain.faviconUrl,
            previewImageUrl = domain.previewImageUrl
        )
        // Copy transient fields
        entity.summary = domain.summary
        entity.parentBubbleId = domain.parentBubbleId
        return entity
    }
}