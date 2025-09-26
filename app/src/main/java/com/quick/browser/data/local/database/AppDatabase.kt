package com.quick.browser.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.quick.browser.data.local.dao.SavedArticleDao
import com.quick.browser.data.local.dao.SettingsDao
import com.quick.browser.data.local.dao.WebPageDao
import com.quick.browser.data.local.entity.*

/**
 * Room database for the QB app
 *
 * This abstract class defines the Room database for the application.
 * It includes entities for web pages, settings, and saved articles,
 * along with their respective DAOs for database access.
 */
@Database(entities = [WebPage::class, Settings::class, SavedArticle::class, Tag::class, ArticleTag::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Get the WebPage DAO
     *
     * @return The WebPageDao instance
     */
    abstract fun webPageDao(): WebPageDao

    /**
     * Get the Settings DAO
     *
     * @return The SettingsDao instance
     */
    abstract fun settingsDao(): SettingsDao

    /**
     * Get the SavedArticle DAO
     *
     * @return The SavedArticleDao instance
     */
    abstract fun savedArticleDao(): SavedArticleDao

    /**
     * Get the Tag DAO
     *
     * @return The TagDao instance
     */
    abstract fun tagDao(): com.quick.browser.data.local.dao.TagDao

    /**
     * Get the ArticleTag DAO
     *
     * @return The ArticleTagDao instance
     */
    abstract fun articleTagDao(): com.quick.browser.data.local.dao.ArticleTagDao

    companion object {
        private const val DATABASE_NAME = "quick_browser.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Get the singleton instance of the AppDatabase
         *
         * @param context The application context
         * @return The singleton instance of the AppDatabase
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(false)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}