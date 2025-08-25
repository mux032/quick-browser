package com.quick.browser.service

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.quick.browser.domain.service.EncryptedPreferencesService
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class SettingsServiceTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var defaultSharedPreferences: SharedPreferences

    @Mock
    private lateinit var bubbleSharedPreferences: SharedPreferences

    @Mock
    private lateinit var encryptedPrefs: EncryptedPreferencesService

    @Mock
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var settingsService: SettingsService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(PreferenceManager.getDefaultSharedPreferences(context)).thenReturn(defaultSharedPreferences)
        `when`(context.getSharedPreferences("bubble_settings", Context.MODE_PRIVATE)).thenReturn(bubbleSharedPreferences)
        `when`(defaultSharedPreferences.edit()).thenReturn(editor)
        `when`(bubbleSharedPreferences.edit()).thenReturn(editor)
        `when`(editor.putBoolean("pref_ad_blocking", true)).thenReturn(editor)
        `when`(editor.putBoolean("pref_javascript", true)).thenReturn(editor)
        `when`(editor.putBoolean("pref_dark_theme", false)).thenReturn(editor)
        `when`(editor.putInt("pref_bubble_size", 100)).thenReturn(editor)
        settingsService = SettingsService(context, encryptedPrefs)
    }

    @Test
    fun `isAdBlockEnabled should return value from preferences`() {
        // Given
        `when`(defaultSharedPreferences.getBoolean("pref_ad_blocking", true)).thenReturn(true)

        // When
        val result = settingsService.isAdBlockEnabled()

        // Then
        assert(result == true)
    }

    @Test
    fun `isJavaScriptEnabled should return value from preferences`() {
        // Given
        `when`(defaultSharedPreferences.getBoolean("pref_javascript", true)).thenReturn(true)

        // When
        val result = settingsService.isJavaScriptEnabled()

        // Then
        assert(result == true)
    }

    @Test
    fun `getBubbleSize should return value from preferences`() {
        // Given
        `when`(defaultSharedPreferences.getInt("pref_bubble_size", 100)).thenReturn(100)

        // When
        val result = settingsService.getBubbleSize()

        // Then
        assert(result == 1.0f)
    }
}