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

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    abstract fun bindHistoryRepository(
        historyRepositoryImpl: HistoryRepositoryImpl
    ): HistoryRepository

    @Binds
    abstract fun bindBubbleRepository(
        bubbleRepositoryImpl: BubbleRepositoryImpl
    ): BubbleRepository

    @Binds
    abstract fun bindArticleRepository(
        articleRepositoryImpl: ArticleRepositoryImpl
    ): ArticleRepository
}