package com.quick.browser.data.repository

import com.quick.browser.data.SettingsDao
import com.quick.browser.data.local.entity.Settings
import com.quick.browser.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Repository implementation for managing app settings
 */
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao
) : SettingsRepository {
    
    override suspend fun getSettings(): com.quick.browser.domain.model.Settings? {
        val entity = settingsDao.getSettings()
        return entity?.let { entityToDomain(it) }
    }
    
    override suspend fun saveSettings(settings: com.quick.browser.domain.model.Settings) {
        settingsDao.insertSettings(domainToEntity(settings))
    }
    
    override suspend fun updateSettings(settings: com.quick.browser.domain.model.Settings) {
        settingsDao.updateSettings(domainToEntity(settings))
    }
    
    private fun entityToDomain(entity: Settings): com.quick.browser.domain.model.Settings {
        return com.quick.browser.domain.model.Settings(
            id = entity.id,
            size = entity.size,
            animationSpeed = entity.animationSpeed,
            savePositions = entity.savePositions,
            blockAds = entity.blockAds,
            defaultColor = entity.defaultColor,
            javascriptEnabled = entity.javascriptEnabled,
            darkTheme = entity.darkTheme,
            bubbleSize = entity.bubbleSize,
            expandedBubbleSize = entity.expandedBubbleSize,
            animSpeed = entity.animSpeed,
            saveHistory = entity.saveHistory,
            encryptData = entity.encryptData,
            bubblePositionRight = entity.bubblePositionRight
        )
    }
    
    private fun domainToEntity(domain: com.quick.browser.domain.model.Settings): Settings {
        return Settings(
            id = domain.id,
            size = domain.size,
            animationSpeed = domain.animationSpeed,
            savePositions = domain.savePositions,
            blockAds = domain.blockAds,
            defaultColor = domain.defaultColor,
            javascriptEnabled = domain.javascriptEnabled,
            darkTheme = domain.darkTheme,
            bubbleSize = domain.bubbleSize,
            expandedBubbleSize = domain.expandedBubbleSize,
            animSpeed = domain.animSpeed,
            saveHistory = domain.saveHistory,
            encryptData = domain.encryptData,
            bubblePositionRight = domain.bubblePositionRight
        )
    }
}