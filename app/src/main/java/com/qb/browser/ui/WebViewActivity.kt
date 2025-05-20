package com.qb.browser.ui

import android.graphics.Bitmap
import android.util.Log
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.LayoutInflater
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.qb.browser.R
import com.qb.browser.util.SummarizationManager
import com.qb.browser.util.SummarizingWebViewClient
import com.qb.browser.util.WebViewClientEx
import com.qb.browser.viewmodel.WebViewModel
import com.qb.browser.Constants
import com.qb.browser.QBApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import org.jsoup.Jsoup

class WebViewActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var webViewModel: WebViewModel
    private lateinit var fabSummarize: FloatingActionButton
    private lateinit var summaryContainer: NestedScrollView
    private lateinit var summaryContent: LinearLayout
    private lateinit var summaryProgress: ProgressBar
    
    private lateinit var url: String
    private var bubbleId: String? = null
    
    // For background summarization
    private var cachedHtmlContent: String? = null
    private var isSummarizationInProgress = false
    private var isSummaryMode = false
    
    companion object {
        // Using centralized constants from Constants.kt
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)
        
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Initialize views
        webView = findViewById(R.id.web_view)
        progressBar = findViewById(R.id.progress_bar)
        fabSummarize = findViewById(R.id.fab_summarize)
        summaryContainer = findViewById(R.id.summary_container)
        summaryContent = findViewById(R.id.summary_content)
        summaryProgress = findViewById(R.id.summary_progress)
        
        // Initialize ViewModel
        webViewModel = ViewModelProvider(this)[WebViewModel::class.java]
        
        // Setup FAB click listener
        fabSummarize.setOnClickListener {
            toggleSummaryMode()
        }
        
        // Check if we're loading an offline page
        val isOffline = intent.getBooleanExtra(Constants.EXTRA_IS_OFFLINE, false)
        
        if (isOffline) {
            // Handle offline page
            val pageId = intent.getStringExtra(Constants.EXTRA_PAGE_ID)
            val pageTitle = intent.getStringExtra(Constants.EXTRA_PAGE_TITLE)
            
            // Set title
            if (pageTitle != null) {
                supportActionBar?.title = pageTitle
                supportActionBar?.subtitle = getString(R.string.offline_pages)
            }
            
            // Set up WebView with offline client
            setupWebViewForOffline(pageId)
            
            // Get URI from intent data
            val uri = intent.data
            if (uri != null) {
                url = uri.toString()
                webView.loadUrl(url)
            }
        } else {
            // Get URL from intent for online mode
            val url = intent.getStringExtra(Constants.EXTRA_URL)
            
            // Load the URL
            if (url != null) {
                bubbleId = intent.getStringExtra(Constants.EXTRA_BUBBLE_ID)
            
                // Set up WebView for online mode
                setupWebView()

                webView.loadUrl(url)
            }
        }
    }
    
    private fun setupWebView() {
        // Get settings manager instance
        val settingsManager = com.qb.browser.util.SettingsManager.getInstance(this)
        
        webView.settings.apply {
            javaScriptEnabled = settingsManager.isJavaScriptEnabled()  // Use user's JavaScript setting
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            
            // Improve caching for better performance
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            databaseEnabled = true
            
            // Set reasonable timeouts
            setGeolocationEnabled(false)  // Disable geolocation by default for privacy
        }
        
        // Set custom WebViewClient with ad blocking and background summarization
        webView.webViewClient = SummarizingWebViewClient(
            this,
            { newUrl ->
                url = newUrl
                updateTitleFromUrl(newUrl)
            },
            { htmlContent ->
                // Store the HTML content for later use
                cachedHtmlContent = htmlContent
                
                // Start background summarization if not already in progress
                if (!isSummarizationInProgress) {
                    startBackgroundSummarization(htmlContent)
                }
            }
        )
        
        // Set WebChromeClient for progress updates
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = newProgress
                } else {
                    progressBar.visibility = View.GONE
                }
                
                // Update progress in bubble if this is associated with a bubble
                bubbleId?.let {
                    webViewModel.updateProgress(it, newProgress)
                }
            }
            
            override fun onReceivedTitle(view: WebView?, title: String?) {
                title?.let {
                    supportActionBar?.title = it
                    
                    // Update title in WebViewModel
                    try {
                        webViewModel.updateTitle(url, it)
                        Log.d("WebViewActivity", "Updated page title: $it for URL: $url")
                    } catch (e: Exception) {
                        Log.e("WebViewActivity", "Error updating title", e)
                    }
                }
            }
            
            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                icon?.let {
                    bubbleId?.let { id ->
                        webViewModel.updateFavicon(id, it)
                    }
                }
            }
        }
    }
    
    /**
     * Set up WebView for offline mode
     */
    private fun setupWebViewForOffline(pageId: String?) {
        if (pageId == null) {
            // Fallback to regular setup if no page ID
            setupWebView()
            return
        }
        
        // Get settings manager instance
        val settingsManager = com.qb.browser.util.SettingsManager.getInstance(this)
        
        // Configure WebView settings for offline use
        webView.settings.apply {
            // Use user's JavaScript setting, but recommend enabling it for offline content
            javaScriptEnabled = settingsManager.isJavaScriptEnabled()
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK
            allowFileAccess = true // Needed for file:// URLs
        }
        
        // If JavaScript is disabled, show a recommendation to enable it for better offline experience
        if (!settingsManager.isJavaScriptEnabled()) {
            android.widget.Toast.makeText(
                this,
                "Enabling JavaScript in Settings may improve offline page rendering",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
        
        // Get the file URI
        val dataUri = intent.data
        if (dataUri != null) {
            // Set up chrome client for offline mode
            webView.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (newProgress < 100) {
                        progressBar.visibility = View.VISIBLE
                        progressBar.progress = newProgress
                    } else {
                        progressBar.visibility = View.GONE
                    }
                }
            }
        } else {
            // Fallback to regular setup
            setupWebView()
        }
    }
    
    private fun updateTitleFromUrl(url: String) {
        val host = try {
            val uri = android.net.Uri.parse(url)
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
        supportActionBar?.subtitle = host
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.web_view_menu, menu)
        
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.menu_refresh -> {
                webView.reload()
                true
            }
            R.id.menu_share -> {
                shareUrl()
                true
            }
            R.id.menu_read_mode -> {
                openReadMode()
                true
            }
            R.id.menu_save_offline -> {
                saveForOffline()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
    
    private fun summarizeContent() {
        try {
            // If we already have cached HTML content, use it directly
            if (cachedHtmlContent != null && cachedHtmlContent!!.length > 100) {
                processSummarization(cachedHtmlContent!!)
            } else {
                // If we don't have cached content, get it from the WebView
                try {
                    // Get the HTML content from the WebView - must be on main thread
                    webView.evaluateJavascript("(function() { return document.documentElement.outerHTML; })()") { html ->
                        try {
                            if (html != null && html.length > 50) {
                                // The result is a JSON string, so we need to parse it
                                val unescapedHtml = html.substring(1, html.length - 1)
                                    .replace("\\\"", "\"")
                                    .replace("\\n", "\n")
                                    .replace("\\\\", "\\")
                                
                                // Cache the HTML content
                                cachedHtmlContent = unescapedHtml
                                
                                // Process the HTML for summarization
                                processSummarization(unescapedHtml)
                            } else {
                                // Handle empty or invalid HTML
                                showSummaryError(getString(R.string.summary_error))
                            }
                        } catch (e: Exception) {
                            Log.e("WebViewActivity", "Error processing HTML for summary", e)
                            showSummaryError(getString(R.string.summary_error))
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WebViewActivity", "Error evaluating JavaScript", e)
                    showSummaryError(getString(R.string.summary_error))
                }
            }
        } catch (e: Exception) {
            Log.e("WebViewActivity", "Error in summarizeContent", e)
            showSummaryError(getString(R.string.summary_error))
        }
    }
    
    /**
     * Process the HTML content for summarization
     */
    private fun processSummarization(htmlContent: String) {
        // Mark summarization as in progress
        isSummarizationInProgress = true
        
        // Start summarization in a coroutine
        lifecycleScope.launch {
            try {
                // Clean the HTML content
                val cleanedHtml = withContext(Dispatchers.IO) {
                    try {
                        val doc = Jsoup.parse(htmlContent)
                        // Remove all script, style, and other non-content elements
                        doc.select("script, style, noscript, iframe, object, embed, header, footer, nav, aside").remove()
                        // Extract only the text content to avoid any HTML tags
                        doc.text()
                    } catch (e: Exception) {
                        Log.e("WebViewActivity", "Error cleaning HTML", e)
                        null
                    }
                }
                
                if (cleanedHtml == null || cleanedHtml.length < 100) {
                    withContext(Dispatchers.Main) {
                        showSummaryError(getString(R.string.summary_not_article))
                    }
                    return@launch
                }
                
                // Get summary points
                val summaryPoints = withContext(Dispatchers.Default) {
                    val summarizationManager = SummarizationManager.getInstance(this@WebViewActivity)
                    summarizationManager.summarizeContent(cleanedHtml)
                }
                
                withContext(Dispatchers.Main) {
                    if (summaryPoints.isNotEmpty()) {
                        displaySummaryPoints(summaryPoints)
                    } else {
                        showSummaryError(getString(R.string.summary_not_article))
                    }
                }
            } catch (e: Exception) {
                Log.e("WebViewActivity", "Error processing summarization", e)
                withContext(Dispatchers.Main) {
                    showSummaryError(getString(R.string.summary_error))
                }
            } finally {
                // Mark summarization as complete
                isSummarizationInProgress = false
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
     * Start background summarization of the HTML content
     */
    private fun startBackgroundSummarization(htmlContent: String) {
        // Don't start if already in progress or if HTML is too short
        if (isSummarizationInProgress || htmlContent.length < 100) {
            return
        }
        
        // Set flag to prevent multiple summarizations
        isSummarizationInProgress = true
        
        // Use a coroutine to do the work in the background
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Get the summarization manager
                val summarizationManager = SummarizationManager.getInstance(this@WebViewActivity)
                
                // Pre-process the HTML to clean it up and extract only text content
                try {
                    val doc = org.jsoup.Jsoup.parse(htmlContent)
                    doc.select("script, style, noscript, iframe, object, embed, header, footer, nav, aside").remove()
                    // Extract only the text content to avoid any HTML tags
                    val cleanedText = doc.text()
                    
                    if (cleanedText.length > 100) {
                        // Start the summarization process
                        summarizationManager.summarizeContent(cleanedText)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("WebViewActivity", "Error pre-processing HTML", e)
                }
            } catch (e: Exception) {
                // Log the error but don't show it to the user
                android.util.Log.e("WebViewActivity", "Background summarization failed", e)
            } finally {
                // Always reset the flag when done, even if there was an error
                isSummarizationInProgress = false
            }
        }
    }
    
    private fun shareUrl() {
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, url)
            type = "text/plain"
        }
        startActivity(android.content.Intent.createChooser(shareIntent, getString(R.string.share)))
    }
    
    private fun openReadMode() {
        val intent = android.content.Intent(this, ReadModeActivity::class.java).apply {
            putExtra(Constants.EXTRA_URL, url)
            bubbleId?.let { putExtra(Constants.EXTRA_BUBBLE_ID, it) }
        }
        startActivity(intent)
    }
    
    private fun saveForOffline() {
        // Use the new direct WebView content saving method for better results
        webViewModel.saveCurrentPageFromWebView(webView, url, webView.title ?: "Untitled")
        
        // Show confirmation message
        android.widget.Toast.makeText(
            this,
            R.string.page_saved_offline,
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                onBackPressedDispatcher.onBackPressed()
            } else {
                @Suppress("DEPRECATION")
                super.onBackPressed()
            }
        }
    }
}
