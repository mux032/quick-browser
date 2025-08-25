package com.quick.browser.di

import android.content.Context
import com.quick.browser.service.ReadabilityService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReadabilityModule {

    @Provides
    @Singleton
    fun provideReadabilityExtractor(@ApplicationContext context: Context): ReadabilityService {
        return ReadabilityService(context)
    }
}