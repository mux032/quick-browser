package com.qb.browser.ui

import android.content.Context
import android.view.ContextThemeWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qb.browser.R
import com.qb.browser.service.BubbleService
import com.qb.browser.util.SettingsManager
import com.qb.browser.viewmodel.WebViewModel
import java.lang.Math.min
import kotlin.math.hypot
import kotlin.math.max
import com.qb.browser.model.Bubble
import com.qb.browser.model.WebPage
import com.qb.browser.ui.adapter.TabsAdapter
import com.qb.browser.Constants
import android.util.Log
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Enhanced floating bubble view with animations and multi-bubble management
 */
class BubbleView @JvmOverloads constructor(
    context: Context,
    val bubbleId: String,
    val url: String,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    private val rootView: View
    private val bubbleIcon: ImageView
    private val progressBar: ProgressBar
    private val expandedContainer: View
    private var contentContainer: FrameLayout
    private var tabsContainer: View
    private var webViewContainer: WebView
    private var tabsAdapter: TabsAdapter? = null

    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var isBubbleExpanded = false
    private var onCloseListener: (() -> Unit)? = null
    private var isActive = false
    private var isShowingAllBubbles = false
    private var isExpanded = false
    
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val settingsManager = SettingsManager.getInstance(context)
    private val bubbleAnimator = BubbleAnimator(context)
    private var webViewModel: WebViewModel? = null

    companion object {
        private const val TAG = "BubbleView"
    }
    
    init {
        // Use application context with theme for inflation
        val themedContext = ContextThemeWrapper(context.applicationContext, R.style.Theme_QBrowser)
        rootView = LayoutInflater.from(themedContext).inflate(R.layout.bubble_layout, this, true)
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
        
        // Initialize WebViewModel when attached to window
        post {
            // Find the ViewModelStoreOwner (usually an Activity or Fragment)
            val lifecycleOwner = findViewTreeLifecycleOwner()
            val viewModelStoreOwner = findViewTreeViewModelStoreOwner()
            
            if (lifecycleOwner != null && viewModelStoreOwner != null) {
                try {
                    // Get the WebViewModel using ViewModelStoreOwner
                    webViewModel = ViewModelProvider(viewModelStoreOwner)[WebViewModel::class.java]
                    
                    // Observe web pages for favicon changes using LifecycleOwner
                    lifecycleOwner.lifecycleScope.launch {
                        webViewModel?.webPages?.collectLatest { pages ->
                            // Check if there's a page with our URL and update favicon if available
                            pages[url]?.let { webPage ->
                                webPage.favicon?.let { favicon ->
                                    Log.d(TAG, "Updating bubble icon with favicon for URL: $url")
                                    updateBubbleIcon(favicon)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error initializing WebViewModel", e)
                }
            } else if (lifecycleOwner != null) {
                // Fallback: Try to use the application context if it implements ViewModelStoreOwner
                val application = context.applicationContext
                if (application is ViewModelStoreOwner) {
                    try {
                        Log.d(TAG, "Using application context as ViewModelStoreOwner")
                        webViewModel = ViewModelProvider(application)[WebViewModel::class.java]
                        
                        // Observe web pages for favicon changes
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
                    } catch (e: Exception) {
                        Log.e(TAG, "Error initializing WebViewModel with application context", e)
                    }
                } else {
                    Log.e(TAG, "Could not find ViewModelStoreOwner and application is not a ViewModelStoreOwner")
                    // Create a standalone instance of WebViewModel as a last resort
                    createStandaloneWebViewModel()
                }
            } else {
                Log.e(TAG, "Could not find LifecycleOwner")
                // Create a standalone instance of WebViewModel as a last resort
                createStandaloneWebViewModel()
            }
        }

        // Set up click listeners
        setOnClickListener {
            // Directly toggle the expanded state for all bubbles
            toggleBubbleExpanded()
            
            // Also notify the service that this bubble is active
            val intent = Intent(context, BubbleService::class.java).apply {
                action = BubbleService.ACTION_ACTIVATE_BUBBLE
                putExtra(BubbleService.EXTRA_BUBBLE_ID, bubbleId)
            }
            context.startService(intent)
        }
        
        findViewById<View>(R.id.btn_close).setOnClickListener {
            closeBubbleWithAnimation()
        }
        
        findViewById<View>(R.id.btn_open_full).setOnClickListener {
            openFullWebView()
        }
        
        findViewById<View>(R.id.btn_read_mode).setOnClickListener {
            openReadMode()
        }
        
        findViewById<View>(R.id.btn_save_offline).setOnClickListener {
            saveForOffline()
        }
        
        // Hide new tab button as we don't need it anymore
        findViewById<View>(R.id.btn_new_tab)?.visibility = View.GONE
        
        // Set up content based on bubble type
        setupContent()
    }

    private fun setupContent() {
        // Show WebView for all bubbles
        webViewContainer.visibility = View.VISIBLE
        tabsContainer.visibility = View.GONE
        
        // Hide the new tab button
        findViewById<View>(R.id.btn_new_tab)?.visibility = View.GONE
        
        // Set up WebView
        setupWebView()
    }

    private fun setupWebView() {
        try {
            Log.d(TAG, "Setting up WebView for bubble: $bubbleId with URL: $url")
        
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
            }

            webViewContainer.webChromeClient =
                    object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            updateProgress(newProgress)
                        }

                        override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                            icon?.let { 
                                // Update local favicon
                                updateFavicon(it)
                                
                                // Also update in WebViewModel to persist the favicon
                                webViewModel?.let { viewModel ->
                                    try {
                                        viewModel.updateFavicon(url, it)
                                        Log.d(TAG, "Updated favicon in WebViewModel for URL: $url")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error updating favicon in WebViewModel", e)
                                    }
                                } ?: run {
                                    // If WebViewModel is null, try to create it
                                    try {
                                        Log.d(TAG, "WebViewModel is null, creating new instance")
                                        webViewModel = WebViewModel()
                                        webViewModel?.updateFavicon(url, it)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error creating WebViewModel on the fly", e)
                                    }
                                }
                                
                                Log.d(TAG, "Received favicon for bubble: $bubbleId")
                            }
                        }
                        
                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            title?.let {
                                Log.d(TAG, "Received page title for bubble $bubbleId: $it")
                                // Update title in WebViewModel
                                webViewModel?.let { viewModel ->
                                    try {
                                        viewModel.updateTitle(url, it)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error updating title in WebViewModel", e)
                                    }
                                } ?: run {
                                    // If WebViewModel is null, try to create it
                                    try {
                                        Log.d(TAG, "WebViewModel is null, creating new instance")
                                        webViewModel = WebViewModel()
                                        webViewModel?.updateTitle(url, it)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error creating WebViewModel on the fly", e)
                                    }
                                }
                            }
                        }
                    }
                
            // Add WebViewClient to handle page loading and errors
            webViewContainer.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    Log.d(TAG, "WebView page loading started for bubble $bubbleId: $url")
                    progressBar.visibility = View.VISIBLE
                }
                
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "WebView page loading finished for bubble $bubbleId: $url")
                    progressBar.visibility = View.GONE
                }
                
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    url?.let {
                        Log.d(TAG, "Loading URL in WebView: $it")
                        view?.loadUrl(it)
                    }
                    return true
                }
                
                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Log.e(TAG, "WebView error for bubble $bubbleId: $errorCode - $description for URL: $failingUrl")
                }
            }
        
            // Set WebView background to white for better visibility
            webViewContainer.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
            
            // Load the actual URL immediately
            if (url.isNotEmpty()) {
                val formattedUrl = formatUrl(url)
                if (formattedUrl.isNotEmpty()) {
                    Log.d(TAG, "Loading URL directly in WebView for bubble $bubbleId: $formattedUrl")
                    webViewContainer.loadUrl(formattedUrl)
                } else {
                    Log.d(TAG, "Invalid URL format for bubble $bubbleId: $url")
                    webViewContainer.loadUrl("about:blank")
                }
            } else {
                // Load a blank page for empty URL
                Log.d(TAG, "No URL provided for bubble $bubbleId")
                webViewContainer.loadUrl("about:blank")
            }
            
            Log.d(TAG, "WebView initialized for bubble: $bubbleId")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up WebView for bubble $bubbleId", e)
        }
    }

    /**
     * Reload the webpage if needed
     */
    private fun reloadWebPageIfNeeded() {
        if (url.isNotEmpty()) {
            try {
                // Check if WebView has loaded the URL
                if (webViewContainer.url == null || webViewContainer.url == "about:blank") {
                    // Format and validate URL
                    val loadUrl = formatUrl(url)
                    if (loadUrl.isNotEmpty()) {
                        Log.d(TAG, "Reloading URL for bubble $bubbleId: $loadUrl")
                        
                        // Load the URL
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
    }

    /**
     * Toggle bubble expanded state with animation
     */
    private fun toggleBubbleExpanded() {
        isBubbleExpanded = !isBubbleExpanded
        
        if (isBubbleExpanded) {
            // Show expanded container with animation
            expandedContainer.visibility = View.VISIBLE
            bubbleAnimator.animateExpand(expandedContainer)
            
            // Bounce the bubble
            bubbleAnimator.animateBounce(rootView.findViewById(R.id.bubble_container), true)
            
            // Make sure WebView is visible
            webViewContainer.visibility = View.VISIBLE
            tabsContainer.visibility = View.GONE
            
            // Set the dimensions for the expanded container to take maximum space
            val layoutParams = expandedContainer.layoutParams
            layoutParams.width = resources.displayMetrics.widthPixels * 9 / 10  // 90% of screen width
            layoutParams.height = resources.displayMetrics.heightPixels * 7 / 10 // 70% of screen height
            expandedContainer.layoutParams = layoutParams
            
            // Force layout update
            expandedContainer.requestLayout()
            
            // Make sure the WebView is visible and loaded
            try {
                Log.d(TAG, "Making WebView visible for bubble $bubbleId with URL: $url")
                
                // Make sure WebView is visible
                webViewContainer.visibility = View.VISIBLE
                
                // Always reload the webpage when expanding to ensure content is displayed
                val formattedUrl = formatUrl(url)
                if (formattedUrl.isNotEmpty()) {
                    Log.d(TAG, "Loading URL in expanded bubble: $formattedUrl")
                    webViewContainer.loadUrl(formattedUrl)
                } else {
                    Log.d(TAG, "Invalid URL format in expanded bubble: $url")
                    webViewContainer.loadUrl("about:blank")
                }
                
                Log.d(TAG, "WebView is now visible for bubble $bubbleId")
            } catch (e: Exception) {
                Log.e(TAG, "Error handling WebView visibility for bubble $bubbleId", e)
            }
            
            Log.d(TAG, "Expanded bubble with WebView visible, loading URL: $url")
        } else {
            // Hide expanded container with animation
            bubbleAnimator.animateCollapse(expandedContainer)
            
            // Slight shrink animation on collapse
            bubbleAnimator.animateBounce(rootView.findViewById(R.id.bubble_container), false)
        }
    }

    /**
     * Close the bubble with animation
     */
    private fun closeBubbleWithAnimation() {
        // First collapse if expanded
        if (isBubbleExpanded) {
            bubbleAnimator.animateCollapse(expandedContainer, onEnd = {
                // Then animate bubble disappearance
                bubbleAnimator.animateDisappear(this, onEnd = {
                    // Finally invoke the close listener
                    onCloseListener?.invoke()
                })
            })
        } else {
            // Animate bubble disappearance directly
            bubbleAnimator.animateDisappear(this, onEnd = {
                onCloseListener?.invoke()
            })
        }
    }
    
    
    /**
     * Open the web page in a full WebView activity
     */
    private fun openFullWebView() {
        try {
            val intent = Intent(context, WebViewActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(WebViewActivity.EXTRA_URL, url)
                putExtra(WebViewActivity.EXTRA_BUBBLE_ID, bubbleId)
            }
            context.startActivity(intent)
            if (isBubbleExpanded) toggleBubbleExpanded()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open full WebView", e)
        }
    }
    
    
    /**
     * Open the web page in read mode
     */
    private fun openReadMode() {
        try {
            val intent = Intent(context, ReadModeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(ReadModeActivity.EXTRA_URL, url)
                putExtra(ReadModeActivity.EXTRA_BUBBLE_ID, bubbleId)
            }
            context.startActivity(intent)
            
            // Collapse the expanded view
            if (isBubbleExpanded) {
                toggleBubbleExpanded()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open read mode", e)
        }
}
    
    /**
     * Save the web page for offline access
     */
    private fun saveForOffline() {
        try {
            // Toggle the save button appearance
            val saveButton = findViewById<ImageView>(R.id.btn_save_offline)
            saveButton.setColorFilter(
                ContextCompat.getColor(context, R.color.colorAccent), 
                PorterDuff.Mode.SRC_IN
            )
            
            // Pulse animation for feedback
            bubbleAnimator.animatePulse(saveButton, 2)
            
            // Send intent to service to save the page
            val intent = Intent(context, BubbleService::class.java).apply {
                action = "com.qb.browser.SAVE_OFFLINE"
                putExtra(BubbleService.EXTRA_URL, url)
                putExtra(BubbleService.EXTRA_BUBBLE_ID, bubbleId)
            }
            context.startService(intent)
            
            // Show feedback to user
            val messageView = findViewById<TextView>(R.id.save_message)
            messageView.text = context.getString(R.string.saving_page_offline)
            messageView.visibility = View.VISIBLE
            
            // Hide message after delay
            postDelayed({
                messageView.text = context.getString(R.string.page_saved_offline)
                
                // Hide the message after additional delay
                postDelayed({
                    messageView.visibility = View.GONE
                }, 2000)
                
                // Reset save button appearance
                saveButton.clearColorFilter()
            }, 3000)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save for offline", e)
        }
    }
    
    // fun updateFromState(bubble: Bubble) {
    //     try {
    //         if (bubble.url != webViewContainer.url) {
    //             webViewContainer.loadUrl(bubble.url)
    //         }
    //         if (bubble.isActive) setActive() else setInactive()
    //         updateFavicon(bubble.favicon)
    //     } catch (e: Exception) {
    //         Log.e(TAG, "Error updating bubble from state", e)
    //     }
    // }    
    
    /**
     * Format and validate URL to ensure it loads correctly
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
        if (settingsManager.isBubblePositionSavingEnabled() && layoutParams is WindowManager.LayoutParams) {
            val layoutParams = layoutParams as WindowManager.LayoutParams
            settingsManager.saveBubblePosition(url, layoutParams.x, layoutParams.y)
        }
    }
    
    /**
     * Load saved position for this bubble if available
     */
    fun loadSavedPosition(): Boolean {
        if (settingsManager.isBubblePositionSavingEnabled() && layoutParams is WindowManager.LayoutParams) {
            val savedPosition = settingsManager.getSavedBubblePosition(url)
            if (savedPosition != null) {
                val layoutParams = layoutParams as WindowManager.LayoutParams
                layoutParams.x = savedPosition.first
                layoutParams.y = savedPosition.second
                windowManager.updateViewLayout(this, layoutParams)
                return true
            }
        }
        return false
    }
    
    /**
     * Handle touch events for dragging with snap to edges
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (layoutParams !is WindowManager.LayoutParams) return super.onTouchEvent(event)
        
        val layoutParams = layoutParams as WindowManager.LayoutParams
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = layoutParams.x.toFloat()
                initialY = layoutParams.y.toFloat()
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY
                
                if (!isDragging && hypot(dx, dy) > touchSlop) {
                    isDragging = true
                    if (isBubbleExpanded) {
                        toggleBubbleExpanded()
                    }
                }
                
                if (isDragging) {
                    // Regular bubbles can move freely
                    layoutParams.x = max(0, min(screenWidth - width, (initialX + dx).toInt()))
                
                    layoutParams.y = max(0, min(screenHeight - height, (initialY + dy).toInt()))
                    windowManager.updateViewLayout(this, layoutParams)
                    return true
                }
            }
            
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    performClick()
                } else {
                    windowManager.updateViewLayout(this, layoutParams)
                    saveBubblePosition()
                }
                return true
            }
        }
        
        return super.onTouchEvent(event)
    }
    
    /**
     * Override performClick for accessibility
     */
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun getSizeValue(sizeString: String): Float {
        return when(sizeString) {
            "small" -> 0.5f
            "medium" -> 0.75f
            "large" -> 1.0f
            "extra_large" -> 1.25f
            else -> 0.75f  // default to medium
        }
    }

    fun updateBubblesList(bubbles: List<Bubble>) {
        // We no longer use the main bubble concept
        Log.d(TAG, "Bubble list update received: ${bubbles.size} bubbles")
    }

    /**
     * Set a listener to be called when the bubble is closed
     */
    fun setOnCloseListener(listener: () -> Unit) {
        onCloseListener = listener
    }

    /**
     * Update the favicon of the bubble
     */
    fun updateFavicon(favicon: Bitmap) {
        updateBubbleIcon(favicon)
    }
    
    /**
     * Update the bubble icon with a favicon
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
     */
    private fun createStandaloneWebViewModel() {
        try {
            Log.d(TAG, "Creating standalone WebViewModel instance")
            webViewModel = WebViewModel()
            
            // Set up a handler to check for favicon updates periodically
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            handler.post(object : Runnable {
                override fun run() {
                    // Check if the WebView has a favicon
                    if (webViewContainer.favicon != null) {
                        webViewContainer.favicon?.let { favicon ->
                            updateBubbleIcon(favicon)
                        }
                    }
                    // Schedule the next check
                    handler.postDelayed(this, 2000) // Check every 2 seconds
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error creating standalone WebViewModel", e)
        }
    }

    /**
     * Update the loading progress of the bubble
     */
    fun updateProgress(progress: Int) {
        if (progress in 0..100) {
            progressBar.progress = progress
            
            // Always show progress bar when loading (1-99%)
            if (progress in 1..99) {
                progressBar.visibility = View.VISIBLE
                
                // Change color based on progress
                val color = when {
                    progress < 30 -> ContextCompat.getColor(context, R.color.colorAccent)
                    progress < 70 -> ContextCompat.getColor(context, R.color.colorPrimary)
                    else -> ContextCompat.getColor(context, android.R.color.holo_green_light)
                }
                progressBar.progressDrawable?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
                
                // Log progress for debugging
                if (progress % 20 == 0) {
                    Log.d(TAG, "Loading progress: $progress%")
                }
            } else {
                // Hide when complete or not started
                progressBar.visibility = View.GONE
            }
        }
    }

    fun setActive() {
        isActive = true
        expandedContainer.visibility = View.VISIBLE
        bubbleAnimator.animateExpand(expandedContainer)
        showWebView()
    }
    
    fun setInactive() {
        isActive = false
        expandedContainer.visibility = View.GONE
    }

    private fun showWebView() {
        webViewContainer.visibility = View.VISIBLE
        tabsContainer.visibility = View.GONE
        
        // Format and validate URL before loading
        val formattedUrl = formatUrl(url)
        if (formattedUrl.isNotEmpty()) {
            Log.d(TAG, "Loading URL in showWebView: $formattedUrl")
            webViewContainer.loadUrl(formattedUrl)
        } else {
            Log.d(TAG, "Invalid URL format in showWebView: $url")
            webViewContainer.loadUrl("about:blank")
        }
    }

    fun setExpanded(expanded: Boolean) {
        isExpanded = expanded
    }
}
