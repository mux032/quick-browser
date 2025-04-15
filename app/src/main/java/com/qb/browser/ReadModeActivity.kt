package com.qb.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
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
import com.qb.browser.util.ContentExtractor
import com.qb.browser.util.SettingsManager
import com.qb.browser.util.TextToSpeechManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.util.Locale

/**
 * Activity for displaying web pages in a clean, reader-friendly format
 */
class ReadModeActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorView: TextView
    private lateinit var settingsManager: SettingsManager
    private lateinit var contentExtractor: ContentExtractor
    
    private var currentUrl: String = ""
    private var bubbleId: String = ""
    private var currentFontSize = 16
    private var isNightMode = false
    
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
        isNightMode = settingsManager.isDarkThemeEnabled()
        currentFontSize = settingsManager.getTextSize()
        
        // Set up WebView
        setupWebView()
        
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
        
        // Apply night mode if enabled
        applyTheme()
    }
    
    private fun loadContent(url: String) {
        progressBar.visibility = View.VISIBLE
        errorView.visibility = View.GONE
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Extract content with improved algorithm
                val readableContent = contentExtractor.extractReadableContent(url)
                
                withContext(Dispatchers.Main) {
                    // Update the page title
                    supportActionBar?.title = readableContent.title
                    
                    // Create HTML with proper styling
                    val htmlContent = createStyledHtml(readableContent.title, readableContent.content, readableContent.byline)
                    
                    // Load the content
                    webView.loadDataWithBaseURL(url, htmlContent, "text/html", "UTF-8", null)
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
        val backgroundColor = if (isNightMode) "#121212" else "#FAFAFA"
        val textColor = if (isNightMode) "#E0E0E0" else "#212121"
        val linkColor = if (isNightMode) "#90CAF9" else "#1976D2"
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: 'Segoe UI', Arial, sans-serif;
                        line-height: 1.6;
                        color: $textColor;
                        background-color: $backgroundColor;
                        padding: 16px;
                        margin: 0;
                    }
                    h1 {
                        font-size: 1.4em;
                        margin-bottom: 8px;
                    }
                    .byline {
                        font-size: 0.9em;
                        color: ${if (isNightMode) "#AAAAAA" else "#757575"};
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
                        color: $linkColor;
                        text-decoration: none;
                    }
                    blockquote {
                        border-left: 4px solid ${if (isNightMode) "#616161" else "#BDBDBD"};
                        padding-left: 16px;
                        margin-left: 0;
                        font-style: italic;
                    }
                    pre, code {
                        background-color: ${if (isNightMode) "#1E1E1E" else "#F5F5F5"};
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
    
    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        webView.visibility = View.GONE
        errorView.visibility = View.VISIBLE
        errorView.text = message
    }
    
    private fun applyTheme() {
        // Apply night mode to WebView
        if (isNightMode) {
            webView.setBackgroundColor(ContextCompat.getColor(this, R.color.read_mode_background_dark))
        } else {
            webView.setBackgroundColor(ContextCompat.getColor(this, R.color.read_mode_background_light))
        }
        
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
    
    private fun toggleNightMode() {
        isNightMode = !isNightMode
        
        // Save the night mode setting
        settingsManager.setDarkThemeEnabled(isNightMode)
        
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
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.read_mode_menu, menu)
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
                toggleNightMode()
                true
            }
            R.id.action_share -> {
                shareCurrentPage()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}