package com.quick.browser.data.repository

import com.quick.browser.data.local.dao.SettingsDao
import com.quick.browser.data.local.entity.Settings
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import com.quick.browser.domain.model.Settings as DomainSettings

class SettingsRepositoryImplTest {

    @Mock
    private lateinit var settingsDao: SettingsDao

    private lateinit var settingsRepositoryImpl: SettingsRepositoryImpl

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        settingsRepositoryImpl = SettingsRepositoryImpl(settingsDao)
    }

    @Test
    fun `getSettings should return settings from dao`() = runBlocking {
        // Given
        val settingsEntity = Settings(
            id = 1,
            size = "medium",
            animationSpeed = "medium",
            savePositions = true,
            blockAds = true,
            defaultColor = "#2196F3",
            javascriptEnabled = true,
            darkTheme = false,
            bubbleSize = 1.0f,
            expandedBubbleSize = 1.5f,
            animSpeed = 1.0f,
            saveHistory = true,
            encryptData = true,
            bubblePositionRight = false
        )
        `when`(settingsDao.getSettings()).thenReturn(settingsEntity)

        // When
        val result = settingsRepositoryImpl.getSettings()

        // Then
        assert(result?.id == settingsEntity.id)
        assert(result?.size == settingsEntity.size)
        assert(result?.animationSpeed == settingsEntity.animationSpeed)
        assert(result?.savePositions == settingsEntity.savePositions)
        assert(result?.blockAds == settingsEntity.blockAds)
        assert(result?.defaultColor == settingsEntity.defaultColor)
        assert(result?.javascriptEnabled == settingsEntity.javascriptEnabled)
        assert(result?.darkTheme == settingsEntity.darkTheme)
        assert(result?.bubbleSize == settingsEntity.bubbleSize)
        assert(result?.expandedBubbleSize == settingsEntity.expandedBubbleSize)
        assert(result?.animSpeed == settingsEntity.animSpeed)
        assert(result?.saveHistory == settingsEntity.saveHistory)
        assert(result?.encryptData == settingsEntity.encryptData)
        assert(result?.bubblePositionRight == settingsEntity.bubblePositionRight)
    }

    @Test
    fun `updateSettings should call dao to update settings`() = runBlocking {
        // Given
        val settings = DomainSettings(
            id = 1,
            size = "medium",
            animationSpeed = "medium",
            savePositions = true,
            blockAds = true,
            defaultColor = "#2196F3",
            javascriptEnabled = true,
            darkTheme = false,
            bubbleSize = 1.0f,
            expandedBubbleSize = 1.5f,
            animSpeed = 1.0f,
            saveHistory = true,
            encryptData = true,
            bubblePositionRight = false
        )

        // When
        settingsRepositoryImpl.updateSettings(settings)

        // Then
        verify(settingsDao).updateSettings(
            Settings(
                id = settings.id,
                size = settings.size,
                animationSpeed = settings.animationSpeed,
                savePositions = settings.savePositions,
                blockAds = settings.blockAds,
                defaultColor = settings.defaultColor,
                javascriptEnabled = settings.javascriptEnabled,
                darkTheme = settings.darkTheme,
                bubbleSize = settings.bubbleSize,
                expandedBubbleSize = settings.expandedBubbleSize,
                animSpeed = settings.animSpeed,
                saveHistory = settings.saveHistory,
                encryptData = settings.encryptData,
                bubblePositionRight = settings.bubblePositionRight
            )
        )
    }
}