package com.quick.browser.di

import android.content.Context
import com.quick.browser.domain.repository.ArticleRepository
import com.quick.browser.domain.service.ArticleSavingService
import com.quick.browser.domain.service.EncryptedPreferencesService
import com.quick.browser.domain.service.ModelDownloadService
import com.quick.browser.domain.service.SecurityPolicyService
import com.quick.browser.service.AdBlockingService
import com.quick.browser.service.SettingsService
import com.quick.browser.service.SummarizationService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideArticleSavingService(
        articleRepository: ArticleRepository
    ): ArticleSavingService {
        return ArticleSavingService(articleRepository)
    }
    
    @Provides
    @Singleton
    fun provideSettingsService(
        @ApplicationContext context: Context,
        encryptedPreferencesService: EncryptedPreferencesService
    ): SettingsService {
        return SettingsService(context, encryptedPreferencesService)
    }

    @Provides
    @Singleton
    fun provideAdBlockingService(
        @ApplicationContext context: Context,
        encryptedPreferencesService: EncryptedPreferencesService
    ): AdBlockingService {
        return AdBlockingService(context, encryptedPreferencesService)
    }

    @Provides
    @Singleton
    fun provideSummarizationService(@ApplicationContext context: Context): SummarizationService {
        return SummarizationService(context)
    }
    
    @Provides
    @Singleton
    fun provideEncryptedPreferencesService(@ApplicationContext context: Context): EncryptedPreferencesService {
        return EncryptedPreferencesService(context, "quick_browser_prefs")
    }
    
    @Provides
    @Singleton
    fun provideModelDownloadService(@ApplicationContext context: Context): ModelDownloadService {
        return ModelDownloadService(context)
    }
    
    @Provides
    @Singleton
    fun provideSecurityPolicyService(@ApplicationContext context: Context): SecurityPolicyService {
        return SecurityPolicyService(context)
    }
}