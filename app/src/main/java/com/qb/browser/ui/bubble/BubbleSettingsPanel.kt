package com.qb.browser.ui.bubble

import android.content.Context
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.qb.browser.R
import com.qb.browser.manager.SettingsManager

/**
 * Handles the settings panel functionality for BubbleView.
 * 
 * This class manages the settings panel visibility, animations, and controls
 * for features like ad blocking and JavaScript toggles. It encapsulates all
 * settings-related logic that was previously scattered throughout BubbleView.
 * 
 * Responsibilities:
 * - Settings panel show/hide with animations
 * - Settings controls setup and event handling
 * - Settings value updates and synchronization
 * - Touch event handling for click-outside-to-close behavior
 * 
 * @param context Android context for accessing resources and services
 * @param settingsManager Manager for persisting and retrieving user settings
 */
class BubbleSettingsPanel(
    private val context: Context,
    private val settingsManager: SettingsManager
) {
    
    // Settings panel state
    private var isVisible = false
    
    // Settings controls - will be initialized when panel is set up
    private var adBlockSwitch: SwitchMaterial? = null
    private var javascriptSwitch: SwitchMaterial? = null
    
    // Callback interface for BubbleView to respond to settings changes
    interface SettingsPanelListener {
        fun onAdBlockingChanged(enabled: Boolean)
        fun onJavaScriptChanged(enabled: Boolean)
        fun onSettingsPanelVisibilityChanged(isVisible: Boolean)
    }
    
    private var listener: SettingsPanelListener? = null
    
    /**
     * Set the listener for settings panel events
     */
    fun setListener(listener: SettingsPanelListener?) {
        this.listener = listener
    }
    
    /**
     * Initialize the settings panel with the provided view and controls
     * 
     * @param settingsPanel The root view of the settings panel
     * @param webView The WebView instance to apply settings changes to
     */
    fun initialize(settingsPanel: View, webView: WebView) {
        setupSettingsControls(settingsPanel, webView)
        setupTouchHandling(settingsPanel)
    }
    
    /**
     * Set up settings panel controls and their event listeners
     * 
     * @param settingsPanel The root view of the settings panel
     * @param webView The WebView instance to apply settings changes to
     */
    private fun setupSettingsControls(settingsPanel: View, webView: WebView) {
        // Set up ad blocking switch
        adBlockSwitch = settingsPanel.findViewById<SwitchMaterial>(R.id.ad_block_switch)
        adBlockSwitch?.let { switch ->
            switch.isChecked = settingsManager.isAdBlockEnabled()
            switch.setOnCheckedChangeListener { _, isChecked ->
                settingsManager.setAdBlockEnabled(isChecked)
                listener?.onAdBlockingChanged(isChecked)
                
                // Reload page to apply ad blocking changes
                if (webView.visibility == View.VISIBLE) {
                    webView.reload()
                }
            }
        }
        
        // Set up JavaScript switch
        javascriptSwitch = settingsPanel.findViewById<SwitchMaterial>(R.id.javascript_switch)
        javascriptSwitch?.let { switch ->
            switch.isChecked = settingsManager.isJavaScriptEnabled()
            switch.setOnCheckedChangeListener { _, isChecked ->
                settingsManager.setJavaScriptEnabled(isChecked)
                webView.settings.javaScriptEnabled = isChecked
                listener?.onJavaScriptChanged(isChecked)
                
                // Reload page to apply JavaScript changes
                if (webView.visibility == View.VISIBLE) {
                    webView.reload()
                }
            }
        }
    }
    
    /**
     * Set up touch handling for the settings panel to prevent unwanted closures
     * 
     * @param settingsPanel The root view of the settings panel
     */
    private fun setupTouchHandling(settingsPanel: View) {
        // Prevent settings panel from closing when clicking on it
        settingsPanel.setOnTouchListener { _, _ ->
            // Consume touch events to prevent them from propagating to parent views
            // This ensures settings panel stays open when interacting with its content
            true
        }
    }
    
    /**
     * Toggle settings panel visibility
     * 
     * @param panel The settings panel view to toggle
     */
    fun toggle(panel: View) {
        if (isVisible) {
            hide(panel)
        } else {
            show(panel)
        }
    }
    
    /**
     * Show settings panel with animation
     * 
     * @param panel The settings panel view to show
     */
    fun show(panel: View) {
        if (isVisible) return // Already visible
        
        // Update settings values to current state
        updateSettingsValues()
        
        // Show panel with animation
        isVisible = true
        panel.visibility = View.VISIBLE
        panel.alpha = 0f
        panel.scaleX = 0.8f
        panel.scaleY = 0.8f
        
        // Set pivot to top-right corner for dropdown effect
        panel.pivotX = panel.width * 0.9f
        panel.pivotY = 0f
        
        panel.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withLayer()
            .withStartAction {
                listener?.onSettingsPanelVisibilityChanged(true)
            }
            .start()
    }
    
    /**
     * Hide settings panel with animation
     * 
     * @param panel The settings panel view to hide
     */
    fun hide(panel: View) {
        if (!isVisible) return // Already hidden
        
        isVisible = false
        
        // Set pivot to top-right corner for dropdown effect
        panel.pivotX = panel.width * 0.9f
        panel.pivotY = 0f
        
        panel.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(150)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withLayer()
            .withEndAction {
                panel.visibility = View.GONE
                listener?.onSettingsPanelVisibilityChanged(false)
            }
            .start()
    }
    
    /**
     * Helper method to safely dismiss settings panel if it's visible
     * 
     * @param panel The settings panel view to dismiss
     */
    fun dismissIfVisible(panel: View) {
        if (isVisible) {
            hide(panel)
        }
    }
    
    /**
     * Update settings values to reflect current state
     */
    private fun updateSettingsValues() {
        // Update switches to reflect current settings state
        adBlockSwitch?.isChecked = settingsManager.isAdBlockEnabled()
        javascriptSwitch?.isChecked = settingsManager.isJavaScriptEnabled()
    }
    
    /**
     * Check if the settings panel is currently visible
     * 
     * @return true if the settings panel is visible, false otherwise
     */
    fun isVisible(): Boolean {
        return isVisible
    }
    
    /**
     * Handle touch events to determine if settings panel should be closed
     * This method should be called from the parent view's touch handler
     * 
     * @param event The motion event to process
     * @param settingsPanel The settings panel view
     * @return true if the touch was handled (panel was closed), false otherwise
     */
    fun handleTouchEvent(event: MotionEvent, settingsPanel: View): Boolean {
        if (!isVisible || event.action != MotionEvent.ACTION_DOWN) {
            return false
        }
        
        val touchX = event.rawX.toInt()
        val touchY = event.rawY.toInt()
        
        // Check if touch is not on settings panel
        val settingsPanelRect = Rect()
        settingsPanel.getGlobalVisibleRect(settingsPanelRect)
        
        if (!settingsPanelRect.contains(touchX, touchY)) {
            hide(settingsPanel)
            return true
        }
        
        return false
    }
    
    /**
     * Get current ad blocking setting
     * 
     * @return true if ad blocking is enabled, false otherwise
     */
    fun isAdBlockEnabled(): Boolean {
        return settingsManager.isAdBlockEnabled()
    }
    
    /**
     * Get current JavaScript setting
     * 
     * @return true if JavaScript is enabled, false otherwise
     */
    fun isJavaScriptEnabled(): Boolean {
        return settingsManager.isJavaScriptEnabled()
    }
    
    /**
     * Force update settings values (useful when settings change externally)
     */
    fun refreshSettingsValues() {
        updateSettingsValues()
    }
}