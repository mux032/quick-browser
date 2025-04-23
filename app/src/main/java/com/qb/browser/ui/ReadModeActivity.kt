package com.qb.browser.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.qb.browser.R
import com.qb.browser.util.ContentExtractor
import com.qb.browser.util.SettingsManager
import com.qb.browser.util.TextToSpeechManager
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
    
    private var currentUrl: String = ""
    private var bubbleId: String = ""
    private var currentFontSize = 16
    private var currentTheme = SettingsManager.THEME_LIGHT
    private var isTtsSpeaking = false
    private var extractedText: String = ""
    
    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_BUBBLE_ID = "extra_bubble_id"
        const val MIN_FONT_SIZE = 12
        const val MAX_FONT_SIZE = 24
        const val DEFAULT_FONT_SIZE = 16
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
        
        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Get extras from intent
        currentUrl = intent.getStringExtra(EXTRA_URL) ?: ""
        bubbleId = intent.getStringExtra(EXTRA_BUBBLE_ID) ?: ""
        
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
            textZoom = (currentFontSize * 100) / DEFAULT_FONT_SIZE // Convert font size to percentage
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
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Extract content with improved algorithm
                val readableContent = contentExtractor.extractReadableContent(validatedUrl)
                
                // Save the current URL and extracted text for TTS
                currentUrl = validatedUrl
                extractedText = readableContent.title + "\n\n" + 
                                (if (readableContent.byline.isNotEmpty()) readableContent.byline + "\n\n" else "") + 
                                stripHtml(readableContent.content)
                
                withContext(Dispatchers.Main) {
                    // Update the page title
                    supportActionBar?.title = readableContent.title
                    
                    // Create HTML with proper styling based on current theme
                    val htmlContent = createStyledHtml(
                        readableContent.title,
                        readableContent.content, 
                        readableContent.byline
                    )
                    
                    // Load the content
                    webView.loadDataWithBaseURL(validatedUrl, htmlContent, "text/html", "UTF-8", null)
                    progressBar.visibility = View.GONE
                    webView.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Failed to load content: ${e.message}")
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
        currentFontSize = currentFontSize.plus(delta).coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
        
        // Save the new font size
        settingsManager.setTextSize(currentFontSize)
        
        // Apply to WebView
        webView.settings.textZoom = (currentFontSize * 100) / DEFAULT_FONT_SIZE
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