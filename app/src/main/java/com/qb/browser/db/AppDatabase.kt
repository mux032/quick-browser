package com.qb.browser.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.qb.browser.model.Settings
import com.qb.browser.model.WebPage

/**
 * Room database for the QB app
 */
@Database(entities = [WebPage::class, Settings::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun webPageDao(): WebPageDao
    abstract fun settingsDao(): SettingsDao
    
    companion object {
        private const val DATABASE_NAME = "qb_browser.db"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
