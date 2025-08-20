package com.quick.browser.data

import com.quick.browser.manager.ReadabilityExtractor
import com.quick.browser.model.SavedArticle
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing saved articles for offline reading
 */
class SavedArticleRepository(
    private val savedArticleDao: SavedArticleDao,
    private val readabilityExtractor: ReadabilityExtractor
) {
    
    fun getAllSavedArticles(): Flow<List<SavedArticle>> {
        return savedArticleDao.getAllSavedArticles()
    }
    
    suspend fun getSavedArticleByUrl(url: String): SavedArticle? {
        return savedArticleDao.getSavedArticleByUrl(url)
    }
    
    suspend fun saveArticle(url: String): Boolean {
        return try {
            // Extract content from URL
            val readableContent = readabilityExtractor.extractFromUrl(url) ?: return false
            
            // Create saved article entity
            val savedArticle = SavedArticle(
                url = url,
                title = readableContent.title,
                content = readableContent.content,
                byline = readableContent.byline,
                siteName = readableContent.siteName,
                publishDate = readableContent.publishDate,
                savedDate = System.currentTimeMillis(),
                excerpt = readableContent.excerpt
            )
            
            // Save to database
            savedArticleDao.insertSavedArticle(savedArticle)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun deleteSavedArticle(article: SavedArticle) {
        savedArticleDao.deleteSavedArticle(article)
    }
    
    suspend fun deleteSavedArticleByUrl(url: String) {
        savedArticleDao.deleteSavedArticleByUrl(url)
    }
    
    suspend fun isArticleSaved(url: String): Boolean {
        return savedArticleDao.isArticleSaved(url) > 0
    }
}