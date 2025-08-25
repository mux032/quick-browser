package com.quick.browser.di

import android.content.Context
import com.quick.browser.utils.managers.AdBlocker
import com.quick.browser.utils.managers.SettingsManager
import com.quick.browser.utils.managers.SummarizationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ManagerModule {

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }

    @Provides
    @Singleton
    fun provideAdBlocker(@ApplicationContext context: Context): AdBlocker {
        return AdBlocker(context)
    }

    @Provides
    @Singleton
    fun provideSummarizationManager(@ApplicationContext context: Context): SummarizationManager {
        return SummarizationManager(context)
    }
}