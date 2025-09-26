package com.quick.browser.domain.service

import com.quick.browser.domain.model.SavedArticle
import com.quick.browser.domain.repository.ArticleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Service for handling article saving functionality
 * This service contains the business logic for saving articles without UI concerns
 */
class ArticleSavingService(
    private val repository: ArticleRepository
) {
    
    /**
     * Save an article for offline reading
     *
     * @param url The URL of the article to save
     * @param tagId The ID of the tag to save the article to (0 for no tag)
     * @param scope The coroutine scope to launch the save operation
     * @param onSuccess Callback when save is successful
     * @param onError Callback when save fails
     * @return true if the operation was initiated, false if article already exists
     */
    suspend fun saveArticleForOfflineReading(
        url: String,
        tagId: Long = 0, // 0 means no tag
        scope: CoroutineScope,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Check if article is already saved
                if (repository.isArticleSaved(url)) {
                    withContext(Dispatchers.Main) {
                        onError("Article already saved")
                    }
                    return@withContext false
                }
                
                // Attempt to save the article by extracting readable content
                val success = repository.saveArticleByUrl(url, tagId)
                
                withContext(Dispatchers.Main) {
                    if (success) {
                        onSuccess()
                    } else {
                        onError("Article extraction failed")
                    }
                }
                
                success
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Failed to save article")
                }
                false
            }
        }
    }
    
    /**
     * Save the original web page content as an article if extraction fails
     *
     * @param url The URL of the page to save
     * @param title The title of the page
     * @param content The HTML content of the page
     * @param scope The coroutine scope to launch the save operation
     * @param onSuccess Callback when save is successful
     * @param onError Callback when save fails
     * @return true if the operation was initiated, false if article already exists
     */
    suspend fun saveOriginalPageAsArticle(
        url: String,
        title: String,
        content: String,
        scope: CoroutineScope,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Check if article is already saved
                if (repository.isArticleSaved(url)) {
                    withContext(Dispatchers.Main) {
                        onError("Article already saved")
                    }
                    return@withContext false
                }
                
                // Save the original page content
                val success = repository.saveOriginalPageAsArticle(url, title, content)
                
                withContext(Dispatchers.Main) {
                    if (success) {
                        onSuccess()
                    } else {
                        onError("Failed to save original page")
                    }
                }
                
                success
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Failed to save original page")
                }
                false
            }
        }
    }
    
    /**
     * Save a SavedArticle directly to the repository
     *
     * @param article The article to save
     * @param scope The coroutine scope to launch the save operation
     * @param onSuccess Callback when save is successful
     * @param onError Callback when save fails
     */
    fun saveArticle(
        article: SavedArticle,
        scope: CoroutineScope,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                repository.saveArticle(article)
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Failed to save article")
                }
            }
        }
    }
    
    /**
     * Check if an article is already saved
     *
     * @param url The URL to check
     * @return true if the article is already saved, false otherwise
     */
    suspend fun isArticleSaved(url: String): Boolean {
        return repository.isArticleSaved(url)
    }
}