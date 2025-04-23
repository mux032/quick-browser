package com.qb.browser.ui

import android.content.Context
import android.view.ContextThemeWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qb.browser.Constants
import com.qb.browser.QBApplication
import com.qb.browser.R
import com.qb.browser.model.Bubble
import com.qb.browser.model.WebPage
import com.qb.browser.service.BubbleService
import com.qb.browser.util.SettingsManager
import com.qb.browser.ui.ReadModeActivity
import com.qb.browser.ui.WebViewActivity
import com.qb.browser.ui.adapter.TabsAdapter
import com.qb.browser.util.AdBlocker
import com.qb.browser.util.ContentExtractor
import com.qb.browser.viewmodel.WebViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Math.min
import kotlin.math.hypot
import kotlin.math.max

/**
 * Enhanced floating bubble view that displays web content in a draggable, expandable bubble.
 * 
 * This view provides a floating UI element that can be dragged around the screen,
 * expanded to show web content, and collapsed to a small bubble icon. It manages
 * its own WebView instance and handles touch events for dragging and expanding.
 * 
 * @property bubbleId Unique identifier for this bubble
 * @property url The URL to load in this bubble's WebView
 */
class BubbleView @JvmOverloads constructor(
    context: Context,
    val bubbleId: String,
    val url: String,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    // UI components
    private lateinit var rootView: View
    private lateinit var bubbleIcon: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var expandedContainer: View
    private lateinit var contentContainer: FrameLayout
    private lateinit var tabsContainer: View
    private lateinit var webViewContainer: WebView
    private var tabsAdapter: TabsAdapter? = null

    // Touch handling state
    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    
    // Bubble state
    private var isBubbleExpanded = false
    private var onCloseListener: (() -> Unit)? = null
    private var isActive = false
    private var isShowingAllBubbles = false
    private var isExpanded = false
    
    // Services and utilities
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val settingsManager = SettingsManager.getInstance(context)
    private val bubbleAnimator = BubbleAnimator(context)
    private var webViewModel: WebViewModel? = null

    companion object {
        private const val TAG = "BubbleView"
    }
    
    /**
     * Initialize the bubble view, set up UI components and event listeners
     */
    init {
        // Initialize UI components
        initializeViews()
        
        // Set up WebViewModel for favicon and title management
        post { initializeWebViewModel() }
        
        // Set up click listeners for bubble actions
        setupClickListeners()
        
        // Set up content based on bubble type
        setupContent()
    }
    
    /**
     * Initialize all view components from the layout
     */
    private fun initializeViews() {
        // Use application context with theme for inflation
        val themedContext = ContextThemeWrapper(context.applicationContext, R.style.Theme_QBrowser)
        rootView = LayoutInflater.from(themedContext).inflate(R.layout.bubble_layout, this, true)
        
        // Find and initialize view references
        bubbleIcon = findViewById(R.id.bubble_icon)
        progressBar = findViewById(R.id.progress_circular)
        expandedContainer = findViewById(R.id.expanded_container)
        contentContainer = findViewById(R.id.content_container)
        tabsContainer = findViewById(R.id.tabs_container)
        webViewContainer = findViewById(R.id.web_view)
        
        // Set up default favicon
        bubbleIcon.setImageResource(R.drawable.ic_globe)
        
        // Set up progress indicator
        progressBar.progress = 0
        
        // Hide new tab button as we don't need it anymore
        findViewById<View>(R.id.btn_new_tab)?.visibility = View.GONE
    }
    
    /**
     * Initialize WebViewModel and set up favicon observers
     */
    private fun initializeWebViewModel() {
        val lifecycleOwner = findViewTreeLifecycleOwner()
        val viewModelStoreOwner = findViewTreeViewModelStoreOwner()
        
        when {
            // Best case: We have both lifecycle owner and view model store owner
            lifecycleOwner != null && viewModelStoreOwner != null -> {
                try {
                    webViewModel = ViewModelProvider(viewModelStoreOwner)[WebViewModel::class.java]
                    observeFaviconChanges(lifecycleOwner)
                } catch (e: Exception) {
                    Log.e(TAG, "Error initializing WebViewModel", e)
                }
            }
            
            // Fallback: We have lifecycle owner but need to use application context
            lifecycleOwner != null -> {
                val application = context.applicationContext
                if (application is ViewModelStoreOwner) {
                    try {
                        Log.d(TAG, "Using application context as ViewModelStoreOwner")
                        webViewModel = ViewModelProvider(application)[WebViewModel::class.java]
                        observeFaviconChanges(lifecycleOwner)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error initializing WebViewModel with application context", e)
                        createStandaloneWebViewModel()
                    }
                } else {
                    Log.e(TAG, "Could not find ViewModelStoreOwner and application is not a ViewModelStoreOwner")
                    createStandaloneWebViewModel()
                }
            }
            
            // Last resort: Create standalone WebViewModel
            else -> {
                Log.e(TAG, "Could not find LifecycleOwner")
                createStandaloneWebViewModel()
            }
        }
    }
    
    /**
     * Observe favicon changes from WebViewModel
     */
    private fun observeFaviconChanges(lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
        lifecycleOwner.lifecycleScope.launch {
            webViewModel?.webPages?.collectLatest { pages ->
                pages[url]?.let { webPage ->
                    webPage.favicon?.let { favicon ->
                        Log.d(TAG, "Updating bubble icon with favicon for URL: $url")
                        updateBubbleIcon(favicon)
                    }
                }
            }
        }
    }
    
    /**
     * Set up click listeners for all interactive elements
     */
    private fun setupClickListeners() {
        // Main bubble click listener
        setOnClickListener {
            toggleBubbleExpanded()
            notifyBubbleActivated()
        }
        
        // Action button listeners
        findViewById<View>(R.id.btn_close).setOnClickListener { closeBubbleWithAnimation() }
        findViewById<View>(R.id.btn_open_full).setOnClickListener { openFullWebView() }
        findViewById<View>(R.id.btn_read_mode).setOnClickListener { openReadMode() }
        findViewById<View>(R.id.btn_save_offline).setOnClickListener { saveForOffline() }
    }
    
    /**
     * Notify the BubbleService that this bubble has been activated
     */
    private fun notifyBubbleActivated() {
        val intent = Intent(context, BubbleService::class.java).apply {
            action = BubbleService.ACTION_ACTIVATE_BUBBLE
            putExtra(Constants.EXTRA_BUBBLE_ID, bubbleId)
        }
        context.startService(intent)
    }

    /**
     * Set up the content container with appropriate visibility
     */
    private fun setupContent() {
        // Show WebView for all bubbles
        webViewContainer.visibility = View.VISIBLE
        tabsContainer.visibility = View.GONE
        
        // Set up WebView
        setupWebView()
    }

    /**
     * Configure the WebView with appropriate settings and clients
     */
    private fun setupWebView() {
        try {
            Log.d(TAG, "Setting up WebView for bubble: $bubbleId with URL: $url")
            
            // Set WebView background to white for better visibility
            webViewContainer.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
            
            // Configure WebView settings
            configureWebViewSettings()
            
            // Set up WebView clients
            setupWebViewClients()
            
            // Make WebView ready to load content in the background
            // It should be VISIBLE but with alpha=0 to ensure it renders properly
            webViewContainer.visibility = View.VISIBLE
            webViewContainer.alpha = 0f
            
            // Ensure WebView has layout params
            val layoutParams = webViewContainer.layoutParams
            if (layoutParams != null) {
                layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT
                layoutParams.height = FrameLayout.LayoutParams.MATCH_PARENT
                webViewContainer.layoutParams = layoutParams
            }
            
            // Force layout to ensure WebView is properly sized
            webViewContainer.requestLayout()
            
            // Load the URL in the background
            // Use post to ensure WebView is fully initialized
            post {
                loadInitialUrl()
                Log.d(TAG, "URL loading initiated for bubble: $bubbleId")
            }
            
            Log.d(TAG, "WebView initialized for bubble: $bubbleId")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up WebView for bubble $bubbleId", e)
        }
    }
    
    /**
     * Configure WebView settings based on user preferences
     */
    private fun configureWebViewSettings() {
        webViewContainer.settings.apply {
            javaScriptEnabled = settingsManager.isJavaScriptEnabled()
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            setGeolocationEnabled(true)
            loadsImagesAutomatically = true
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            allowContentAccess = true
            allowFileAccess = true
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            databaseEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            
            // Additional settings to ensure content loads properly
            blockNetworkImage = false
            blockNetworkLoads = false
            mediaPlaybackRequiresUserGesture = false
            
            // Set default text encoding
            defaultTextEncodingName = "UTF-8"
            
            // Enable hardware acceleration
            setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH)
        }
        
        // Enable hardware acceleration on the WebView
        webViewContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }
    
    /**
     * Set up WebChromeClient and WebViewClient for the WebView
     */
    private fun setupWebViewClients() {
        // Set up WebChromeClient for progress, favicon and title handling
        webViewContainer.webChromeClient = createWebChromeClient()
        
        // Set up WebViewClient for page loading and error handling
        webViewContainer.webViewClient = createWebViewClient()
    }
    
    /**
     * Create a WebChromeClient to handle progress updates, favicons, and titles
     */
    private fun createWebChromeClient(): WebChromeClient {
        return object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                updateProgress(newProgress)
            }

            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                icon?.let { handleReceivedFavicon(it) }
            }
            
            override fun onReceivedTitle(view: WebView?, title: String?) {
                title?.let { handleReceivedTitle(it) }
            }
        }
    }
    
    /**
     * Handle a received favicon from the WebView
     */
    private fun handleReceivedFavicon(favicon: Bitmap) {
        // Update local favicon
        updateFavicon(favicon)
        
        // Update in WebViewModel to persist the favicon
        webViewModel?.let { viewModel ->
            try {
                viewModel.updateFavicon(url, favicon)
                Log.d(TAG, "Updated favicon in WebViewModel for URL: $url")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating favicon in WebViewModel", e)
            }
        } ?: run {
            // If WebViewModel is null, try to create it
            try {
                Log.d(TAG, "WebViewModel is null, creating new instance")
                webViewModel = WebViewModel()
                webViewModel?.updateFavicon(url, favicon)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating WebViewModel on the fly", e)
            }
        }
        
        Log.d(TAG, "Received favicon for bubble: $bubbleId")
    }
    
    /**
     * Handle a received page title from the WebView
     */
    private fun handleReceivedTitle(title: String) {
        Log.d(TAG, "Received page title for bubble $bubbleId: $title")
        
        // Update title in WebViewModel
        webViewModel?.let { viewModel ->
            try {
                viewModel.updateTitle(url, title)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating title in WebViewModel", e)
            }
        } ?: run {
            // If WebViewModel is null, try to create it
            try {
                Log.d(TAG, "WebViewModel is null, creating new instance")
                webViewModel = WebViewModel()
                webViewModel?.updateTitle(url, title)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating WebViewModel on the fly", e)
            }
        }
        
        // Update title in history database
        try {
            // Use WebViewModel to update the title
            webViewModel?.updateTitle(url, title)
            Log.d(TAG, "Updated page title in WebViewModel: $title")
            
            // Also update in the database if we have access to it
            val app = context.applicationContext as? QBApplication
            app?.webViewModel?.updateTitle(url, title)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating title in history database", e)
        }
    }
    
    /**
     * Create a WebViewClient to handle page loading and errors
     */
    private fun createWebViewClient(): android.webkit.WebViewClient {
        return object : android.webkit.WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d(TAG, "WebView page loading started for bubble $bubbleId: $url")
                progressBar.visibility = View.VISIBLE
                
                // Ensure the WebView is in a good state for rendering
                view?.invalidate()
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "WebView page loading finished for bubble $bubbleId: $url")
                progressBar.visibility = View.GONE
                
                // Force a redraw to ensure content is visible
                view?.invalidate()
                
                // If this is the initial URL, make sure it's properly loaded
                if (url != null && url == formatUrl(this@BubbleView.url)) {
                    Log.d(TAG, "Initial URL loaded successfully: $url")
                    
                    // If the bubble is expanded, make sure the WebView is fully visible
                    if (isBubbleExpanded) {
                        webViewContainer.alpha = 1f
                    }
                }
            }
            
            // For older Android versions
            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return handleUrlLoading(view, url)
            }
            
            // For newer Android versions
            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                return handleUrlLoading(view, request?.url?.toString())
            }
            
            // Ad blocking implementation
            override fun shouldInterceptRequest(view: WebView?, request: android.webkit.WebResourceRequest?): WebResourceResponse? {
                if (settingsManager.isAdBlockEnabled()) {
                    val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)
                    val adBlocker = AdBlocker.getInstance(context)
                    val blockResponse = adBlocker.shouldBlockRequest(url)
                    if (blockResponse != null) {
                        Log.d(TAG, "Blocked ad request: $url")
                        return blockResponse
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }
            
            // For older Android versions
            @Suppress("DEPRECATION")
            override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
                if (settingsManager.isAdBlockEnabled()) {
                    if (url != null) {
                        val adBlocker = AdBlocker.getInstance(context)
                        val blockResponse = adBlocker.shouldBlockRequest(url)
                        if (blockResponse != null) {
                            Log.d(TAG, "Blocked ad request: $url")
                            return blockResponse
                        }
                    }
                }
                return super.shouldInterceptRequest(view, url)
            }
            
            // Common URL handling logic
            private fun handleUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let {
                    Log.d(TAG, "Loading URL in WebView: $it")
                    // Only override special URLs, let WebView handle normal URLs
                    if (it.startsWith("tel:") || it.startsWith("mailto:") || it.startsWith("sms:") || 
                        it.startsWith("intent:") || it.startsWith("market:")) {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(it)))
                            return true
                        } catch (e: Exception) {
                            Log.e(TAG, "Error handling special URL: $it", e)
                        }
                    }
                }
                // Return false to let WebView handle normal URLs
                return false
            }
            
            // Handle HTTP errors
            @Suppress("DEPRECATION")
            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                Log.e(TAG, "WebView error for bubble $bubbleId: $errorCode - $description for URL: $failingUrl")
                
                // Only handle errors for the main page, not resources
                if (failingUrl == view?.url) {
                    handleWebViewError(view, errorCode, description, failingUrl)
                }
            }
            
            // Handle newer API errors
            override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                super.onReceivedError(view, request, error)
                
                val errorCode = error?.errorCode ?: -1
                val description = error?.description?.toString() ?: "Unknown error"
                val failingUrl = request?.url?.toString()
                
                Log.e(TAG, "WebView error (new API) for bubble $bubbleId: $errorCode - $description for URL: $failingUrl")
                
                // Only handle errors for the main page, not resources
                if (request?.isForMainFrame == true) {
                    handleWebViewError(view, errorCode, description, failingUrl)
                }
            }
            
            // Common error handling logic
            private fun handleWebViewError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                // For connection errors, try to reload after a delay
                if (errorCode == android.webkit.WebViewClient.ERROR_CONNECT || 
                    errorCode == android.webkit.WebViewClient.ERROR_TIMEOUT ||
                    errorCode == android.webkit.WebViewClient.ERROR_HOST_LOOKUP) {
                    
                    postDelayed({
                        Log.d(TAG, "Attempting to reload after error: $failingUrl")
                        view?.reload()
                    }, 2000) // 2 second delay before retry
                }
            }
            
            // Handle SSL errors - accept all for simplicity
            override fun onReceivedSslError(view: WebView?, handler: android.webkit.SslErrorHandler?, error: android.net.http.SslError?) {
                Log.d(TAG, "SSL Error received for bubble $bubbleId: ${error?.toString()}")
                // Accept SSL certificate for simplicity
                handler?.proceed()
            }
        }
    }
    
    /**
     * Load the initial URL in the WebView
     */
    private fun loadInitialUrl() {
        if (url.isNotEmpty()) {
            val formattedUrl = formatUrl(url)
            if (formattedUrl.isNotEmpty()) {
                try {
                    Log.d(TAG, "Loading URL directly in WebView for bubble $bubbleId: $formattedUrl")
                    
                    // Clear any existing page
                    webViewContainer.clearHistory()
                    webViewContainer.clearCache(true)
                    
                    // Make sure WebView is in a good state for loading
                    webViewContainer.stopLoading()
                    
                    // Load the URL with additional headers to ensure proper loading
                    val headers = HashMap<String, String>()
                    headers["User-Agent"] = webViewContainer.settings.userAgentString
                    headers["Accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
                    headers["Accept-Language"] = "en-US,en;q=0.5"
                    
                    // Load the URL with headers
                    webViewContainer.loadUrl(formattedUrl, headers)
                    
                    // Log success
                    Log.d(TAG, "URL load initiated for bubble $bubbleId")
                    
                    // Set a fallback timer to reload if needed
                    postDelayed({
                        if (webViewContainer.url == null || webViewContainer.url == "about:blank") {
                            Log.d(TAG, "Fallback: Reloading URL for bubble $bubbleId: $formattedUrl")
                            webViewContainer.loadUrl(formattedUrl, headers)
                        }
                    }, 1000) // 1 second fallback
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading URL for bubble $bubbleId", e)
                    webViewContainer.loadUrl("about:blank")
                }
            } else {
                Log.d(TAG, "Invalid URL format for bubble $bubbleId: $url")
                webViewContainer.loadUrl("about:blank")
            }
        } else {
            // Load a blank page for empty URL
            Log.d(TAG, "No URL provided for bubble $bubbleId")
            webViewContainer.loadUrl("about:blank")
        }
    }

    /**
     * Reload the webpage if needed
     */
    private fun reloadWebPageIfNeeded() {
        if (url.isEmpty()) return
        
        try {
            // Only reload if WebView hasn't loaded content yet
            if (webViewContainer.url == null || webViewContainer.url == "about:blank") {
                val loadUrl = formatUrl(url)
                if (loadUrl.isNotEmpty()) {
                    Log.d(TAG, "Reloading URL for bubble $bubbleId: $loadUrl")
                    webViewContainer.loadUrl(loadUrl)
                    Log.d(TAG, "Successfully reloaded URL for bubble $bubbleId: $loadUrl")
                } else {
                    Log.d(TAG, "Invalid URL format for bubble $bubbleId: $url")
                }
            } else {
                Log.d(TAG, "WebView already has content for bubble $bubbleId: ${webViewContainer.url}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reloading URL for bubble $bubbleId", e)
        }
    }

    /**
     * Toggle bubble expanded state with animation
     * 
     * When expanded, the bubble shows a WebView with the loaded URL.
     * When collapsed, only the bubble icon is visible.
     */
    private fun toggleBubbleExpanded() {
        isBubbleExpanded = !isBubbleExpanded
        
        if (isBubbleExpanded) {
            expandBubble()
        } else {
            collapseBubble()
        }
    }
    
    /**
     * Expand the bubble to show web content
     */
    private fun expandBubble() {
        // Show expanded container with animation
        expandedContainer.visibility = View.VISIBLE
        bubbleAnimator.animateExpand(expandedContainer)
        
        // Bounce the bubble
        bubbleAnimator.animateBounce(rootView.findViewById(R.id.bubble_container), true)
        
        // Configure container visibility
        webViewContainer.visibility = View.VISIBLE
        webViewContainer.alpha = 1f
        tabsContainer.visibility = View.GONE
        
        // Set the dimensions for the expanded container
        resizeExpandedContainer()
        
        // Make WebView visible and ensure content is loaded
        loadContentInExpandedWebView()
    }
    
    /**
     * Resize the expanded container to take appropriate screen space
     */
    private fun resizeExpandedContainer() {
        val layoutParams = expandedContainer.layoutParams
        layoutParams.width = resources.displayMetrics.widthPixels * 9 / 10  // 90% of screen width
        layoutParams.height = resources.displayMetrics.heightPixels * 7 / 10 // 70% of screen height
        expandedContainer.layoutParams = layoutParams
        
        // Force layout update
        expandedContainer.requestLayout()
    }
    
    /**
     * Make WebView visible when bubble is expanded
     */
    private fun loadContentInExpandedWebView() {
        try {
            Log.d(TAG, "Making WebView visible for bubble $bubbleId with URL: $url")
            
            // Make WebView fully visible with animation
            webViewContainer.visibility = View.VISIBLE
            webViewContainer.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
            
            // Check if the page is loaded
            val currentUrl = webViewContainer.url
            Log.d(TAG, "Current WebView URL: $currentUrl")
            
            // If the page hasn't loaded yet or is blank, reload it
            if (currentUrl == null || currentUrl == "about:blank" || currentUrl.isEmpty()) {
                val formattedUrl = formatUrl(url)
                if (formattedUrl.isNotEmpty()) {
                    Log.d(TAG, "Loading URL in expanded bubble (fallback): $formattedUrl")
                    
                    // Load with headers for better compatibility
                    val headers = HashMap<String, String>()
                    headers["User-Agent"] = webViewContainer.settings.userAgentString
                    headers["Accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
                    headers["Accept-Language"] = "en-US,en;q=0.5"
                    
                    // Stop any current loading and load the URL
                    webViewContainer.stopLoading()
                    webViewContainer.loadUrl(formattedUrl, headers)
                    
                    // Log the reload attempt
                    Log.d(TAG, "Reloaded URL in expanded bubble: $formattedUrl")
                }
            } else {
                // If the page is already loaded, make sure it's visible
                webViewContainer.invalidate()
                Log.d(TAG, "WebView already has content loaded: $currentUrl")
            }
            
            // Force layout update to ensure content is visible
            webViewContainer.requestLayout()
            
            Log.d(TAG, "WebView is now visible for bubble $bubbleId")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling WebView visibility for bubble $bubbleId", e)
        }
    }
    
    /**
     * Collapse the bubble to show only the icon
     */
    private fun collapseBubble() {
        // Hide expanded container with animation
        bubbleAnimator.animateCollapse(expandedContainer)
        
        // Slight shrink animation on collapse
        bubbleAnimator.animateBounce(rootView.findViewById(R.id.bubble_container), false)
        
        // Keep WebView loaded but invisible
        webViewContainer.visibility = View.INVISIBLE
        webViewContainer.alpha = 0f
        
        // Don't destroy WebView content - just hide it
        // This ensures the content stays loaded in the background
    }

    /**
     * Close the bubble with animation
     * 
     * If the bubble is expanded, it will first collapse and then disappear.
     * Otherwise, it will disappear directly.
     */
    private fun closeBubbleWithAnimation() {
        if (isBubbleExpanded) {
            // First collapse if expanded, then disappear
            bubbleAnimator.animateCollapse(expandedContainer, onEnd = {
                animateBubbleDisappearance()
            })
        } else {
            // Animate bubble disappearance directly
            animateBubbleDisappearance()
        }
    }
    
    /**
     * Animate the bubble disappearing and notify listeners
     */
    private fun animateBubbleDisappearance() {
        bubbleAnimator.animateDisappear(this, onEnd = {
            onCloseListener?.invoke()
        })
    }
    
    /**
     * Open the web page in a full WebView activity
     */
    private fun openFullWebView() {
        try {
            launchActivity(WebViewActivity::class.java, 
                Constants.EXTRA_URL to url,
                Constants.EXTRA_BUBBLE_ID to bubbleId
            )
            
            // Collapse if expanded
            if (isBubbleExpanded) toggleBubbleExpanded()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open full WebView", e)
        }
    }
    
    /**
     * Open the web page in read mode directly in the bubble's WebView
     */
    private fun openReadMode() {
        try {
            // Show loading indicator
            progressBar.visibility = View.VISIBLE
            progressBar.isIndeterminate = true
            
            // Create content extractor
            val contentExtractor = ContentExtractor(context)
            
            // Use a coroutine scope directly instead of relying on lifecycleOwner
            val coroutineScope = CoroutineScope(Dispatchers.Main)
            
            coroutineScope.launch {
                try {
                    // Extract readable content in the background
                    val readableContent = withContext(Dispatchers.IO) {
                        contentExtractor.extractReadableContent(url)
                    }
                    
                    // Create styled HTML
                    val isNightMode = settingsManager.isDarkThemeEnabled()
                    val styledHtml = createStyledHtml(readableContent, isNightMode)
                    
                    // Load the content in the WebView on the main thread
                    withContext(Dispatchers.Main) {
                        // Make sure the bubble is expanded to show the content
                        if (!isBubbleExpanded) {
                            toggleBubbleExpanded()
                        }
                        
                        // Load the content
                        webViewContainer.loadDataWithBaseURL(url, styledHtml, "text/html", "UTF-8", null)
                        
                        // Hide loading indicator
                        progressBar.visibility = View.GONE
                        progressBar.isIndeterminate = false
                        
                        // Make sure WebView is visible
                        webViewContainer.alpha = 1f
                        
                        Log.d(TAG, "Reader mode content loaded successfully")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error extracting content for read mode", e)
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        progressBar.isIndeterminate = false
                        Toast.makeText(context, "Failed to load reader mode", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open read mode", e)
            progressBar.visibility = View.GONE
            progressBar.isIndeterminate = false
        }
    }
    
    /**
     * Create styled HTML for reader mode
     */
    private fun createStyledHtml(content: ContentExtractor.ReadableContent, isNightMode: Boolean): String {
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
                <h1>${content.title}</h1>
                ${if (content.byline.isNotEmpty()) "<div class=\"byline\">${content.byline}</div>" else ""}
                ${content.content}
            </body>
            </html>
        """.trimIndent()
    }
    
    /**
     * Helper method to launch activities with extras
     */
    private fun launchActivity(activityClass: Class<*>, vararg extras: Pair<String, String>) {
        val intent = Intent(context, activityClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            extras.forEach { (key, value) -> putExtra(key, value) }
        }
        context.startActivity(intent)
    }
    
    /**
     * Save the web page for offline access
     */
    private fun saveForOffline() {
        try {
            // Get UI components
            val saveButton = findViewById<ImageView>(R.id.btn_save_offline)
            val messageView = findViewById<TextView>(R.id.save_message)
            
            // Update save button appearance with animation
            highlightSaveButton(saveButton)
            
            // Send intent to service to save the page
            sendSaveOfflineIntent()
            
            // Show feedback to user with animations
            showSaveFeedbackMessage(messageView, saveButton)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save for offline", e)
        }
    }
    
    /**
     * Highlight the save button to indicate action
     */
    private fun highlightSaveButton(saveButton: ImageView) {
        saveButton.setColorFilter(
            ContextCompat.getColor(context, R.color.colorAccent), 
            PorterDuff.Mode.SRC_IN
        )
        
        // Pulse animation for feedback
        bubbleAnimator.animatePulse(saveButton, 2)
    }
    
    /**
     * Send intent to service to save the page for offline access
     */
    private fun sendSaveOfflineIntent() {
        val intent = Intent(context, BubbleService::class.java).apply {
            action = "com.qb.browser.SAVE_OFFLINE"
            putExtra(Constants.EXTRA_URL, url)
            putExtra(Constants.EXTRA_BUBBLE_ID, bubbleId)
        }
        context.startService(intent)
    }
    
    /**
     * Show feedback message for save operation
     */
    private fun showSaveFeedbackMessage(messageView: TextView, saveButton: ImageView) {
        // Show initial saving message
        messageView.text = context.getString(R.string.saving_page_offline)
        messageView.visibility = View.VISIBLE
        
        // Update to saved message after delay
        postDelayed({
            messageView.text = context.getString(R.string.page_saved_offline)
            
            // Hide the message after additional delay
            postDelayed({
                messageView.visibility = View.GONE
            }, 2000)
            
            // Reset save button appearance
            saveButton.clearColorFilter()
        }, 3000)
    }
    
    // Commented out code removed for clarity
    
    /**
     * Format and validate URL to ensure it loads correctly
     * 
     * @param inputUrl The URL to format
     * @return A properly formatted URL or empty string if invalid
     */
    private fun formatUrl(inputUrl: String): String {
        return when {
            // If it's already a valid URL with scheme, use it as is
            inputUrl.startsWith("http://") || inputUrl.startsWith("https://") -> inputUrl
            
            // If it's a special URL like about:blank, use it as is
            inputUrl.startsWith("about:") || inputUrl.startsWith("file:") || 
            inputUrl.startsWith("javascript:") || inputUrl.startsWith("data:") -> inputUrl
            
            // If it looks like a domain (contains dots), add https://
            inputUrl.contains(".") -> "https://$inputUrl"
            
            // If it's not a valid URL, return empty string
            else -> ""
        }
    }
    
    /**
     * Save the current bubble position in preferences
     */
    private fun saveBubblePosition() {
        if (!settingsManager.isBubblePositionSavingEnabled() || layoutParams !is WindowManager.LayoutParams) {
            return
        }
        
        val params = layoutParams as WindowManager.LayoutParams
        settingsManager.saveBubblePosition(url, params.x, params.y)
    }
    
    /**
     * Load saved position for this bubble if available
     * 
     * @return true if position was loaded successfully, false otherwise
     */
    fun loadSavedPosition(): Boolean {
        if (!settingsManager.isBubblePositionSavingEnabled() || layoutParams !is WindowManager.LayoutParams) {
            return false
        }
        
        val savedPosition = settingsManager.getSavedBubblePosition(url) ?: return false
        
        val params = layoutParams as WindowManager.LayoutParams
        params.x = savedPosition.first
        params.y = savedPosition.second
        windowManager.updateViewLayout(this, params)
        return true
    }
    
    /**
     * Handle touch events for dragging with snap to edges
     * 
     * This method handles:
     * - Detecting the start of a drag operation
     * - Moving the bubble during drag
     * - Collapsing the bubble if expanded when dragging starts
     * - Saving the bubble position when dragging ends
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (layoutParams !is WindowManager.LayoutParams) return super.onTouchEvent(event)
        
        val params = layoutParams as WindowManager.LayoutParams
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                handleTouchDown(event, params)
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                handleTouchMove(event, params, screenWidth, screenHeight)
                return isDragging
            }
            
            MotionEvent.ACTION_UP -> {
                handleTouchUp(params)
                return true
            }
        }
        
        return super.onTouchEvent(event)
    }
    
    /**
     * Handle the touch down event
     */
    private fun handleTouchDown(event: MotionEvent, params: WindowManager.LayoutParams) {
        initialX = params.x.toFloat()
        initialY = params.y.toFloat()
        initialTouchX = event.rawX
        initialTouchY = event.rawY
        isDragging = false
    }
    
    /**
     * Handle the touch move event
     */
    private fun handleTouchMove(
        event: MotionEvent, 
        params: WindowManager.LayoutParams,
        screenWidth: Int,
        screenHeight: Int
    ) {
        val dx = event.rawX - initialTouchX
        val dy = event.rawY - initialTouchY
        
        // Check if we've moved enough to consider it a drag
        if (!isDragging && hypot(dx, dy) > touchSlop) {
            isDragging = true
            // Collapse if expanded when starting to drag
            if (isBubbleExpanded) {
                toggleBubbleExpanded()
            }
        }
        
        if (isDragging) {
            // Keep bubble within screen bounds
            params.x = max(0, min(screenWidth - width, (initialX + dx).toInt()))
            params.y = max(0, min(screenHeight - height, (initialY + dy).toInt()))
            windowManager.updateViewLayout(this, params)
        }
    }
    
    /**
     * Handle the touch up event
     */
    private fun handleTouchUp(params: WindowManager.LayoutParams) {
        if (!isDragging) {
            performClick()
        } else {
            windowManager.updateViewLayout(this, params)
            saveBubblePosition()
        }
    }
    
    /**
     * Override performClick for accessibility
     */
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    /**
     * Get size multiplier based on size string
     * 
     * @param sizeString String representation of size (small, medium, large, extra_large)
     * @return Float multiplier for the size
     */
    private fun getSizeValue(sizeString: String): Float {
        return when(sizeString) {
            "small" -> 0.5f
            "medium" -> 0.75f
            "large" -> 1.0f
            "extra_large" -> 1.25f
            else -> 0.75f  // default to medium
        }
    }

    /**
     * Update the list of bubbles (currently just logs the update)
     * 
     * @param bubbles List of Bubble objects
     */
    fun updateBubblesList(bubbles: List<Bubble>) {
        // We no longer use the main bubble concept
        Log.d(TAG, "Bubble list update received: ${bubbles.size} bubbles")
    }

    /**
     * Set a listener to be called when the bubble is closed
     * 
     * @param listener Callback function to invoke when bubble is closed
     */
    fun setOnCloseListener(listener: () -> Unit) {
        onCloseListener = listener
    }

    /**
     * Update the favicon of the bubble (public method)
     * 
     * @param favicon Bitmap to use as favicon
     */
    fun updateFavicon(favicon: Bitmap) {
        updateBubbleIcon(favicon)
    }
    
    /**
     * Update the bubble icon with a favicon
     * 
     * @param favicon Bitmap to use as favicon
     */
    private fun updateBubbleIcon(favicon: Bitmap) {
        try {
            // Run on UI thread to update the ImageView
            post {
                bubbleIcon.setImageBitmap(favicon)
                Log.d(TAG, "Bubble icon updated with favicon for bubble: $bubbleId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating bubble icon", e)
            // Fallback to default icon if there's an error
            post {
                bubbleIcon.setImageResource(R.drawable.ic_globe)
            }
        }
    }
    
    /**
     * Create a standalone instance of WebViewModel when we can't get it from ViewModelProvider
     * 
     * This is a fallback mechanism when the normal ViewModel architecture can't be used.
     */
    private fun createStandaloneWebViewModel() {
        try {
            Log.d(TAG, "Creating standalone WebViewModel instance")
            webViewModel = WebViewModel()
            
            // Set up periodic favicon checking
            setupPeriodicFaviconCheck()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating standalone WebViewModel", e)
        }
    }
    
    /**
     * Set up a periodic check for favicon updates from the WebView
     */
    private fun setupPeriodicFaviconCheck() {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                // Check if the WebView has a favicon and update if available
                webViewContainer.favicon?.let { favicon ->
                    updateBubbleIcon(favicon)
                }
                // Schedule the next check
                handler.postDelayed(this, 2000) // Check every 2 seconds
            }
        })
    }

    /**
     * Update the loading progress of the bubble
     * 
     * @param progress Progress value (0-100)
     */
    fun updateProgress(progress: Int) {
        if (progress !in 0..100) return
        
        progressBar.progress = progress
        
        when {
            // Show progress bar when loading (1-99%)
            progress in 1..99 -> {
                progressBar.visibility = View.VISIBLE
                updateProgressColor(progress)
                
                // Log progress for debugging at 20% intervals
                if (progress % 20 == 0) {
                    Log.d(TAG, "Loading progress: $progress%")
                }
            }
            // Hide when complete or not started
            else -> progressBar.visibility = View.GONE
        }
    }
    
    /**
     * Update the progress bar color based on progress value
     * 
     * @param progress Progress value (0-100)
     */
    private fun updateProgressColor(progress: Int) {
        val color = when {
            progress < 30 -> ContextCompat.getColor(context, R.color.colorAccent)
            progress < 70 -> ContextCompat.getColor(context, R.color.colorPrimary)
            else -> ContextCompat.getColor(context, android.R.color.holo_green_light)
        }
        progressBar.progressDrawable?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
    }

    /**
     * Set the bubble as active (expanded and showing content)
     */
    fun setActive() {
        isActive = true
        expandedContainer.visibility = View.VISIBLE
        bubbleAnimator.animateExpand(expandedContainer)
        showWebView()
    }
    
    /**
     * Set the bubble as inactive (collapsed)
     */
    fun setInactive() {
        isActive = false
        expandedContainer.visibility = View.GONE
    }

    /**
     * Show the WebView and load content
     */
    private fun showWebView() {
        webViewContainer.visibility = View.VISIBLE
        tabsContainer.visibility = View.GONE
        
        // Load URL in WebView
        loadUrlInWebView()
    }
    
    /**
     * Load the URL in the WebView with proper formatting
     */
    private fun loadUrlInWebView() {
        val formattedUrl = formatUrl(url)
        if (formattedUrl.isNotEmpty()) {
            Log.d(TAG, "Loading URL in showWebView: $formattedUrl")
            webViewContainer.loadUrl(formattedUrl)
        } else {
            Log.d(TAG, "Invalid URL format in showWebView: $url")
            webViewContainer.loadUrl("about:blank")
        }
    }

    /**
     * Set the expanded state of the bubble
     * 
     * @param expanded Whether the bubble should be expanded
     */
    fun setExpanded(expanded: Boolean) {
        isExpanded = expanded
    }
}
