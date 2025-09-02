package com.quick.browser.presentation.ui.browser

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.quick.browser.utils.Logger

/**
 * Handles touch interactions for the resize bar at the bottom of the bubble.
 */
class BubbleResizeBarHandler(
    private val context: Context,
    private val bubbleView: BubbleView
) {
    
    private var initialTouchY = 0f
    private var initialHeight = 0
    private var initialWidth = 0
    private var initialX = 0
    private var initialY = 0
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    companion object {
        private const val TAG = "BubbleResizeBarHandler"
    }
    
    /**
     * Set up touch listener for the resize bar
     */
    fun setupResizeBarTouch(resizeBar: View, delegate: BubbleTouchHandler.BubbleTouchDelegate) {
        resizeBar.setOnTouchListener { view, event ->
            if (bubbleView.layoutParams !is WindowManager.LayoutParams) return@setOnTouchListener false
            
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Start resizing
                    initialTouchY = event.rawY
                    initialHeight = delegate.getExpandedContainer().height
                    initialWidth = delegate.getExpandedContainer().width
                    
                    // Get current window position
                    val windowParams = bubbleView.layoutParams as WindowManager.LayoutParams
                    initialX = windowParams.x
                    initialY = windowParams.y
                    
                    Logger.d(TAG, "Resize bar touch started")
                    return@setOnTouchListener true
                }
                
                MotionEvent.ACTION_MOVE -> {
                    // Calculate the change in position
                    val dy = event.rawY - initialTouchY
                    
                    // Resize based on vertical movement
                    resizeBubbleFromBottom(dy, delegate)
                    
                    // Keep the resize bar under the finger during dragging
                    // This is handled by the system, but we ensure we consume the event
                    return@setOnTouchListener true
                }
                
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    Logger.d(TAG, "Resize bar touch ended")
                    return@setOnTouchListener true
                }
            }
            
            return@setOnTouchListener false
        }
    }
    
    /**
     * Resize the bubble from the bottom (uniform scaling from all sides)
     */
    private fun resizeBubbleFromBottom(dy: Float, delegate: BubbleTouchHandler.BubbleTouchDelegate) {
        // Define minimum and maximum dimensions
        val minWidth = context.resources.displayMetrics.widthPixels / 3 // Keep the old width value
        val minHeight = (context.resources.displayMetrics.heightPixels / 3) * 3 / 4 // Reduce height by 25% (multiply by 0.75)
        val maxWidth = context.resources.displayMetrics.widthPixels - 50
        val maxHeight = context.resources.displayMetrics.heightPixels - 100
        
        // Calculate new dimensions (moving up decreases size, moving down increases size)
        // Positive dy means moving down, negative dy means moving up
        val sizeChange = (dy * 0.5).toInt() // Reduce speed to 0.5x for better control
        var newWidth = (initialWidth + sizeChange).coerceIn(minWidth, maxWidth)
        var newHeight = (initialHeight + sizeChange).coerceIn(minHeight, maxHeight)
        
        // Calculate new position to keep the center fixed
        val widthChange = newWidth - initialWidth
        val heightChange = newHeight - initialHeight
        val newX = initialX - widthChange / 2
        val newY = initialY - heightChange / 2
        
        // Check if we should auto-collapse the bubble
        val collapseThresholdWidth = minWidth // Collapse when width is below minimum
        val collapseThresholdHeight = minHeight // Collapse when height is below minimum
        if (newWidth <= collapseThresholdWidth && newHeight <= collapseThresholdHeight) {
            // Auto-collapse the bubble with bounce animation
            bounceAndCollapse(delegate)
            return
        }
        
        // Apply the new dimensions
        applyBubbleResize(newWidth, newHeight, newX, newY, delegate)
    }
    
    /**
     * Apply the resize changes to the bubble
     */
    private fun applyBubbleResize(
        newWidth: Int,
        newHeight: Int,
        newX: Int,
        newY: Int,
        delegate: BubbleTouchHandler.BubbleTouchDelegate
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
        
        // Update window position with bottom margin constraint
        val windowParams = bubbleView.layoutParams as WindowManager.LayoutParams
        windowParams.x = newX.coerceAtLeast(0)
        
        // Ensure the window doesn't go too far down - leave space for the resize bar
        val screenHeight = context.resources.displayMetrics.heightPixels
        val resizeBarHeight = 4 // Height of the resize bar
        val bottomMargin = 20 // Additional margin to prevent touching bottom edge
        val maxY = screenHeight - newHeight - resizeBarHeight - bottomMargin
        windowParams.y = newY.coerceIn(0, maxY)
        
        try {
            windowManager.updateViewLayout(bubbleView, windowParams)
        } catch (e: Exception) {
            Logger.e(TAG, "Error updating window layout", e)
        }
        
        // Calculate and apply zoom level
        val screenWidth = context.resources.displayMetrics.widthPixels
        val widthRatio = newWidth.toFloat() / screenWidth
        val calculatedZoomPercent = delegate.calculateBubbleZoomLevel(widthRatio)
        
        // Apply the dynamic zoom level
        delegate.applyBubbleDynamicZoom(calculatedZoomPercent)
        
        // Force layout update
        expandedContainer.requestLayout()
        webViewContainer.requestLayout()
        contentContainer.requestLayout()
    }
    
    /**
     * Perform a bounce animation and then collapse the bubble
     */
    private fun bounceAndCollapse(delegate: BubbleTouchHandler.BubbleTouchDelegate) {
        val expandedContainer = delegate.getExpandedContainer()
        
        // Hide resize handles container immediately to prevent them from showing during bounce
        delegate.getResizeHandlesContainer().visibility = View.GONE
        
        // Perform bounce animation and collapse when it completes
        bubbleView.getBubbleAnimator().animateBounce(expandedContainer, false) { // false for collapse bounce
            delegate.onBubbleToggleExpanded()
        }
    }
}