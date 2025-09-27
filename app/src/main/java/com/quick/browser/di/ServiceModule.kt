package com.quick.browser.di

import android.content.Context
import com.quick.browser.di.AppConstants.ENCRYPTED_PREFS_NAME
import com.quick.browser.domain.repository.ArticleRepository
import com.quick.browser.domain.service.ArticleSavingService
import com.quick.browser.domain.service.EncryptedPreferencesService
import com.quick.browser.domain.service.ModelDownloadService
import com.quick.browser.domain.service.SecurityPolicyService
import com.quick.browser.service.AdBlockingService
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
        articleRepository: ArticleRepository,
        articleTagRepository: com.quick.browser.domain.repository.ArticleTagRepository
    ): ArticleSavingService = ArticleSavingService(articleRepository, articleTagRepository)

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
    ): AdBlockingService = AdBlockingService(context, encryptedPreferencesService)

    /**
     * Provide the SummarizationService instance
     *
     * @param context The application context
     * @return The SummarizationService instance
     */
    @Provides
    @Singleton
    fun provideSummarizationService(@ApplicationContext context: Context): SummarizationService = SummarizationService(context)

    /**
     * Provide the EncryptedPreferencesService instance
     *
     * @param context The application context
     * @return The EncryptedPreferencesService instance
     */
    @Provides
    @Singleton
    fun provideEncryptedPreferencesService(@ApplicationContext context: Context): EncryptedPreferencesService = EncryptedPreferencesService(context, ENCRYPTED_PREFS_NAME)

    /**
     * Provide the ModelDownloadService instance
     *
     * @param context The application context
     * @return The ModelDownloadService instance
     */
    @Provides
    @Singleton
    fun provideModelDownloadService(@ApplicationContext context: Context): ModelDownloadService = ModelDownloadService(context)

    /**
     * Provide the SecurityPolicyService instance
     *
     * @param context The application context
     * @return The SecurityPolicyService instance
     */
    @Provides
    @Singleton
    fun provideSecurityPolicyService(@ApplicationContext context: Context): SecurityPolicyService = SecurityPolicyService(context)
}