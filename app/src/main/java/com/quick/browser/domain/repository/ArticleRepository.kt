package com.quick.browser.domain.repository

import com.quick.browser.domain.model.SavedArticle
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing saved articles
 */
interface ArticleRepository {
    fun getAllSavedArticles(): Flow<List<SavedArticle>>
    suspend fun getSavedArticleByUrl(url: String): SavedArticle?
    suspend fun saveArticle(article: SavedArticle)
    suspend fun saveArticleByUrl(url: String): Boolean
    suspend fun saveOriginalPageAsArticle(url: String, title: String, content: String): Boolean
    suspend fun deleteArticle(article: SavedArticle)
    suspend fun deleteArticleByUrl(url: String)
    suspend fun isArticleSaved(url: String): Boolean
}