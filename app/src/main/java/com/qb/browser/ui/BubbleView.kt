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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qb.browser.R
import com.qb.browser.service.BubbleService
import com.qb.browser.util.SettingsManager
import java.lang.Math.min
import kotlin.math.hypot
import kotlin.math.max
import com.qb.browser.model.Bubble
import com.qb.browser.ui.adapter.TabsAdapter
import com.qb.browser.Constants
import android.util.Log

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
    private var isMainBubble = false
    private var isActive = false
    private var isShowingAllBubbles = false
    private var isExpanded = false
    
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val settingsManager = SettingsManager.getInstance(context)
    private val bubbleAnimator = BubbleAnimator(context)

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
        
        // Determine if this is main bubble
        isMainBubble = bubbleId == "main_bubble"
        
        // Initialize tabs adapter for main bubble
        if (isMainBubble) {
            tabsAdapter = TabsAdapter(
                context,
                onTabSelected = { bubbleId ->
                    val intent = Intent(context, BubbleService::class.java).apply {
                        action = Constants.ACTION_CREATE_BUBBLE
                        putExtra(BubbleService.EXTRA_BUBBLE_ID, bubbleId)
                    }
                    context.startService(intent)
                },
                onTabClosed = { bubbleId ->
                    val intent = Intent(context, BubbleService::class.java).apply {
                        action = Constants.ACTION_CLOSE_BUBBLE
                        putExtra(BubbleService.EXTRA_BUBBLE_ID, bubbleId)
                    }
                    context.startService(intent)
                }
            )
        }

        // Set up click listeners
        setOnClickListener {
            val intent = Intent(context, BubbleService::class.java).apply {
                if (isMainBubble) {
                    action = BubbleService.ACTION_TOGGLE_BUBBLES
                } else {
                    action = BubbleService.ACTION_ACTIVATE_BUBBLE
                    putExtra(BubbleService.EXTRA_BUBBLE_ID, bubbleId)
                }
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
        
        findViewById<View>(R.id.btn_new_tab)?.setOnClickListener {
            if (isMainBubble) {
                val intent = Intent(context, BubbleService::class.java).apply {
                    action = Constants.ACTION_CREATE_BUBBLE
                }
                context.startService(intent)
            }
        }
        
        // Set up content based on bubble type
        setupContent()
    }

    private fun setupContent() {
        if (isMainBubble) {
            // Show tabs container for main bubble
            tabsContainer.visibility = View.VISIBLE
            webViewContainer.visibility = View.GONE
            
            // Set up RecyclerView
            val recyclerView = findViewById<RecyclerView>(R.id.tabs_recycler_view)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = tabsAdapter
            
            // Hide browser-specific buttons
            findViewById<View>(R.id.btn_read_mode).visibility = View.GONE
            findViewById<View>(R.id.btn_save_offline).visibility = View.GONE
            findViewById<View>(R.id.btn_open_full).visibility = View.GONE
        } else {
            // Show WebView for regular bubbles
            webViewContainer.visibility = View.VISIBLE
            tabsContainer.visibility = View.GONE
            
            // Set up WebView
            setupWebView()
        }
    }

    private fun setupWebView() {
    try {
        webViewContainer.settings.apply {
            javaScriptEnabled = settingsManager.isJavaScriptEnabled()
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
        }

        webViewContainer.webChromeClient =
                object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        updateProgress(newProgress)
                    }

                    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                        icon?.let { updateFavicon(it) }
                    }
                }

        // Load the URL
        Log.e(TAG, "Loading URL: $url")
        webViewContainer.loadUrl(url)
    } catch (e: Exception) {
        Log.e(TAG, "Error setting up WebView", e)
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
            
            // For non-main bubbles, load the webpage when expanded
            if (!isMainBubble) {
                webViewContainer.loadUrl(url)
            }
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
                    if (isMainBubble) {
                        // Main bubble can only move vertically when near edges
                        val isNearLeftEdge = layoutParams.x < screenWidth / 4
                        val isNearRightEdge = layoutParams.x > (screenWidth * 3 / 4)
                        
                        if (isNearLeftEdge) {
                            layoutParams.x = 0
                        } else if (isNearRightEdge) {
                            layoutParams.x = screenWidth - width
                        } else {
                            layoutParams.x = max(0, min(screenWidth - width, (initialX + dx).toInt()))
                        }
                    } else {
                        // Regular bubbles can move freely
                        layoutParams.x = max(0, min(screenWidth - width, (initialX + dx).toInt()))
                    }
                    layoutParams.y = max(0, min(screenHeight - height, (initialY + dy).toInt()))
                    windowManager.updateViewLayout(this, layoutParams)
                    return true
                }
            }
            
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    performClick()
                } else {
                    if (isMainBubble) {
                        // Force main bubble to snap to either left or right edge
                        layoutParams.x = if (layoutParams.x < screenWidth / 2) 0 else screenWidth - width
                    }
                    windowManager.updateViewLayout(this, layoutParams)
                    saveBubblePosition()
                }
                if (isDragging) {
                    val params = layoutParams
                    if (isMainBubble && !isExpanded) {
                        // Save position for main bubble when minimized
                        val intent = Intent(context, BubbleService::class.java).apply {
                            action = Constants.ACTION_SAVE_POSITION
                            putExtra(BubbleService.EXTRA_X, params.x)
                            putExtra(BubbleService.EXTRA_Y, params.y)
                        }
                        bubbleAnimator.animateBounce(this, true)
                        context.startService(intent)
                    }
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
        if (isMainBubble) {
            tabsAdapter?.submitList(bubbles)
        }
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
        bubbleIcon.setImageBitmap(favicon)
    }

    /**
     * Update the loading progress of the bubble
     */
    fun updateProgress(progress: Int) {
        if (progress in 0..100) {
            progressBar.progress = progress
            progressBar.visibility = if (progress in 1..99) View.VISIBLE else View.GONE
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
        webViewContainer.loadUrl(url)
    }

    fun setExpanded(expanded: Boolean) {
        isExpanded = expanded
    }
}
