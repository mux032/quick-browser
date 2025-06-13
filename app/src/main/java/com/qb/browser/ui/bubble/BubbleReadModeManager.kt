package com.qb.browser.ui.bubble

import android.content.Context
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.qb.browser.R
import com.qb.browser.manager.ReadabilityExtractor
import com.qb.browser.manager.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages the read mode functionality for BubbleView.
 * 
 * This class handles all read mode-related operations including content extraction,
 * HTML styling, UI state management, and WebView configuration. It provides
 * a clean interface for toggling between normal web view and reader mode.
 * 
 * Responsibilities:
 * - Read mode toggle and UI state management
 * - Content extraction using ReadabilityExtractor
 * - HTML styling and theme application
 * - WebView settings configuration for read mode
 * - Error handling and fallback mechanisms
 * 
 * @param context Android context for accessing resources and services
 * @param settingsManager Manager for accessing user preferences like dark theme
 */
class BubbleReadModeManager(
    private val context: Context,
    private val settingsManager: SettingsManager
) {
    
    companion object {
        private const val TAG = "BubbleReadModeManager"
    }
    
    // UI components - will be initialized when views are set up
    private var btnReadMode: MaterialButton? = null
    private var webView: WebView? = null
    private var progressBar: ProgressBar? = null
    
    // Read mode state
    private var isReadMode = false
    private var originalContent: String? = null
    private var currentUrl: String? = null
    
    // Content extraction
    private val contentExtractor by lazy { ReadabilityExtractor(context) }
    
    // Callback interface for BubbleView to respond to read mode events
    interface ReadModeManagerListener {
        fun onReadModeChanged(isReadMode: Boolean)
        fun onReadModeLoadingStarted()
        fun onReadModeLoadingCompleted(success: Boolean)
        fun onReadModeError(message: String)
        fun onBubbleExpandRequested()
    }
    
    private var listener: ReadModeManagerListener? = null
    
    /**
     * Set the listener for read mode manager events
     */
    fun setListener(listener: ReadModeManagerListener?) {
        this.listener = listener
    }
    
    /**
     * Initialize the read mode manager with the required UI components
     * 
     * @param btnReadMode The button to toggle read mode
     * @param webView The WebView instance to display content
     * @param progressBar The progress indicator for loading
     * @param currentUrl The current URL being displayed
     */
    fun initialize(
        btnReadMode: MaterialButton,
        webView: WebView,
        progressBar: ProgressBar,
        currentUrl: String
    ) {
        this.btnReadMode = btnReadMode
        this.webView = webView
        this.progressBar = progressBar
        this.currentUrl = currentUrl
        
        updateReadModeButton()
    }
    
    /**
     * Toggle between normal web view and read mode
     */
    fun toggleReadMode() {
        isReadMode = !isReadMode
        listener?.onReadModeChanged(isReadMode)
        
        if (isReadMode) {
            enterReadMode()
        } else {
            exitReadMode()
        }
        updateReadModeButton()
    }
    
    /**
     * Enter read mode by extracting and styling content
     */
    private fun enterReadMode() {
        // Save current URL before switching to reader mode
        originalContent = webView?.url
        
        // Request bubble expansion if not already expanded
        listener?.onBubbleExpandRequested()
        
        openReadMode()
    }
    
    /**
     * Exit read mode and return to normal web view
     */
    private fun exitReadMode() {
        // Restore WebView settings for normal mode
        webView?.settings?.apply {
            javaScriptEnabled = settingsManager.isJavaScriptEnabled()
            builtInZoomControls = true
            displayZoomControls = false
            textZoom = 100
        }
        
        // Return to normal web view using cached content if available
        val urlToLoad = originalContent ?: currentUrl
        urlToLoad?.let { url ->
            webView?.loadUrl(url)
        }
        
        // Announce mode change for accessibility
        webView?.announceForAccessibility(context.getString(R.string.web_view_mode))
        Toast.makeText(context, R.string.web_view_mode, Toast.LENGTH_SHORT).show()
        
        listener?.onReadModeLoadingCompleted(true)
    }
    
    /**
     * Open read mode by extracting content and applying styling
     */
    private fun openReadMode() {
        try {
            val url = currentUrl
            if (url.isNullOrEmpty()) {
                handleReadModeError("No URL available for read mode")
                return
            }
            
            // Show loading indicator
            progressBar?.visibility = View.VISIBLE
            progressBar?.isIndeterminate = true
            listener?.onReadModeLoadingStarted()
            
            // Extract content in background
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val readableContent = withContext(Dispatchers.IO) {
                        contentExtractor.extractFromUrl(url)
                    }
                    
                    if (readableContent == null) {
                        handleReadModeError("Failed to extract readable content")
                        return@launch
                    }
                    
                    // Create styled HTML with current theme
                    val isNightMode = settingsManager.isDarkThemeEnabled()
                    val styledHtml = createStyledHtml(readableContent, isNightMode)
                    
                    // Apply read mode settings to WebView
                    configureWebViewForReadMode()
                    
                    // Load the styled content
                    webView?.loadDataWithBaseURL(url, styledHtml, "text/html", "UTF-8", null)
                    
                    // Hide loading indicator
                    progressBar?.visibility = View.GONE
                    progressBar?.isIndeterminate = false
                    webView?.alpha = 1f
                    
                    // Announce success
                    webView?.announceForAccessibility(context.getString(R.string.reader_mode_loaded))
                    Toast.makeText(context, R.string.reader_mode_loaded, Toast.LENGTH_SHORT).show()
                    
                    listener?.onReadModeLoadingCompleted(true)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error extracting content for read mode", e)
                    handleReadModeError("Error processing content: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open read mode", e)
            handleReadModeError("Failed to enter read mode: ${e.message}")
        }
    }
    
    /**
     * Configure WebView settings for optimal read mode experience
     */
    private fun configureWebViewForReadMode() {
        webView?.settings?.apply {
            // Disable JavaScript for reader mode (cleaner experience)
            javaScriptEnabled = false
            // Enable built-in zoom for text scaling
            builtInZoomControls = true
            displayZoomControls = false
            // Reset text zoom to default
            textZoom = 100
        }
    }
    
    /**
     * Handle read mode errors and reset state
     */
    private fun handleReadModeError(message: String) {
        // Hide loading indicator
        progressBar?.visibility = View.GONE
        progressBar?.isIndeterminate = false
        
        // Show error message
        Toast.makeText(context, R.string.failed_to_load_reader_mode, Toast.LENGTH_SHORT).show()
        Log.e(TAG, "Read mode error: $message")
        
        // Reset read mode state on error
        isReadMode = false
        updateReadModeButton()
        
        // Restore original content if available
        originalContent?.let { url ->
            webView?.loadUrl(url)
        }
        
        listener?.onReadModeError(message)
        listener?.onReadModeLoadingCompleted(false)
    }
    
    /**
     * Update the read mode button appearance based on current state
     */
    private fun updateReadModeButton() {
        btnReadMode?.apply {
            val iconRes = if (isReadMode) R.drawable.ic_globe else R.drawable.ic_read_mode
            val contentDesc = if (isReadMode) 
                context.getString(R.string.web_view_mode) 
            else 
                context.getString(R.string.read_mode)
                
            setIconResource(iconRes)
            setIconTint(ContextCompat.getColorStateList(context, R.color.colorPrimary))
            contentDescription = contentDesc
        }
    }
    
    /**
     * Create styled HTML for reader mode with responsive design and theme support
     */
    private fun createStyledHtml(content: ReadabilityExtractor.ReadableContent, isNightMode: Boolean): String {
        val backgroundColor = if (isNightMode) "#121212" else "#FAFAFA"
        val textColor = if (isNightMode) "#E0E0E0" else "#212121"
        val linkColor = if (isNightMode) "#90CAF9" else "#1976D2"
        val secondaryTextColor = if (isNightMode) "#B0B0B0" else "#666666"
        val codeBackgroundColor = if (isNightMode) "#1E1E1E" else "#F5F5F5"
        val borderColor = if (isNightMode) "#616161" else "#BDBDBD"
        
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>${content.title}</title>
                <style>
                    :root {
                        --content-width: 100%;
                        --body-padding: clamp(16px, 5%, 32px);
                        --content-max-width: 800px;
                    }
                    
                    * {
                        box-sizing: border-box;
                    }
                    
                    body {
                        font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;
                        line-height: 1.8;
                        color: $textColor;
                        background-color: $backgroundColor;
                        padding: var(--body-padding);
                        margin: 0 auto;
                        max-width: var(--content-max-width);
                        text-rendering: optimizeLegibility;
                        -webkit-font-smoothing: antialiased;
                        -moz-osx-font-smoothing: grayscale;
                    }
                    
                    article {
                        width: var(--content-width);
                        margin: 0 auto;
                    }
                    
                    h1, h2, h3, h4, h5, h6 {
                        line-height: 1.3;
                        margin: 1.5em 0 0.5em;
                        font-weight: 600;
                        color: $textColor;
                    }
                    
                    h1 {
                        font-size: clamp(1.5em, 5vw, 2em);
                        letter-spacing: -0.02em;
                        margin-bottom: 0.5em;
                        border-bottom: 2px solid ${linkColor}40;
                        padding-bottom: 0.5em;
                    }
                    
                    h2 {
                        font-size: clamp(1.3em, 4vw, 1.75em);
                        margin-top: 2em;
                    }
                    
                    h3 {
                        font-size: clamp(1.2em, 3vw, 1.5em);
                    }
                    
                    p {
                        margin: 1.5em 0;
                        font-size: clamp(1em, 2vw, 1.1em);
                        text-align: justify;
                        hyphens: auto;
                    }
                    
                    .byline {
                        font-size: 0.9em;
                        color: $secondaryTextColor;
                        margin-bottom: 2em;
                        padding: 1em 0;
                        border-bottom: 1px solid ${borderColor}40;
                        font-style: italic;
                    }
                    
                    a {
                        color: $linkColor;
                        text-decoration: none;
                        border-bottom: 1px solid ${linkColor}40;
                        transition: all 0.2s ease;
                        word-wrap: break-word;
                    }
                    
                    a:hover {
                        border-bottom-color: $linkColor;
                        background-color: ${linkColor}10;
                        padding: 0 2px;
                        border-radius: 2px;
                    }
                    
                    img {
                        max-width: 100%;
                        height: auto;
                        margin: 2em 0;
                        border-radius: 8px;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.1);
                        display: block;
                        margin-left: auto;
                        margin-right: auto;
                    }
                    
                    figure {
                        margin: 2em 0;
                        text-align: center;
                    }
                    
                    figcaption {
                        font-size: 0.9em;
                        color: $secondaryTextColor;
                        margin-top: 0.5em;
                        font-style: italic;
                    }
                    
                    blockquote {
                        margin: 2em 0;
                        padding: 1.5em 2em;
                        border-left: 4px solid $linkColor;
                        background-color: ${linkColor}08;
                        font-style: italic;
                        color: $secondaryTextColor;
                        border-radius: 0 8px 8px 0;
                        position: relative;
                    }
                    
                    blockquote::before {
                        content: '"';
                        font-size: 3em;
                        color: ${linkColor}40;
                        position: absolute;
                        top: 0.2em;
                        left: 0.3em;
                        line-height: 1;
                    }
                    
                    code {
                        font-family: 'SF Mono', Consolas, Monaco, 'Andale Mono', 'Ubuntu Mono', monospace;
                        background-color: $codeBackgroundColor;
                        padding: 0.2em 0.4em;
                        border-radius: 4px;
                        font-size: 0.9em;
                        border: 1px solid ${borderColor}40;
                    }
                    
                    pre {
                        background-color: $codeBackgroundColor;
                        padding: 1.5em;
                        border-radius: 8px;
                        overflow-x: auto;
                        font-size: 0.9em;
                        border: 1px solid ${borderColor}40;
                        margin: 2em 0;
                        line-height: 1.4;
                    }
                    
                    pre code {
                        background: none;
                        padding: 0;
                        border: none;
                        border-radius: 0;
                    }
                    
                    ul, ol {
                        padding-left: 2em;
                        margin: 1.5em 0;
                    }
                    
                    li {
                        margin: 0.8em 0;
                        line-height: 1.6;
                    }
                    
                    li p {
                        margin: 0.5em 0;
                    }
                    
                    hr {
                        border: none;
                        border-top: 2px solid ${borderColor}40;
                        margin: 3em 0;
                        border-radius: 1px;
                    }
                    
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin: 2em 0;
                        font-size: 0.9em;
                    }
                    
                    th, td {
                        padding: 0.8em;
                        text-align: left;
                        border-bottom: 1px solid ${borderColor}40;
                    }
                    
                    th {
                        background-color: ${linkColor}10;
                        font-weight: 600;
                        color: $textColor;
                    }
                    
                    tr:hover {
                        background-color: ${textColor}05;
                    }
                    
                    /* Responsive adjustments */
                    @media (max-width: 600px) {
                        :root {
                            --body-padding: 16px;
                        }
                        
                        h1 {
                            font-size: 1.5em;
                        }
                        
                        blockquote {
                            padding: 1em;
                            margin: 1.5em 0;
                        }
                        
                        pre {
                            padding: 1em;
                            font-size: 0.8em;
                        }
                        
                        table {
                            font-size: 0.8em;
                        }
                        
                        th, td {
                            padding: 0.5em;
                        }
                    }
                    
                    /* Accessibility improvements */
                    @media (prefers-reduced-motion: reduce) {
                        * {
                            animation-duration: 0.01ms !important;
                            animation-iteration-count: 1 !important;
                            transition-duration: 0.01ms !important;
                            scroll-behavior: auto !important;
                        }
                    }
                    
                    @media (prefers-contrast: high) {
                        a {
                            border-bottom-width: 2px;
                        }
                        
                        blockquote {
                            border-left-width: 6px;
                        }
                    }
                    
                    /* Focus styles for better accessibility */
                    a:focus {
                        outline: 2px solid $linkColor;
                        outline-offset: 2px;
                        border-radius: 4px;
                    }
                </style>
            </head>
            <body>
                <article>
                    <h1>${content.title}</h1>
                    ${if (!content.byline.isNullOrEmpty()) "<div class=\"byline\">${content.byline}</div>" else ""}
                    ${content.content}
                </article>
            </body>
            </html>
        """.trimIndent()
    }
    
    /**
     * Update the current URL (useful when navigating to new pages)
     */
    fun updateCurrentUrl(url: String) {
        currentUrl = url
        // Clear cached content when URL changes
        originalContent = null
    }
    
    /**
     * Check if currently in read mode
     */
    fun isReadMode(): Boolean {
        return isReadMode
    }
    
    /**
     * Force exit read mode (useful when bubble is collapsed or closed)
     */
    fun forceExitReadMode() {
        if (isReadMode) {
            isReadMode = false
            exitReadMode()
        }
    }
    
    /**
     * Check if read mode is available for the current URL
     */
    fun isReadModeAvailable(): Boolean {
        val url = currentUrl ?: return false
        // Read mode is available for HTTP/HTTPS URLs
        return url.startsWith("http://") || url.startsWith("https://")
    }
    
    /**
     * Get the original URL before entering read mode
     */
    fun getOriginalUrl(): String? {
        return originalContent
    }
}