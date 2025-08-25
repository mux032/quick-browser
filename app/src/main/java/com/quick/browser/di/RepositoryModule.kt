package com.quick.browser.di

import com.quick.browser.data.repository.ArticleRepositoryImpl
import com.quick.browser.data.repository.BubbleRepositoryImpl
import com.quick.browser.data.repository.HistoryRepositoryImpl
import com.quick.browser.data.repository.SettingsRepositoryImpl
import com.quick.browser.domain.repository.ArticleRepository
import com.quick.browser.domain.repository.BubbleRepository
import com.quick.browser.domain.repository.HistoryRepository
import com.quick.browser.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Dagger Hilt module for providing repository implementations
 *
 * This module binds concrete repository implementations to their respective interfaces,
 * allowing for dependency injection throughout the application.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Bind SettingsRepositoryImpl to SettingsRepository interface
     *
     * @param settingsRepositoryImpl The concrete implementation
     * @return The SettingsRepository interface
     */
    @Binds
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    /**
     * Bind HistoryRepositoryImpl to HistoryRepository interface
     *
     * @param historyRepositoryImpl The concrete implementation
     * @return The HistoryRepository interface
     */
    @Binds
    abstract fun bindHistoryRepository(
        historyRepositoryImpl: HistoryRepositoryImpl
    ): HistoryRepository

    /**
     * Bind BubbleRepositoryImpl to BubbleRepository interface
     *
     * @param bubbleRepositoryImpl The concrete implementation
     * @return The BubbleRepository interface
     */
    @Binds
    abstract fun bindBubbleRepository(
        bubbleRepositoryImpl: BubbleRepositoryImpl
    ): BubbleRepository

    /**
     * Bind ArticleRepositoryImpl to ArticleRepository interface
     *
     * @param articleRepositoryImpl The concrete implementation
     * @return The ArticleRepository interface
     */
    @Binds
    abstract fun bindArticleRepository(
        articleRepositoryImpl: ArticleRepositoryImpl
    ): ArticleRepository
}