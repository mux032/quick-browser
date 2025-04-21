package com.qb.browser.ui

import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.qb.browser.R
import com.qb.browser.util.OfflinePageManager
import com.qb.browser.util.OfflineWebViewClient
import com.qb.browser.util.WebViewClientEx
import com.qb.browser.viewmodel.WebViewModel
import com.qb.browser.Constants

class WebViewActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var webViewModel: WebViewModel
    
    private lateinit var url: String
    private var bubbleId: String? = null
    
    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_BUBBLE_ID = "extra_bubble_id"
        const val EXTRA_IS_OFFLINE = "is_offline"
        const val EXTRA_PAGE_ID = "page_id"
        const val EXTRA_PAGE_TITLE = "page_title"
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
        
        // Initialize ViewModel
        webViewModel = ViewModelProvider(this)[WebViewModel::class.java]
        
        // Check if we're loading an offline page
        val isOffline = intent.getBooleanExtra(EXTRA_IS_OFFLINE, false)
        
        if (isOffline) {
            // Handle offline page
            val pageId = intent.getStringExtra(EXTRA_PAGE_ID)
            val pageTitle = intent.getStringExtra(EXTRA_PAGE_TITLE)
            
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
                bubbleId = intent.getStringExtra(EXTRA_BUBBLE_ID)
            
                // Set up WebView for online mode
                setupWebView()

                webView.loadUrl(url)
            }
        }
    }
    
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = false  // Disabled by default for privacy
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
        }
        
        // Set custom WebViewClient with ad blocking
        webView.webViewClient = WebViewClientEx(this) { newUrl ->
            url = newUrl
            updateTitleFromUrl(newUrl)
        }
        
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
        
        // Configure WebView settings for offline use
        webView.settings.apply {
            javaScriptEnabled = true  // Enable JS for better experience with offline content
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK
            allowFileAccess = true // Needed for file:// URLs
        }
        
        // Get the file URI
        val dataUri = intent.data
        if (dataUri != null) {
            // Set up custom WebViewClient for offline resources
            webView.webViewClient = OfflineWebViewClient(
                context = this,
                pageId = pageId,
                baseUri = dataUri
            )
            
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
    
    private var isOfflinePage = false
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.web_view_menu, menu)
        
        // If this is an offline page, hide unnecessary menu items
        isOfflinePage = intent.getBooleanExtra(EXTRA_IS_OFFLINE, false)
        if (isOfflinePage) {
            menu.findItem(R.id.menu_save_offline)?.isVisible = false
            // If it's an offline page we already have a clean version,
            // so read mode isn't necessary
            menu.findItem(R.id.menu_read_mode)?.isVisible = false
        }
        
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
            putExtra(ReadModeActivity.EXTRA_URL, url)
            bubbleId?.let { putExtra(ReadModeActivity.EXTRA_BUBBLE_ID, it) }
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
