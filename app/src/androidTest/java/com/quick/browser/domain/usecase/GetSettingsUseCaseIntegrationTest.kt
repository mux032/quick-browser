package com.quick.browser.domain.usecase

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quick.browser.data.local.database.AppDatabase
import com.quick.browser.data.repository.SettingsRepositoryImpl
import com.quick.browser.domain.model.Settings
import com.quick.browser.domain.result.Result
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GetSettingsUseCaseIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var getSettingsUseCase: GetSettingsUseCase

    @Before
    fun setup() {
        // Create an in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        val settingsRepository = SettingsRepositoryImpl(database.settingsDao())
        getSettingsUseCase = GetSettingsUseCase(settingsRepository)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun invoke_shouldReturnSettingsFromRepository() = runBlocking {
        // Given
        val expectedSettings = Settings(
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

        // Save settings to database
        database.settingsDao().insertSettings(
            com.quick.browser.data.local.entity.Settings(
                id = expectedSettings.id,
                size = expectedSettings.size,
                animationSpeed = expectedSettings.animationSpeed,
                savePositions = expectedSettings.savePositions,
                blockAds = expectedSettings.blockAds,
                defaultColor = expectedSettings.defaultColor,
                javascriptEnabled = expectedSettings.javascriptEnabled,
                darkTheme = expectedSettings.darkTheme,
                bubbleSize = expectedSettings.bubbleSize,
                expandedBubbleSize = expectedSettings.expandedBubbleSize,
                animSpeed = expectedSettings.animSpeed,
                saveHistory = expectedSettings.saveHistory,
                encryptData = expectedSettings.encryptData,
                bubblePositionRight = expectedSettings.bubblePositionRight
            )
        )

        // When
        val result = getSettingsUseCase()

        // Then
        assert(result is Result.Success)
        val settings = (result as Result.Success).data
        assert(settings.id == expectedSettings.id)
        assert(settings.size == expectedSettings.size)
        assert(settings.animationSpeed == expectedSettings.animationSpeed)
        assert(settings.savePositions == expectedSettings.savePositions)
        assert(settings.blockAds == expectedSettings.blockAds)
        assert(settings.defaultColor == expectedSettings.defaultColor)
        assert(settings.javascriptEnabled == expectedSettings.javascriptEnabled)
        assert(settings.darkTheme == expectedSettings.darkTheme)
        assert(settings.bubbleSize == expectedSettings.bubbleSize)
        assert(settings.expandedBubbleSize == expectedSettings.expandedBubbleSize)
        assert(settings.animSpeed == expectedSettings.animSpeed)
        assert(settings.saveHistory == expectedSettings.saveHistory)
        assert(settings.encryptData == expectedSettings.encryptData)
        assert(settings.bubblePositionRight == expectedSettings.bubblePositionRight)
    }
}