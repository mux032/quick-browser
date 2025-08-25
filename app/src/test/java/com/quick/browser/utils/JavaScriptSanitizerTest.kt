package com.quick.browser.utils

import org.junit.Test

class JavaScriptSanitizerTest {

    @Test
    fun testSanitizeJavaScriptWithSafeScript() {
        val script = "console.log('hello world');"
        val result = JavaScriptSanitizer.sanitizeJavaScript(script)
        assert(result.contains("console.log"))
    }

    @Test
    fun testSanitizeJavaScriptWithDangerousEval() {
        val script = "eval('alert(\"xss\")');"
        val result = JavaScriptSanitizer.sanitizeJavaScript(script)
        assert(!result.contains("eval"))
    }

    @Test
    fun testSanitizeJavaScriptWithDangerousDocumentWrite() {
        val script = "document.write('<script>alert(\"xss\")</script>');"
        val result = JavaScriptSanitizer.sanitizeJavaScript(script)
        assert(!result.contains("document.write"))
    }

    @Test
    fun testIsJavaScriptSafeWithSafeScript() {
        val script = "console.log('hello world');"
        val result = JavaScriptSanitizer.isJavaScriptSafe(script)
        assert(result == true)
    }

    @Test
    fun testIsJavaScriptSafeWithDangerousScript() {
        val script = "eval('alert(\"xss\")');"
        val result = JavaScriptSanitizer.isJavaScriptSafe(script)
        assert(result == false)
    }

    @Test
    fun testGenerateCSPHeader() {
        val csp = JavaScriptSanitizer.generateCSPHeader()
        assert(csp.contains("default-src *"))
        assert(csp.contains("script-src *"))
    }
}