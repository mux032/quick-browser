package com.quick.browser.ui.bubble

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import com.google.android.material.button.MaterialButton
import com.quick.browser.R

/**
 * BubbleUIManager handles all UI components and interactions for a single BubbleView.
 * 
 * This class is responsible for:
 * - View initialization and references
 * - Click listeners setup
 * - Progress bar and icon management
 * - Keyboard/input handling
 * - Toolbar and UI element visibility
 * - URL bar management
 * 
 * This separates UI concerns from the main BubbleView class, making the code more maintainable
 * and testable while following the Single Responsibility Principle.
 */
class BubbleUIManager(
    private val context: Context,
    private val bubbleView: BubbleView,
    private val bubbleId: String
) {
    
    companion object {
        private const val TAG = "BubbleUIManager"
    }
    
    // Core UI components
    private lateinit var rootView: View
    private lateinit var bubbleIcon: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var bubbleContainer: View
    private lateinit var urlBarContainer: View
    private lateinit var urlBarIcon: ImageView
    private lateinit var urlBarText: EditText
    private lateinit var btnUrlBarShare: MaterialButton
    private lateinit var btnUrlBarSettings: MaterialButton
    private lateinit var expandedContainer: View
    private lateinit var contentContainer: FrameLayout
    private lateinit var toolbarContainer: View
    
    // Resize handles
    private lateinit var resizeHandlesContainer: FrameLayout
    private lateinit var resizeHandleTopLeft: ImageView
    private lateinit var resizeHandleTopRight: ImageView
    private lateinit var resizeHandleBottomLeft: ImageView
    private lateinit var resizeHandleBottomRight: ImageView
    
    // Action buttons
    private lateinit var btnClose: MaterialButton
    private lateinit var btnOpenFull: MaterialButton
    private lateinit var btnReadMode: MaterialButton
    private lateinit var btnSummarize: MaterialButton
    private lateinit var btnSaveArticle: MaterialButton
    
    // Utility
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    // Callbacks for UI interactions
    interface UIInteractionListener {
        fun onToggleBubbleExpanded()
        fun onCloseBubble()
        fun onOpenFullWebView()
        fun onToggleReadMode()
        fun onToggleSummaryMode()
        fun onSaveArticle()
        fun onSettingsButtonClicked()
        fun onShareButtonClicked()
        fun onUrlSubmitted(url: String)
        fun onUrlBarFocusChanged(hasFocus: Boolean)
        fun onUrlBarClicked()
    }
    
    private var uiListener: UIInteractionListener? = null
    
    /**
     * Initialize all UI components and set up the bubble layout
     */
    fun initialize(listener: UIInteractionListener) {
        this.uiListener = listener
        initializeViews()
        setupClickListeners()
        setupUrlBarInput()
        setupDefaultStates()
    }
    
    /**
     * Initialize all view components from the layout
     */
    private fun initializeViews() {
        Log.d(TAG, "Initializing UI components for bubble: $bubbleId")
        
        try {
            // Use application context with theme for inflation (same as original BubbleView)
            val themedContext = ContextThemeWrapper(context.applicationContext, R.style.Theme_QBrowser)
            
            // Inflate the bubble layout
            rootView = LayoutInflater.from(themedContext).inflate(R.layout.bubble_layout, bubbleView, true)
            Log.d(TAG, "Layout inflated successfully for bubble: $bubbleId")
            
            // Initialize core UI components
            bubbleIcon = bubbleView.findViewById(R.id.bubble_icon) ?: throw IllegalStateException("bubble_icon not found")
            progressBar = bubbleView.findViewById(R.id.progress_circular) ?: throw IllegalStateException("progress_circular not found")
            bubbleContainer = bubbleView.findViewById(R.id.bubble_container) ?: throw IllegalStateException("bubble_container not found")
            urlBarContainer = bubbleView.findViewById(R.id.url_bar_container) ?: throw IllegalStateException("url_bar_container not found")
            urlBarIcon = bubbleView.findViewById(R.id.url_bar_icon) ?: throw IllegalStateException("url_bar_icon not found")
            urlBarText = bubbleView.findViewById(R.id.url_bar_text) ?: throw IllegalStateException("url_bar_text not found")
            btnUrlBarShare = bubbleView.findViewById(R.id.btn_url_bar_share) ?: throw IllegalStateException("btn_url_bar_share not found")
            btnUrlBarSettings = bubbleView.findViewById(R.id.btn_url_bar_settings) ?: throw IllegalStateException("btn_url_bar_settings not found")
            expandedContainer = bubbleView.findViewById(R.id.expanded_container) ?: throw IllegalStateException("expanded_container not found")
            contentContainer = bubbleView.findViewById(R.id.content_container) ?: throw IllegalStateException("content_container not found")
            toolbarContainer = bubbleView.findViewById(R.id.toolbar_container) ?: throw IllegalStateException("toolbar_container not found")
            
            // Initialize resize handles
            resizeHandlesContainer = bubbleView.findViewById(R.id.resize_handles_container) ?: throw IllegalStateException("resize_handles_container not found")
            resizeHandleTopLeft = bubbleView.findViewById(R.id.resize_handle_top_left) ?: throw IllegalStateException("resize_handle_top_left not found")
            resizeHandleTopRight = bubbleView.findViewById(R.id.resize_handle_top_right) ?: throw IllegalStateException("resize_handle_top_right not found")
            resizeHandleBottomLeft = bubbleView.findViewById(R.id.resize_handle_bottom_left) ?: throw IllegalStateException("resize_handle_bottom_left not found")
            resizeHandleBottomRight = bubbleView.findViewById(R.id.resize_handle_bottom_right) ?: throw IllegalStateException("resize_handle_bottom_right not found")
            
            // Initialize action buttons
            btnClose = bubbleView.findViewById(R.id.btn_close) ?: throw IllegalStateException("btn_close not found")
            btnOpenFull = bubbleView.findViewById(R.id.btn_open_full) ?: throw IllegalStateException("btn_open_full not found")
            btnReadMode = bubbleView.findViewById(R.id.btn_read_mode) ?: throw IllegalStateException("btn_read_mode not found")
            btnSummarize = bubbleView.findViewById(R.id.btn_summarize) ?: throw IllegalStateException("btn_summarize not found")
            btnSaveArticle = bubbleView.findViewById(R.id.btn_save_article) ?: throw IllegalStateException("btn_save_article not found")
            
            Log.d(TAG, "UI components initialized successfully for bubble: $bubbleId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing UI components for bubble: $bubbleId", e)
            throw e
        }
    }
    
    /**
     * Set up click listeners for all interactive elements
     */
    private fun setupClickListeners() {
        Log.d(TAG, "Setting up click listeners for bubble: $bubbleId")
        
        // Main bubble click listener
        bubbleView.setOnClickListener {
            uiListener?.onToggleBubbleExpanded()
        }
        
        // Action button listeners
        btnClose.setOnClickListener { 
            uiListener?.onCloseBubble()
        }
        
        btnOpenFull.setOnClickListener { 
            uiListener?.onOpenFullWebView()
        }
        
        btnReadMode.setOnClickListener { 
            uiListener?.onToggleReadMode()
        }
        
        btnSummarize.setOnClickListener { 
            uiListener?.onToggleSummaryMode()
        }
        
        btnSaveArticle.setOnClickListener {
            uiListener?.onSaveArticle()
        }

        btnUrlBarShare.setOnClickListener { 
            uiListener?.onShareButtonClicked()
        }
        
        // URL bar settings button listener
        btnUrlBarSettings.setOnClickListener { 
            uiListener?.onSettingsButtonClicked()
        }
        
        Log.d(TAG, "Click listeners set up successfully for bubble: $bubbleId")
    }
    
    /**
     * Set up URL bar input handling
     */
    private fun setupUrlBarInput() {
        Log.d(TAG, "Setting up URL bar input for bubble: $bubbleId")
        
        // Handle URL input submission
        urlBarText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                val inputUrl = urlBarText.text.toString().trim()
                if (inputUrl.isNotEmpty()) {
                    uiListener?.onUrlSubmitted(inputUrl)
                    hideKeyboard()
                }
                true
            } else {
                false
            }
        }
        
        // Handle focus changes
        urlBarText.setOnFocusChangeListener { _, hasFocus ->
            uiListener?.onUrlBarFocusChanged(hasFocus)
        }
        
        // Handle click to show keyboard and select all text
        urlBarText.setOnClickListener {
            uiListener?.onUrlBarClicked()
            urlBarText.requestFocus()
            urlBarText.selectAll()
            showKeyboard()
        }
        
        Log.d(TAG, "URL bar input setup complete for bubble: $bubbleId")
    }
    
    /**
     * Set up default states for UI components
     */
    private fun setupDefaultStates() {
        // Set default favicon
        bubbleIcon.setImageResource(R.drawable.ic_globe)
        
        // Set default progress
        progressBar.progress = 0
        
        // Hide expanded container initially
        expandedContainer.visibility = View.GONE
        
        // Hide resize handles initially
        resizeHandlesContainer.visibility = View.GONE
        
        Log.d(TAG, "Default UI states set for bubble: $bubbleId")
    }
    
    // ================== PUBLIC INTERFACE METHODS ==================
    
    /**
     * Update the bubble icon with a new bitmap
     */
    fun updateBubbleIcon(bitmap: Bitmap?) {
        if (bitmap != null) {
            bubbleIcon.setImageBitmap(bitmap)
            Log.d(TAG, "Bubble icon updated for bubble: $bubbleId")
        } else {
            bubbleIcon.setImageResource(R.drawable.ic_globe)
            Log.d(TAG, "Bubble icon reset to default for bubble: $bubbleId")
        }
    }
    
    /**
     * Update the URL bar icon
     */
    fun updateUrlBarIcon(bitmap: Bitmap?) {
        if (bitmap != null) {
            urlBarIcon.setImageBitmap(bitmap)
        } else {
            urlBarIcon.setImageResource(R.drawable.ic_globe)
        }
    }
    
    /**
     * Update progress bar value
     */
    fun updateProgress(progress: Int) {
        progressBar.progress = progress.coerceIn(0, 100)
    }
    
    /**
     * Show or hide progress bar
     */
    fun setProgressVisible(visible: Boolean) {
        progressBar.visibility = if (visible) View.VISIBLE else View.GONE
    }
    
    /**
     * Update URL bar text
     */
    fun updateUrlBarText(url: String) {
        if (urlBarText.text.toString() != url) {
            urlBarText.setText(url)
        }
    }
    
    /**
     * Show expanded container with animation
     */
    fun showExpandedContainer() {
        expandedContainer.visibility = View.VISIBLE
        urlBarContainer.visibility = View.VISIBLE
    }
    
    /**
     * Hide expanded container
     */
    fun hideExpandedContainer() {
        expandedContainer.visibility = View.GONE
        urlBarContainer.visibility = View.GONE
        hideKeyboard()
    }
    
    /**
     * Show resize handles
     */
    fun showResizeHandles() {
        resizeHandlesContainer.visibility = View.VISIBLE
        val handles = listOf(
            resizeHandleTopLeft,
            resizeHandleTopRight,
            resizeHandleBottomLeft,
            resizeHandleBottomRight
        )
        // Animation would be handled by animator
        handles.forEach { it.visibility = View.VISIBLE }
    }
    
    /**
     * Hide resize handles
     */
    fun hideResizeHandles() {
        val handles = listOf(
            resizeHandleTopLeft,
            resizeHandleTopRight,
            resizeHandleBottomLeft,
            resizeHandleBottomRight
        )
        handles.forEach { it.visibility = View.GONE }
        resizeHandlesContainer.visibility = View.GONE
    }
    
    /**
     * Show toolbar
     */
    fun showToolbar() {
        toolbarContainer.visibility = View.VISIBLE
    }
    
    /**
     * Hide toolbar
     */
    fun hideToolbar() {
        toolbarContainer.visibility = View.GONE
    }
    
    /**
     * Enable window focus to allow keyboard input
     */
    fun enableWindowFocus() {
        try {
            val params = bubbleView.layoutParams as? WindowManager.LayoutParams ?: return
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            windowManager.updateViewLayout(bubbleView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling window focus for bubble: $bubbleId", e)
        }
    }
    
    /**
     * Disable window focus to prevent accidental keyboard
     */
    fun disableWindowFocus() {
        try {
            val params = bubbleView.layoutParams as? WindowManager.LayoutParams ?: return
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            windowManager.updateViewLayout(bubbleView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling window focus for bubble: $bubbleId", e)
        }
    }
    
    /**
     * Show the soft keyboard
     */
    fun showKeyboard() {
        urlBarText.post {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(urlBarText, InputMethodManager.SHOW_IMPLICIT)
        }
    }
    
    /**
     * Hide the soft keyboard
     */
    fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(urlBarText.windowToken, 0)
        urlBarText.clearFocus()
    }
    
    // ================== GETTERS FOR VIEW REFERENCES ==================
    
    fun getBubbleIcon(): ImageView = bubbleIcon
    fun getProgressBar(): ProgressBar = progressBar
    fun getBubbleContainer(): View = bubbleContainer
    fun getUrlBarContainer(): View = urlBarContainer
    fun getUrlBarIcon(): ImageView = urlBarIcon
    fun getUrlBarText(): EditText = urlBarText
    fun getBtnUrlBarShare(): MaterialButton = btnUrlBarShare
    fun getBtnUrlBarSettings(): MaterialButton = btnUrlBarSettings
    fun getExpandedContainer(): View = expandedContainer
    fun getContentContainer(): FrameLayout = contentContainer
    fun getToolbarContainer(): View = toolbarContainer
    fun getResizeHandlesContainer(): FrameLayout = resizeHandlesContainer
    fun getResizeHandleTopLeft(): ImageView = resizeHandleTopLeft
    fun getResizeHandleTopRight(): ImageView = resizeHandleTopRight
    fun getResizeHandleBottomLeft(): ImageView = resizeHandleBottomLeft
    fun getResizeHandleBottomRight(): ImageView = resizeHandleBottomRight
    
    fun getBtnSaveArticle(): MaterialButton = btnSaveArticle
    
    /**
     * Clean up resources and references
     */
    fun cleanup() {
        uiListener = null
        Log.d(TAG, "UI manager cleaned up for bubble: $bubbleId")
    }
}