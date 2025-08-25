package com.quick.browser.manager

import android.content.Context
import com.quick.browser.utils.managers.SecurityPolicyManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SecurityPolicyManagerTest {

    private lateinit var securityPolicyManager: SecurityPolicyManager
    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.application
        securityPolicyManager = SecurityPolicyManager(context)
    }

    @Test
    fun testValidateAndFormatUrlWithValidHttpsUrl() {
        val url = "https://example.com"
        val result = securityPolicyManager.validateAndFormatUrl(url)
        assert(result == url)
    }

    @Test
    fun testValidateAndFormatUrlWithValidHttpUrl() {
        val url = "http://example.com"
        val result = securityPolicyManager.validateAndFormatUrl(url)
        assert(result == url)
    }

    @Test
    fun testValidateAndFormatUrlWithoutProtocol() {
        val url = "example.com"
        val result = securityPolicyManager.validateAndFormatUrl(url)
        assert(result == "https://$url")
    }

    @Test
    fun testValidateAndFormatUrlWithDangerousProtocol() {
        val url = "ftp://example.com"
        val result = securityPolicyManager.validateAndFormatUrl(url)
        assert(result == null)
    }

    @Test
    fun testValidateAndFormatUrlWithJavaScriptProtocol() {
        val url = "javascript:alert('xss')"
        val result = securityPolicyManager.validateAndFormatUrl(url)
        // Our implementation removes the javascript: part and defaults to https
        assert(result == "https://alert('xss')")
    }

    @Test
    fun testIsUrlSafeToLoadWithSafeUrl() {
        val url = "https://example.com"
        val result = securityPolicyManager.isUrlSafeToLoad(url)
        assert(result == true)
    }

    @Test
    fun testSanitizeInputWithDangerousCharacters() {
        val input = "<script>alert('xss')</script>"
        val result = securityPolicyManager.sanitizeInput(input)
        // Our implementation removes < and > characters
        assert(!result.contains("<"))
        assert(!result.contains(">"))
    }
}