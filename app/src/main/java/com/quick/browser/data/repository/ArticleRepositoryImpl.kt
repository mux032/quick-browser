package com.quick.browser.data.repository

import com.quick.browser.data.local.dao.SavedArticleDao
import com.quick.browser.data.mapper.toDomain
import com.quick.browser.data.mapper.toEntity
import com.quick.browser.domain.model.SavedArticle
import com.quick.browser.domain.repository.ArticleRepository
import com.quick.browser.domain.repository.ArticleTagRepository
import com.quick.browser.service.ReadabilityService
import com.quick.browser.utils.Logger
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
    private val readabilityService: ReadabilityService,
    private val articleTagRepository: ArticleTagRepository
) : ArticleRepository {

    /**
     * Get all saved articles as a flow
     *
     * @return A flow of lists of saved articles
     */
    override fun getAllSavedArticles(): Flow<List<SavedArticle>> {
        return savedArticleDao.getAllSavedArticles().map { list ->
            list.map { it.toDomain() }
        }
    }

    /**
     * Get saved articles by tag ID as a flow
     *
     * @param tagId The ID of the tag to retrieve articles from
     * @return A flow of lists of saved articles with the specified tag
     */
    override fun getSavedArticlesByTagId(tagId: Long): Flow<List<SavedArticle>> {
        return savedArticleDao.getSavedArticlesByTagId(tagId).map { list ->
            list.map { it.toDomain() }
        }
    }

    /**
     * Search saved articles by title or content
     *
     * @param query The search query
     * @return A flow of lists of saved articles matching the query
     */
    override fun searchSavedArticles(query: String): Flow<List<SavedArticle>> {
        return savedArticleDao.searchSavedArticles(query).map { list ->
            list.map { it.toDomain() }
        }
    }

    /**
     * Get a saved article by its URL
     *
     * @param url The URL of the article to retrieve
     * @return The saved article or null if not found
     */
    override suspend fun getSavedArticleByUrl(url: String): SavedArticle? {
        val entity = savedArticleDao.getSavedArticleByUrl(url)
        return entity?.toDomain()
    }

    /**
     * Save an article
     *
     * @param article The article to save
     */
    override suspend fun saveArticle(article: SavedArticle) {
        savedArticleDao.insertSavedArticle(article.toEntity())
    }

    /**
     * Save an article by extracting content from its URL
     *
     * @param url The URL of the article to save
     * @param tagId The ID of the tag to save the article to (0 for no tag)
     * @return True if the article was saved successfully, false otherwise
     */
    override suspend fun saveArticleByUrl(url: String, tagId: Long): Boolean {
        return try {
            // Extract content from URL using ReadabilityExtractor
            val readableContent = readabilityService.extractFromUrl(url)

            if (readableContent != null) {
                // Create saved article with extracted content
                val savedArticle = SavedArticle(
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
                savedArticleDao.insertSavedArticle(savedArticle.toEntity())

                // Associate article with tag if a tag ID is provided
                if (tagId > 0) {
                    articleTagRepository.addTagToArticle(url, tagId)
                }

                true
            } else {
                // Extraction failed, but we'll handle this in the caller
                false
            }
        } catch (e: Exception) {
            // Log the error but don't crash
            Logger.e(TAG, "Failed to save article by URL", e)
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
            val savedArticle = SavedArticle(
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
            savedArticleDao.insertSavedArticle(savedArticle.toEntity())
            true
        } catch (e: Exception) {
            // Log the error but don't crash
            Logger.e(TAG, "Failed to save original page as article", e)
            false
        }
    }

    /**
     * Delete a saved article
     *
     * @param article The article to delete
     */
    override suspend fun deleteArticle(article: SavedArticle) {
        savedArticleDao.deleteSavedArticle(article.toEntity())
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

    companion object {
        private const val TAG = "ArticleRepositoryImpl"
    }
}