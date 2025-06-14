package com.qb.browser.ui.bubble

import android.content.Context
import android.view.ContextThemeWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.qb.browser.Constants
import com.qb.browser.QBApplication
import com.qb.browser.R
import com.qb.browser.model.Bubble
import com.qb.browser.model.WebPage
import com.qb.browser.service.BubbleService
import com.qb.browser.manager.SettingsManager
import com.qb.browser.manager.AdBlocker
import com.qb.browser.manager.AuthenticationHandler
import com.qb.browser.manager.ReadabilityExtractor
import com.qb.browser.manager.SummarizationManager
import com.qb.browser.viewmodel.WebViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
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
    var url: String,  // Changed from val to var to allow URL updates when navigating
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), BubbleTouchHandler.BubbleTouchDelegate, BubbleStateManager.Companion.StateChangeListener, BubbleWebViewManagerInterface {
    
    // UI components
    private lateinit var rootView: View
    private lateinit var bubbleIcon: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var bubbleContainer: View
    private lateinit var urlBarContainer: View
    private lateinit var urlBarIcon: ImageView
    private lateinit var urlBarText: EditText
    private lateinit var btnUrlBarSettings: MaterialButton
    private lateinit var expandedContainer: View
    private lateinit var contentContainer: FrameLayout
    private lateinit var webViewContainer: WebView
    
    // Resize handles
    private lateinit var resizeHandlesContainer: FrameLayout
    private lateinit var resizeHandleTopLeft: ImageView
    private lateinit var resizeHandleTopRight: ImageView
    private lateinit var resizeHandleBottomLeft: ImageView
    private lateinit var resizeHandleBottomRight: ImageView

    // Touch handling state - moved to BubbleTouchHandler
    // Resize state - moved to BubbleTouchHandler
    
    // State Management - centralized in BubbleStateManager
    private val stateManager = BubbleStateManager(bubbleId).apply {
        setStateChangeListener(this@BubbleView)
    }
    
    // Services and utilities
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val settingsManager = SettingsManager.getInstance(context)
    private val bubbleAnimator = BubbleAnimator(context)
    private val touchHandler = BubbleTouchHandler(context, this)
    private var webViewModel: WebViewModel? = null
    
    // WebView Manager - handles all WebView-related functionality
    private lateinit var webViewManager: BubbleWebViewManager

    // Summary/Summarization UI and State
    private lateinit var btnSummarize: MaterialButton // Changed from fabSummarize to btnSummarize
    private lateinit var summaryContainer: FrameLayout
    private lateinit var summaryContent: LinearLayout
    private lateinit var summaryProgress: ProgressBar
    private lateinit var summaryManager: BubbleSummaryManager
    
    // Read Mode UI and State
    private lateinit var btnReadMode: MaterialButton
    private lateinit var readModeManager: BubbleReadModeManager
    
    // Toolbar container
    private lateinit var toolbarContainer: View
    
    // Settings panel
    private lateinit var settingsPanel: View
    private lateinit var settingsPanelManager: BubbleSettingsPanel

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
        
        // Initialize touch handler after all views are set up
        touchHandler.initialize(this)
    }
    
    /**
     * Initialize all view components from the layout
     * 
     * @return Unit
     */
    private fun initializeViews() {
        // Use application context with theme for inflation
        val themedContext = ContextThemeWrapper(context.applicationContext, R.style.Theme_QBrowser)
        rootView = LayoutInflater.from(themedContext).inflate(R.layout.bubble_layout, this, true)
        
        // Find and initialize view references
        bubbleIcon = findViewById(R.id.bubble_icon)
        progressBar = findViewById(R.id.progress_circular)
        bubbleContainer = findViewById(R.id.bubble_container)
        urlBarContainer = findViewById(R.id.url_bar_container)
        urlBarIcon = findViewById(R.id.url_bar_icon)
        urlBarText = findViewById(R.id.url_bar_text)
        btnUrlBarSettings = findViewById(R.id.btn_url_bar_settings)
        expandedContainer = findViewById(R.id.expanded_container)
        contentContainer = findViewById(R.id.content_container)
        webViewContainer = findViewById(R.id.web_view)
        
        // Initialize resize handles
        resizeHandlesContainer = findViewById(R.id.resize_handles_container)
        resizeHandleTopLeft = findViewById(R.id.resize_handle_top_left)
        resizeHandleTopRight = findViewById(R.id.resize_handle_top_right)
        resizeHandleBottomLeft = findViewById(R.id.resize_handle_bottom_left)
        resizeHandleBottomRight = findViewById(R.id.resize_handle_bottom_right)
        
        // Set up default favicon
        bubbleIcon.setImageResource(R.drawable.ic_globe)
        
        // Set up progress indicator
        progressBar.progress = 0   
        
        // Ensure summary views and FAB are initialized after layout is ready
        initializeSummaryViews()
        
        // Initialize settings panel
        settingsPanel = findViewById(R.id.settings_panel)
        
        // Initialize settings panel manager
        settingsPanelManager = BubbleSettingsPanel(context, settingsManager, bubbleAnimator)
        
        // Initialize summary manager
        summaryManager = BubbleSummaryManager(context, bubbleAnimator)
        
        // Initialize read mode manager
        readModeManager = BubbleReadModeManager(context, settingsManager)
        
        // Initialize WebView manager
        webViewManager = BubbleWebViewManager(context, bubbleId, this)
        
        // Note: Resize handle setup is now handled by BubbleTouchHandler
    }
    
    /**
     * Initialize WebViewModel and set up favicon observers
     * 
     * @return Unit
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
     * 
     * @param lifecycleOwner The lifecycle owner to use for launching coroutines
     * @return Unit
     */
    private fun observeFaviconChanges(lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
        lifecycleOwner.lifecycleScope.launch {
            webViewModel?.webPages?.collectLatest { pages ->
                pages[url]?.let { webPage ->
                    webPage.favicon?.let { favicon ->
                        Log.d(TAG, "Updating bubble icon with favicon for URL: $url")
                        updateBubbleIcon(favicon)
                        // Also update URL bar icon if bubble is expanded
                        if (stateManager.isBubbleExpanded) {
                            urlBarIcon.setImageBitmap(favicon)
                        }
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
        
        // Action button listeners - close settings first, then perform action
        findViewById<View>(R.id.btn_close).setOnClickListener { 
            settingsPanelManager.dismissIfVisible(settingsPanel)
            closeBubbleWithAnimation() 
        }
        findViewById<View>(R.id.btn_open_full).setOnClickListener { 
            settingsPanelManager.dismissIfVisible(settingsPanel)
            openFullWebView() 
        }
        findViewById<View>(R.id.btn_read_mode).setOnClickListener { 
            settingsPanelManager.dismissIfVisible(settingsPanel)
            readModeManager.toggleReadMode() 
        }
        findViewById<View>(R.id.btn_summarize).setOnClickListener { 
            settingsPanelManager.dismissIfVisible(settingsPanel)
            summaryManager.toggleSummaryMode() 
        }

        
        // URL bar settings button listener
        btnUrlBarSettings.setOnClickListener { 
            if (settingsPanelManager.isVisible()) {
                settingsPanelManager.hide(settingsPanel)
            } else {
                settingsPanelManager.show(settingsPanel, btnUrlBarSettings)
            }
        }
        
        // Note: Click-outside-to-close is handled in onTouchEvent for better reliability
        
        // Handle clicks on expanded container - close settings when clicking inside container
        // but outside of specific interactive elements
        expandedContainer.setOnTouchListener { _, event ->
            settingsPanelManager.handleTouchEvent(event, settingsPanel)
            false // Don't consume the event, let child views handle it
        }
        
        // URL bar input handling
        setupUrlBarInput()
        
        // Initialize toolbar container reference
        toolbarContainer = findViewById(R.id.toolbar_container)
        
        // Note: Toolbar drag functionality is now handled by BubbleTouchHandler
    }
    
    /**
     * Set up URL bar input handling
     */
    private fun setupUrlBarInput() {
        // Handle URL input submission
        urlBarText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                val inputUrl = urlBarText.text.toString().trim()
                if (inputUrl.isNotEmpty()) {
                    loadNewUrl(inputUrl)
                    hideKeyboard()
                }
                true
            } else {
                false
            }
        }
        
        // Handle focus changes to show/hide keyboard appropriately
        urlBarText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showKeyboard()
            } else {
                hideKeyboard()
            }
        }
        
        // Handle click to show keyboard and select all text - close settings first
        urlBarText.setOnClickListener {
            settingsPanelManager.dismissIfVisible(settingsPanel)
            urlBarText.requestFocus()
            urlBarText.selectAll()
            showKeyboard()
        }
    }
    
    /**
     * Load a new URL in the WebView
     */
    private fun loadNewUrl(inputUrl: String) {
        val formattedUrl = formatUrl(inputUrl)
        url = formattedUrl
        webViewContainer.loadUrl(formattedUrl)
        updateUrlBar()
        
        // Update read mode manager with new URL
        readModeManager.updateCurrentUrl(formattedUrl)
    }
    
    /**
     * Show the soft keyboard
     */
    private fun showKeyboard() {
        urlBarText.post {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(urlBarText, InputMethodManager.SHOW_IMPLICIT)
        }
    }
    
    /**
     * Hide the soft keyboard
     */
    private fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(urlBarText.windowToken, 0)
        urlBarText.clearFocus()
    }
    
    /**
     * Enable window focus to allow keyboard input
     */
    private fun enableWindowFocus() {
        try {
            val params = layoutParams as? WindowManager.LayoutParams ?: return
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            windowManager.updateViewLayout(this, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling window focus", e)
        }
    }
    
    /**
     * Disable window focus to prevent accidental keyboard
     */
    private fun disableWindowFocus() {
        try {
            val params = layoutParams as? WindowManager.LayoutParams ?: return
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            windowManager.updateViewLayout(this, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling window focus", e)
        }
    }
    
    // Resize handle setup is now handled by BubbleTouchHandler
    
    // Resize functionality moved to BubbleTouchHandler
    
    /**
     * Show resize handles when bubble is expanded
     */
    private fun showResizeHandles() {
        val handles = listOf(
            resizeHandleTopLeft,
            resizeHandleTopRight,
            resizeHandleBottomLeft,
            resizeHandleBottomRight
        )
        
        bubbleAnimator.animateResizeHandlesShow(handles)
    }
    
    /**
     * Calculate a smooth zoom level based on the width ratio of the bubble to screen
     * 
     * This function uses a sigmoid function to create an extremely smooth S-curve:
     * - Provides a very natural, gradual transition between zoom levels
     * - Creates a mathematically elegant curve with no sudden changes
     * - Maintains higher zoom levels until the window gets quite small
     * - Ensures zoom never goes below 75% even for very small windows
     * 
     * @param widthRatio The ratio of bubble width to screen width (0.0-1.0)
     * @return The calculated zoom percentage (75-100)
     */
    private fun calculateSmoothZoomLevel(widthRatio: Float): Float {
        // Sigmoid function parameters
        val steepness = 10.0 // Controls how steep the S-curve is (higher = steeper transition)
        val midpoint = 0.6 // The width ratio at which the sigmoid is centered (inflection point)
        val minZoom = 75f // Minimum zoom level
        val maxZoom = 100f // Maximum zoom level
        val zoomRange = maxZoom - minZoom
        
        // Apply sigmoid function: f(x) = 1 / (1 + e^(-steepness * (x - midpoint)))
        // This creates a smooth S-curve that transitions gradually between 0 and 1
        val sigmoidValue = 1.0 / (1.0 + Math.exp(-steepness * (widthRatio - midpoint)))
        
        // Map the sigmoid output (0-1) to our zoom range (75-100)
        return (minZoom + (sigmoidValue * zoomRange).toFloat()).coerceIn(minZoom, maxZoom)
    }

    /**
     * Apply dynamic zoom level to the WebView content based on window size
     * 
     * @param zoomPercent The zoom percentage to apply (75-100)
     */
    private fun applyDynamicZoom(zoomPercent: Float) {
        // Store the current zoom level for persistence when bubble is collapsed/expanded
        stateManager.setZoomPercent(zoomPercent)
        
        // Apply zoom via WebViewManager
        webViewManager.applyDynamicZoom(zoomPercent.toInt())
    }
    
    /**
     * Hide resize handles when bubble is collapsed
     */
    private fun hideResizeHandles() {
        val handles = listOf(
            resizeHandleTopLeft,
            resizeHandleTopRight,
            resizeHandleBottomLeft,
            resizeHandleBottomRight
        )
        
        bubbleAnimator.animateResizeHandlesHide(handles) {
            resizeHandlesContainer.visibility = View.GONE
        }
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
        
        // Set up WebView using the WebViewManager
        setupWebViewManager()
    }
    
    /**
     * Set up WebView manager with proper initialization and callbacks
     */
    private fun setupWebViewManager() {
        try {
            Log.d(TAG, "Setting up WebViewManager for bubble: $bubbleId with URL: $url")
            
            // Initialize the manager with WebView components
            webViewManager.initialize(webViewContainer, progressBar, webViewModel)
            
            // Initialize settings panel manager with WebView
            setupSettingsPanelManager()
            
            // Initialize summary manager with WebView
            setupSummaryManager()
            
            // Initialize read mode manager with WebView
            setupReadModeManager()
            
            // Set up touch listener for WebView to handle settings dismissal
            webViewContainer.setOnTouchListener { _, event ->
                settingsPanelManager.handleTouchEvent(event, settingsPanel)
                false // Don't consume the event, let WebView handle it normally
            }
            
            // Make WebView ready to load content in the background with alpha=0
            webViewContainer.alpha = 0f
            
            // Load the initial URL
            if (url.isNotEmpty()) {
                post { webViewManager.loadUrl(url) }
            }
            
            Log.d(TAG, "WebViewManager setup complete for bubble: $bubbleId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up WebViewManager for bubble $bubbleId", e)
        }
    }
    
    // Note: Scroll listener functionality is now handled by BubbleWebViewManager
    
    /**
     * Hide toolbar with animation
     */
    private fun hideToolbar() {
        if (!stateManager.isToolbarVisible) return // Already hidden
        
        stateManager.setToolbarVisible(false)
        bubbleAnimator.animateToolbarSlide(toolbarContainer, false)
    }
    
    /**
     * Show toolbar with animation
     */
    private fun showToolbar() {
        if (stateManager.isToolbarVisible) return // Already visible
        
        stateManager.setToolbarVisible(true)
        bubbleAnimator.animateToolbarSlide(toolbarContainer, true)
    }
    


    // Note: setupWebView() method removed - functionality moved to BubbleWebViewManager
    
    /**
     * Set up the settings panel manager with proper initialization and listener
     */
    private fun setupSettingsPanelManager() {
        // Initialize the settings panel manager with WebView
        settingsPanelManager.initialize(settingsPanel, webViewContainer)
        
        // Set up listener for settings changes
        settingsPanelManager.setListener(object : BubbleSettingsPanel.SettingsPanelListener {
            override fun onAdBlockingChanged(enabled: Boolean) {
                // Handle ad blocking change - refresh page if needed
                Log.d(TAG, "Ad blocking ${if (enabled) "enabled" else "disabled"} for bubble $bubbleId")
            }
            
            override fun onJavaScriptChanged(enabled: Boolean) {
                // Handle JavaScript change - refresh page if needed
                Log.d(TAG, "JavaScript ${if (enabled) "enabled" else "disabled"} for bubble $bubbleId")
            }
            
            override fun onSettingsPanelVisibilityChanged(isVisible: Boolean) {
                // Update any UI state that depends on settings panel visibility
                Log.d(TAG, "Settings panel ${if (isVisible) "shown" else "hidden"} for bubble $bubbleId")
            }
        })
    }
    
    /**
     * Set up the summary manager with proper initialization and listener
     */
    private fun setupSummaryManager() {
        // Initialize the summary manager with WebView and UI components
        summaryManager.initialize(
            summaryContainer,
            summaryContent,
            summaryProgress,
            btnSummarize,
            webViewContainer
        )
        
        // Set up listener for summary events
        summaryManager.setListener(object : BubbleSummaryManager.SummaryManagerListener {
            override fun onSummaryModeChanged(isSummaryMode: Boolean) {
                // Update any UI state that depends on summary mode
                Log.d(TAG, "Summary mode ${if (isSummaryMode) "enabled" else "disabled"} for bubble $bubbleId")
            }
            
            override fun onSummarizationStarted() {
                Log.d(TAG, "Summarization started for bubble $bubbleId")
            }
            
            override fun onSummarizationCompleted(success: Boolean) {
                Log.d(TAG, "Summarization ${if (success) "completed successfully" else "failed"} for bubble $bubbleId")
            }
            
            override fun onSummarizationError(message: String) {
                Log.e(TAG, "Summarization error for bubble $bubbleId: $message")
            }
        })
    }
    
    /**
     * Set up the read mode manager with proper initialization and listener
     */
    private fun setupReadModeManager() {
        // Initialize the read mode manager with WebView and UI components
        readModeManager.initialize(
            btnReadMode,
            webViewContainer,
            progressBar,
            url
        )
        
        // Set up listener for read mode events
        readModeManager.setListener(object : BubbleReadModeManager.ReadModeManagerListener {
            override fun onReadModeChanged(isReadMode: Boolean) {
                Log.d(TAG, "Read mode ${if (isReadMode) "enabled" else "disabled"} for bubble $bubbleId")
            }
            
            override fun onReadModeLoadingStarted() {
                Log.d(TAG, "Read mode loading started for bubble $bubbleId")
            }
            
            override fun onReadModeLoadingCompleted(success: Boolean) {
                Log.d(TAG, "Read mode loading ${if (success) "completed" else "failed"} for bubble $bubbleId")
            }
            
            override fun onReadModeError(message: String) {
                Log.e(TAG, "Read mode error for bubble $bubbleId: $message")
            }
            
            override fun onBubbleExpandRequested() {
                // Expand bubble if not already expanded
                if (!stateManager.isBubbleExpanded) {
                    toggleBubbleExpanded()
                }
            }
        })
    }
    
    // Note: WebView configuration methods removed - functionality moved to BubbleWebViewManager
    // - configureWebViewSettings()
    // - configureBasicWebViewSettings() 
    // - configureSecuritySettings()
    // - configurePerformanceSettings()
    // - configureContentSettings()
    // - setupWebViewClients()
    // - createWebChromeClient()
    
    // Note: handleReceivedFavicon() and handleReceivedTitle() methods removed
    // - functionality moved to BubbleWebViewManager
    
    // Note: Large WebView client and URL loading methods removed
    // - createWebViewClient() - moved to BubbleWebViewManager  
    // - loadInitialUrl() - moved to BubbleWebViewManager
    // - reloadWebPageIfNeeded() - moved to BubbleWebViewManager

    /**
     * Toggle bubble expanded state with animation
     * 
     * When expanded, the bubble shows a WebView with the loaded URL.
     * When collapsed, only the bubble icon is visible.
     */
    private fun toggleBubbleExpanded() {
        stateManager.toggleExpansion()
        
        if (stateManager.isBubbleExpanded) {
            expandBubble()
        } else {
            collapseBubble()
        }
    }
    
    /**
     * Expand the bubble to show web content
     */
    private fun expandBubble() {
        // Update URL bar with current URL
        updateUrlBar()
        
        // Enable input focus for keyboard
        enableWindowFocus()
        
        // Reset toolbar state
        stateManager.setToolbarVisible(true)
        toolbarContainer.translationY = 0f
        
        // Configure container visibility
        webViewContainer.visibility = View.VISIBLE
        webViewContainer.alpha = 1f
        
        // Set the dimensions for the expanded container
        resizeExpandedContainer()
        
        // Start the expand animation with proper sequencing
        bubbleAnimator.animateExpandFromBubble(
            bubbleContainer = bubbleContainer,
            urlBarContainer = urlBarContainer,
            expandedContainer = expandedContainer,
            onEnd = {
                // Show resize handles after expansion is complete
                showResizeHandles()
                resizeHandlesContainer.visibility = View.VISIBLE
                
                // Make WebView visible and ensure content is loaded
                loadContentInExpandedWebView()
            }
        )
    }
    
    /**
     * Update the URL bar with current URL and favicon
     */
    private fun updateUrlBar() {
        // Set the URL text
        urlBarText.setText(url)
        
        // Update the favicon in the URL bar
        webViewModel?.webPages?.value?.get(url)?.favicon?.let { favicon ->
            urlBarIcon.setImageBitmap(favicon)
        } ?: run {
            // Use default globe icon if no favicon available
            urlBarIcon.setImageResource(R.drawable.ic_globe)
        }
    }
    
    /**
     * Resize the expanded container to take appropriate screen space.
     * Initial dimensions are defined in dimens.xml and can be customized there.
     */
    private fun resizeExpandedContainer() {
        val layoutParams = expandedContainer.layoutParams
        
        if (stateManager.hasStoredDimensions && stateManager.storedWidth > 0 && stateManager.storedHeight > 0) {
            // Use stored dimensions if available
            layoutParams.width = stateManager.storedWidth
            layoutParams.height = stateManager.storedHeight
        } else {
            // Use default dimensions from resources
            layoutParams.width = resources.getDimensionPixelSize(R.dimen.bubble_expanded_default_width)
            layoutParams.height = resources.getDimensionPixelSize(R.dimen.bubble_expanded_default_height)
            
            // Store these default dimensions
            stateManager.updateDimensions(layoutParams.width, layoutParams.height)
        }
        
        expandedContainer.layoutParams = layoutParams
        
        // Update WebView dimensions to match the container
        val webViewParams = webViewContainer.layoutParams
        webViewParams.width = layoutParams.width
        webViewParams.height = layoutParams.height
        webViewContainer.layoutParams = webViewParams
        
        // Update content container dimensions to match
        val contentParams = contentContainer.layoutParams
        contentParams.width = layoutParams.width
        contentParams.height = layoutParams.height
        contentContainer.layoutParams = contentParams
        
        // If we have a stored zoom level from previous resize operations, use it
        // Otherwise, calculate based on window width
        val zoomPercent = if (stateManager.currentZoomPercent != 100f) {
            // Use the previously stored zoom level
            stateManager.currentZoomPercent
        } else {
            // Calculate initial zoom level based on window width relative to screen width
            val screenWidth = resources.displayMetrics.widthPixels
            val widthRatio = layoutParams.width.toFloat() / screenWidth
            calculateSmoothZoomLevel(widthRatio)
        }
        
        // Apply the dynamic zoom level using JavaScript
        applyDynamicZoom(zoomPercent)
        
        // Force layout update
        expandedContainer.requestLayout()
        webViewContainer.requestLayout()
        contentContainer.requestLayout()
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
            
            // Check if the page is loaded using WebViewManager
            val currentUrl = webViewManager.getCurrentUrl()
            Log.d(TAG, "Current WebView URL: $currentUrl")
            
            // If the page hasn't loaded yet or is blank, reload it
            if (currentUrl == null || currentUrl == "about:blank" || currentUrl.isEmpty()) {
                if (url.isNotEmpty()) {
                    Log.d(TAG, "Loading URL in expanded bubble (fallback): $url")
                    
                    // Use WebViewManager to load the URL
                    webViewManager.loadUrl(url)
                    
                    // Apply the stored zoom level after a short delay to ensure the page has loaded
                    postDelayed({
                        applyDynamicZoom(stateManager.currentZoomPercent)
                    }, 500)
                }
            } else {
                // If the page is already loaded, make sure it's visible
                // Also reapply the stored zoom level
                applyDynamicZoom(stateManager.currentZoomPercent)
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
        // Disable input focus to prevent accidental keyboard
        disableWindowFocus()
        
        // Hide keyboard if visible
        hideKeyboard()
        
        // Hide settings panel if visible
        settingsPanelManager.dismissIfVisible(settingsPanel)
        
        // Exit summary mode if active
        summaryManager.forceExitSummaryMode()
        
        // Exit read mode if active
        readModeManager.forceExitReadMode()
        
        // Hide resize handles immediately
        hideResizeHandles()
        
        // Keep WebView loaded but make it invisible immediately to prevent flash
        webViewContainer.visibility = View.INVISIBLE
        webViewContainer.alpha = 0f
        
        // Start the collapse animation with proper sequencing
        bubbleAnimator.animateCollapseTobubble(
            expandedContainer = expandedContainer,
            urlBarContainer = urlBarContainer,
            bubbleContainer = bubbleContainer,
            onEnd = {
                // Final cleanup after animation completes
                // Don't destroy WebView content - just hide it
                // This ensures the content stays loaded in the background
            }
        )
    }

    /**
     * Close the bubble with animation
     * 
     * Directly animates the bubble disappearing, regardless of its current state.
     */
    private fun closeBubbleWithAnimation() {
        // Hide resize handles immediately to prevent them from showing during animation
        if (stateManager.isBubbleExpanded) {
            hideResizeHandles()
        }
        
        // Hide settings panel if visible
        settingsPanelManager.dismissIfVisible(settingsPanel)
        
        // Exit summary mode if active
        summaryManager.forceExitSummaryMode()
        
        // Exit read mode if active
        readModeManager.forceExitReadMode()
        
        // Hide WebView immediately to prevent flash during animation
        if (stateManager.isBubbleExpanded) {
            webViewContainer.visibility = View.INVISIBLE
            webViewContainer.alpha = 0f
        }
        
        // Animate the entire bubble view disappearing directly
        animateBubbleDisappearance()
    }
    
    /**
     * Animate the bubble disappearing and notify listeners
     */
    private fun animateBubbleDisappearance() {
        if (stateManager.isBubbleExpanded) {
            // For expanded bubbles, animate the expanded UI elements scaling down gracefully
            bubbleAnimator.animateExpandedBubbleClose(
                urlBarContainer = urlBarContainer,
                expandedContainer = expandedContainer,
                bubbleContainer = bubbleContainer,
                onEnd = {
                    stateManager.triggerClose()
                }
            )
        } else {
            // For collapsed bubbles, just animate the bubble icon disappearing
            bubbleContainer.visibility = View.INVISIBLE
            bubbleAnimator.animateDisappear(this, onEnd = {
                stateManager.triggerClose()
            })
        }
    }
    
    /**
     * Open the web page in the device's default browser
     */
    private fun openFullWebView() {
        try {
            val formattedUrl = formatUrl(url)
            if (formattedUrl.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(formattedUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Invalid URL", Toast.LENGTH_SHORT).show()
            }
            // Close the bubble if openFullWebView is called
            closeBubbleWithAnimation()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open in default browser", e)
            Toast.makeText(context, "Could not open in browser", Toast.LENGTH_SHORT).show()
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
     * Handle touch events for dragging the bubble and handling click events.
     * 
     * This method delegates to BubbleTouchHandler for all touch handling logic.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Delegate to touch handler
        val handled = touchHandler.handleTouchEvent(event)
        return if (handled) true else super.onTouchEvent(event)
    }
    
    /**
     * Close the bubble (called by touch handler)
     */
    private fun closeBubble() {
        // Logic to close the bubble
        stateManager.setActive(false)
        expandedContainer.visibility = View.GONE
        bubbleContainer.visibility = View.GONE
        resizeHandlesContainer.visibility = View.GONE
        stateManager.triggerClose()
    }
    
    /**
     * Override performClick for accessibility
     */
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
    
    // ======================================
    // BubbleTouchDelegate Implementation
    // ======================================
    
    override fun onBubbleDragged(x: Int, y: Int) {
        // Handle bubble drag position updates if needed
        // This is called when the bubble position changes during drag
    }
    
    override fun onBubbleClicked() {
        toggleBubbleExpanded()
        notifyBubbleActivated()
    }
    
    override fun onBubbleClosed() {
        closeBubble()
    }
    
    override fun onBubbleToggleExpanded() {
        toggleBubbleExpanded()
    }
    
    override fun hideBubbleSettingsPanel() {
        settingsPanelManager.dismissIfVisible(settingsPanel)
    }
    
    override fun isSettingsPanelVisible(): Boolean {
        return settingsPanelManager.isVisible()
    }
    
    override fun isBubbleExpanded(): Boolean {
        return stateManager.isBubbleExpanded
    }
    
    override fun getExpandedContainer(): View {
        return expandedContainer
    }
    
    override fun getSettingsPanel(): View {
        return settingsPanel
    }
    
    override fun getSettingsButton(): MaterialButton {
        return btnUrlBarSettings
    }
    
    override fun getToolbarContainer(): View {
        return toolbarContainer
    }
    
    override fun getResizeHandles(): List<ImageView> {
        return listOf(
            resizeHandleTopLeft,
            resizeHandleTopRight, 
            resizeHandleBottomLeft,
            resizeHandleBottomRight
        )
    }
    
    override fun getContentContainer(): FrameLayout {
        return contentContainer
    }
    
    override fun getWebViewContainer(): View {
        return webViewContainer
    }
    
    override fun updateDimensions(width: Int, height: Int) {
        stateManager.updateDimensions(width, height)
    }
    
    override fun applyBubbleDynamicZoom(zoomPercent: Float) {
        // Call the existing private method
        applyDynamicZoom(zoomPercent)
    }
    
    override fun calculateBubbleZoomLevel(widthRatio: Float): Float {
        return calculateSmoothZoomLevel(widthRatio)
    }
    
    override fun getCurrentZoomPercent(): Float {
        return stateManager.currentZoomPercent
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
     * Set a listener to be called when the bubble is closed
     * 
     * @param listener Callback function to invoke when bubble is closed
     */
    fun setOnCloseListener(listener: () -> Unit) {
        stateManager.setOnCloseListener(listener)
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
     * 
     * @return Unit
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
     * Helper method to safely update the WebViewModel
     * Creates a new instance if needed
     * 
     * @param action The action to perform with the WebViewModel
     * @return Unit
     */
    private fun updateWebViewModel(action: (WebViewModel) -> Unit) {
        webViewModel?.let { viewModel ->
            try {
                action(viewModel)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating WebViewModel", e)
            }
        } ?: run {
            // If WebViewModel is null, try to create it
            try {
                Log.d(TAG, "WebViewModel is null, creating new instance")
                webViewModel = WebViewModel()
                webViewModel?.let { action(it) }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating WebViewModel on the fly", e)
            }
        }
    }
    
    /**
     * Set up a periodic check for favicon updates from the WebView
     * 
     * @return Unit
     */
    private fun setupPeriodicFaviconCheck() {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val faviconRunnable = object : Runnable {
            override fun run() {
                // Only continue if the view is attached to window
                if (!isAttachedToWindow) {
                    Log.d(TAG, "Stopping favicon checks as view is detached")
                    return
                }
                
                // Check if the WebView has a favicon and update if available
                webViewContainer.favicon?.let { favicon ->
                    updateBubbleIcon(favicon)
                }
                
                // Schedule the next check with a longer interval (5 seconds instead of 2)
                handler.postDelayed(this, 5000)
            }
        }
        
        // Start the periodic check
        handler.post(faviconRunnable)
        
        // Make sure to remove callbacks when view is detached
        addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                // Restart checks when view is reattached
                handler.post(faviconRunnable)
            }
            
            override fun onViewDetachedFromWindow(v: View) {
                // Stop checks when view is detached
                handler.removeCallbacks(faviconRunnable)
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
        stateManager.setActive(true)
        expandedContainer.visibility = View.VISIBLE
        bubbleAnimator.animateExpand(expandedContainer)
        
        // Show resize handles when expanded container is visible
        resizeHandlesContainer.visibility = View.VISIBLE
        
        // Show the regular web view (not the summary view)
        webViewContainer.visibility = View.VISIBLE
        loadUrlInWebView()
    }
    
    /**
     * Set the bubble as inactive (collapsed)
     */
    fun setInactive() {
        stateManager.setActive(false)
        expandedContainer.visibility = View.GONE
        
        // Hide resize handles when expanded container is hidden
        resizeHandlesContainer.visibility = View.GONE
    }

    /**
     * Show the WebView and load content
     */
    private fun showWebView() {
        webViewContainer.visibility = View.VISIBLE
        
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
            
            // Check if this is an authentication URL that should be handled with Custom Tabs
            if (AuthenticationHandler.isAuthenticationUrl(formattedUrl)) {
                Log.d(TAG, "Authentication URL detected in loadUrlInWebView, opening in Custom Tab: $formattedUrl")
                AuthenticationHandler.openInCustomTab(context, formattedUrl, bubbleId)
                // Load a blank page in the WebView to avoid showing the authentication page
                webViewContainer.loadUrl("about:blank")
            } else {
                // Load the URL in the WebView
                webViewContainer.loadUrl(formattedUrl)
            }
        } else {
            Log.d(TAG, "Invalid URL format in showWebView: $url")
            webViewContainer.loadUrl("about:blank")
        }
    }

    /**
     * Set the expanded state of the bubble
     * 
     * @param expanded Whether the bubble should be expanded
     * @return Unit
     */
    fun setExpanded(expanded: Boolean) {
        stateManager.setExpanded(expanded)
        
        // Update UI based on expanded state
        if (expanded) {
            expandBubble()
        } else {
            collapseBubble()
        }
    }
    
    /**
     * Initialize summary views
     */
    private fun initializeSummaryViews() {
        // Inflate or find summary container and content
        summaryContainer = findViewById(R.id.summary_container) ?: FrameLayout(context).also {
            it.id = R.id.summary_container
            it.visibility = View.GONE
            (expandedContainer as? ViewGroup)?.addView(it)
        }
        summaryContent = findViewById(R.id.summary_content) ?: LinearLayout(context).also {
            it.id = R.id.summary_content
            it.orientation = LinearLayout.VERTICAL
            (summaryContainer as? ViewGroup)?.addView(it)
        }
        summaryProgress = findViewById(R.id.summary_progress) ?: ProgressBar(context).also {
            it.id = R.id.summary_progress
            it.visibility = View.GONE
            (summaryContainer as? ViewGroup)?.addView(it)
        }
        
        // Initialize the summarize button from the toolbar
        btnSummarize = findViewById(R.id.btn_summarize)
        btnReadMode = findViewById(R.id.btn_read_mode)
        
        // Set background color for summary container and content
        summaryContainer.setBackgroundColor(android.graphics.Color.WHITE)
        summaryContent.setBackgroundColor(android.graphics.Color.WHITE)
    }

    // ======================================
    // BubbleStateManager.StateChangeListener Implementation
    // ======================================
    
    override fun onExpansionStateChanged(isExpanded: Boolean) {
        Log.d(TAG, "Expansion state changed for bubble $bubbleId: $isExpanded")
        // Additional UI updates can be handled here if needed
        // The main expansion/collapse logic is handled in toggleBubbleExpanded()
    }
    
    override fun onActiveStateChanged(isActive: Boolean) {
        Log.d(TAG, "Active state changed for bubble $bubbleId: $isActive")
        // Handle active state UI updates
        if (isActive) {
            expandedContainer.visibility = View.VISIBLE
            resizeHandlesContainer.visibility = View.VISIBLE
        } else {
            expandedContainer.visibility = View.GONE
            resizeHandlesContainer.visibility = View.GONE
        }
    }
    
    override fun onDimensionsChanged(width: Int, height: Int) {
        Log.d(TAG, "Dimensions changed for bubble $bubbleId: ${width}x${height}")
        // Handle dimension changes if needed
        // This is already handled in the updateDimensions method
    }
    
    override fun onZoomChanged(zoomPercent: Float) {
        Log.d(TAG, "Zoom changed for bubble $bubbleId: $zoomPercent%")
        // Apply zoom changes to WebView
        applyDynamicZoom(zoomPercent)
    }
    
    override fun onToolbarVisibilityChanged(isVisible: Boolean) {
        Log.d(TAG, "Toolbar visibility changed for bubble $bubbleId: $isVisible")
        // Handle toolbar visibility changes
        if (isVisible) {
            showToolbar()
        } else {
            hideToolbar()
        }
    }
    
    // ============================================================================
    // Lifecycle Methods
    // ============================================================================
    
    /**
     * Clean up resources when the view is detached from the window
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        
        // Clean up WebViewManager resources
        webViewManager.cleanup()
        
        Log.d(TAG, "BubbleView detached and cleaned up for bubble: $bubbleId")
    }
    
    // ============================================================================
    // BubbleWebViewManagerInterface Implementation
    // ============================================================================
    
    override fun onWebViewUrlChanged(newUrl: String) {
        url = newUrl
        // Update URL bar if bubble is expanded
        if (stateManager.isBubbleExpanded) {
            updateUrlBar()
        }
        // Update read mode manager with new URL
        readModeManager.updateCurrentUrl(newUrl)
    }
    
    override fun onWebViewHtmlContentLoaded(htmlContent: String) {
        // HTML content received, cache it for summarization
        Log.d(TAG, "HTML content received, length: ${htmlContent.length}")
        summaryManager.cacheHtmlContent(htmlContent)
        summaryManager.startBackgroundSummarization(htmlContent)
    }
    
    override fun onWebViewScrollDown() {
        if (stateManager.isToolbarVisible) {
            stateManager.setToolbarVisible(false)
        }
    }
    
    override fun onWebViewScrollUp() {
        if (!stateManager.isToolbarVisible) {
            stateManager.setToolbarVisible(true)
        }
    }
    
    override fun onWebViewFaviconReceived(favicon: Bitmap) {
        // Update local favicon
        updateFavicon(favicon)
        
        Log.d(TAG, "Received favicon for bubble: $bubbleId")
    }
    
    override fun onWebViewTitleReceived(title: String) {
        Log.d(TAG, "Received page title for bubble $bubbleId: $title")
        // Title updates are already handled by the WebViewManager
        // Additional UI updates can be added here if needed
    }
    
    override fun onWebViewProgressChanged(progress: Int) {
        updateProgress(progress)
    }


}
