package com.quick.browser.data.repository

import com.quick.browser.data.SavedArticleDao
import com.quick.browser.data.local.entity.SavedArticle
import com.quick.browser.domain.repository.ArticleRepository
import com.quick.browser.manager.ReadabilityExtractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Repository implementation for managing saved articles
 */
class ArticleRepositoryImpl @Inject constructor(
    private val savedArticleDao: SavedArticleDao,
    private val readabilityExtractor: ReadabilityExtractor
) : ArticleRepository {
    
    override fun getAllSavedArticles(): Flow<List<com.quick.browser.domain.model.SavedArticle>> {
        return savedArticleDao.getAllSavedArticles().map { list ->
            list.map { entityToDomain(it) }
        }
    }
    
    override suspend fun getSavedArticleByUrl(url: String): com.quick.browser.domain.model.SavedArticle? {
        val entity = savedArticleDao.getSavedArticleByUrl(url)
        return entity?.let { entityToDomain(it) }
    }
    
    override suspend fun saveArticle(article: com.quick.browser.domain.model.SavedArticle) {
        savedArticleDao.insertSavedArticle(domainToEntity(article))
    }
    
    override suspend fun deleteArticle(article: com.quick.browser.domain.model.SavedArticle) {
        savedArticleDao.deleteSavedArticle(domainToEntity(article))
    }
    
    override suspend fun deleteArticleByUrl(url: String) {
        savedArticleDao.deleteSavedArticleByUrl(url)
    }
    
    override suspend fun isArticleSaved(url: String): Boolean {
        return savedArticleDao.isArticleSaved(url) > 0
    }
    
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