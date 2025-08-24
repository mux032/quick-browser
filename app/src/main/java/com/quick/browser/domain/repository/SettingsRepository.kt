package com.quick.browser.domain.repository

import com.quick.browser.domain.model.Settings

/**
 * Repository interface for managing app settings
 */
interface SettingsRepository {
    suspend fun getSettings(): Settings?
    suspend fun saveSettings(settings: Settings)
    suspend fun updateSettings(settings: Settings)
}