package com.quick.browser.data.local.dao

import androidx.room.*
import com.quick.browser.data.local.entity.Settings

/** Data Access Object for Settings entity */
@Dao
interface SettingsDao {

    /** Get the current settings */
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1") suspend fun getSettings(): Settings?

    /** Insert or update settings */
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE) suspend fun insertSettings(settings: Settings)

    /** Update settings */
    @Update
    suspend fun updateSettings(settings: Settings)
}