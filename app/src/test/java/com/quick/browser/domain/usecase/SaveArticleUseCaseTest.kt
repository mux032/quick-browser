package com.quick.browser.domain.usecase

import com.quick.browser.domain.model.SavedArticle
import com.quick.browser.domain.repository.ArticleRepository
import com.quick.browser.domain.result.Result
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class SaveArticleUseCaseTest {

    @Mock
    private lateinit var articleRepository: ArticleRepository

    private lateinit var saveArticleUseCase: SaveArticleUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        saveArticleUseCase = SaveArticleUseCase(articleRepository)
    }

    @Test
    fun `invoke should call repository to save article`() = runBlocking {
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
        
        `when`(articleRepository.saveArticle(article)).thenReturn(Unit)

        // When
        val result = saveArticleUseCase(article)

        // Then
        assert(result is Result.Success)
        verify(articleRepository).saveArticle(article)
    }
}