package com.qb.browser.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.view.LayoutInflater
import androidx.core.widget.NestedScrollView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.qb.browser.R
import com.qb.browser.Constants
import com.qb.browser.util.ContentExtractor
import com.qb.browser.util.SettingsManager
import com.qb.browser.util.TextToSpeechManager
import com.qb.browser.util.ErrorHandler
import com.qb.browser.util.withErrorHandlingAndFallback
import com.qb.browser.util.SummarizationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity for displaying web pages in a clean, reader-friendly format
 * with added features for text-to-speech, font customization and theme selection
 */
class ReadModeActivity : AppCompatActivity(), TextToSpeechManager.TtsCallback {
    
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorView: TextView
    private lateinit var settingsManager: SettingsManager
    private lateinit var contentExtractor: ContentExtractor
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var fabSummarize: FloatingActionButton
    private lateinit var summaryContainer: NestedScrollView
    private lateinit var summaryContent: LinearLayout
    private lateinit var summaryProgress: ProgressBar
    
    private var currentUrl: String = ""
    private var bubbleId: String = ""
    private var currentFontSize = Constants.DEFAULT_FONT_SIZE
    private var currentTheme = SettingsManager.THEME_LIGHT
    private var isTtsSpeaking = false
    private var extractedText: String = ""
    private var isSummaryMode = false
    private var summarizationManager: SummarizationManager? = null
    
    companion object {
        // Using centralized constants from Constants.kt
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_mode)
        
        settingsManager = SettingsManager.getInstance(this)
        contentExtractor = ContentExtractor(this)
        ttsManager = TextToSpeechManager(this)
        
        // Initialize UI components
        webView = findViewById(R.id.webview_read_mode)
        progressBar = findViewById(R.id.progress_bar)
        errorView = findViewById(R.id.error_text)
        fabSummarize = findViewById(R.id.fab_summarize)
        summaryContainer = findViewById(R.id.summary_container)
        summaryContent = findViewById(R.id.summary_content)
        summaryProgress = findViewById(R.id.summary_progress)
        
        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Initialize summarization manager
        summarizationManager = SummarizationManager.getInstance(this)
        
        // Setup FAB click listener
        fabSummarize.setOnClickListener {
            toggleSummaryMode()
        }
        
        // Get extras from intent
        currentUrl = intent.getStringExtra(Constants.EXTRA_URL) ?: ""
        bubbleId = intent.getStringExtra(Constants.EXTRA_BUBBLE_ID) ?: ""
        
        // Load user preferences
        currentTheme = settingsManager.getCurrentTheme()
        currentFontSize = settingsManager.getTextSize()
        
        // Set up WebView
        setupWebView()
        
        // Initialize TTS
        ttsManager.initialize(this)
        
        // Load content
        if (currentUrl.isNotEmpty()) {
            loadContent(currentUrl)
        } else {
            showError("No URL provided")
        }
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = false
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            textZoom = (currentFontSize * 100) / Constants.DEFAULT_FONT_SIZE // Convert font size to percentage
        }
        
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                // Handle links within the read mode
                val clickedUrl = request.url.toString()
                if (clickedUrl.startsWith("http")) {
                    // For external links, reload in read mode
                    loadContent(clickedUrl)
                    return true
                }
                return false
            }
        }
        
        // Apply current theme
        applyTheme()
    }
    
    private fun loadContent(url: String) {
        progressBar.visibility = View.VISIBLE
        errorView.visibility = View.GONE
        webView.visibility = View.GONE
        summaryContainer.visibility = View.GONE
        fabSummarize.visibility = View.GONE
        
        // Reset summary mode
        isSummaryMode = false
        
        // Stop TTS if playing
        if (isTtsSpeaking) {
            stopTextToSpeech()
        }
        
        // Validate URL format
        val validatedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }
        
        lifecycleScope.launch {
            // Using the withErrorHandlingAndFallback extension function
            val result = withErrorHandlingAndFallback(
                tag = "ReadModeActivity",
                errorMessage = "Failed to load content from $validatedUrl",
                fallback = null,
                context = this@ReadModeActivity,
                view = webView,
                showError = true
            ) {
                // This code runs with error handling
                withContext(Dispatchers.IO) {
                    contentExtractor.extractReadableContent(validatedUrl)
                }
            }
            
            if (result != null) {
                // Save the current URL and extracted text for TTS
                currentUrl = validatedUrl
                extractedText = result.title + "\n\n" + 
                                (if (result.byline.isNotEmpty()) result.byline + "\n\n" else "") + 
                                stripHtml(result.content)
                
                withContext(Dispatchers.Main) {
                    // Update the page title
                    supportActionBar?.title = result.title
                    
                    // Create HTML with proper styling based on current theme
                    val htmlContent = createStyledHtml(
                        result.title,
                        result.content, 
                        result.byline
                    )
                    
                    // Store the raw content in the WebView tag for later use
                    webView.tag = result.content
                    
                    // Load the content
                    webView.loadDataWithBaseURL(validatedUrl, htmlContent, "text/html", "UTF-8", null)
                    progressBar.visibility = View.GONE
                    webView.visibility = View.VISIBLE
                    fabSummarize.visibility = View.VISIBLE
                }
            } else {
                withContext(Dispatchers.Main) {
                    showError("Failed to load content from $validatedUrl")
                }
            }
        }
    }
    
    private fun createStyledHtml(title: String, content: String, byline: String): String {
        // Get colors based on current theme
        val (backgroundColor, textColor, linkColor) = when (currentTheme) {
            SettingsManager.THEME_DARK -> Triple(
                ContextCompat.getColor(this, R.color.read_mode_background_dark),
                ContextCompat.getColor(this, R.color.read_mode_text_dark),
                ContextCompat.getColor(this, R.color.read_mode_link_dark)
            )
            SettingsManager.THEME_SEPIA -> Triple(
                ContextCompat.getColor(this, R.color.read_mode_background_sepia),
                ContextCompat.getColor(this, R.color.read_mode_text_sepia),
                ContextCompat.getColor(this, R.color.read_mode_link_sepia)
            )
            else -> Triple(
                ContextCompat.getColor(this, R.color.read_mode_background_light),
                ContextCompat.getColor(this, R.color.read_mode_text_light),
                ContextCompat.getColor(this, R.color.read_mode_link_light)
            )
        }
        
        // Convert colors to hex format for HTML
        val bgHex = String.format("#%06X", 0xFFFFFF and backgroundColor)
        val textHex = String.format("#%06X", 0xFFFFFF and textColor)
        val linkHex = String.format("#%06X", 0xFFFFFF and linkColor)
        
        // Get font family based on user preference
        val fontFamily = when (settingsManager.getFontFamily()) {
            SettingsManager.FONT_FAMILY_SERIF -> "'Georgia', 'Times New Roman', serif"
            SettingsManager.FONT_FAMILY_SANS_SERIF -> "'Segoe UI', 'Roboto', 'Helvetica', sans-serif"
            SettingsManager.FONT_FAMILY_MONOSPACE -> "'Courier New', 'Monaco', monospace"
            else -> "'Segoe UI', 'Roboto', 'Helvetica', sans-serif"
        }
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: $fontFamily;
                        line-height: 1.6;
                        color: $textHex;
                        background-color: $bgHex;
                        padding: 16px;
                        margin: 0;
                    }
                    h1, h2, h3, h4, h5, h6 {
                        font-family: $fontFamily;
                        line-height: 1.3;
                    }
                    h1 {
                        font-size: 1.4em;
                        margin-bottom: 8px;
                    }
                    .byline {
                        font-size: 0.9em;
                        color: ${
                            when (currentTheme) {
                                SettingsManager.THEME_DARK -> "#AAAAAA"
                                SettingsManager.THEME_SEPIA -> "#8E644B"
                                else -> "#757575"
                            }
                        };
                        margin-bottom: 24px;
                    }
                    img {
                        max-width: 100%;
                        height: auto;
                        margin: 16px 0;
                    }
                    p {
                        margin-bottom: 16px;
                    }
                    a {
                        color: $linkHex;
                        text-decoration: none;
                    }
                    blockquote {
                        border-left: 4px solid ${
                            when (currentTheme) {
                                SettingsManager.THEME_DARK -> "#616161"
                                SettingsManager.THEME_SEPIA -> "#D7C9B8"
                                else -> "#BDBDBD"
                            }
                        };
                        padding-left: 16px;
                        margin-left: 0;
                        font-style: italic;
                    }
                    pre, code {
                        background-color: ${
                            when (currentTheme) {
                                SettingsManager.THEME_DARK -> "#1E1E1E"
                                SettingsManager.THEME_SEPIA -> "#EBE0D0"
                                else -> "#F5F5F5"
                            }
                        };
                        padding: 16px;
                        border-radius: 4px;
                        overflow: auto;
                    }
                </style>
            </head>
            <body>
                <h1>$title</h1>
                ${if (byline.isNotEmpty()) "<div class=\"byline\">$byline</div>" else ""}
                $content
            </body>
            </html>
        """.trimIndent()
    }
    
    /**
     * Remove HTML tags from content for TTS
     */
    private fun stripHtml(html: String): String {
        return html.replace(Regex("<.*?>"), "")
                   .replace(Regex("&nbsp;"), " ")
                   .replace(Regex("&lt;"), "<")
                   .replace(Regex("&gt;"), ">")
                   .replace(Regex("&amp;"), "&")
                   .replace(Regex("&quot;"), "\"")
                   .replace(Regex("\n+"), "\n")
                   .replace(Regex(" +"), " ")
                   .trim()
    }
    
    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        webView.visibility = View.GONE
        summaryContainer.visibility = View.GONE
        fabSummarize.visibility = View.GONE
        errorView.visibility = View.VISIBLE
        errorView.text = message
    }
    
    private fun applyTheme() {
        // Set WebView background based on current theme
        val backgroundColor = when (currentTheme) {
            SettingsManager.THEME_DARK -> 
                ContextCompat.getColor(this, R.color.read_mode_background_dark)
            SettingsManager.THEME_SEPIA -> 
                ContextCompat.getColor(this, R.color.read_mode_background_sepia)
            else -> 
                ContextCompat.getColor(this, R.color.read_mode_background_light)
        }
        
        webView.setBackgroundColor(backgroundColor)
        
        // If content is loaded, refresh it with new theme
        if (currentUrl.isNotEmpty()) {
            loadContent(currentUrl)
        }
    }
    
    private fun changeFontSize(delta: Int) {
        // Update font size within bounds
        currentFontSize = currentFontSize.plus(delta).coerceIn(Constants.MIN_FONT_SIZE, Constants.MAX_FONT_SIZE)
        
        // Save the new font size
        settingsManager.setTextSize(currentFontSize)
        
        // Apply to WebView
        webView.settings.textZoom = (currentFontSize * 100) / Constants.DEFAULT_FONT_SIZE
    }
    
    private fun cycleTheme() {
        // Cycle through available themes: Light -> Dark -> Sepia -> Light
        currentTheme = when (currentTheme) {
            SettingsManager.THEME_LIGHT -> SettingsManager.THEME_DARK
            SettingsManager.THEME_DARK -> SettingsManager.THEME_SEPIA
            else -> SettingsManager.THEME_LIGHT
        }
        
        // Save the theme setting
        settingsManager.setCurrentTheme(currentTheme)
        
        // Apply the theme
        applyTheme()
    }
    
    private fun shareCurrentPage() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, supportActionBar?.title ?: "Shared from QB Browser")
            putExtra(Intent.EXTRA_TEXT, "Check out this article: $currentUrl")
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }
    
    /**
     * Toggle between web view and summary view
     */
    private fun toggleSummaryMode() {
        if (isSummaryMode) {
            // Switch back to web view
            showWebView()
        } else {
            // Switch to summary view
            showSummaryView()
        }
    }
    
    /**
     * Show the web view and hide the summary view
     */
    private fun showWebView() {
        isSummaryMode = false
        
        // Update UI
        webView.visibility = View.VISIBLE
        summaryContainer.visibility = View.GONE
        
        // Update FAB icon
        fabSummarize.setImageResource(R.drawable.ic_summarize)
        fabSummarize.contentDescription = getString(R.string.summarize)
        
        // Show a toast
        Toast.makeText(this, R.string.showing_web_view, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Show the summary view and hide the web view
     */
    private fun showSummaryView() {
        if (webView.visibility != View.VISIBLE) {
            Toast.makeText(this, R.string.summary_error, Toast.LENGTH_SHORT).show()
            return
        }
        
        isSummaryMode = true
        
        // Update UI
        webView.visibility = View.GONE
        summaryContainer.visibility = View.VISIBLE
        summaryProgress.visibility = View.VISIBLE
        
        // Clear previous summary content
        summaryContent.removeAllViews()
        
        // Keep only the title and progress bar
        val titleView = summaryContent.findViewById<TextView>(R.id.summary_title)
        titleView?.text = getString(R.string.summary_title)
        
        // Update FAB icon
        fabSummarize.setImageResource(R.drawable.ic_web_page)
        fabSummarize.contentDescription = getString(R.string.show_web_view)
        
        // Show a toast
        Toast.makeText(this, R.string.summarizing, Toast.LENGTH_SHORT).show()
        
        // Start summarization
        summarizeContent()
    }
    
    /**
     * Summarize the content using SummarizationManager
     */
    private fun summarizeContent() {
        // Get the content for summarization
        val content = getContentForSummarization()
        
        if (content.isEmpty()) {
            showSummaryError(getString(R.string.summary_error))
            return
        }
        
        // Start summarization in a coroutine
        lifecycleScope.launch {
            try {
                // Get summary points
                val summaryPoints = withContext(Dispatchers.Default) {
                    summarizationManager?.summarizeContent(content) ?: emptyList()
                }
                
                withContext(Dispatchers.Main) {
                    if (summaryPoints.isNotEmpty()) {
                        displaySummaryPoints(summaryPoints)
                    } else {
                        showSummaryError(getString(R.string.summary_not_article))
                    }
                }
            } catch (e: Exception) {
                Log.e("ReadModeActivity", "Error summarizing content", e)
                withContext(Dispatchers.Main) {
                    showSummaryError(getString(R.string.summary_error))
                }
            }
        }
    }
    
    /**
     * Display the summary points in the UI
     */
    private fun displaySummaryPoints(points: List<String>) {
        // Hide progress
        summaryProgress.visibility = View.GONE
        
        // Add bullet points
        for (point in points) {
            val bulletPoint = LayoutInflater.from(this).inflate(
                R.layout.item_summary_point, 
                summaryContent, 
                false
            ) as TextView
            
            bulletPoint.text = "• $point"
            
            summaryContent.addView(bulletPoint)
        }
    }
    
    /**
     * Show an error in the summary view
     */
    private fun showSummaryError(message: String) {
        // Hide progress
        summaryProgress.visibility = View.GONE
        
        // Add error message
        val errorText = TextView(this)
        errorText.text = message
        errorText.setPadding(16, 16, 16, 16)
        errorText.textSize = 16f
        
        summaryContent.addView(errorText)
    }
    
    /**
     * Get the content for summarization
     * This uses the already extracted content from ContentExtractor
     */
    private fun getContentForSummarization(): String {
        // We already have the extracted content in the extractedText variable
        // Just return the content part without title and byline
        return if (::contentExtractor.isInitialized) {
            // We already have the content in the result variable from loadContent()
            // Just return the content part
            val content = webView.tag as? String ?: ""
            if (content.isNotEmpty()) {
                content
            } else {
                // If we don't have the content cached, use the extractedText
                // which already has HTML tags stripped
                extractedText
            }
        } else {
            ""
        }
    }
    
    private fun toggleTextToSpeech() {
        if (isTtsSpeaking) {
            stopTextToSpeech()
        } else {
            startTextToSpeech()
        }
    }
    
    private fun startTextToSpeech() {
        if (extractedText.isEmpty()) {
            Toast.makeText(this, "No content to read", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Apply user TTS settings
        ttsManager.setSpeechRate(settingsManager.getTtsSpeechRate())
        ttsManager.setPitch(settingsManager.getTtsPitch())
        
        val success = ttsManager.speak(extractedText)
        if (!success) {
            Toast.makeText(this, "Failed to start Text-to-Speech", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun stopTextToSpeech() {
        ttsManager.stop()
        isTtsSpeaking = false
        invalidateOptionsMenu() // Update menu icons
    }
    
    override fun onInitialized(status: Boolean) {
        if (!status) {
            Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onStart() {
        super.onStart() // Call the superclass implementation
        isTtsSpeaking = true
        invalidateOptionsMenu() // Update menu icons

        Snackbar.make(webView, "Reading aloud", Snackbar.LENGTH_SHORT).show()
    }
    
    override fun onDone() {
        isTtsSpeaking = false
        invalidateOptionsMenu() // Update menu icons
    }
    
    override fun onError(errorMessage: String) {
        isTtsSpeaking = false
        invalidateOptionsMenu() // Update menu icons
        
        Toast.makeText(this, "Text-to-Speech error: $errorMessage", Toast.LENGTH_SHORT).show()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.read_mode_menu, menu)
        
        // Update TTS icon based on speaking state
        val ttsItem = menu.findItem(R.id.action_text_to_speech)
        ttsItem?.setIcon(
            if (isTtsSpeaking) R.drawable.ic_pause_tts else R.drawable.ic_text_to_speech
        )
        
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_increase_text -> {
                changeFontSize(1)
                true
            }
            R.id.action_decrease_text -> {
                changeFontSize(-1)
                true
            }
            R.id.action_toggle_night_mode -> {
                cycleTheme()
                true
            }
            R.id.action_text_to_speech -> {
                toggleTextToSpeech()
                true
            }
            R.id.action_share -> {
                shareCurrentPage()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onDestroy() {
        // Clean up resources
        ttsManager.shutdown()
        super.onDestroy()
    }
}