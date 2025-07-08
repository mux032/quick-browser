package com.qb.browser.ui.bubble

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import com.qb.browser.QBApplication
import com.qb.browser.manager.AdBlocker
import com.qb.browser.manager.SettingsManager
import com.qb.browser.viewmodel.WebViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manager class responsible for WebView setup, configuration, and interaction
 *
 * This class encapsulates all WebView-related functionality including:
 * - WebView configuration and settings
 * - WebView client setup (WebChromeClient and WebViewClient)
 * - URL loading and navigation
 * - JavaScript injection and evaluation
 * - Progress tracking and error handling
 * - Favicon and title management
 *
 * @param context The context used for WebView operations
 * @param bubbleId Unique identifier for the bubble
 * @param bubbleView Reference to the parent BubbleView for callbacks
 * @param settingsManager Manager for app settings
 * @param adBlocker Ad blocking functionality
 */
class BubbleWebViewManager(
    private val context: Context,
    private val bubbleId: String,
    private val bubbleView: BubbleView,
    private val settingsManager: SettingsManager,
    private val adBlocker: AdBlocker
) {

    companion object {
        private const val TAG = "BubbleWebViewManager"
    }

    // Dependencies
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    // WebView reference
    private var webView: WebView? = null
    private var progressBar: ProgressBar? = null
    private var webViewModel: WebViewModel? = null

    // Callbacks
    private var onUrlChangedCallback: ((String) -> Unit)? = null
    private var onHtmlContentLoadedCallback: ((String) -> Unit)? = null
    private var onScrollDownCallback: (() -> Unit)? = null
    private var onScrollUpCallback: (() -> Unit)? = null
    private var onFaviconReceivedCallback: ((Bitmap) -> Unit)? = null
    private var onTitleReceivedCallback: ((String) -> Unit)? = null
    private var onProgressChangedCallback: ((Int) -> Unit)? = null

    /**
     * Initialize the WebView manager with required components
     *
     * @param webView The WebView instance to manage
     * @param progressBar Progress bar for loading indication
     * @param webViewModel WebViewModel for favicon and title management
     */
    fun initialize(
        webView: WebView,
        progressBar: ProgressBar,
        webViewModel: WebViewModel?
    ) {
        this.webView = webView
        this.progressBar = progressBar
        this.webViewModel = webViewModel

        setupWebView()
    }

    /**
     * Set up WebView configuration, settings, and clients
     */
    private fun setupWebView() {
        val webView = this.webView ?: return

        try {
            Log.d(TAG, "Setting up WebView for bubble: $bubbleId")

            // Set WebView background
            webView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))

            // Configure WebView settings
            configureWebViewSettings(webView)

            // Set up WebView clients
            setupWebViewClients(webView)

            // Set up JavaScript interface for scroll detection
            setupScrollDetection(webView)

            Log.d(TAG, "WebView setup complete for bubble: $bubbleId")

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up WebView for bubble $bubbleId", e)
        }
    }

    /**
     * Configure WebView settings based on user preferences
     */
    private fun configureWebViewSettings(webView: WebView) {
        webView.settings.apply {
            // Basic settings
            loadWithOverviewMode = true
            useWideViewPort = true

            // Zoom settings
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)

            // Default encoding
            defaultTextEncodingName = "UTF-8"

            // JavaScript settings based on user preferences
            javaScriptEnabled = settingsManager.isJavaScriptEnabled()
            javaScriptCanOpenWindowsAutomatically = settingsManager.isJavaScriptEnabled()

            // Mixed content settings
            mixedContentMode = if (settingsManager.isSecureMode()) {
                android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            } else {
                android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            // File access settings
            allowContentAccess = !settingsManager.isSecureMode()
            allowFileAccess = !settingsManager.isSecureMode()

            // Storage settings
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

            // Content settings
            loadsImagesAutomatically = true
            blockNetworkImage = false
            blockNetworkLoads = false

            // Media settings
            mediaPlaybackRequiresUserGesture = false

            // Location settings
            setGeolocationEnabled(true)

            // Window settings
            setSupportMultipleWindows(true)
        }

        // Configure performance settings
        configurePerformanceSettings(webView)
    }

    /**
     * Configure performance-related WebView settings
     */
    private fun configurePerformanceSettings(webView: WebView) {
        try {
            @Suppress("DEPRECATION")
            webView.settings.setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH)
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting hardware acceleration", e)
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
    }

    /**
     * Set up WebChromeClient and WebViewClient
     */
    private fun setupWebViewClients(webView: WebView) {
        // Set up WebChromeClient for progress, favicon and title handling
        webView.webChromeClient = createWebChromeClient()

        // Set up WebViewClient for page loading and error handling
        webView.webViewClient = createScrollAwareWebViewClient()
    }

    /**
     * Create WebChromeClient for handling progress, favicons, and titles
     */
    private fun createWebChromeClient(): WebChromeClient {
        return object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                onProgressChangedCallback?.invoke(newProgress)
            }

            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                icon?.let {
                    handleReceivedFavicon(it)
                    onFaviconReceivedCallback?.invoke(it)
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                title?.let {
                    handleReceivedTitle(it)
                    onTitleReceivedCallback?.invoke(it)
                }
            }
        }
    }

    /**
     * Create ScrollAwareWebViewClient for handling page loading and scroll detection
     */
    private fun createScrollAwareWebViewClient(): ScrollAwareWebViewClient {
        return ScrollAwareWebViewClient(
            context,
            onPageUrlChanged = { newUrl ->
                onUrlChangedCallback?.invoke(newUrl)
            },
            onHtmlContentLoaded = { htmlContent ->
                onHtmlContentLoadedCallback?.invoke(htmlContent)
            },
            onScrollDown = {
                onScrollDownCallback?.invoke()
            },
            onScrollUp = {
                onScrollUpCallback?.invoke()
            },
            settingsManager = settingsManager,
            adBlocker = adBlocker
        )
    }

    /**
     * Set up JavaScript interface for scroll detection
     */
    private fun setupScrollDetection(webView: WebView) {
        webView.addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun onScrollDown() {
                coroutineScope.launch {
                    onScrollDownCallback?.invoke()
                }
            }

            @android.webkit.JavascriptInterface
            fun onScrollUp() {
                coroutineScope.launch {
                    onScrollUpCallback?.invoke()
                }
            }
        }, "ScrollDetector")
    }

    /**
     * Handle received favicon from WebView
     */
    private fun handleReceivedFavicon(favicon: Bitmap) {
        Log.d(TAG, "Received favicon for bubble: $bubbleId")

        // Update in WebViewModel to persist the favicon
        webViewModel?.let { viewModel ->
            val currentUrl = webView?.url
            if (currentUrl != null) {
                viewModel.updateFavicon(currentUrl, favicon)
                Log.d(TAG, "Updated favicon in WebViewModel for URL: $currentUrl")
            }
        }
    }

    /**
     * Handle received page title from WebView
     */
    private fun handleReceivedTitle(title: String) {
        Log.d(TAG, "Received page title for bubble $bubbleId: $title")

        // Update title in WebViewModel
        webViewModel?.let { viewModel ->
            val currentUrl = webView?.url
            if (currentUrl != null) {
                viewModel.updateTitle(currentUrl, title)
                Log.d(TAG, "Updated title in WebViewModel for URL: $currentUrl")
            }
        }

        // Note: Global WebViewModel access removed as part of DI refactoring
        // Title updates now handled properly through injected WebViewModel instances
    }

    /**
     * Load URL in the WebView
     *
     * @param url The URL to load
     */
    fun loadUrl(url: String) {
        val webView = this.webView ?: return

        if (url.isEmpty()) {
            Log.d(TAG, "Empty URL provided for bubble $bubbleId")
            webView.loadUrl("about:blank")
            return
        }

        val formattedUrl = formatUrl(url)
        if (formattedUrl.isEmpty()) {
            Log.d(TAG, "Invalid URL format for bubble $bubbleId: $url")
            webView.loadUrl("about:blank")
            return
        }

        try {
            Log.d(TAG, "Loading URL in WebView for bubble $bubbleId: $formattedUrl")

            // Clear any existing page
            webView.clearHistory()
            webView.clearCache(true)

            // Make sure WebView is in a good state for loading
            webView.stopLoading()

            // Load the URL with additional headers
            val headers = HashMap<String, String>()
            headers["User-Agent"] = webView.settings.userAgentString
            headers["Accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
            headers["Accept-Language"] = "en-US,en;q=0.5"

            webView.loadUrl(formattedUrl, headers)

            Log.d(TAG, "URL load initiated for bubble $bubbleId")

        } catch (e: Exception) {
            Log.e(TAG, "Error loading URL for bubble $bubbleId", e)
            webView.loadUrl("about:blank")
        }
    }

    /**
     * Reload the current page
     */
    fun reload() {
        webView?.reload()
    }

    /**
     * Stop loading the current page
     */
    fun stopLoading() {
        webView?.stopLoading()
    }

    /**
     * Go back in WebView history
     */
    fun goBack() {
        webView?.let { webView ->
            if (webView.canGoBack()) {
                webView.goBack()
            }
        }
    }

    /**
     * Go forward in WebView history
     */
    fun goForward() {
        webView?.let { webView ->
            if (webView.canGoForward()) {
                webView.goForward()
            }
        }
    }

    /**
     * Evaluate JavaScript in the WebView
     *
     * @param script The JavaScript code to execute
     * @param callback Optional callback to receive the result
     */
    fun evaluateJavaScript(script: String, callback: ((String?) -> Unit)? = null) {
        webView?.evaluateJavascript(script, callback)
    }

    /**
     * Apply dynamic zoom to the WebView content
     *
     * @param zoomPercent The zoom percentage (e.g., 150 for 150%)
     */
    fun applyDynamicZoom(zoomPercent: Int) {
        val zoomFactor = zoomPercent / 100.0
        val widthPercent = (100.0 / zoomFactor).toInt()

        val script = """
            javascript:(function() {
                var meta = document.querySelector('meta[name="viewport"]');
                if (!meta) {
                    meta = document.createElement('meta');
                    meta.name = 'viewport';
                    document.head.appendChild(meta);
                }
                meta.content = 'width=device-width, initial-scale=${zoomFactor}, maximum-scale=1.0, user-scalable=yes';
                
                document.body.style.zoom = "${zoomPercent}%";
                document.body.style.transformOrigin = "0 0";
                document.body.style.transform = "scale(${zoomFactor})";
                document.body.style.width = "${widthPercent}%";
                
                return "Zoom applied: ${zoomPercent}%";
            })()
        """.trimIndent()

        evaluateJavaScript(script)
        Log.d(TAG, "Applied dynamic zoom: $zoomPercent%")
    }

    /**
     * Get the current URL of the WebView
     */
    fun getCurrentUrl(): String? {
        return webView?.url
    }

    /**
     * Get the current title of the WebView
     */
    fun getCurrentTitle(): String? {
        return webView?.title
    }

    /**
     * Check if the WebView can go back
     */
    fun canGoBack(): Boolean {
        return webView?.canGoBack() ?: false
    }

    /**
     * Check if the WebView can go forward
     */
    fun canGoForward(): Boolean {
        return webView?.canGoForward() ?: false
    }

    /**
     * Set the WebView's visibility and alpha
     */
    fun setVisibility(visibility: Int, alpha: Float = 1f) {
        webView?.visibility = visibility
        webView?.alpha = alpha
    }

    /**
     * Format URL to ensure it has proper protocol
     */
    private fun formatUrl(url: String): String {
        return when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.startsWith("file://") -> url
            url.startsWith("data:") -> url
            url.startsWith("javascript:") -> url
            url.contains("://") -> url // Already has a protocol
            url.isEmpty() -> ""
            else -> "https://$url" // Default to HTTPS
        }
    }

    // Callback setters
    fun setOnUrlChangedCallback(callback: (String) -> Unit) {
        onUrlChangedCallback = callback
    }

    fun setOnHtmlContentLoadedCallback(callback: (String) -> Unit) {
        onHtmlContentLoadedCallback = callback
    }

    fun setOnScrollDownCallback(callback: () -> Unit) {
        onScrollDownCallback = callback
    }

    fun setOnScrollUpCallback(callback: () -> Unit) {
        onScrollUpCallback = callback
    }

    fun setOnFaviconReceivedCallback(callback: (Bitmap) -> Unit) {
        onFaviconReceivedCallback = callback
    }

    fun setOnTitleReceivedCallback(callback: (String) -> Unit) {
        onTitleReceivedCallback = callback
    }

    fun setOnProgressChangedCallback(callback: (Int) -> Unit) {
        onProgressChangedCallback = callback
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        webView?.let { webView ->
            webView.clearHistory()
            webView.clearCache(true)
            webView.removeJavascriptInterface("ScrollDetector")
            webView.webViewClient = android.webkit.WebViewClient()  // Use default WebViewClient instead of null
            webView.webChromeClient = android.webkit.WebChromeClient()  // Use default WebChromeClient instead of null
        }

        // Clear references
        webView = null
        progressBar = null
        webViewModel = null

        // Clear callbacks
        onUrlChangedCallback = null
        onHtmlContentLoadedCallback = null
        onScrollDownCallback = null
        onScrollUpCallback = null
        onFaviconReceivedCallback = null
        onTitleReceivedCallback = null
        onProgressChangedCallback = null
    }
}