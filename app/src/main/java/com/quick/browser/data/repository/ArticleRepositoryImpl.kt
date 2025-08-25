package com.quick.browser.data.repository

import com.quick.browser.data.SavedArticleDao
import com.quick.browser.data.local.entity.SavedArticle
import com.quick.browser.domain.repository.ArticleRepository
import com.quick.browser.service.ReadabilityService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Repository implementation for managing saved articles
 *
 * This class implements the ArticleRepository interface and provides concrete
 * implementations for accessing and modifying saved articles in the database.
 * It also handles content extraction using the ReadabilityService.
 *
 * @param savedArticleDao The DAO for accessing saved articles in the database
 * @param readabilityService The service for extracting readable content from web pages
 */
class ArticleRepositoryImpl @Inject constructor(
    private val savedArticleDao: SavedArticleDao,
    private val readabilityService: ReadabilityService
) : ArticleRepository {
    
    /**
     * Get all saved articles as a flow
     *
     * @return A flow of lists of saved articles
     */
    override fun getAllSavedArticles(): Flow<List<com.quick.browser.domain.model.SavedArticle>> {
        return savedArticleDao.getAllSavedArticles().map { list ->
            list.map { entityToDomain(it) }
        }
    }
    
    /**
     * Get a saved article by its URL
     *
     * @param url The URL of the article to retrieve
     * @return The saved article or null if not found
     */
    override suspend fun getSavedArticleByUrl(url: String): com.quick.browser.domain.model.SavedArticle? {
        val entity = savedArticleDao.getSavedArticleByUrl(url)
        return entity?.let { entityToDomain(it) }
    }
    
    /**
     * Save an article
     *
     * @param article The article to save
     */
    override suspend fun saveArticle(article: com.quick.browser.domain.model.SavedArticle) {
        savedArticleDao.insertSavedArticle(domainToEntity(article))
    }
    
    /**
     * Save an article by extracting content from its URL
     *
     * @param url The URL of the article to save
     * @return True if the article was saved successfully, false otherwise
     */
    override suspend fun saveArticleByUrl(url: String): Boolean {
        return try {
            // Extract content from URL using ReadabilityExtractor
            val readableContent = readabilityService.extractFromUrl(url)
            
            if (readableContent != null) {
                // Create saved article with extracted content
                val savedArticle = com.quick.browser.domain.model.SavedArticle(
                    url = url,
                    title = readableContent.title,
                    content = readableContent.content,
                    savedDate = System.currentTimeMillis(),
                    author = readableContent.byline,
                    siteName = readableContent.siteName,
                    publishDate = readableContent.publishDate,
                    excerpt = readableContent.excerpt
                )
                
                // Save to database
                savedArticleDao.insertSavedArticle(domainToEntity(savedArticle))
                true
            } else {
                // Extraction failed, but we'll handle this in the caller
                false
            }
        } catch (e: Exception) {
            // Log the error but don't crash
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Save an original page as an article
     *
     * @param url The URL of the page
     * @param title The title of the page
     * @param content The content of the page
     * @return True if the article was saved successfully, false otherwise
     */
    override suspend fun saveOriginalPageAsArticle(url: String, title: String, content: String): Boolean {
        return try {
            // Create saved article with original page content
            val savedArticle = com.quick.browser.domain.model.SavedArticle(
                url = url,
                title = title.ifEmpty { url },
                content = content,
                savedDate = System.currentTimeMillis(),
                author = null,
                siteName = null,
                publishDate = null,
                excerpt = null
            )
            
            // Save to database
            savedArticleDao.insertSavedArticle(domainToEntity(savedArticle))
            true
        } catch (e: Exception) {
            // Log the error but don't crash
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Delete a saved article
     *
     * @param article The article to delete
     */
    override suspend fun deleteArticle(article: com.quick.browser.domain.model.SavedArticle) {
        savedArticleDao.deleteSavedArticle(domainToEntity(article))
    }
    
    /**
     * Delete a saved article by its URL
     *
     * @param url The URL of the article to delete
     */
    override suspend fun deleteArticleByUrl(url: String) {
        savedArticleDao.deleteSavedArticleByUrl(url)
    }
    
    /**
     * Check if an article is saved
     *
     * @param url The URL of the article to check
     * @return True if the article is saved, false otherwise
     */
    override suspend fun isArticleSaved(url: String): Boolean {
        return savedArticleDao.isArticleSaved(url) > 0
    }
    
    /**
     * Convert a saved article entity to a domain model
     *
     * @param entity The saved article entity to convert
     * @return The domain model representation of the saved article
     */
    private fun entityToDomain(entity: SavedArticle): com.quick.browser.domain.model.SavedArticle {
        return com.quick.browser.domain.model.SavedArticle(
            url = entity.url,
            title = entity.title,
            content = entity.content,
            savedDate = entity.savedDate,
            author = entity.byline,
            siteName = entity.siteName,
            publishDate = entity.publishDate,
            excerpt = entity.excerpt
        )
    }
    
    /**
     * Convert a domain model to a saved article entity
     *
     * @param domain The domain model to convert
     * @return The entity representation of the saved article
     */
    private fun domainToEntity(domain: com.quick.browser.domain.model.SavedArticle): SavedArticle {
        return SavedArticle(
            url = domain.url,
            title = domain.title,
            content = domain.content,
            savedDate = domain.savedDate,
            byline = domain.author,
            siteName = domain.siteName,
            publishDate = domain.publishDate,
            excerpt = domain.excerpt
        )
    }
}