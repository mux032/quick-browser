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

/**
 * Dagger Hilt module for providing service dependencies
 *
 * This module provides various service instances required by the application,
 * including settings, ad-blocking, summarization, and other utility services.
 */
@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    /**
     * Provide the ArticleSavingService instance
     *
     * @param articleRepository The article repository dependency
     * @return The ArticleSavingService instance
     */
    @Provides
    @Singleton
    fun provideArticleSavingService(
        articleRepository: ArticleRepository
    ): ArticleSavingService {
        return ArticleSavingService(articleRepository)
    }
    
    /**
     * Provide the SettingsService instance
     *
     * @param context The application context
     * @param encryptedPreferencesService The encrypted preferences service dependency
     * @return The SettingsService instance
     */
    @Provides
    @Singleton
    fun provideSettingsService(
        @ApplicationContext context: Context,
        encryptedPreferencesService: EncryptedPreferencesService
    ): SettingsService {
        return SettingsService(context, encryptedPreferencesService)
    }

    /**
     * Provide the AdBlockingService instance
     *
     * @param context The application context
     * @param encryptedPreferencesService The encrypted preferences service dependency
     * @return The AdBlockingService instance
     */
    @Provides
    @Singleton
    fun provideAdBlockingService(
        @ApplicationContext context: Context,
        encryptedPreferencesService: EncryptedPreferencesService
    ): AdBlockingService {
        return AdBlockingService(context, encryptedPreferencesService)
    }

    /**
     * Provide the SummarizationService instance
     *
     * @param context The application context
     * @return The SummarizationService instance
     */
    @Provides
    @Singleton
    fun provideSummarizationService(@ApplicationContext context: Context): SummarizationService {
        return SummarizationService(context)
    }
    
    /**
     * Provide the EncryptedPreferencesService instance
     *
     * @param context The application context
     * @return The EncryptedPreferencesService instance
     */
    @Provides
    @Singleton
    fun provideEncryptedPreferencesService(@ApplicationContext context: Context): EncryptedPreferencesService {
        return EncryptedPreferencesService(context, "quick_browser_prefs")
    }
    
    /**
     * Provide the ModelDownloadService instance
     *
     * @param context The application context
     * @return The ModelDownloadService instance
     */
    @Provides
    @Singleton
    fun provideModelDownloadService(@ApplicationContext context: Context): ModelDownloadService {
        return ModelDownloadService(context)
    }
    
    /**
     * Provide the SecurityPolicyService instance
     *
     * @param context The application context
     * @return The SecurityPolicyService instance
     */
    @Provides
    @Singleton
    fun provideSecurityPolicyService(@ApplicationContext context: Context): SecurityPolicyService {
        return SecurityPolicyService(context)
    }
}