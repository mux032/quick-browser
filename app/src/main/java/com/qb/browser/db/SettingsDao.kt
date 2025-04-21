package com.qb.browser.db

import androidx.room.*
import com.qb.browser.model.Settings

/** Data Access Object for Settings entity */
@Dao
interface SettingsDao {

    /** Get the current settings */
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1") suspend fun getSettings(): Settings?

    /** Insert or update settings */
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSettings(settings: Settings)

    /** Update settings */
    @Update suspend fun updateSettings(settings: Settings)
}
