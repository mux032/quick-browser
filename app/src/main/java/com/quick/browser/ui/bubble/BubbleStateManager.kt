package com.quick.browser.ui.bubble

import android.util.Log

/**
 * Manages the state of a BubbleView, including expansion state, dimensions, zoom, 
 * toolbar visibility, and various mode states.
 * 
 * This class extracts state management logic from BubbleView to improve maintainability
 * and testability while following the Single Responsibility Principle.
 * 
 * @property bubbleId Unique identifier for the bubble this state manager belongs to
 */
class BubbleStateManager(private val bubbleId: String) {
    
    companion object {
        private const val TAG = "BubbleStateManager"
        
        // Default zoom level
        const val DEFAULT_ZOOM_PERCENT = 100f
        
        // State change listeners
        interface StateChangeListener {
            fun onExpansionStateChanged(isExpanded: Boolean)
            fun onActiveStateChanged(isActive: Boolean)
            fun onDimensionsChanged(width: Int, height: Int)
            fun onZoomChanged(zoomPercent: Float)
            fun onToolbarVisibilityChanged(isVisible: Boolean)
        }
    }
    
    // State change listener
    private var stateChangeListener: StateChangeListener? = null
    
    // ======================================
    // Core Bubble State
    // ======================================
    
    /**
     * Whether the bubble is currently expanded to show content
     */
    private var _isBubbleExpanded = false
    val isBubbleExpanded: Boolean get() = _isBubbleExpanded
    
    /**
     * Whether the bubble is currently active (visible and interactive)
     */
    private var _isActive = false
    val isActive: Boolean get() = _isActive
    
    /**
     * Whether all bubbles are currently being shown (for visualization purposes)
     */
    private var _isShowingAllBubbles = false
    val isShowingAllBubbles: Boolean get() = _isShowingAllBubbles
    
    /**
     * Callback to invoke when the bubble is closed
     */
    private var onCloseListener: (() -> Unit)? = null
    
    // ======================================
    // Dimension State
    // ======================================
    
    /**
     * Stored width of the expanded container
     */
    private var _storedWidth = 0
    val storedWidth: Int get() = _storedWidth
    
    /**
     * Stored height of the expanded container
     */
    private var _storedHeight = 0
    val storedHeight: Int get() = _storedHeight
    
    /**
     * Whether dimensions have been stored
     */
    private var _hasStoredDimensions = false
    val hasStoredDimensions: Boolean get() = _hasStoredDimensions
    
    // ======================================
    // Zoom State
    // ======================================
    
    /**
     * Current zoom level as a percentage (100% = normal size)
     */
    private var _currentZoomPercent = DEFAULT_ZOOM_PERCENT
    val currentZoomPercent: Float get() = _currentZoomPercent
    
    // ======================================
    // Toolbar State
    // ======================================
    
    /**
     * Whether the toolbar is currently visible
     */
    private var _isToolbarVisible = true
    val isToolbarVisible: Boolean get() = _isToolbarVisible
    
    /**
     * Last recorded scroll Y position (for toolbar animation)
     */
    private var _lastScrollY = 0
    val lastScrollY: Int get() = _lastScrollY
    
    // ======================================
    // Mode States
    // ======================================
    
    /**
     * Whether read mode is currently active
     */
    private var _isReadModeActive = false
    val isReadModeActive: Boolean get() = _isReadModeActive
    
    /**
     * Whether summary mode is currently active
     */
    private var _isSummaryModeActive = false
    val isSummaryModeActive: Boolean get() = _isSummaryModeActive
    
    /**
     * Whether settings panel is currently visible
     */
    private var _isSettingsPanelVisible = false
    val isSettingsPanelVisible: Boolean get() = _isSettingsPanelVisible
    
    // ======================================
    // State Management Methods
    // ======================================
    
    /**
     * Set the state change listener
     * 
     * @param listener The listener to notify of state changes
     */
    fun setStateChangeListener(listener: StateChangeListener) {
        stateChangeListener = listener
    }
    
    /**
     * Toggle the expansion state of the bubble
     * 
     * @return The new expansion state
     */
    fun toggleExpansion(): Boolean {
        setExpanded(!_isBubbleExpanded)
        return _isBubbleExpanded
    }
    
    /**
     * Set the expansion state of the bubble
     * 
     * @param expanded Whether the bubble should be expanded
     */
    fun setExpanded(expanded: Boolean) {
        if (_isBubbleExpanded != expanded) {
            _isBubbleExpanded = expanded
            Log.d(TAG, "Bubble $bubbleId expansion state changed to: $expanded")
            stateChangeListener?.onExpansionStateChanged(expanded)
            
            // Reset toolbar state when expanding
            if (expanded) {
                setToolbarVisible(true)
            }
        }
    }
    
    /**
     * Set the active state of the bubble
     * 
     * @param active Whether the bubble should be active
     */
    fun setActive(active: Boolean) {
        if (_isActive != active) {
            _isActive = active
            Log.d(TAG, "Bubble $bubbleId active state changed to: $active")
            stateChangeListener?.onActiveStateChanged(active)
        }
    }
    
    /**
     * Set whether all bubbles are being shown
     * 
     * @param showing Whether all bubbles are being shown
     */
    fun setShowingAllBubbles(showing: Boolean) {
        _isShowingAllBubbles = showing
        Log.d(TAG, "Bubble $bubbleId showing all bubbles state changed to: $showing")
    }
    
    /**
     * Set the close listener
     * 
     * @param listener Callback to invoke when bubble is closed
     */
    fun setOnCloseListener(listener: (() -> Unit)?) {
        onCloseListener = listener
    }
    
    /**
     * Trigger the close listener
     */
    fun triggerClose() {
        Log.d(TAG, "Triggering close for bubble $bubbleId")
        onCloseListener?.invoke()
    }
    
    // ======================================
    // Dimension Management
    // ======================================
    
    /**
     * Update the stored dimensions
     * 
     * @param width The width to store
     * @param height The height to store
     */
    fun updateDimensions(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            _storedWidth = width
            _storedHeight = height
            _hasStoredDimensions = true
            Log.d(TAG, "Updated dimensions for bubble $bubbleId: ${width}x${height}")
            stateChangeListener?.onDimensionsChanged(width, height)
        } else {
            Log.w(TAG, "Invalid dimensions provided for bubble $bubbleId: ${width}x${height}")
        }
    }
    
    /**
     * Clear stored dimensions
     */
    fun clearDimensions() {
        _storedWidth = 0
        _storedHeight = 0
        _hasStoredDimensions = false
        Log.d(TAG, "Cleared dimensions for bubble $bubbleId")
    }
    
    /**
     * Get dimensions as a Pair
     * 
     * @return Pair of (width, height) or null if no dimensions stored
     */
    fun getDimensions(): Pair<Int, Int>? {
        return if (_hasStoredDimensions && _storedWidth > 0 && _storedHeight > 0) {
            Pair(_storedWidth, _storedHeight)
        } else {
            null
        }
    }
    
    // ======================================
    // Zoom Management
    // ======================================
    
    /**
     * Set the current zoom percentage
     * 
     * @param zoomPercent The zoom percentage (e.g., 75.0 for 75%)
     */
    fun setZoomPercent(zoomPercent: Float) {
        val clampedZoom = zoomPercent.coerceIn(50f, 200f) // Reasonable zoom range
        if (_currentZoomPercent != clampedZoom) {
            _currentZoomPercent = clampedZoom
            Log.d(TAG, "Zoom changed for bubble $bubbleId: $clampedZoom%")
            stateChangeListener?.onZoomChanged(clampedZoom)
        }
    }
    
    /**
     * Reset zoom to default
     */
    fun resetZoom() {
        setZoomPercent(DEFAULT_ZOOM_PERCENT)
    }
    
    // ======================================
    // Toolbar Management
    // ======================================
    
    /**
     * Set toolbar visibility
     * 
     * @param visible Whether the toolbar should be visible
     */
    fun setToolbarVisible(visible: Boolean) {
        if (_isToolbarVisible != visible) {
            _isToolbarVisible = visible
            Log.d(TAG, "Toolbar visibility changed for bubble $bubbleId: $visible")
            stateChangeListener?.onToolbarVisibilityChanged(visible)
        }
    }
    
    /**
     * Toggle toolbar visibility
     * 
     * @return The new visibility state
     */
    fun toggleToolbarVisibility(): Boolean {
        setToolbarVisible(!_isToolbarVisible)
        return _isToolbarVisible
    }
    
    /**
     * Update the last scroll Y position
     * 
     * @param scrollY The current scroll Y position
     */
    fun updateScrollY(scrollY: Int) {
        _lastScrollY = scrollY
    }
    
    // ======================================
    // Mode State Management
    // ======================================
    
    /**
     * Set read mode state
     * 
     * @param active Whether read mode should be active
     */
    fun setReadModeActive(active: Boolean) {
        if (_isReadModeActive != active) {
            _isReadModeActive = active
            Log.d(TAG, "Read mode state changed for bubble $bubbleId: $active")
            
            // Exit summary mode if read mode is activated
            if (active && _isSummaryModeActive) {
                setSummaryModeActive(false)
            }
        }
    }
    
    /**
     * Set summary mode state
     * 
     * @param active Whether summary mode should be active
     */
    fun setSummaryModeActive(active: Boolean) {
        if (_isSummaryModeActive != active) {
            _isSummaryModeActive = active
            Log.d(TAG, "Summary mode state changed for bubble $bubbleId: $active")
            
            // Exit read mode if summary mode is activated
            if (active && _isReadModeActive) {
                setReadModeActive(false)
            }
        }
    }
    
    /**
     * Set settings panel visibility
     * 
     * @param visible Whether the settings panel should be visible
     */
    fun setSettingsPanelVisible(visible: Boolean) {
        _isSettingsPanelVisible = visible
        Log.d(TAG, "Settings panel visibility changed for bubble $bubbleId: $visible")
    }
    
    /**
     * Exit all special modes (read mode, summary mode)
     */
    fun exitAllModes() {
        setReadModeActive(false)
        setSummaryModeActive(false)
        setSettingsPanelVisible(false)
        Log.d(TAG, "Exited all modes for bubble $bubbleId")
    }
    
    // ======================================
    // State Reset and Cleanup
    // ======================================
    
    /**
     * Reset the bubble to its default state
     */
    fun resetToDefault() {
        Log.d(TAG, "Resetting bubble $bubbleId to default state")
        
        _isBubbleExpanded = false
        _isActive = false
        _isShowingAllBubbles = false
        
        // Don't reset dimensions as they should persist
        
        _currentZoomPercent = DEFAULT_ZOOM_PERCENT
        _isToolbarVisible = true
        _lastScrollY = 0
        
        exitAllModes()
        
        // Notify listener of state reset
        stateChangeListener?.onExpansionStateChanged(false)
        stateChangeListener?.onActiveStateChanged(false)
        stateChangeListener?.onZoomChanged(DEFAULT_ZOOM_PERCENT)
        stateChangeListener?.onToolbarVisibilityChanged(true)
    }
    
    /**
     * Get a summary of the current state for debugging
     * 
     * @return String representation of the current state
     */
    fun getStateSnapshot(): String {
        return """
            BubbleState[$bubbleId]:
            - Expanded: $_isBubbleExpanded
            - Active: $_isActive
            - ShowingAll: $_isShowingAllBubbles
            - Dimensions: ${if (_hasStoredDimensions) "${_storedWidth}x${_storedHeight}" else "none"}
            - Zoom: $_currentZoomPercent%
            - Toolbar: $_isToolbarVisible
            - ReadMode: $_isReadModeActive
            - SummaryMode: $_isSummaryModeActive
            - SettingsPanel: $_isSettingsPanelVisible
        """.trimIndent()
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up state for bubble $bubbleId")
        stateChangeListener = null
        onCloseListener = null
    }
}