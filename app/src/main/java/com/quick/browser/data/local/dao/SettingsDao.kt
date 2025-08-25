package com.quick.browser.data.local.dao

import androidx.room.*
import com.quick.browser.data.local.entity.Settings

/**
 * Data Access Object for Settings entity
 *
 * This interface provides methods for accessing and modifying settings in the database.
 */
@Dao
interface SettingsDao {

    /**
     * Get the current settings
     *
     * @return The current settings or null if no settings exist
     */
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1") 
    suspend fun getSettings(): Settings?

    /**
     * Insert or update settings
     *
     * @param settings The settings to insert or update
     */
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE) 
    suspend fun insertSettings(settings: Settings)

    /**
     * Update settings
     *
     * @param settings The settings to update
     */
    @Update
    suspend fun updateSettings(settings: Settings)
}