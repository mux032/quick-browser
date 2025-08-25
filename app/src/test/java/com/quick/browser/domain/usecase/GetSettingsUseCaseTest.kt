package com.quick.browser.domain.usecase

import com.quick.browser.domain.model.Settings
import com.quick.browser.domain.repository.SettingsRepository
import com.quick.browser.domain.result.Result
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class GetSettingsUseCaseTest {

    @Mock
    private lateinit var settingsRepository: SettingsRepository

    private lateinit var getSettingsUseCase: GetSettingsUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        getSettingsUseCase = GetSettingsUseCase(settingsRepository)
    }

    @Test
    fun `invoke should return settings from repository`() = runBlocking {
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
        `when`(settingsRepository.getSettings()).thenReturn(expectedSettings)

        // When
        val result = getSettingsUseCase()

        // Then
        assert(result is Result.Success)
        assert((result as Result.Success).data == expectedSettings)
    }
}