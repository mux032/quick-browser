package com.qb.browser.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.qb.browser.model.Settings
import com.qb.browser.model.WebPage

/**
 * Room database for the QB app
 */
@Database(entities = [WebPage::class, Settings::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun webPageDao(): WebPageDao
    abstract fun settingsDao(): SettingsDao
    
    companion object {
        private const val DATABASE_NAME = "qb_browser.db"
        
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
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_2_3)
                // Keep fallback for other migrations
                .fallbackToDestructiveMigration()
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
