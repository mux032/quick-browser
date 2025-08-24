package com.quick.browser.di

import android.content.Context
import androidx.room.Room
import com.quick.browser.data.AppDatabase
import com.quick.browser.data.SavedArticleDao
import com.quick.browser.data.SettingsDao
import com.quick.browser.data.WebPageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

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

    @Provides
    fun provideSettingsDao(database: AppDatabase): SettingsDao {
        return database.settingsDao()
    }

    @Provides
    fun provideWebPageDao(database: AppDatabase): WebPageDao {
        return database.webPageDao()
    }

    @Provides
    fun provideSavedArticleDao(database: AppDatabase): SavedArticleDao {
        return database.savedArticleDao()
    }
}