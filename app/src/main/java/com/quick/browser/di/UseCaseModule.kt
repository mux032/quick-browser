package com.quick.browser.di

import com.quick.browser.domain.repository.ArticleRepository
import com.quick.browser.domain.repository.BubbleRepository
import com.quick.browser.domain.repository.HistoryRepository
import com.quick.browser.domain.repository.SettingsRepository
import com.quick.browser.domain.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Dagger Hilt module for providing use case instances
 *
 * This module provides all use case instances required by the application,
 * organizing them by domain area (Article, Settings, History, Bubble).
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    // Article Use Cases
    /**
     * Provide GetSavedArticlesUseCase instance
     *
     * @param articleRepository The article repository dependency
     * @return The GetSavedArticlesUseCase instance
     */
    @Provides
    fun provideGetSavedArticlesUseCase(
        articleRepository: ArticleRepository
    ): GetSavedArticlesUseCase {
        return GetSavedArticlesUseCase(articleRepository)
    }

    /**
     * Provide SaveArticleUseCase instance
     *
     * @param articleRepository The article repository dependency
     * @return The SaveArticleUseCase instance
     */
    @Provides
    fun provideSaveArticleUseCase(
        articleRepository: ArticleRepository
    ): SaveArticleUseCase {
        return SaveArticleUseCase(articleRepository)
    }

    /**
     * Provide DeleteArticleUseCase instance
     *
     * @param articleRepository The article repository dependency
     * @return The DeleteArticleUseCase instance
     */
    @Provides
    fun provideDeleteArticleUseCase(
        articleRepository: ArticleRepository
    ): DeleteArticleUseCase {
        return DeleteArticleUseCase(articleRepository)
    }

    // Settings Use Cases
    /**
     * Provide GetSettingsUseCase instance
     *
     * @param settingsRepository The settings repository dependency
     * @return The GetSettingsUseCase instance
     */
    @Provides
    fun provideGetSettingsUseCase(
        settingsRepository: SettingsRepository
    ): GetSettingsUseCase {
        return GetSettingsUseCase(settingsRepository)
    }

    /**
     * Provide UpdateSettingsUseCase instance
     *
     * @param settingsRepository The settings repository dependency
     * @return The UpdateSettingsUseCase instance
     */
    @Provides
    fun provideUpdateSettingsUseCase(
        settingsRepository: SettingsRepository
    ): UpdateSettingsUseCase {
        return UpdateSettingsUseCase(settingsRepository)
    }

    // History Use Cases
    /**
     * Provide GetHistoryUseCase instance
     *
     * @param historyRepository The history repository dependency
     * @return The GetHistoryUseCase instance
     */
    @Provides
    fun provideGetHistoryUseCase(
        historyRepository: HistoryRepository
    ): GetHistoryUseCase {
        return GetHistoryUseCase(historyRepository)
    }

    /**
     * Provide SaveWebPageUseCase instance
     *
     * @param historyRepository The history repository dependency
     * @return The SaveWebPageUseCase instance
     */
    @Provides
    fun provideSaveWebPageUseCase(
        historyRepository: HistoryRepository
    ): SaveWebPageUseCase {
        return SaveWebPageUseCase(historyRepository)
    }

    /**
     * Provide DeleteWebPageUseCase instance
     *
     * @param historyRepository The history repository dependency
     * @return The DeleteWebPageUseCase instance
     */
    @Provides
    fun provideDeleteWebPageUseCase(
        historyRepository: HistoryRepository
    ): DeleteWebPageUseCase {
        return DeleteWebPageUseCase(historyRepository)
    }

    /**
     * Provide UpdateOfflineStatusUseCase instance
     *
     * @param historyRepository The history repository dependency
     * @return The UpdateOfflineStatusUseCase instance
     */
    @Provides
    fun provideUpdateOfflineStatusUseCase(
        historyRepository: HistoryRepository
    ): UpdateOfflineStatusUseCase {
        return UpdateOfflineStatusUseCase(historyRepository)
    }

    /**
     * Provide IncrementVisitCountUseCase instance
     *
     * @param historyRepository The history repository dependency
     * @return The IncrementVisitCountUseCase instance
     */
    @Provides
    fun provideIncrementVisitCountUseCase(
        historyRepository: HistoryRepository
    ): IncrementVisitCountUseCase {
        return IncrementVisitCountUseCase(historyRepository)
    }

    /**
     * Provide DeleteAllPagesUseCase instance
     *
     * @param historyRepository The history repository dependency
     * @return The DeleteAllPagesUseCase instance
     */
    @Provides
    fun provideDeleteAllPagesUseCase(
        historyRepository: HistoryRepository
    ): DeleteAllPagesUseCase {
        return DeleteAllPagesUseCase(historyRepository)
    }

    /**
     * Provide SearchHistoryUseCase instance
     *
     * @param historyRepository The history repository dependency
     * @return The SearchHistoryUseCase instance
     */
    @Provides
    fun provideSearchHistoryUseCase(
        historyRepository: HistoryRepository
    ): SearchHistoryUseCase {
        return SearchHistoryUseCase(historyRepository)
    }

    /**
     * Provide GetRecentPagesUseCase instance
     *
     * @param historyRepository The history repository dependency
     * @return The GetRecentPagesUseCase instance
     */
    @Provides
    fun provideGetRecentPagesUseCase(
        historyRepository: HistoryRepository
    ): GetRecentPagesUseCase {
        return GetRecentPagesUseCase(historyRepository)
    }

    /**
     * Provide GetMostVisitedPagesUseCase instance
     *
     * @param historyRepository The history repository dependency
     * @return The GetMostVisitedPagesUseCase instance
     */
    @Provides
    fun provideGetMostVisitedPagesUseCase(
        historyRepository: HistoryRepository
    ): GetMostVisitedPagesUseCase {
        return GetMostVisitedPagesUseCase(historyRepository)
    }

    // Bubble Use Cases
    /**
     * Provide CreateBubbleUseCase instance
     *
     * @param bubbleRepository The bubble repository dependency
     * @return The CreateBubbleUseCase instance
     */
    @Provides
    fun provideCreateBubbleUseCase(
        bubbleRepository: BubbleRepository
    ): CreateBubbleUseCase {
        return CreateBubbleUseCase(bubbleRepository)
    }

    /**
     * Provide CloseBubbleUseCase instance
     *
     * @param bubbleRepository The bubble repository dependency
     * @return The CloseBubbleUseCase instance
     */
    @Provides
    fun provideCloseBubbleUseCase(
        bubbleRepository: BubbleRepository
    ): CloseBubbleUseCase {
        return CloseBubbleUseCase(bubbleRepository)
    }
}