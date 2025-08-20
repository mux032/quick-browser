package com.quick.browser.ui.bubble

import android.content.Context
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.quick.browser.R
import com.quick.browser.manager.ReadabilityExtractor
import com.quick.browser.manager.SettingsManager
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
        fun onReadModeScrollDown()
        fun onReadModeScrollUp()
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
            javaScriptEnabled = true
            builtInZoomControls = false
            displayZoomControls = false
        }
        
        // Load original content if available
        originalContent?.let { url ->
            webView?.loadUrl(url)
        }
        
        // Update button state
        updateReadModeButton()
        
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
                    
                    // Create styled HTML with current settings
                    val styledHtml = createStyledHtml(readableContent)
                    
                    // Apply read mode settings to WebView
                    configureWebViewForReadMode()
                    
                    // Load the styled content
                    webView?.loadDataWithBaseURL(url, styledHtml, "text/html", "UTF-8", null)
                    
                    // Add JavaScript interface for scroll detection
                    setupScrollDetection()
                    
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
            // Enable JavaScript for scroll detection, but disable other scripts for cleaner experience
            javaScriptEnabled = true
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
        // Remove JavaScript interface for reader mode
        webView?.removeJavascriptInterface("ReaderModeScrollDetector")
        
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
     * Set up JavaScript interface for scroll detection in reader mode
     */
    private fun setupScrollDetection() {
        webView?.addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun onScrollDown() {
                // Post to main thread to update UI
                webView?.post {
                    listener?.onReadModeScrollDown()
                }
            }

            @android.webkit.JavascriptInterface
            fun onScrollUp() {
                // Post to main thread to update UI
                webView?.post {
                    listener?.onReadModeScrollUp()
                }
            }
        }, "ReaderModeScrollDetector") // Use a different name to avoid conflicts
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
     * 
     * @param content The extracted readable content
     * @return The styled HTML string
     */
    private fun createStyledHtml(content: ReadabilityExtractor.ReadableContent): String {
        // Get current reader mode settings
        val readerBackground = settingsManager.getReaderBackground()
        val fontSize = settingsManager.getReaderFontSize()
        val textAlign = settingsManager.getReaderTextAlign()
        
        // Define color schemes for different backgrounds
        val colors = when (readerBackground) {
            SettingsManager.READER_BG_DARK -> arrayOf("#121212", "#E0E0E0", "#90CAF9", "#B0B0B0", "#1E1E1E", "#616161")
            SettingsManager.READER_BG_SEPIA -> arrayOf("#F4F1E8", "#5D4E37", "#8B4513", "#8B7355", "#EAE7DC", "#D2B48C")
            else -> arrayOf("#FFFFFF", "#212121", "#1976D2", "#757575", "#F5F5F5", "#9E9E9E")
        }
        
        // Map text alignment values
        val textAlignStyle = when (textAlign) {
            SettingsManager.READER_ALIGN_LEFT -> "left"
            SettingsManager.READER_ALIGN_CENTER -> "center"
            SettingsManager.READER_ALIGN_RIGHT -> "right"
            SettingsManager.READER_ALIGN_JUSTIFY -> "justify"
            else -> "left"
        }
        
        // Create responsive HTML with embedded CSS
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
                        background-color: ${colors[0]};
                        color: ${colors[1]};
                        margin: 0;
                        padding: 20px;
                        font-size: ${fontSize}px;
                        line-height: 1.6;
                        text-align: $textAlignStyle;
                    }
                    h1, h2, h3, h4, h5, h6 {
                        color: ${colors[2]};
                        margin-top: 1.5em;
                        margin-bottom: 0.5em;
                    }
                    h1 {
                        font-size: 1.8em;
                        border-bottom: 1px solid ${colors[3]};
                        padding-bottom: 0.3em;
                    }
                    h2 {
                        font-size: 1.5em;
                    }
                    h3 {
                        font-size: 1.3em;
                    }
                    p {
                        margin-top: 0;
                        margin-bottom: 1em;
                        text-align: $textAlignStyle;
                    }
                    a {
                        color: ${colors[2]};
                        text-decoration: none;
                    }
                    a:hover {
                        text-decoration: underline;
                    }
                    img {
                        max-width: 100%;
                        height: auto;
                        display: block;
                        margin: 1em auto;
                        border-radius: 4px;
                    }
                    blockquote {
                        border-left: 4px solid ${colors[4]};
                        margin: 1.5em 0;
                        padding: 0.5em 1em;
                        color: ${colors[5]};
                        font-style: italic;
                    }
                    pre, code {
                        background-color: ${colors[4]};
                        border-radius: 4px;
                        padding: 0.2em 0.4em;
                        font-family: 'Courier New', Courier, monospace;
                        overflow-x: auto;
                    }
                    pre {
                        padding: 1em;
                        overflow-x: auto;
                        white-space: pre-wrap;
                        word-wrap: break-word;
                    }
                    pre code {
                        background-color: transparent;
                        padding: 0;
                    }
                    hr {
                        border: 0;
                        border-top: 1px solid ${colors[3]};
                        margin: 2em 0;
                    }
                    ul, ol {
                        padding-left: 1.5em;
                        margin: 1em 0;
                    }
                    li {
                        margin-bottom: 0.5em;
                    }
                    table {
                        border-collapse: collapse;
                        width: 100%;
                        margin: 1em 0;
                    }
                    th, td {
                        border: 1px solid ${colors[3]};
                        padding: 0.5em;
                        text-align: left;
                    }
                    th {
                        background-color: ${colors[4]};
                    }
                    // Add JavaScript for scroll detection
                    // Variables for scroll tracking
                    var lastScrollY = window.scrollY || document.documentElement.scrollTop;
                    var lastScrollDirection = null;
                    var scrollThreshold = 3; // Lower threshold for more sensitivity
                    var consecutiveThreshold = 2; // Number of consecutive scrolls in same direction to trigger
                    var consecutiveCount = 0;
                    var lastNotifiedDirection = null;
                    
                    // Use requestAnimationFrame for smoother performance
                    var ticking = false;
                    
                    // Main scroll handler
                    window.addEventListener('scroll', function() {
                        if (!ticking) {
                            window.requestAnimationFrame(function() {
                                var currentScrollY = window.scrollY || document.documentElement.scrollTop;
                                var scrollDelta = currentScrollY - lastScrollY;
                                
                                // Determine scroll direction
                                if (Math.abs(scrollDelta) > scrollThreshold) {
                                    var currentDirection = scrollDelta > 0 ? 'down' : 'up';
                                    
                                    // Check if we're continuing in the same direction
                                    if (currentDirection === lastScrollDirection) {
                                        consecutiveCount++;
                                    } else {
                                        consecutiveCount = 1;
                                        lastScrollDirection = currentDirection;
                                    }
                                    
                                    // Only notify when we have enough consecutive scrolls in the same direction
                                    // or when direction changes from the last notification
                                    if ((consecutiveCount >= consecutiveThreshold && 
                                        currentDirection !== lastNotifiedDirection) || 
                                        (currentDirection !== lastNotifiedDirection && 
                                        Math.abs(scrollDelta) > scrollThreshold * 3)) {
                                        
                                        if (currentDirection === 'down' && window.ReaderModeScrollDetector) {
                        window.ReaderModeScrollDetector.onScrollDown();
                    } else if (window.ReaderModeScrollDetector) {
                        window.ReaderModeScrollDetector.onScrollUp();
                    }
                                        lastNotifiedDirection = currentDirection;
                                    }
                                    
                                    lastScrollY = currentScrollY;
                                }
                                
                                ticking = false;
                            });
                            
                            ticking = true;
                        }
                    }, { passive: true });
                </style>
            </head>
            <body>
                <article>
                    <header>
                        <h1>${content.title}</h1>
                        ${if (content.byline != null) "<p class='byline'>By ${content.byline}</p>" else ""}
                        ${if (content.siteName != null || content.publishDate != null) """
                        <div class='article-meta'>
                            ${if (content.siteName != null) "<span class='site-name'>${content.siteName}</span>" else ""}
                            ${if (content.publishDate != null) "<span class='publish-date'>${content.publishDate}</span>" else ""}
                        </div>
                        """ else ""}
                    </header>
                    <main>
                        ${content.content}
                    </main>
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
    
    /**
     * Refresh reader mode content with current settings
     * Should be called when reader mode settings change
     */
    fun refreshReaderModeContent() {
        if (isReadMode && currentUrl != null) {
            openReadMode()
        }
    }
}