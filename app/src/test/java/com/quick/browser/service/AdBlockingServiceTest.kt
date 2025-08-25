package com.quick.browser.service

import android.content.Context
import com.quick.browser.domain.service.EncryptedPreferencesService
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

/**
 * Unit test for AdBlockingService
 */
class AdBlockingServiceTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var encryptedPrefs: EncryptedPreferencesService

    private lateinit var adBlockingService: AdBlockingService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        adBlockingService = AdBlockingService(context, encryptedPrefs)
    }

    @Test
    fun `shouldBlockRequest should return null for non-ad URLs`() {
        // Given
        val url = "https://example.com/index.html"

        // When
        val result = adBlockingService.shouldBlockRequest(url)

        // Then
        assert(result == null)
    }

    @Test
    fun `shouldBlockRequest should return blocked response for known ad URLs`() {
        // Given
        val url = "http://ad.doubleclick.net/ads/somead"

        // When
        val result = adBlockingService.shouldBlockRequest(url)

        // Then
        assert(result != null)
    }

    @Test
    fun `isWhitelisted should return false for non-whitelisted domains`() {
        // Given
        val domain = "example.com"

        // When
        val result = adBlockingService.isWhitelisted(domain)

        // Then
        assert(result == false)
    }

    @Test
    fun `isBlacklisted should return false for non-blacklisted domains`() {
        // Given
        val domain = "example.com"

        // When
        val result = adBlockingService.isBlacklisted(domain)

        // Then
        assert(result == false)
    }
}