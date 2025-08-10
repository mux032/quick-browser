/*
 * Quick Browser
 * Copyright (C) 2024  Quick Browser Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.quick.browser.ui.bubble

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.webkit.WebView
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.*
import com.google.android.material.button.MaterialButton
import com.quick.browser.Constants
import com.quick.browser.R
import com.quick.browser.manager.AdBlocker
import com.quick.browser.manager.AuthenticationHandler
import com.quick.browser.manager.SettingsManager
import com.quick.browser.manager.SummarizationManager
import com.quick.browser.service.BubbleService
import com.quick.browser.viewmodel.WebViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.exp
import androidx.core.net.toUri
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * Enhanced floating bubble view that displays web content in a draggable, expandable bubble.
 *
 * This view provides a floating UI element that can be dragged around the screen,
 * expanded to show web content, and collapsed to a small bubble icon. It manages
 * its own WebView instance and handles touch events for dragging and expanding.
 *
 * @property bubbleId Unique identifier for this bubble
 * @property url The URL to load in this bubble's WebView
 * @property settingsManager Settings manager for configuration
 * @property adBlocker Ad blocker for content filtering
 * @property summarizationManager Manager for text summarization
 */
class BubbleView @JvmOverloads constructor(
    context: Context,
    val bubbleId: String,
    var url: String,  // Changed from val to var to allow URL updates when navigating
    private val settingsManager: SettingsManager,
    private val adBlocker: AdBlocker,
    private val summarizationManager: SummarizationManager,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), BubbleTouchHandler.BubbleTouchDelegate,
    BubbleStateManager.Companion.StateChangeListener, BubbleWebViewManagerInterface,
    BubbleUIManager.UIInteractionListener {

    // UI Manager - handles all UI components and interactions
    private var uiManager: BubbleUIManager

    // WebView needs to remain separate as it's managed by WebViewManager
    private lateinit var webViewContainer: WebView

    // Touch handling state - moved to BubbleTouchHandler
    // Resize state - moved to BubbleTouchHandler

    // State Management - centralized in BubbleStateManager
    private val stateManager = BubbleStateManager(bubbleId).apply {
        setStateChangeListener(this@BubbleView)
    }

    // Dependencies passed through constructor
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

    // Settings panel
    private lateinit var settingsPanel: View
    private lateinit var settingsPanelManager: BubbleSettingsPanel

    // Add SwipeRefreshLayout property
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    companion object {
        private const val TAG = "BubbleView"
    }

    /**
     * Initialize the bubble view, set up UI components and event listeners
     */
    init {
        try {
            Log.d(TAG, "Initializing BubbleView for bubble: $bubbleId")

            // Initialize UI Manager first - this inflates the layout
            uiManager = BubbleUIManager(context, this, bubbleId)
            uiManager.initialize(this)

            // Initialize remaining views that are not managed by UIManager
            // This must happen after UIManager initialization since it depends on inflated layout
            initializeViews()

            // Set up WebViewModel for favicon and title management
            post { initializeWebViewModel() }

            // Set up content based on bubble type
            setupContent()

            // Initialize touch handler after all views are set up
            touchHandler.initialize(this)

            Log.d(TAG, "BubbleView initialization completed for bubble: $bubbleId")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing BubbleView for bubble: $bubbleId", e)
            throw e
        }
    }

    /**
     * Initialize remaining view components not handled by UIManager
     *
     * @return Unit
     */
    private fun initializeViews() {
        try {
            Log.d(TAG, "Initializing remaining views for bubble: $bubbleId")

            // Initialize SwipeRefreshLayout and wrap the WebView
            swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout) as? SwipeRefreshLayout
                ?: SwipeRefreshLayout(context).also { srl ->
                    srl.id = R.id.swipe_refresh_layout
                    // Remove webViewContainer from its parent and add to SwipeRefreshLayout
                    val parent = webViewContainer.parent as? ViewGroup
                    parent?.removeView(webViewContainer)
                    srl.addView(webViewContainer)
                    parent?.addView(srl)
                }

            // Set up pull-to-refresh listener
            swipeRefreshLayout.setOnRefreshListener {
                // Refresh the current page
                webViewManager.reload()
            }

            // Optionally, set refresh indicator colors
            swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.colorAccent,
                R.color.secondaryColor
            )

            // WebView container (managed separately due to WebViewManager requirements)
            webViewContainer = findViewById(R.id.web_view) ?: throw IllegalStateException("WebView not found in layout")

            // Ensure summary views and FAB are initialized after layout is ready
            initializeSummaryViews()

            // Initialize settings panel
            settingsPanel =
                findViewById(R.id.settings_panel) ?: throw IllegalStateException("Settings panel not found in layout")

            // Initialize settings panel manager
            settingsPanelManager = BubbleSettingsPanel(context, settingsManager, bubbleAnimator)

            // Initialize summary manager
            summaryManager = BubbleSummaryManager(context, summarizationManager, bubbleAnimator)

            // Initialize read mode manager
            readModeManager = BubbleReadModeManager(context, settingsManager)

            // Initialize WebView manager
            webViewManager = BubbleWebViewManager(context, bubbleId, this, settingsManager, adBlocker)

            Log.d(TAG, "Remaining views initialized successfully for bubble: $bubbleId")

            // Note: All basic UI components are now handled by BubbleUIManager
            // Note: Resize handle setup is now handled by BubbleTouchHandler
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing remaining views for bubble: $bubbleId", e)
            throw e
        }
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
    private fun observeFaviconChanges(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycleScope.launch {
            webViewModel?.webPages?.collectLatest { pages ->
                pages[url]?.let { webPage ->
                    webPage.favicon?.let { favicon ->
                        Log.d(TAG, "Updating bubble icon with favicon for URL: $url")
                        uiManager.updateBubbleIcon(favicon)
                        // Also update URL bar icon if bubble is expanded
                        if (stateManager.isBubbleExpanded) {
                            uiManager.updateUrlBarIcon(favicon)
                        }
                    }
                }
            }
        }
    }

    // Note: Click listeners are now handled by BubbleUIManager

    // Note: URL bar input handling is now managed by BubbleUIManager

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

    // Note: Keyboard and window focus methods are now handled by BubbleUIManager

    // Resize handle setup is now handled by BubbleTouchHandler

    // Resize functionality moved to BubbleTouchHandler

    /**
     * Show resize handles when bubble is expanded
     */
    private fun showResizeHandles() {
        val handles = listOf(
            uiManager.getResizeHandleTopLeft(),
            uiManager.getResizeHandleTopRight(),
            uiManager.getResizeHandleBottomLeft(),
            uiManager.getResizeHandleBottomRight()
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
        val sigmoidValue = 1.0 / (1.0 + exp(-steepness * (widthRatio - midpoint)))

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
            uiManager.getResizeHandleTopLeft(),
            uiManager.getResizeHandleTopRight(),
            uiManager.getResizeHandleBottomLeft(),
            uiManager.getResizeHandleBottomRight()
        )

        bubbleAnimator.animateResizeHandlesHide(handles) {
            uiManager.getResizeHandlesContainer().visibility = GONE
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
        webViewContainer.visibility = VISIBLE

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
            webViewManager.initialize(webViewContainer, uiManager.getProgressBar(), webViewModel)
            
            // Connect WebView progress updates to UI progress bar
            webViewManager.setOnProgressChangedCallback { progress -> onWebViewProgressChanged(progress) }

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
        bubbleAnimator.animateToolbarSlide(uiManager.getToolbarContainer(), false)
    }

    /**
     * Show toolbar with animation
     */
    private fun showToolbar() {
        if (stateManager.isToolbarVisible) return // Already visible

        stateManager.setToolbarVisible(true)
        bubbleAnimator.animateToolbarSlide(uiManager.getToolbarContainer(), true)
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

            override fun onReaderFontSizeChanged(size: Int) {
                // Refresh reader mode content with new font size
                Log.d(TAG, "Reader font size changed to ${size}px for bubble $bubbleId")
                readModeManager.refreshReaderModeContent()
            }

            override fun onReaderBackgroundChanged(background: String) {
                // Refresh reader mode content with new background
                Log.d(TAG, "Reader background changed to $background for bubble $bubbleId")
                readModeManager.refreshReaderModeContent()
            }

            override fun onReaderTextAlignChanged(alignment: String) {
                // Refresh reader mode content with new text alignment
                Log.d(TAG, "Reader text alignment changed to $alignment for bubble $bubbleId")
                readModeManager.refreshReaderModeContent()
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
            findViewById(R.id.btn_read_mode), // btnReadMode is handled by separate managers, not UIManager
            webViewContainer,
            uiManager.getProgressBar(),
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
        uiManager.enableWindowFocus()

        // Reset toolbar state
        stateManager.setToolbarVisible(true)
        uiManager.getToolbarContainer().translationY = 0f

        // Configure container visibility
        webViewContainer.visibility = VISIBLE
        webViewContainer.alpha = 1f

        // Set the dimensions for the expanded container
        resizeExpandedContainer()

        // Start the expand animation with proper sequencing
        bubbleAnimator.animateExpandFromBubble(
            bubbleContainer = uiManager.getBubbleContainer(),
            urlBarContainer = uiManager.getUrlBarContainer(),
            expandedContainer = uiManager.getExpandedContainer(),
            onEnd = {
                // Show resize handles after expansion is complete
                showResizeHandles()
                uiManager.getResizeHandlesContainer().visibility = VISIBLE

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
        uiManager.updateUrlBarText(url)

        // Update the favicon in the URL bar
        webViewModel?.webPages?.value?.get(url)?.favicon?.let { favicon ->
            uiManager.updateUrlBarIcon(favicon)
        } ?: run {
            // Use default globe icon if no favicon available
            uiManager.updateUrlBarIcon(null)
        }
    }

    /**
     * Resize the expanded container to take appropriate screen space.
     * Initial dimensions are defined in dimens.xml and can be customized there.
     */
    private fun resizeExpandedContainer() {
        val layoutParams = uiManager.getExpandedContainer().layoutParams

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

        uiManager.getExpandedContainer().layoutParams = layoutParams

        // Update WebView dimensions to match the container
        val webViewParams = webViewContainer.layoutParams
        webViewParams.width = layoutParams.width
        webViewParams.height = layoutParams.height
        webViewContainer.layoutParams = webViewParams

        // Update content container dimensions to match
        val contentParams = uiManager.getContentContainer().layoutParams
        contentParams.width = layoutParams.width
        contentParams.height = layoutParams.height
        uiManager.getContentContainer().layoutParams = contentParams

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
        uiManager.getExpandedContainer().requestLayout()
        webViewContainer.requestLayout()
        uiManager.getContentContainer().requestLayout()
    }

    /**
     * Make WebView visible when bubble is expanded
     */
    private fun loadContentInExpandedWebView() {
        try {
            Log.d(TAG, "Making WebView visible for bubble $bubbleId with URL: $url")

            // Make WebView fully visible with animation
            webViewContainer.visibility = VISIBLE
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
        uiManager.disableWindowFocus()

        // Hide keyboard if visible
        uiManager.hideKeyboard()

        // Hide settings panel if visible
        settingsPanelManager.dismissIfVisible(settingsPanel)

        // Exit summary mode if active
        summaryManager.forceExitSummaryMode()

        // Exit read mode if active
        readModeManager.forceExitReadMode()

        // Hide resize handles immediately
        hideResizeHandles()

        // Keep WebView loaded but make it invisible immediately to prevent flash
        webViewContainer.visibility = INVISIBLE
        webViewContainer.alpha = 0f

        // Start the collapse animation with proper sequencing
        bubbleAnimator.animateCollapseTobubble(
            expandedContainer = uiManager.getExpandedContainer(),
            urlBarContainer = uiManager.getUrlBarContainer(),
            bubbleContainer = uiManager.getBubbleContainer(),
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
            webViewContainer.visibility = INVISIBLE
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
                urlBarContainer = uiManager.getUrlBarContainer(),
                expandedContainer = uiManager.getExpandedContainer(),
                bubbleContainer = uiManager.getBubbleContainer(),
                onEnd = {
                    stateManager.triggerClose()
                }
            )
        } else {
            // For collapsed bubbles, just animate the bubble icon disappearing
            uiManager.getBubbleContainer().visibility = INVISIBLE
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
                val intent = Intent(Intent.ACTION_VIEW, formattedUrl.toUri()).apply {
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
        uiManager.getExpandedContainer().visibility = GONE
        uiManager.getBubbleContainer().visibility = GONE
        uiManager.getResizeHandlesContainer().visibility = GONE
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
        return uiManager.getExpandedContainer()
    }

    override fun getSettingsPanel(): View {
        return settingsPanel
    }

    override fun getSettingsButton(): MaterialButton {
        return uiManager.getBtnUrlBarSettings()
    }

    override fun getToolbarContainer(): View {
        return uiManager.getToolbarContainer()
    }

    override fun getResizeHandles(): List<ImageView> {
        return listOf(
            uiManager.getResizeHandleTopLeft(),
            uiManager.getResizeHandleTopRight(),
            uiManager.getResizeHandleBottomLeft(),
            uiManager.getResizeHandleBottomRight()
        )
    }

    override fun getContentContainer(): FrameLayout {
        return uiManager.getContentContainer()
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
        return when (sizeString) {
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
                uiManager.updateBubbleIcon(favicon)
                Log.d(TAG, "Bubble icon updated with favicon for bubble: $bubbleId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating bubble icon", e)
            // Fallback to default icon if there's an error
            post {
                uiManager.updateBubbleIcon(null)
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

        uiManager.updateProgress(progress)

        // Hide the refresh indicator when loading is complete
        if (::swipeRefreshLayout.isInitialized) {
            swipeRefreshLayout.isRefreshing = progress in 1..99
        }

        when {
            // Show progress bar when loading (1-99%)
            progress in 1..99 -> {
                uiManager.setProgressVisible(true)
                updateProgressColor(progress)

                // Log progress for debugging at 20% intervals
                if (progress % 20 == 0) {
                    Log.d(TAG, "Loading progress: $progress%")
                }
            }
            // Hide when complete or not started
            else -> uiManager.setProgressVisible(false)
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
        val progressBarDrawable = uiManager.getProgressBar().progressDrawable
        if (progressBarDrawable != null) {
            DrawableCompat.setTint(progressBarDrawable, color)
            DrawableCompat.setTintMode(progressBarDrawable, PorterDuff.Mode.SRC_IN)
        }
    }

    /**
     * Set the bubble as active (expanded and showing content)
     */
    fun setActive() {
        stateManager.setActive(true)
        uiManager.getExpandedContainer().visibility = VISIBLE
        bubbleAnimator.animateExpand(uiManager.getExpandedContainer())

        // Show resize handles when expanded container is visible
        uiManager.getResizeHandlesContainer().visibility = VISIBLE

        // Show the regular web view (not the summary view)
        webViewContainer.visibility = VISIBLE
        loadUrlInWebView()
    }

    /**
     * Set the bubble as inactive (collapsed)
     */
    fun setInactive() {
        stateManager.setActive(false)
        uiManager.getExpandedContainer().visibility = GONE

        // Hide resize handles when expanded container is hidden
        uiManager.getResizeHandlesContainer().visibility = GONE
    }

    /**
     * Show the WebView and load content
     */
    private fun showWebView() {
        webViewContainer.visibility = VISIBLE

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
            it.visibility = GONE
            (uiManager.getExpandedContainer() as? ViewGroup)?.addView(it)
        }
        summaryContent = findViewById(R.id.summary_content) ?: LinearLayout(context).also {
            it.id = R.id.summary_content
            it.orientation = LinearLayout.VERTICAL
            (summaryContainer as? ViewGroup)?.addView(it)
        }
        summaryProgress = findViewById(R.id.summary_progress) ?: ProgressBar(context).also {
            it.id = R.id.summary_progress
            it.visibility = GONE
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
            uiManager.getExpandedContainer().visibility = VISIBLE
            uiManager.getResizeHandlesContainer().visibility = VISIBLE
        } else {
            uiManager.getExpandedContainer().visibility = GONE
            uiManager.getResizeHandlesContainer().visibility = GONE
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
            hideToolbar()
        }
    }

    override fun onWebViewScrollUp() {
        if (!stateManager.isToolbarVisible) {
            showToolbar()
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

    // ================== UIInteractionListener Implementation ==================

    override fun onToggleBubbleExpanded() {
        toggleBubbleExpanded()
        notifyBubbleActivated()
    }

    override fun onCloseBubble() {
        settingsPanelManager.dismissIfVisible(settingsPanel)
        closeBubbleWithAnimation()
    }

    override fun onOpenFullWebView() {
        settingsPanelManager.dismissIfVisible(settingsPanel)
        openFullWebView()
    }

    override fun onToggleReadMode() {
        settingsPanelManager.dismissIfVisible(settingsPanel)
        readModeManager.toggleReadMode()
        // Update settings panel to show/hide reader mode controls
        settingsPanelManager.setReaderMode(readModeManager.isReadMode())
    }

    override fun onToggleSummaryMode() {
        settingsPanelManager.dismissIfVisible(settingsPanel)
        summaryManager.toggleSummaryMode()
    }

    override fun onSettingsButtonClicked() {
        if (settingsPanelManager.isVisible()) {
            settingsPanelManager.hide(settingsPanel)
        } else {
            // Update reader mode state before showing settings panel
            settingsPanelManager.setReaderMode(readModeManager.isReadMode())
            settingsPanelManager.show(settingsPanel, uiManager.getBtnUrlBarSettings())
        }
    }

    override fun onUrlSubmitted(url: String) {
        loadNewUrl(url)
        uiManager.hideKeyboard()
    }

    override fun onUrlBarFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            uiManager.showKeyboard()
        } else {
            uiManager.hideKeyboard()
        }
    }

    override fun onUrlBarClicked() {
        settingsPanelManager.dismissIfVisible(settingsPanel)
    }

    override fun onShareButtonClicked() {
        try {
            // Collapse bubble view before sharing
            setExpanded(false)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, url)
            }
            val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_via))
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing URL", e)
            Toast.makeText(context, context.getString(R.string.share_failed), Toast.LENGTH_SHORT).show()
        }
    }
}
