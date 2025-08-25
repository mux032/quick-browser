package com.quick.browser.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quick.browser.data.local.database.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.quick.browser.domain.model.Settings as DomainSettings

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryImplIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var settingsRepositoryImpl: SettingsRepositoryImpl

    @Before
    fun setup() {
        // Create an in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        settingsRepositoryImpl = SettingsRepositoryImpl(database.settingsDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun saveSettings_shouldSaveSettingsToDatabase() = runBlocking {
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
        settingsRepositoryImpl.saveSettings(settings)

        // Then
        val savedSettings = settingsRepositoryImpl.getSettings()
        assert(savedSettings?.id == settings.id)
        assert(savedSettings?.size == settings.size)
        assert(savedSettings?.animationSpeed == settings.animationSpeed)
        assert(savedSettings?.savePositions == settings.savePositions)
        assert(savedSettings?.blockAds == settings.blockAds)
        assert(savedSettings?.defaultColor == settings.defaultColor)
        assert(savedSettings?.javascriptEnabled == settings.javascriptEnabled)
        assert(savedSettings?.darkTheme == settings.darkTheme)
        assert(savedSettings?.bubbleSize == settings.bubbleSize)
        assert(savedSettings?.expandedBubbleSize == settings.expandedBubbleSize)
        assert(savedSettings?.animSpeed == settings.animSpeed)
        assert(savedSettings?.saveHistory == settings.saveHistory)
        assert(savedSettings?.encryptData == settings.encryptData)
        assert(savedSettings?.bubblePositionRight == settings.bubblePositionRight)
    }

    @Test
    fun updateSettings_shouldUpdateExistingSettings() = runBlocking {
        // Given
        val initialSettings = DomainSettings(
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

        val updatedSettings = DomainSettings(
            id = 1,
            size = "large",
            animationSpeed = "fast",
            savePositions = false,
            blockAds = false,
            defaultColor = "#FF0000",
            javascriptEnabled = false,
            darkTheme = true,
            bubbleSize = 2.0f,
            expandedBubbleSize = 3.0f,
            animSpeed = 2.0f,
            saveHistory = false,
            encryptData = false,
            bubblePositionRight = true
        )

        // When
        settingsRepositoryImpl.saveSettings(initialSettings)
        settingsRepositoryImpl.updateSettings(updatedSettings)

        // Then
        val savedSettings = settingsRepositoryImpl.getSettings()
        assert(savedSettings?.id == updatedSettings.id)
        assert(savedSettings?.size == updatedSettings.size)
        assert(savedSettings?.animationSpeed == updatedSettings.animationSpeed)
        assert(savedSettings?.savePositions == updatedSettings.savePositions)
        assert(savedSettings?.blockAds == updatedSettings.blockAds)
        assert(savedSettings?.defaultColor == updatedSettings.defaultColor)
        assert(savedSettings?.javascriptEnabled == updatedSettings.javascriptEnabled)
        assert(savedSettings?.darkTheme == updatedSettings.darkTheme)
        assert(savedSettings?.bubbleSize == updatedSettings.bubbleSize)
        assert(savedSettings?.expandedBubbleSize == updatedSettings.expandedBubbleSize)
        assert(savedSettings?.animSpeed == updatedSettings.animSpeed)
        assert(savedSettings?.saveHistory == updatedSettings.saveHistory)
        assert(savedSettings?.encryptData == updatedSettings.encryptData)
        assert(savedSettings?.bubblePositionRight == updatedSettings.bubblePositionRight)
    }
}