package com.quick.browser.di

import android.content.Context
import com.quick.browser.service.ReadabilityService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing readability service dependencies
 *
 * This module provides the ReadabilityService instance for extracting
 * readable content from web pages.
 */
@Module
@InstallIn(SingletonComponent::class)
object ReadabilityModule {

    /**
     * Provide the singleton ReadabilityService instance
     *
     * @param context The application context
     * @return The singleton ReadabilityService instance
     */
    @Provides
    @Singleton
    fun provideReadabilityExtractor(@ApplicationContext context: Context): ReadabilityService {
        return ReadabilityService(context)
    }
}