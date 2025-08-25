package com.quick.browser.domain.usecase

import com.quick.browser.domain.error.DomainError
import com.quick.browser.domain.model.Settings
import com.quick.browser.domain.repository.SettingsRepository
import com.quick.browser.domain.result.Result
import javax.inject.Inject

/**
 * Use case for updating app settings
 *
 * @param settingsRepository The repository to update settings in
 */
class UpdateSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    /**
     * Update the app settings
     *
     * @param settings The settings to update
     * @return A Result indicating success or failure
     */
    suspend operator fun invoke(settings: Settings): Result<Unit, DomainError> {
        return try {
            settingsRepository.saveSettings(settings)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DomainError.DatabaseError("Failed to update settings", e))
        }
    }
}