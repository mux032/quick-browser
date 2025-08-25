package com.quick.browser.di

import android.content.Context
import androidx.room.Room
import com.quick.browser.data.SavedArticleDao
import com.quick.browser.data.local.dao.SettingsDao
import com.quick.browser.data.local.dao.WebPageDao
import com.quick.browser.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing database-related dependencies
 *
 * This module provides the Room database instance and its associated DAOs
 * for accessing and modifying data in the local database.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provide the singleton AppDatabase instance
     *
     * @param context The application context
     * @return The singleton AppDatabase instance
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "quick_browser.db"
        )
            .fallbackToDestructiveMigration(false)
            .build()
    }

    /**
     * Provide the SettingsDao instance
     *
     * @param database The AppDatabase instance
     * @return The SettingsDao instance
     */
    @Provides
    fun provideSettingsDao(database: AppDatabase): SettingsDao {
        return database.settingsDao()
    }

    /**
     * Provide the WebPageDao instance
     *
     * @param database The AppDatabase instance
     * @return The WebPageDao instance
     */
    @Provides
    fun provideWebPageDao(database: AppDatabase): WebPageDao {
        return database.webPageDao()
    }

    /**
     * Provide the SavedArticleDao instance
     *
     * @param database The AppDatabase instance
     * @return The SavedArticleDao instance
     */
    @Provides
    fun provideSavedArticleDao(database: AppDatabase): SavedArticleDao {
        return database.savedArticleDao()
    }
}