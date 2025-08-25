package com.quick.browser.domain.repository

import com.quick.browser.domain.model.Settings

/**
 * Repository interface for managing app settings
 *
 * This interface defines the contract for accessing and modifying application settings.
 * It provides methods to retrieve, save, and update settings in a data source.
 */
interface SettingsRepository {
    /**
     * Get the current app settings
     *
     * @return The current settings or null if no settings exist
     */
    suspend fun getSettings(): Settings?

    /**
     * Save new app settings
     *
     * @param settings The settings to save
     */
    suspend fun saveSettings(settings: Settings)

    /**
     * Update existing app settings
     *
     * @param settings The settings to update
     */
    suspend fun updateSettings(settings: Settings)
}