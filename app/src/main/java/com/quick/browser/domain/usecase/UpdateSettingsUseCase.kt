package com.quick.browser.domain.usecase

import com.quick.browser.domain.model.Settings
import com.quick.browser.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case for updating app settings
 */
class UpdateSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(settings: Settings) {
        settingsRepository.saveSettings(settings)
    }
}