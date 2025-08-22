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
@Database(entities = [WebPage::class, Settings::class, SavedArticle::class], version = 5, exportSchema = false)
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
        
        // Migration from version 4 to version 5 (remove previewImage BLOB, add previewImageUrl)
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create new table without previewImage BLOB column
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `web_pages_new` (
                        `url` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `content` TEXT NOT NULL,
                        `isAvailableOffline` INTEGER NOT NULL,
                        `visitCount` INTEGER NOT NULL,
                        `favicon` BLOB,
                        `faviconUrl` TEXT,
                        `previewImageUrl` TEXT,
                        PRIMARY KEY(`url`)
                    )
                """.trimIndent())
                
                // Copy data from old table (excluding previewImage)
                db.execSQL("""
                    INSERT INTO web_pages_new (url, title, timestamp, content, isAvailableOffline, visitCount, favicon, faviconUrl, previewImageUrl)
                    SELECT url, title, timestamp, content, isAvailableOffline, visitCount, favicon, faviconUrl, NULL
                    FROM web_pages
                """.trimIndent())
                
                // Drop old table and rename new one
                db.execSQL("DROP TABLE web_pages")
                db.execSQL("ALTER TABLE web_pages_new RENAME TO web_pages")
            }
        }
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    // Keep fallback for other migrations
                    .fallbackToDestructiveMigration(false)
                    .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
