package com.qb.browser.ui.bubble

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import com.google.android.material.button.MaterialButton
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Handles all touch interactions for the BubbleView including:
 * - Bubble dragging
 * - Resize handle interactions
 * - Toolbar dragging
 * - Settings panel touch handling
 * 
 * This class extracts the complex touch handling logic from BubbleView
 * to improve maintainability and testability.
 */
class BubbleTouchHandler(
    private val context: Context,
    private val bubbleView: BubbleView
) {
    
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
    
    // Services and utilities
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    // Delegate interface for communicating with BubbleView
    interface BubbleTouchDelegate {
        fun onBubbleDragged(x: Int, y: Int)
        fun onBubbleClicked()
        fun onBubbleClosed()
        fun onBubbleToggleExpanded()
        fun hideBubbleSettingsPanel()
        fun isSettingsPanelVisible(): Boolean
        fun isBubbleExpanded(): Boolean
        fun getExpandedContainer(): View
        fun getSettingsPanel(): View
        fun getSettingsButton(): MaterialButton
        fun getToolbarContainer(): View
        fun getResizeHandles(): List<ImageView>
        fun getContentContainer(): FrameLayout
        fun getWebViewContainer(): View
        fun updateDimensions(width: Int, height: Int)
        fun applyBubbleDynamicZoom(zoomPercent: Float)
        fun calculateBubbleZoomLevel(widthRatio: Float): Float
        fun getCurrentZoomPercent(): Float
        fun performClick(): Boolean
    }
    
    private lateinit var delegate: BubbleTouchDelegate
    
    companion object {
        private const val TAG = "BubbleTouchHandler"
    }
    
    /**
     * Initialize the touch handler with the delegate
     */
    fun initialize(delegate: BubbleTouchDelegate) {
        this.delegate = delegate
        setupResizeHandles()
        setupToolbarDrag()
    }
    
    /**
     * Handle touch events for the main bubble view
     */
    fun handleTouchEvent(event: MotionEvent): Boolean {
        if (bubbleView.layoutParams !is WindowManager.LayoutParams) return false
        
        // If we're currently resizing, let the resize handle touch listener handle it
        if (isResizing) {
            return true
        }
        
        // Handle settings panel touch logic
        if (handleSettingsPanelTouch(event)) {
            return false // Settings panel interaction handled
        }
        
        val params = bubbleView.layoutParams as WindowManager.LayoutParams
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        
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
        
        return false
    }
    
    /**
     * Handle settings panel touch interactions
     */
    private fun handleSettingsPanelTouch(event: MotionEvent): Boolean {
        if (!delegate.isSettingsPanelVisible() || event.action != MotionEvent.ACTION_DOWN) {
            return false
        }
        
        val touchX = event.rawX.toInt()
        val touchY = event.rawY.toInt()
        
        // Get rect for settings panel
        val settingsPanelRect = Rect()
        delegate.getSettingsPanel().getGlobalVisibleRect(settingsPanelRect)
        
        // Get rect for settings button
        val settingsButtonRect = Rect()
        delegate.getSettingsButton().getGlobalVisibleRect(settingsButtonRect)
        
        // Check if touch is inside settings panel - if so, keep settings open
        if (settingsPanelRect.contains(touchX, touchY)) {
            return true // Allow normal interaction with settings
        }
        
        // Check if touch is on settings button - let button handle toggle
        if (settingsButtonRect.contains(touchX, touchY)) {
            return true // Let button handle the click
        }
        
        // For any other click, hide settings
        delegate.hideBubbleSettingsPanel()
        return false // Don't consume the event
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
            if (delegate.isBubbleExpanded() && event.y < delegate.getExpandedContainer().top) {
                delegate.onBubbleToggleExpanded()
            }
        }
        
        if (isDragging) {
            // Keep bubble within screen bounds
            val newX = max(0, min(screenWidth - bubbleView.width, (initialX + dx).toInt()))
            val newY = max(0, min(screenHeight - bubbleView.height, (initialY + dy).toInt()))
            
            params.x = newX
            params.y = newY
            
            // Check if the bubble is moved to the bottom edge
            if (params.y >= screenHeight - bubbleView.height) {
                delegate.onBubbleClosed()
                return
            }

            windowManager.updateViewLayout(bubbleView, params)
            delegate.onBubbleDragged(newX, newY)
        }
    }
    
    /**
     * Handle the touch up event
     */
    private fun handleTouchUp(params: WindowManager.LayoutParams) {
        if (!isDragging) {
            delegate.onBubbleClicked()
        } else {
            windowManager.updateViewLayout(bubbleView, params)
        }
        isDragging = false
    }
    
    /**
     * Set up resize handles with touch listeners
     */
    private fun setupResizeHandles() {
        delegate.getResizeHandles().forEach { handle ->
            setupResizeHandleTouch(handle)
        }
    }
    
    /**
     * Set up touch listener for a specific resize handle
     */
    private fun setupResizeHandleTouch(handle: ImageView) {
        handle.setOnTouchListener { view, event ->
            if (bubbleView.layoutParams !is WindowManager.LayoutParams) return@setOnTouchListener false
            
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Start resizing
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    initialWidth = delegate.getExpandedContainer().width
                    initialHeight = delegate.getExpandedContainer().height
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
        val minWidth = context.resources.displayMetrics.widthPixels / 3
        val minHeight = context.resources.displayMetrics.heightPixels / 3
        val maxWidth = context.resources.displayMetrics.widthPixels - 50
        val maxHeight = context.resources.displayMetrics.heightPixels - 100
        
        // Get current window position and dimensions
        val windowParams = bubbleView.layoutParams as WindowManager.LayoutParams
        val expandedContainer = delegate.getExpandedContainer()
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
        
        // Get resize handles
        val resizeHandles = delegate.getResizeHandles()
        val resizeHandleTopLeft = resizeHandles[0] // Assuming order: TL, TR, BL, BR
        val resizeHandleTopRight = resizeHandles[1]
        val resizeHandleBottomLeft = resizeHandles[2]
        val resizeHandleBottomRight = resizeHandles[3]
        
        when (handle) {
            resizeHandleBottomRight -> {
                // Bottom-right corner: just resize width and height
                newWidth = (initialWidth + dx).toInt().coerceIn(minWidth, maxWidth)
                newHeight = (initialHeight + dy).toInt().coerceIn(minHeight, maxHeight)
            }
            
            resizeHandleBottomLeft -> {
                // Bottom-left corner: resize width inversely and height directly
                val desiredWidth = (initialWidth - dx).toInt().coerceIn(minWidth, maxWidth)
                newHeight = (initialHeight + dy).toInt().coerceIn(minHeight, maxHeight)
                
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
                newWidth = (initialWidth + dx).toInt().coerceIn(minWidth, maxWidth)
                val desiredHeight = (initialHeight - dy).toInt().coerceIn(minHeight, maxHeight)
                
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
                val desiredWidth = (initialWidth - dx).toInt().coerceIn(minWidth, maxWidth)
                val desiredHeight = (initialHeight - dy).toInt().coerceIn(minHeight, maxHeight)
                
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
        
        // Apply the new dimensions
        applyBubbleResize(newWidth, newHeight, newX, newY, originalX, originalY)
    }
    
    /**
     * Apply the resize changes to the bubble
     */
    private fun applyBubbleResize(
        newWidth: Int, 
        newHeight: Int, 
        newX: Int, 
        newY: Int, 
        originalX: Int, 
        originalY: Int
    ) {
        val expandedContainer = delegate.getExpandedContainer()
        val webViewContainer = delegate.getWebViewContainer()
        val contentContainer = delegate.getContentContainer()
        
        // Apply the new dimensions to the container
        val containerParams = expandedContainer.layoutParams
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
        
        // Update delegate with new dimensions
        delegate.updateDimensions(newWidth, newHeight)
        
        // Update window position if it changed
        if (newX != originalX || newY != originalY) {
            val windowParams = bubbleView.layoutParams as WindowManager.LayoutParams
            windowParams.x = newX
            windowParams.y = newY
            try {
                windowManager.updateViewLayout(bubbleView, windowParams)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating window layout", e)
            }
        }
        
        // Calculate and apply zoom level
        val screenWidth = context.resources.displayMetrics.widthPixels
        val widthRatio = newWidth.toFloat() / screenWidth
        val calculatedZoomPercent = delegate.calculateBubbleZoomLevel(widthRatio)
        
        // Only update the zoom if it's significantly different from the current zoom
        val currentZoomPercent = delegate.getCurrentZoomPercent()
        val zoomPercent = if (kotlin.math.abs(calculatedZoomPercent - currentZoomPercent) > 2f) {
            calculatedZoomPercent
        } else {
            currentZoomPercent
        }
        
        // Apply the dynamic zoom level
        delegate.applyBubbleDynamicZoom(zoomPercent)
        
        // Force layout update
        expandedContainer.requestLayout()
        webViewContainer.requestLayout()
        contentContainer.requestLayout()
    }
    
    /**
     * Set up toolbar drag functionality to allow dragging the expanded bubble
     * when the toolbar is touched and dragged
     */
    private fun setupToolbarDrag() {
        delegate.getToolbarContainer().setOnTouchListener { _, event ->
            if (bubbleView.layoutParams !is WindowManager.LayoutParams || !delegate.isBubbleExpanded()) {
                return@setOnTouchListener false
            }
            
            val params = bubbleView.layoutParams as WindowManager.LayoutParams
            val screenWidth = context.resources.displayMetrics.widthPixels
            val screenHeight = context.resources.displayMetrics.heightPixels
            
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
                        val newX = max(0, min(screenWidth - bubbleView.width, (initialX + dx).toInt()))
                        val newY = max(0, min(screenHeight - bubbleView.height, (initialY + dy).toInt()))
                        
                        params.x = newX
                        params.y = newY
                        windowManager.updateViewLayout(bubbleView, params)
                        delegate.onBubbleDragged(newX, newY)
                    }
                    true
                }
                
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        windowManager.updateViewLayout(bubbleView, params)
                    }
                    isDragging = false
                    true
                }
                
                else -> false
            }
        }
    }
    
    /**
     * Check if currently resizing
     */
    fun isResizing(): Boolean = isResizing
    
    /**
     * Check if currently dragging
     */
    fun isDragging(): Boolean = isDragging
}