package com.quick.browser.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.quick.browser.model.SavedArticle
import com.quick.browser.model.Settings
import com.quick.browser.model.WebPage

/**
 * Room database for the QB app
 */
@Database(entities = [WebPage::class, Settings::class, SavedArticle::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun webPageDao(): WebPageDao
    abstract fun settingsDao(): SettingsDao
    abstract fun savedArticleDao(): SavedArticleDao
    
    companion object {
        private const val DATABASE_NAME = "quick_browser.db"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        // Migration from version 2 to version 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns for preview image and favicon
                db.execSQL("ALTER TABLE WebPage ADD COLUMN previewImage BLOB")
                db.execSQL("ALTER TABLE WebPage ADD COLUMN faviconUrl TEXT")
            }
        }
        
        // Migration from version 3 to version 4 (add saved articles table)
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create saved articles table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `saved_articles` (
                        `url` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `byline` TEXT,
                        `siteName` TEXT,
                        `publishDate` TEXT,
                        `savedDate` INTEGER NOT NULL,
                        `excerpt` TEXT,
                        PRIMARY KEY(`url`)
                    )
                """.trimIndent())
            }
        }
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    // Keep fallback for other migrations
                    .fallbackToDestructiveMigration(false)
                    .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
