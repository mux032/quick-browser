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

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    // Article Use Cases
    @Provides
    fun provideGetSavedArticlesUseCase(
        articleRepository: ArticleRepository
    ): GetSavedArticlesUseCase {
        return GetSavedArticlesUseCase(articleRepository)
    }

    @Provides
    fun provideSaveArticleUseCase(
        articleRepository: ArticleRepository
    ): SaveArticleUseCase {
        return SaveArticleUseCase(articleRepository)
    }

    @Provides
    fun provideDeleteArticleUseCase(
        articleRepository: ArticleRepository
    ): DeleteArticleUseCase {
        return DeleteArticleUseCase(articleRepository)
    }

    // Settings Use Cases
    @Provides
    fun provideGetSettingsUseCase(
        settingsRepository: SettingsRepository
    ): GetSettingsUseCase {
        return GetSettingsUseCase(settingsRepository)
    }

    @Provides
    fun provideUpdateSettingsUseCase(
        settingsRepository: SettingsRepository
    ): UpdateSettingsUseCase {
        return UpdateSettingsUseCase(settingsRepository)
    }

    // History Use Cases
    @Provides
    fun provideGetHistoryUseCase(
        historyRepository: HistoryRepository
    ): GetHistoryUseCase {
        return GetHistoryUseCase(historyRepository)
    }

    @Provides
    fun provideSaveWebPageUseCase(
        historyRepository: HistoryRepository
    ): SaveWebPageUseCase {
        return SaveWebPageUseCase(historyRepository)
    }

    @Provides
    fun provideDeleteWebPageUseCase(
        historyRepository: HistoryRepository
    ): DeleteWebPageUseCase {
        return DeleteWebPageUseCase(historyRepository)
    }

    @Provides
    fun provideUpdateOfflineStatusUseCase(
        historyRepository: HistoryRepository
    ): UpdateOfflineStatusUseCase {
        return UpdateOfflineStatusUseCase(historyRepository)
    }

    @Provides
    fun provideIncrementVisitCountUseCase(
        historyRepository: HistoryRepository
    ): IncrementVisitCountUseCase {
        return IncrementVisitCountUseCase(historyRepository)
    }

    @Provides
    fun provideDeleteAllPagesUseCase(
        historyRepository: HistoryRepository
    ): DeleteAllPagesUseCase {
        return DeleteAllPagesUseCase(historyRepository)
    }

    @Provides
    fun provideSearchHistoryUseCase(
        historyRepository: HistoryRepository
    ): SearchHistoryUseCase {
        return SearchHistoryUseCase(historyRepository)
    }

    @Provides
    fun provideGetRecentPagesUseCase(
        historyRepository: HistoryRepository
    ): GetRecentPagesUseCase {
        return GetRecentPagesUseCase(historyRepository)
    }

    @Provides
    fun provideGetMostVisitedPagesUseCase(
        historyRepository: HistoryRepository
    ): GetMostVisitedPagesUseCase {
        return GetMostVisitedPagesUseCase(historyRepository)
    }

    // Bubble Use Cases
    @Provides
    fun provideCreateBubbleUseCase(
        bubbleRepository: BubbleRepository
    ): CreateBubbleUseCase {
        return CreateBubbleUseCase(bubbleRepository)
    }

    @Provides
    fun provideCloseBubbleUseCase(
        bubbleRepository: BubbleRepository
    ): CloseBubbleUseCase {
        return CloseBubbleUseCase(bubbleRepository)
    }
}