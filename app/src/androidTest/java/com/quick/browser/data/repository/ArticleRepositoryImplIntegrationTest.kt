package com.quick.browser.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quick.browser.data.local.database.AppDatabase
import com.quick.browser.domain.model.SavedArticle
import com.quick.browser.service.ReadabilityService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArticleRepositoryImplIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var articleRepositoryImpl: ArticleRepositoryImpl

    @Before
    fun setup() {
        // Create an in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val readabilityService = ReadabilityService(context)
        articleRepositoryImpl = ArticleRepositoryImpl(database.savedArticleDao(), readabilityService)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun saveArticle_shouldSaveArticleToDatabase() = runBlocking {
        // Given
        val article = SavedArticle(
            url = "https://example.com",
            title = "Example Article",
            content = "This is an example article",
            savedDate = System.currentTimeMillis(),
            author = "Author Name",
            siteName = "Example Site",
            publishDate = "2023-01-01",
            excerpt = "This is an excerpt"
        )

        // When
        articleRepositoryImpl.saveArticle(article)

        // Then
        // Note: We're not testing the getAllArticles method here because it's not part of the ArticleRepository interface
        // Instead, we'll verify the article was saved by trying to retrieve it by URL
        val savedArticle = articleRepositoryImpl.getSavedArticleByUrl(article.url)
        assert(savedArticle != null)
        assert(savedArticle?.url == article.url)
        assert(savedArticle?.title == article.title)
        assert(savedArticle?.content == article.content)
        assert(savedArticle?.savedDate == article.savedDate)
        assert(savedArticle?.author == article.author)
        assert(savedArticle?.siteName == article.siteName)
        assert(savedArticle?.publishDate == article.publishDate)
        assert(savedArticle?.excerpt == article.excerpt)
    }

    @Test
    fun deleteArticle_shouldRemoveArticleFromDatabase() = runBlocking {
        // Given
        val article = SavedArticle(
            url = "https://example.com",
            title = "Example Article",
            content = "This is an example article",
            savedDate = System.currentTimeMillis(),
            author = "Author Name",
            siteName = "Example Site",
            publishDate = "2023-01-01",
            excerpt = "This is an excerpt"
        )

        // When
        articleRepositoryImpl.saveArticle(article)
        articleRepositoryImpl.deleteArticle(article)

        // Then
        // Note: We're not testing the getAllArticles method here because it's not part of the ArticleRepository interface
        // Instead, we'll verify the article was deleted by trying to retrieve it by URL
        val savedArticle = articleRepositoryImpl.getSavedArticleByUrl(article.url)
        assert(savedArticle == null)
    }

    @Test
    fun getSavedArticleByUrl_shouldReturnSavedArticle() = runBlocking {
        // Given
        val article1 = SavedArticle(
            url = "https://example.com/1",
            title = "Example Article 1",
            content = "This is an example article 1",
            savedDate = System.currentTimeMillis(),
            author = "Author Name",
            siteName = "Example Site",
            publishDate = "2023-01-01",
            excerpt = "This is an excerpt 1"
        )

        val article2 = SavedArticle(
            url = "https://example.com/2",
            title = "Example Article 2",
            content = "This is an example article 2",
            savedDate = System.currentTimeMillis(),
            author = "Author Name",
            siteName = "Example Site",
            publishDate = "2023-01-02",
            excerpt = "This is an excerpt 2"
        )

        // When
        articleRepositoryImpl.saveArticle(article1)
        articleRepositoryImpl.saveArticle(article2)

        // Then
        val savedArticle1 = articleRepositoryImpl.getSavedArticleByUrl(article1.url)
        val savedArticle2 = articleRepositoryImpl.getSavedArticleByUrl(article2.url)
        
        assert(savedArticle1 != null)
        assert(savedArticle1?.url == article1.url)
        
        assert(savedArticle2 != null)
        assert(savedArticle2?.url == article2.url)
    }
}