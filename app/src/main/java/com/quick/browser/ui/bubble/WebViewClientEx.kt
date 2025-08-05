package com.quick.browser.ui.bubble

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.quick.browser.manager.AdBlocker
import com.quick.browser.manager.AuthenticationHandler
import com.quick.browser.manager.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Extended WebViewClient with ad blocking and other features
 * This class is open for extension
 */
open class WebViewClientEx(
    protected val context: Context,
    protected val onPageUrlChanged: (String) -> Unit,
    private val settingsManager: SettingsManager,
    private val adBlocker: AdBlocker
) : WebViewClient() {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    override open fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val url = request.url.toString()

        // Only block ads if enabled in settings
        if (settingsManager.isAdBlockEnabled()) {
            // Check if this resource should be blocked
            val blockResponse = adBlocker.shouldBlockRequest(url)
            return blockResponse ?: super.shouldInterceptRequest(view, request)
        }

        // If ad blocking is disabled, don't block anything
        return super.shouldInterceptRequest(view, request)
    }

    // override open fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
    //     super.onPageStarted(view, url, favicon)
    //     url?.let { onPageUrlChanged(it) }
    // }

    // override open fun onPageFinished(view: WebView?, url: String?) {
    //     super.onPageFinished(view, url)

    //     // Apply content extraction script for read mode only if ad blocking is enabled
    //     url?.let {
    //         view?.let { webView ->
    //             // Only inject ad-blocking script if the setting is enabled AND JavaScript is enabled
    //             if (settingsManager.isAdBlockEnabled() && settingsManager.isJavaScriptEnabled()) {
    //                 // Execute JS to strip out unnecessary elements, can be used later for read mode
    //                 val cleanupScript = """
    //                     javascript:(function() {
    //                         var elements = document.querySelectorAll('iframe, ins, script[src*="ads"], script[src*="analytics"], div[id*="banner"], div[id*="ad-"], div[class*="banner"], div[class*="ad-"]');
    //                         for (var i = 0; i < elements.length; i++) {
    //                             elements[i].style.display = 'none';
    //                         }
    //                     })()
    //                 """.trimIndent()

    //                 webView.loadUrl(cleanupScript)
    //             }
    //         }
    //     }
    // }

    // For newer Android versions
    override open fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        // Custom URL handling
        val url = request?.url?.toString() ?: return false

        // Check for authentication URLs first - before any other processing
        if (AuthenticationHandler.isAuthenticationUrl(url)) {
            android.util.Log.d("WebViewClientEx", "Authentication URL detected in shouldOverrideUrlLoading: $url")
            view?.context?.let { context ->
                AuthenticationHandler.openInCustomTab(context, url)
                return true
            }
        }

        // Always notify about URL change to update UI
        onPageUrlChanged(url)

        return handleUrlOverride(view, url)
    }

    // For older Android versions (API < 24)
    @Suppress("DEPRECATION")
    override open fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        if (url == null) return false

        // Check for authentication URLs first - before any other processing
        if (AuthenticationHandler.isAuthenticationUrl(url)) {
            android.util.Log.d(
                "WebViewClientEx",
                "Authentication URL detected in shouldOverrideUrlLoading (legacy): $url"
            )
            view?.context?.let { context ->
                AuthenticationHandler.openInCustomTab(context, url)
                return true
            }
        }

        // Always notify about URL change to update UI
        onPageUrlChanged(url)

        return handleUrlOverride(view, url)
    }

    // Also intercept page loads before they start
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        // Check if this is an authentication URL before the page starts loading
        if (url != null && AuthenticationHandler.isAuthenticationUrl(url)) {
            android.util.Log.d("WebViewClientEx", "Authentication URL detected in onPageStarted: $url")
            view?.stopLoading() // Stop the WebView from loading this URL

            view?.context?.let { context ->
                AuthenticationHandler.openInCustomTab(context, url)
                // Don't call super to prevent the WebView from loading this URL
                return
            }
        }

        super.onPageStarted(view, url, favicon)
        url?.let { onPageUrlChanged(it) }
    }

    // Common URL handling logic for both API versions
    private fun handleUrlOverride(view: WebView?, url: String): Boolean {
        // Check if this is an authentication URL that should be handled with Custom Tabs
        if (AuthenticationHandler.isAuthenticationUrl(url)) {
            view?.context?.let { context ->
                return AuthenticationHandler.openInCustomTab(context, url)
            }
        }

        return when {
            // Handle external schemes (tel, mailto, etc)
            url.startsWith("tel:") ||
                    url.startsWith("mailto:") ||
                    url.startsWith("geo:") ||
                    url.startsWith("sms:") ||
                    url.startsWith("intent:") ||
                    url.startsWith("market:") -> {
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    view?.context?.startActivity(intent)
                    true
                } catch (e: Exception) {
                    // If the intent fails, try to handle market:// links as http links to Play Store
                    if (url.startsWith("market:")) {
                        try {
                            val playStoreUrl = "https://play.google.com/store/apps/details?id=" +
                                    url.substringAfter("id=").substringBefore("&")
                            val browserIntent =
                                android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(playStoreUrl))
                            browserIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            view?.context?.startActivity(browserIntent)
                            return true
                        } catch (e2: Exception) {
                            // If all else fails, let the system handle it
                            return false
                        }
                    }
                    false
                }
            }
            // Handle file downloads
            url.endsWith(".pdf") ||
                    url.endsWith(".doc") ||
                    url.endsWith(".docx") ||
                    url.endsWith(".xls") ||
                    url.endsWith(".xlsx") ||
                    url.endsWith(".zip") ||
                    url.endsWith(".rar") ||
                    url.endsWith(".apk") -> {
                try {
                    // Let the system download manager handle it
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                    intent.data = Uri.parse(url)
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    view?.context?.startActivity(intent)
                    true
                } catch (e: Exception) {
                    // If no app can handle it, let WebView try to handle it
                    false
                }
            }
            // Handle regular URLs - let WebView load them directly
            else -> {
                // Return false to let WebView handle normal URLs
                // This is critical for links to work properly
                false
            }
        }
    }
}
