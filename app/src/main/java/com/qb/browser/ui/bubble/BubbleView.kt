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
) : FrameLayout(context, attrs, defStyleAttr) {
    
    // UI components
    private lateinit var rootView: View
    private lateinit var bubbleIcon: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var bubbleContainer: View
    private lateinit var urlBarContainer: View
    private lateinit var urlBarIcon: ImageView
    private lateinit var urlBarText: EditText
    private lateinit var btnMinimize: MaterialButton
    private lateinit var expandedContainer: View
    private lateinit var contentContainer: FrameLayout
    private lateinit var webViewContainer: WebView
    
    // Resize handles
    private lateinit var resizeHandlesContainer: FrameLayout
    private lateinit var resizeHandleTopLeft: ImageView
    private lateinit var resizeHandleTopRight: ImageView
    private lateinit var resizeHandleBottomLeft: ImageView
    private lateinit var resizeHandleBottomRight: ImageView

    // Touch handling state
    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    
    // Resize state
    private var isResizing = false
    private var activeResizeHandle: ImageView? = null
    private var initialWidth = 0
    private var initialHeight = 0
    
    // Stored dimensions for the expanded container
    private var storedWidth = 0
    private var storedHeight = 0
    private var hasStoredDimensions = false
    
    // Bubble state
    private var isBubbleExpanded = false
    private var onCloseListener: (() -> Unit)? = null
    private var isActive = false
    private var isShowingAllBubbles = false
    
    // Current zoom level (100% by default)
    private var currentZoomPercent = 100f
    
    // Services and utilities
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val settingsManager = SettingsManager.getInstance(context)
    private val bubbleAnimator = BubbleAnimator(context)
    private var webViewModel: WebViewModel? = null

    // Summary/Summarization UI and State
    private lateinit var btnSummarize: MaterialButton // Changed from fabSummarize to btnSummarize
    private lateinit var summaryContainer: FrameLayout
    private lateinit var summaryContent: LinearLayout
    private lateinit var summaryProgress: ProgressBar
    private var isSummaryMode = false
    private var isSummarizationInProgress = false
    private var cachedHtmlContent: String? = null
    
    // Read Mode UI and State
    private var isReadMode = false
    private lateinit var btnReadMode: MaterialButton
    
    // Toolbar container
    private lateinit var toolbarContainer: View
    private var isToolbarVisible = true
    private var lastScrollY = 0
    
    // Settings panel
    private lateinit var settingsPanel: View
    private var isSettingsPanelVisible = false

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
        btnMinimize = findViewById(R.id.btn_minimize)
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
        
        // Set up resize handle touch listeners
        setupResizeHandles()
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
                        if (isBubbleExpanded) {
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
        
        // Action button listeners
        findViewById<View>(R.id.btn_close).setOnClickListener { closeBubbleWithAnimation() }
        findViewById<View>(R.id.btn_open_full).setOnClickListener { openFullWebView() }
        findViewById<View>(R.id.btn_read_mode).setOnClickListener { toggleReadMode() }
        findViewById<View>(R.id.btn_summarize).setOnClickListener { toggleSummaryMode() }
        findViewById<View>(R.id.btn_settings).setOnClickListener { toggleSettingsPanel() }
        
        // URL bar minimize button listener
        btnMinimize.setOnClickListener { 
            toggleBubbleExpanded()
            notifyBubbleActivated()
        }
        
        // URL bar input handling
        setupUrlBarInput()
        
        // Initialize toolbar container reference
        toolbarContainer = findViewById(R.id.toolbar_container)
        
        // Set up toolbar drag functionality
        setupToolbarDrag()
        
        // Set up settings panel controls
        setupSettingsControls()
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
        
        // Handle click to show keyboard and select all text
        urlBarText.setOnClickListener {
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
    
    /**
     * Set up resize handles with touch listeners
     */
    private fun setupResizeHandles() {
        // Set up touch listeners for each resize handle
        setupResizeHandleTouch(resizeHandleTopLeft)
        setupResizeHandleTouch(resizeHandleTopRight)
        setupResizeHandleTouch(resizeHandleBottomLeft)
        setupResizeHandleTouch(resizeHandleBottomRight)
    }
    
    /**
     * Set up touch listener for a specific resize handle
     */
    private fun setupResizeHandleTouch(handle: ImageView) {
        handle.setOnTouchListener { view, event ->
            if (layoutParams !is WindowManager.LayoutParams) return@setOnTouchListener false
            
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Start resizing
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    initialWidth = expandedContainer.width
                    initialHeight = expandedContainer.height
                    isResizing = true
                    activeResizeHandle = handle
                    return@setOnTouchListener true
                }
                
                MotionEvent.ACTION_MOVE -> {
                    if (isResizing) {
                        // Calculate the change in position
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        
                        // Resize based on which handle is being dragged
                        resizeBubble(handle, dx, dy)
                        return@setOnTouchListener true
                    }
                }
                
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Stop resizing
                    isResizing = false
                    activeResizeHandle = null
                    return@setOnTouchListener true
                }
            }
            
            return@setOnTouchListener false
        }
    }
    
    /**
     * Resize the bubble based on which handle is being dragged
     */
    private fun resizeBubble(handle: ImageView, dx: Float, dy: Float) {
        // Define minimum and maximum dimensions
        val minWidth = resources.displayMetrics.widthPixels / 3  // Minimum 1/3 of screen width
        val minHeight = resources.displayMetrics.heightPixels / 3 // Minimum 1/3 of screen height
        val maxWidth = resources.displayMetrics.widthPixels - 50 // Maximum screen width minus margin
        val maxHeight = resources.displayMetrics.heightPixels - 100 // Maximum screen height minus margin
        
        // Get current window position and dimensions
        val windowParams = this.layoutParams as WindowManager.LayoutParams
        val containerParams = expandedContainer.layoutParams
        
        // Store original values to calculate changes
        val originalX = windowParams.x
        val originalY = windowParams.y
        val originalWidth = containerParams.width
        val originalHeight = containerParams.height
        
        // Variables to track changes
        var newWidth = originalWidth
        var newHeight = originalHeight
        var newX = originalX
        var newY = originalY
        
        when (handle) {
            resizeHandleBottomRight -> {
                // Bottom-right corner: just resize width and height
                newWidth = (initialWidth + dx).toInt().coerceIn(minWidth.toInt(), maxWidth.toInt())
                newHeight = (initialHeight + dy).toInt().coerceIn(minHeight.toInt(), maxHeight.toInt())
            }
            
            resizeHandleBottomLeft -> {
                // Bottom-left corner: resize width inversely and height directly
                val desiredWidth = (initialWidth - dx).toInt().coerceIn(minWidth.toInt(), maxWidth.toInt())
                newHeight = (initialHeight + dy).toInt().coerceIn(minHeight.toInt(), maxHeight.toInt())
                
                // Calculate how much the width will actually change
                val widthChange = originalWidth - desiredWidth
                
                // Only change width if we can also adjust the X position
                if (originalX + widthChange >= 0) {
                    newWidth = desiredWidth
                    newX = originalX + widthChange
                }
            }
            
            resizeHandleTopRight -> {
                // Top-right corner: resize width directly and height inversely
                newWidth = (initialWidth + dx).toInt().coerceIn(minWidth.toInt(), maxWidth.toInt())
                val desiredHeight = (initialHeight - dy).toInt().coerceIn(minHeight.toInt(), maxHeight.toInt())
                
                // Calculate how much the height will actually change
                val heightChange = originalHeight - desiredHeight
                
                // Only change height if we can also adjust the Y position
                if (originalY + heightChange >= 0) {
                    newHeight = desiredHeight
                    newY = originalY + heightChange
                }
            }
            
            resizeHandleTopLeft -> {
                // Top-left corner: resize both width and height inversely
                val desiredWidth = (initialWidth - dx).toInt().coerceIn(minWidth.toInt(), maxWidth.toInt())
                val desiredHeight = (initialHeight - dy).toInt().coerceIn(minHeight.toInt(), maxHeight.toInt())
                
                // Calculate position changes
                val widthChange = originalWidth - desiredWidth
                val heightChange = originalHeight - desiredHeight
                
                // Apply width change if X position can be adjusted
                if (originalX + widthChange >= 0) {
                    newWidth = desiredWidth
                    newX = originalX + widthChange
                }
                
                // Apply height change if Y position can be adjusted
                if (originalY + heightChange >= 0) {
                    newHeight = desiredHeight
                    newY = originalY + heightChange
                }
            }
        }
        
        // Apply the new dimensions to the container
        containerParams.width = newWidth
        containerParams.height = newHeight
        expandedContainer.layoutParams = containerParams
        
        // Update the WebView dimensions to match the container
        val webViewParams = webViewContainer.layoutParams
        webViewParams.width = newWidth
        webViewParams.height = newHeight
        webViewContainer.layoutParams = webViewParams
        
        // Also update content container to match
        val contentParams = contentContainer.layoutParams
        contentParams.width = newWidth
        contentParams.height = newHeight
        contentContainer.layoutParams = contentParams
        
        // Store the new dimensions for future use
        storedWidth = newWidth
        storedHeight = newHeight
        hasStoredDimensions = true
        
        // Update window position if it changed
        if (newX != originalX || newY != originalY) {
            windowParams.x = newX
            windowParams.y = newY
            try {
                windowManager.updateViewLayout(this, windowParams)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating window layout", e)
            }
        }
        
        // Calculate zoom level based on window width relative to screen width
        val screenWidth = resources.displayMetrics.widthPixels
        val widthRatio = newWidth.toFloat() / screenWidth
        val calculatedZoomPercent = calculateSmoothZoomLevel(widthRatio)
        
        // Only update the zoom if it's significantly different from the current zoom
        // This prevents constant small adjustments that might disrupt the user experience
        val zoomPercent = if (Math.abs(calculatedZoomPercent - currentZoomPercent) > 2f) {
            calculatedZoomPercent
        } else {
            currentZoomPercent
        }
        
        // Apply the dynamic zoom level using JavaScript
        applyDynamicZoom(zoomPercent)
        
        // Force layout update
        expandedContainer.requestLayout()
        webViewContainer.requestLayout()
        contentContainer.requestLayout()
    }
    
    /**
     * Show resize handles when bubble is expanded
     */
    private fun showResizeHandles() {
        resizeHandleTopLeft.visibility = View.VISIBLE
        resizeHandleTopRight.visibility = View.VISIBLE
        resizeHandleBottomLeft.visibility = View.VISIBLE
        resizeHandleBottomRight.visibility = View.VISIBLE
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
        currentZoomPercent = zoomPercent
        
        // Convert percentage to decimal (e.g., 75% -> 0.75)
        val zoomFactor = zoomPercent / 100f
        
        // Calculate the inverse width percentage to maintain content width
        // For example, if zoom is 75%, width should be 133.33% (100/0.75)
        val widthPercent = if (zoomFactor > 0) (100f / zoomFactor) else 100f
        
        // Apply zoom via JavaScript
        webViewContainer.evaluateJavascript("""
            (function() {
                // Set viewport to control initial scale
                var meta = document.querySelector('meta[name="viewport"]');
                if (!meta) {
                    meta = document.createElement('meta');
                    meta.name = 'viewport';
                    document.head.appendChild(meta);
                }
                meta.content = 'width=device-width, initial-scale=${zoomFactor}, maximum-scale=1.0, user-scalable=yes';
                
                // Apply multiple zoom techniques for better compatibility
                document.body.style.zoom = "${zoomPercent}%";
                document.body.style.transformOrigin = "0 0";
                document.body.style.transform = "scale(${zoomFactor})";
                document.body.style.width = "${widthPercent}%";
                
                return "Zoom applied: ${zoomPercent}%";
            })()
        """.trimIndent(), null)
        
        Log.d(TAG, "Applied dynamic zoom: $zoomPercent%")
    }
    
    /**
     * Hide resize handles when bubble is collapsed
     */
    private fun hideResizeHandles() {
        resizeHandlesContainer.visibility = View.GONE
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
        
        // Set up WebView
        setupWebView()
        
        // Set up scroll listener for toolbar animation
        setupScrollListener()
    }
    
    /**
     * Set up scroll listener to show/hide toolbar based on scroll direction
     */
    private fun setupScrollListener() {
        // Add JavaScript interface for scroll detection
        webViewContainer.addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun onScrollDown() {
                post {
                    if (isToolbarVisible) {
                        hideToolbar()
                    }
                }
            }
            
            @android.webkit.JavascriptInterface
            fun onScrollUp() {
                post {
                    if (!isToolbarVisible) {
                        showToolbar()
                    }
                }
            }
        }, "ScrollDetector")
    }
    
    /**
     * Hide toolbar with animation
     */
    private fun hideToolbar() {
        if (!isToolbarVisible) return // Already hidden
        
        isToolbarVisible = false
        toolbarContainer.animate()
            .translationY(toolbarContainer.height.toFloat() + 16f) // Add margin to fully hide
            .setDuration(150) // Faster animation for hiding
            .setInterpolator(android.view.animation.AccelerateInterpolator(1.5f)) // More aggressive acceleration
            .withLayer() // Hardware acceleration for smoother animation
            .start()
    }
    
    /**
     * Show toolbar with animation
     */
    private fun showToolbar() {
        if (isToolbarVisible) return // Already visible
        
        isToolbarVisible = true
        toolbarContainer.animate()
            .translationY(0f)
            .setDuration(200) // Slightly slower for showing
            .setInterpolator(android.view.animation.DecelerateInterpolator(1.5f)) // More aggressive deceleration
            .withLayer() // Hardware acceleration for smoother animation
            .start()
    }
    
    /**
     * Toggle settings panel visibility
     */
    private fun toggleSettingsPanel() {
        if (isSettingsPanelVisible) {
            hideSettingsPanel()
        } else {
            showSettingsPanel()
        }
    }
    
    /**
     * Show settings panel with animation
     */
    private fun showSettingsPanel() {
        if (isSettingsPanelVisible) return // Already visible
        
        // Update settings values to current state
        updateSettingsValues()
        
        // Show panel with animation
        isSettingsPanelVisible = true
        settingsPanel.visibility = View.VISIBLE
        settingsPanel.alpha = 0f
        settingsPanel.scaleX = 0.8f
        settingsPanel.scaleY = 0.8f
        
        settingsPanel.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withLayer()
            .start()
    }
    
    /**
     * Hide settings panel with animation
     */
    private fun hideSettingsPanel() {
        if (!isSettingsPanelVisible) return // Already hidden
        
        isSettingsPanelVisible = false
        
        settingsPanel.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(150)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withLayer()
            .withEndAction {
                settingsPanel.visibility = View.GONE
            }
            .start()
    }
    
    /**
     * Set up settings panel controls
     */
    private fun setupSettingsControls() {
        // Set up ad blocking switch
        val adBlockSwitch = findViewById<SwitchMaterial>(R.id.ad_block_switch)
        adBlockSwitch.isChecked = settingsManager.isAdBlockEnabled()
        adBlockSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setAdBlockEnabled(isChecked)
            // Reload page to apply ad blocking changes
            if (webViewContainer.visibility == View.VISIBLE) {
                webViewContainer.reload()
            }
        }
        
        // Set up JavaScript switch
        val javascriptSwitch = findViewById<SwitchMaterial>(R.id.javascript_switch)
        javascriptSwitch.isChecked = settingsManager.isJavaScriptEnabled()
        javascriptSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setJavaScriptEnabled(isChecked)
            webViewContainer.settings.javaScriptEnabled = isChecked
            // Reload page to apply JavaScript changes
            if (webViewContainer.visibility == View.VISIBLE) {
                webViewContainer.reload()
            }
        }
    }
    
    /**
     * Update settings values to reflect current state
     */
    private fun updateSettingsValues() {
        // Update switches
        findViewById<SwitchMaterial>(R.id.ad_block_switch).isChecked = settingsManager.isAdBlockEnabled()
        findViewById<SwitchMaterial>(R.id.javascript_switch).isChecked = settingsManager.isJavaScriptEnabled()
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
            
            // Make WebView ready to load content in the background with alpha=0
            webViewContainer.alpha = 0f
            
            // Load the URL in the background
        post { loadInitialUrl() }

    } catch (e: Exception) {
            Log.e(TAG, "Error setting up WebView for bubble $bubbleId", e)
        }
    }
    

    
    /**
     * Configure WebView settings based on user preferences
     * 
     * @return Unit
     */
    private fun configureWebViewSettings() {
        // Apply basic settings
        configureBasicWebViewSettings()
        
        // Configure security settings
        configureSecuritySettings()
        
        // Configure performance settings
        configurePerformanceSettings()
        
        // Configure content settings
        configureContentSettings()
    }
    
    /**
     * Configure basic WebView settings like zoom and viewport
     * 
     * @return Unit
     */
    private fun configureBasicWebViewSettings() {
        webViewContainer.settings.apply {
            // Viewport settings
            loadWithOverviewMode = true
            useWideViewPort = true
            
            // Zoom settings
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            
            // Default encoding
            defaultTextEncodingName = "UTF-8"
        }
    }
    
    /**
     * Configure security-related WebView settings
     * 
     * @return Unit
     */
    private fun configureSecuritySettings() {
        webViewContainer.settings.apply {
            // JavaScript settings based on user preferences
            javaScriptEnabled = settingsManager.isJavaScriptEnabled()
            javaScriptCanOpenWindowsAutomatically = settingsManager.isJavaScriptEnabled()
            
            // Mixed content - consider using a more restrictive setting
            // MIXED_CONTENT_NEVER_ALLOW is more secure
            mixedContentMode = if (settingsManager.isSecureMode()) {
                android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            } else {
                android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
            
            // File access - restrict based on security settings
            allowContentAccess = !settingsManager.isSecureMode()
            allowFileAccess = !settingsManager.isSecureMode()
        }
    }
    
    /**
     * Configure performance-related WebView settings
     * 
     * @return Unit
     */
    private fun configurePerformanceSettings() {
        // Enable hardware acceleration if device supports it
        try {
            @Suppress("DEPRECATION")
            webViewContainer.settings.setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH)
            webViewContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting hardware acceleration", e)
            webViewContainer.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
    }
    
    /**
     * Configure content-related WebView settings
     * 
     * @return Unit
     */
    private fun configureContentSettings() {
        webViewContainer.settings.apply {
            // Storage settings
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            
            // Content settings
            loadsImagesAutomatically = true
            blockNetworkImage = false
            blockNetworkLoads = false
            
            // Media settings
            mediaPlaybackRequiresUserGesture = false
            
            // Location settings
            setGeolocationEnabled(true)
            
            // Window settings
            setSupportMultipleWindows(true)
        }
    }
    
    /**
     * Set up WebChromeClient and WebViewClient for the WebView
     */
    private fun setupWebViewClients() {
        // Set up WebChromeClient for progress, favicon and title handling
        webViewContainer.webChromeClient = createWebChromeClient()
        
        // Set up WebViewClient for page loading and error handling with summarization support
        // and scroll detection for toolbar animation
        webViewContainer.webViewClient = ScrollAwareWebViewClient(
            context,
            { newUrl ->
                url = newUrl
                // Update URL bar if bubble is expanded
                if (isBubbleExpanded) {
                    updateUrlBar()
                }
            },
            { htmlContent ->
                // HTML content received, but we're not using it for summarization anymore
                Log.d(TAG, "HTML content received, length: ${htmlContent.length}")
                cachedHtmlContent = htmlContent
            },
            // Scroll down callback
            {
                if (isToolbarVisible) {
                    hideToolbar()
                }
            },
            // Scroll up callback
            {
                if (!isToolbarVisible) {
                    showToolbar()
                }
            }
        )
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
     * 
     * @param favicon The bitmap favicon received from the WebView
     * @return Unit
     */
    private fun handleReceivedFavicon(favicon: Bitmap) {
        // Update local favicon
        updateFavicon(favicon)
        
        // Update in WebViewModel to persist the favicon
        updateWebViewModel { viewModel ->
            viewModel.updateFavicon(url, favicon)
            Log.d(TAG, "Updated favicon in WebViewModel for URL: $url")
        }
        
        Log.d(TAG, "Received favicon for bubble: $bubbleId")
    }
    
    /**
     * Handle a received page title from the WebView
     * 
     * @param title The title received from the WebView
     * @return Unit
     */
    private fun handleReceivedTitle(title: String) {
        Log.d(TAG, "Received page title for bubble $bubbleId: $title")
        
        // Update title in WebViewModel
        updateWebViewModel { viewModel ->
            viewModel.updateTitle(url, title)
        }
        
        // Update title in history database if we have access to the application
        try {
            // Log that we've updated the title
            Log.d(TAG, "Updated page title in WebViewModel: $title")
            
            // Also update in the application's WebViewModel if available
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
                if (url == null) return false
                
                Log.d(TAG, "Loading URL in WebView: $url")
                
                // Check if this is an authentication URL that should be handled with Custom Tabs
                if (AuthenticationHandler.isAuthenticationUrl(url)) {
                    Log.d(TAG, "Authentication URL detected in BubbleView, opening in Custom Tab: $url")
                    AuthenticationHandler.openInCustomTab(context, url, bubbleId)
                    return true
                }
                
                // Handle special URLs that should be opened by external apps
                if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("sms:") || 
                    url.startsWith("intent:") || url.startsWith("market:")) {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                        return true
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling special URL: $url", e)
                    }
                }
                
                // Handle normal HTTP/HTTPS URLs
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    // Update the current URL
                    this@BubbleView.url = url
                    
                    // Update URL bar if bubble is expanded
                    if (isBubbleExpanded) {
                        updateUrlBar()
                    }
                    
                    // We don't need to call loadUrl here, as returning false will let the WebView handle it
                    // This ensures proper handling of all navigation states, history, etc.
                    
                    // Return false to let WebView handle the URL loading
                    return false
                }
                
                // Return false to let WebView handle other URLs
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
        // Hide bubble container and show URL bar
        bubbleContainer.visibility = View.GONE
        urlBarContainer.visibility = View.VISIBLE
        
        // Update URL bar with current URL
        updateUrlBar()
        
        // Enable input focus for keyboard
        enableWindowFocus()
        
        // Show expanded container with animation
        expandedContainer.visibility = View.VISIBLE
        bubbleAnimator.animateExpand(expandedContainer)
        
        // Show resize handles when expanded container is visible
        resizeHandlesContainer.visibility = View.VISIBLE
        
        // Reset toolbar state
        isToolbarVisible = true
        toolbarContainer.translationY = 0f
        
        // Configure container visibility
        webViewContainer.visibility = View.VISIBLE
        webViewContainer.alpha = 1f
        
        // Set the dimensions for the expanded container
        resizeExpandedContainer()
        
        // Show resize handles
        showResizeHandles()
        
        // Make WebView visible and ensure content is loaded
        loadContentInExpandedWebView()
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
     * Resize the expanded container to take appropriate screen space
     */
    private fun resizeExpandedContainer() {
        val layoutParams = expandedContainer.layoutParams
        
        if (hasStoredDimensions && storedWidth > 0 && storedHeight > 0) {
            // Use stored dimensions if available
            layoutParams.width = storedWidth
            layoutParams.height = storedHeight
        } else {
            // Use default dimensions
            layoutParams.width = resources.displayMetrics.widthPixels * 9 / 10  // 90% of screen width
            layoutParams.height = resources.displayMetrics.heightPixels * 7 / 10 // 70% of screen height
            
            // Store these default dimensions
            storedWidth = layoutParams.width
            storedHeight = layoutParams.height
            hasStoredDimensions = true
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
        val zoomPercent = if (currentZoomPercent != 100f) {
            // Use the previously stored zoom level
            currentZoomPercent
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
                    
                    // Apply the stored zoom level after a short delay to ensure the page has loaded
                    postDelayed({
                        applyDynamicZoom(currentZoomPercent)
                    }, 500)
                }
            } else {
                // If the page is already loaded, make sure it's visible
                // Also reapply the stored zoom level
                applyDynamicZoom(currentZoomPercent)
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
        // Show bubble container and hide URL bar
        bubbleContainer.visibility = View.VISIBLE
        urlBarContainer.visibility = View.GONE
        
        // Disable input focus to prevent accidental keyboard
        disableWindowFocus()
        
        // Hide keyboard if visible
        hideKeyboard()
        
        // Hide expanded container with animation
        bubbleAnimator.animateCollapse(expandedContainer)
        
        // Hide settings panel if visible
        if (isSettingsPanelVisible) {
            hideSettingsPanel()
        }
        
        // Hide resize handles
        hideResizeHandles()
        
        // Slight shrink animation on collapse
        bubbleAnimator.animateBounce(bubbleContainer, false)
        
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
        // First hide the bubble icon to prevent flicker
        val bubbleContainer = findViewById<View>(R.id.bubble_container)
        bubbleContainer.visibility = View.INVISIBLE
        
        // Then animate the entire view disappearing
        bubbleAnimator.animateDisappear(this, onEnd = {
            onCloseListener?.invoke()
        })
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
     * Open the web page in read mode directly in the bubble's WebView
     */
    // Store original content URL
    private var originalContent: String? = null
    
    private fun toggleReadMode() {
        isReadMode = !isReadMode
        if (isReadMode) {
            // Save current URL before switching to reader mode
            originalContent = webViewContainer.url
            openReadMode()
        } else {
            // Restore WebView settings for normal mode
            webViewContainer.settings.apply {
                javaScriptEnabled = true
                builtInZoomControls = true
                displayZoomControls = false
                textZoom = 100
            }
            
            // Return to normal web view using cached content if available
            originalContent?.let { savedUrl ->
                webViewContainer.loadUrl(savedUrl)
            } ?: webViewContainer.loadUrl(url) // Fallback to current URL if cache is empty
            
            // Announce mode change for accessibility
            webViewContainer.announceForAccessibility(context.getString(R.string.web_view_mode))
        }
        updateReadModeButton()
    }

    private fun updateReadModeButton() {
        btnReadMode = findViewById(R.id.btn_read_mode)
        btnReadMode.setIconResource(if (isReadMode) R.drawable.ic_globe else R.drawable.ic_read_mode)
    }

    private fun openReadMode() {
        try {
            progressBar.visibility = View.VISIBLE
            progressBar.isIndeterminate = true
            val contentExtractor = ReadabilityExtractor(context)
            val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
            coroutineScope.launch {
                try {
                    val readableContent = withContext(kotlinx.coroutines.Dispatchers.IO) {
                        contentExtractor.extractFromUrl(url)
                    }
                    val isNightMode = settingsManager.isDarkThemeEnabled()
                    if (readableContent == null) {
                        handleReadModeError()
                        return@launch
                    }
                    val styledHtml = createStyledHtml(readableContent, isNightMode)
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (!isBubbleExpanded) {
                            toggleBubbleExpanded()
                        }
                        // Cache the original content
                        originalContent?.let { webViewContainer.loadUrl(it) }
                        
                        
                        // Load the reader mode content
                        webViewContainer.settings.apply {
                            // Disable JavaScript for reader mode
                            javaScriptEnabled = false
                            // Enable built-in zoom
                            builtInZoomControls = true
                            displayZoomControls = false
                            // Enable text size adjustment
                            textZoom = 100
                        }
                        
                        
                        webViewContainer.loadDataWithBaseURL(url, styledHtml, "text/html", "UTF-8", null)
                        progressBar.visibility = View.GONE
                        progressBar.isIndeterminate = false
                        webViewContainer.alpha = 1f
                        
                        
                        // Announce reader mode for accessibility
                        webViewContainer.announceForAccessibility(context.getString(R.string.reader_mode_loaded))
                        android.widget.Toast.makeText(context, R.string.reader_mode_loaded, android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error extracting content for read mode", e)
                    handleReadModeError()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open read mode", e)
            handleReadModeError()
        }
    }
    
    private fun handleReadModeError() {
        progressBar.visibility = View.GONE
        progressBar.isIndeterminate = false
        android.widget.Toast.makeText(context, R.string.failed_to_load_reader_mode, android.widget.Toast.LENGTH_SHORT).show()
        // Reset read mode state on error
        isReadMode = false
        updateReadModeButton()
        // Restore original content if available
        originalContent?.let { webViewContainer.loadUrl(it) }
    }
    
    /**
     * Create styled HTML for reader mode
     */
    private fun createStyledHtml(content: ReadabilityExtractor.ReadableContent, isNightMode: Boolean): String {
        val backgroundColor = if (isNightMode) "#121212" else "#FAFAFA"
        val textColor = if (isNightMode) "#E0E0E0" else "#212121"
        val linkColor = if (isNightMode) "#90CAF9" else "#1976D2"
        val secondaryTextColor = if (isNightMode) "#B0B0B0" else "#666666"
        
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    :root {
                        --content-width: 100%;
                        --body-padding: clamp(16px, 5%, 32px);
                        --content-max-width: 800px;
                    }
                    
                    body {
                        font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;
                        line-height: 1.8;
                        color: $textColor;
                        background-color: $backgroundColor;
                        padding: var(--body-padding);
                        margin: 0 auto;
                        max-width: var(--content-max-width);
                        text-rendering: optimizeLegibility;
                        -webkit-font-smoothing: antialiased;
                    }
                    
                    article {
                        width: var(--content-width);
                        margin: 0 auto;
                    }
                    
                    h1, h2, h3, h4, h5, h6 {
                        line-height: 1.3;
                        margin: 1.5em 0 0.5em;
                        font-weight: 600;
                    }
                    
                    h1 {
                        font-size: clamp(1.5em, 5vw, 2em);
                        letter-spacing: -0.02em;
                    }
                    
                    p {
                        margin: 1.5em 0;
                        font-size: clamp(1em, 2vw, 1.2em);
                    }
                    
                    a {
                        color: ${linkColor};
                        text-decoration: none;
                        border-bottom: 1px solid ${linkColor}40;
                        transition: border-bottom-color 0.2s;
                    }
                    
                    a:hover {
                        border-bottom-color: ${linkColor};
                    }
                    
                    img {
                        max-width: 100%;
                        height: auto;
                        margin: 1.5em 0;
                        border-radius: 4px;
                    }
                    
                    blockquote {
                        margin: 2em 0;
                        padding: 1em 2em;
                        border-left: 4px solid ${linkColor}40;
                        background-color: ${linkColor}10;
                        font-style: italic;
                        color: ${secondaryTextColor};
                    }
                    
                    code {
                        font-family: 'SF Mono', Consolas, Monaco, 'Andale Mono', monospace;
                        background-color: ${textColor}10;
                        padding: 0.2em 0.4em;
                        border-radius: 3px;
                        font-size: 0.9em;
                    }
                    
                    pre {
                        background-color: ${textColor}10;
                        padding: 1em;
                        border-radius: 4px;
                        overflow-x: auto;
                        font-size: 0.9em;
                    }
                    
                    ul, ol {
                        padding-left: 1.5em;
                        margin: 1.5em 0;
                    }
                    
                    li {
                        margin: 0.5em 0;
                    }
                    
                    hr {
                        border: none;
                        border-top: 1px solid ${textColor}20;
                        margin: 2em 0;
                    }
                    
                    .meta {
                        color: ${secondaryTextColor};
                        font-size: 0.9em;
                        margin-bottom: 2em;
                    }
                    
                    @media (prefers-reduced-motion: reduce) {
                        * {
                            animation-duration: 0.01ms !important;
                            animation-iteration-count: 1 !important;
                            transition-duration: 0.01ms !important;
                            scroll-behavior: auto !important;
                        }
                    }
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
                ${if (!content.byline.isNullOrEmpty()) "<div class=\"byline\">${content.byline}</div>" else ""}
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
     * Handle touch events for dragging with snap to edges
     * 
     * This method handles:
     * - Detecting the start of a drag operation
     * - Moving the bubble during drag
     * - Collapsing the bubble if expanded when dragging starts
     * - Saving the bubble position when dragging ends
     * - Hiding settings panel when clicking outside of it
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (layoutParams !is WindowManager.LayoutParams) return super.onTouchEvent(event)
        
        // If we're currently resizing, let the resize handle touch listener handle it
        if (isResizing) {
            return true
        }
        
        // Hide settings panel if visible and user clicks outside of it
        if (isSettingsPanelVisible && event.action == MotionEvent.ACTION_DOWN) {
            // Check if the touch is outside the settings panel
            val settingsPanelRect = Rect()
            settingsPanel.getGlobalVisibleRect(settingsPanelRect)
            if (!settingsPanelRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                hideSettingsPanel()
            }
        }
        
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
            // Only collapse if expanded when starting to drag from the bubble itself
            // (not from the toolbar, which has its own drag handler)
            if (isBubbleExpanded && event.y < expandedContainer.top) {
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
        }
    }
    
    /**
     * Set up toolbar drag functionality to allow dragging the expanded bubble
     * when the toolbar is touched and dragged
     */
    private fun setupToolbarDrag() {
        toolbarContainer.setOnTouchListener { _, event ->
            if (layoutParams !is WindowManager.LayoutParams || !isBubbleExpanded) return@setOnTouchListener false
            
            val params = layoutParams as WindowManager.LayoutParams
            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels
            
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x.toFloat()
                    initialY = params.y.toFloat()
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    
                    // Check if we've moved enough to consider it a drag
                    if (!isDragging && hypot(dx, dy) > touchSlop) {
                        isDragging = true
                    }
                    
                    if (isDragging) {
                        // Keep bubble within screen bounds
                        params.x = max(0, min(screenWidth - width, (initialX + dx).toInt()))
                        params.y = max(0, min(screenHeight - height, (initialY + dy).toInt()))
                        windowManager.updateViewLayout(this, params)
                    }
                    true
                }
                
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        windowManager.updateViewLayout(this, params)
                    }
                    isDragging = false
                    true
                }
                
                else -> false
            }
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
        isActive = true
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
        isActive = false
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
        isBubbleExpanded = expanded
        
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

    /**
     * Toggle between web view and summary view
     */
    private fun toggleSummaryMode() {
        if (isSummaryMode) {
            showWebViewOnly()
        } else {
            showSummaryView()
        }
    }

    /**
     * Show only the web view, hide summary
     */
    private fun showWebViewOnly() {
        isSummaryMode = false
        webViewContainer.visibility = View.VISIBLE
        summaryContainer.visibility = View.GONE
        btnSummarize.setIconResource(R.drawable.ic_summarize)
        btnSummarize.setIconTint(ContextCompat.getColorStateList(context, R.color.colorPrimary))
        btnSummarize.contentDescription = context.getString(R.string.summarize)
        Toast.makeText(context, R.string.showing_web_view, Toast.LENGTH_SHORT).show()
    }

    /**
     * Show the summary view and hide the web view
     */
    private fun showSummaryView() {
        if (webViewContainer.visibility != View.VISIBLE) {
            Toast.makeText(context, R.string.summary_error, Toast.LENGTH_SHORT).show()
            return
        }
        isSummaryMode = true
        webViewContainer.visibility = View.GONE
        summaryContainer.visibility = View.VISIBLE
        summaryProgress.visibility = View.VISIBLE
        summaryContent.removeAllViews()
        btnSummarize.setIconResource(R.drawable.ic_web_page)
        btnSummarize.setIconTint(ContextCompat.getColorStateList(context, R.color.colorPrimary))
        btnSummarize.contentDescription = context.getString(R.string.show_web_view)
        Toast.makeText(context, R.string.summarizing, Toast.LENGTH_SHORT).show()
        summarizeContent()
    }

    /**
     * Summarize the current page content
     */
    private fun summarizeContent() {
        try {
            if (cachedHtmlContent != null && cachedHtmlContent!!.length > 100) {
                processSummarization(cachedHtmlContent!!)
            } else {
                webViewContainer.evaluateJavascript("(function() { return document.documentElement.outerHTML; })()") { html ->
                    try {
                        if (html != null && html.length > 50) {
                            val unescapedHtml = html.substring(1, html.length - 1)
                                .replace("\\\"", "\"")
                                .replace("\\n", "\n")
                                .replace("\\\\", "\\")
                            cachedHtmlContent = unescapedHtml
                            processSummarization(unescapedHtml)
                        } else {
                            showSummaryError(context.getString(R.string.summary_error))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing HTML for summary", e)
                        showSummaryError(context.getString(R.string.summary_error))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in summarizeContent", e)
            showSummaryError(context.getString(R.string.summary_error))
        }
    }

    /**
     * Process the HTML content for summarization
     */
    private fun processSummarization(htmlContent: String) {
        isSummarizationInProgress = true
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val cleanedHtml = withContext(Dispatchers.IO) {
                    try {
                        val doc = org.jsoup.Jsoup.parse(htmlContent)
                        doc.select("script, style, noscript, iframe, object, embed, header, footer, nav, aside").remove()
                        doc.text()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error cleaning HTML", e)
                        null
                    }
                }
                if (cleanedHtml == null || cleanedHtml.length < 100) {
                    showSummaryError(context.getString(R.string.summary_not_article))
                    return@launch
                }
                val summaryPoints = withContext(Dispatchers.Default) {
                    val summarizationManager = SummarizationManager.getInstance(context)
                    summarizationManager.summarizeContent(cleanedHtml)
                }
                if (summaryPoints.isNotEmpty()) {
                    displaySummaryPoints(summaryPoints)
                } else {
                    showSummaryError(context.getString(R.string.summary_not_article))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing summarization", e)
                showSummaryError(context.getString(R.string.summary_error))
            } finally {
                isSummarizationInProgress = false
            }
        }
    }

    /**
     * Display the summary points in the UI
     */
    private fun displaySummaryPoints(points: List<String>) {
        summaryProgress.visibility = View.GONE
        for (point in points) {
            val bulletPoint = TextView(context)
            bulletPoint.text = "• $point"
            bulletPoint.setPadding(16, 16, 16, 16)
            bulletPoint.textSize = 16f
            summaryContent.addView(bulletPoint)
        }
    }

    /**
     * Show an error in the summary view
     */
    private fun showSummaryError(message: String) {
        summaryProgress.visibility = View.GONE
        val errorText = TextView(context)
        errorText.text = message
        errorText.setPadding(16, 16, 16, 16)
        errorText.textSize = 16f
        summaryContent.addView(errorText)
    }

    /**
     * Start background summarization of the HTML content
     */
    private fun startBackgroundSummarization(htmlContent: String) {
        if (isSummarizationInProgress || htmlContent.length < 100) return
        isSummarizationInProgress = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val summarizationManager = SummarizationManager.getInstance(context)
                val doc = org.jsoup.Jsoup.parse(htmlContent)
                doc.select("script, style, noscript, iframe, object, embed, header, footer, nav, aside").remove()
                val cleanedText = doc.text()
                if (cleanedText.length > 100) {
                    summarizationManager.summarizeContent(cleanedText)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Background summarization failed", e)
            } finally {
                isSummarizationInProgress = false
            }
        }
    }
}
