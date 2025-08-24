package com.quick.browser.domain.usecase

import com.quick.browser.domain.model.Settings
import com.quick.browser.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case for getting app settings
 */
class GetSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Settings? {
        return settingsRepository.getSettings()
    }
}