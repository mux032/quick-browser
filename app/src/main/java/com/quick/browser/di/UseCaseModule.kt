package com.quick.browser.di

import com.quick.browser.domain.repository.ArticleRepository
import com.quick.browser.domain.repository.SettingsRepository
import com.quick.browser.domain.usecase.GetSavedArticlesUseCase
import com.quick.browser.domain.usecase.GetSettingsUseCase
import com.quick.browser.domain.usecase.SaveArticleUseCase
import com.quick.browser.domain.usecase.UpdateSettingsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

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
}