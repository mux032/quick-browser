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
            putExtra(BubbleService.EXTRA_BUBBLE_ID, bubbleId)
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
            
            // Configure WebView settings
            configureWebViewSettings()
            
            // Set up WebView clients
            setupWebViewClients()
            
            // Set WebView background to white for better visibility
            webViewContainer.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
            
            // Load the URL
            loadInitialUrl()
            
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
        }
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
    }
    
    /**
     * Load the initial URL in the WebView
     */
    private fun loadInitialUrl() {
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
        tabsContainer.visibility = View.GONE
        
        // Set the dimensions for the expanded container
        resizeExpandedContainer()
        
        // Load content in WebView
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
     * Load content in the WebView when bubble is expanded
     */
    private fun loadContentInExpandedWebView() {
        try {
            Log.d(TAG, "Making WebView visible for bubble $bubbleId with URL: $url")
            
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
    }
    
    /**
     * Collapse the bubble to show only the icon
     */
    private fun collapseBubble() {
        // Hide expanded container with animation
        bubbleAnimator.animateCollapse(expandedContainer)
        
        // Slight shrink animation on collapse
        bubbleAnimator.animateBounce(rootView.findViewById(R.id.bubble_container), false)
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
                WebViewActivity.EXTRA_URL to url,
                WebViewActivity.EXTRA_BUBBLE_ID to bubbleId
            )
            
            // Collapse if expanded
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
            launchActivity(ReadModeActivity::class.java,
                ReadModeActivity.EXTRA_URL to url,
                ReadModeActivity.EXTRA_BUBBLE_ID to bubbleId
            )
            
            // Collapse if expanded
            if (isBubbleExpanded) toggleBubbleExpanded()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open read mode", e)
        }
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
            putExtra(BubbleService.EXTRA_URL, url)
            putExtra(BubbleService.EXTRA_BUBBLE_ID, bubbleId)
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
