package com.quick.browser.domain.usecase

import com.quick.browser.domain.error.DomainError
import com.quick.browser.domain.model.Settings
import com.quick.browser.domain.repository.SettingsRepository
import com.quick.browser.domain.result.Result
import javax.inject.Inject

/**
 * Use case for getting app settings
 *
 * @param settingsRepository The repository to get settings from
 */
class GetSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    /**
     * Get the app settings
     *
     * @return A Result containing the settings or an error
     */
    suspend operator fun invoke(): Result<Settings, DomainError> {
        return try {
            val settings = settingsRepository.getSettings()
            if (settings != null) {
                Result.success(settings)
            } else {
                Result.failure(DomainError.GeneralError("Settings not found"))
            }
        } catch (e: Exception) {
            Result.failure(DomainError.DatabaseError("Failed to get settings", e))
        }
    }
}