package com.quick.browser.presentation.ui.browser

import android.content.Context
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.quick.browser.R
import com.quick.browser.service.SettingsService

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
 * @param settingsService Manager for persisting and retrieving user settings
 * @param bubbleAnimator Enhanced animator for professional animations
 */
class BubbleSettingsPanel(
    private val context: Context,
    private val settingsService: SettingsService,
    private val bubbleAnimator: BubbleAnimator
) {

    // Settings panel state
    private var isVisible = false
    private var isReaderMode = false

    // Settings controls - will be initialized when panel is set up
    private var adBlockSwitch: SwitchMaterial? = null
    private var javascriptSwitch: SwitchMaterial? = null

    // UI sections
    private var browserSettingsSection: View? = null
    private var readerSettingsSection: View? = null

    // Reader mode controls
    private var btnFontDecrease: MaterialButton? = null
    private var btnFontIncrease: MaterialButton? = null
    private var fontSizeDisplay: TextView? = null
    private var btnBgWhite: MaterialButton? = null
    private var btnBgSepia: MaterialButton? = null
    private var btnBgDark: MaterialButton? = null
    private var btnAlignLeft: MaterialButton? = null
    private var btnAlignCenter: MaterialButton? = null
    private var btnAlignRight: MaterialButton? = null
    private var btnAlignJustify: MaterialButton? = null

    // Callback interface for BubbleView to respond to settings changes
    interface SettingsPanelListener {
        fun onAdBlockingChanged(enabled: Boolean)
        fun onJavaScriptChanged(enabled: Boolean)
        fun onSettingsPanelVisibilityChanged(isVisible: Boolean)
        fun onReaderFontSizeChanged(size: Int)
        fun onReaderBackgroundChanged(background: String)
        fun onReaderTextAlignChanged(alignment: String)
        fun onSaveOfflineRequested()
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
        setupReaderModeControls(settingsPanel)
        setupTouchHandling(settingsPanel)
    }

    /**
     * Set up settings panel controls and their event listeners
     *
     * @param settingsPanel The root view of the settings panel
     * @param webView The WebView instance to apply settings changes to
     */
    private fun setupSettingsControls(settingsPanel: View, webView: WebView) {
        // Get UI sections
        browserSettingsSection = settingsPanel.findViewById(R.id.browser_settings_section)
        readerSettingsSection = settingsPanel.findViewById(R.id.reader_settings_section)

        // Set up ad blocking switch
        adBlockSwitch = settingsPanel.findViewById(R.id.ad_block_switch)
        adBlockSwitch?.let { switch ->
            switch.isChecked = settingsService.isAdBlockEnabled()
            switch.setOnCheckedChangeListener { _, isChecked ->
                settingsService.setAdBlockEnabled(isChecked)
                listener?.onAdBlockingChanged(isChecked)

                // Reload page to apply ad blocking changes
                if (webView.visibility == View.VISIBLE) {
                    webView.reload()
                }
            }
        }

        // Set up JavaScript switch
        javascriptSwitch = settingsPanel.findViewById(R.id.javascript_switch)
        javascriptSwitch?.let { switch ->
            switch.isChecked = settingsService.isJavaScriptEnabled()
            switch.setOnCheckedChangeListener { _, isChecked ->
                settingsService.setJavaScriptEnabled(isChecked)
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
     * Set up reader mode controls and their event listeners
     *
     * @param settingsPanel The root view of the settings panel
     */
    private fun setupReaderModeControls(settingsPanel: View) {
        // Initialize reader mode controls
        btnFontDecrease = settingsPanel.findViewById(R.id.btn_font_decrease)
        btnFontIncrease = settingsPanel.findViewById(R.id.btn_font_increase)
        fontSizeDisplay = settingsPanel.findViewById(R.id.font_size_display)
        btnBgWhite = settingsPanel.findViewById(R.id.btn_bg_white)
        btnBgSepia = settingsPanel.findViewById(R.id.btn_bg_sepia)
        btnBgDark = settingsPanel.findViewById(R.id.btn_bg_dark)
        btnAlignLeft = settingsPanel.findViewById(R.id.btn_align_left)
        btnAlignCenter = settingsPanel.findViewById(R.id.btn_align_center)
        btnAlignRight = settingsPanel.findViewById(R.id.btn_align_right)
        btnAlignJustify = settingsPanel.findViewById(R.id.btn_align_justify)

        // Font size controls
        btnFontDecrease?.setOnClickListener {
            val currentSize = settingsService.getReaderFontSize()
            val newSize = (currentSize - 2).coerceAtLeast(12)
            settingsService.setReaderFontSize(newSize)
            updateFontSizeDisplay()
            updateFontSizeButtons()
            listener?.onReaderFontSizeChanged(newSize)
        }

        btnFontIncrease?.setOnClickListener {
            val currentSize = settingsService.getReaderFontSize()
            val newSize = (currentSize + 2).coerceAtMost(32)
            settingsService.setReaderFontSize(newSize)
            updateFontSizeDisplay()
            updateFontSizeButtons()
            listener?.onReaderFontSizeChanged(newSize)
        }

        // Background color controls
        btnBgWhite?.setOnClickListener {
            settingsService.setReaderBackground(SettingsService.READER_BG_WHITE)
            updateBackgroundButtons()
            listener?.onReaderBackgroundChanged(SettingsService.READER_BG_WHITE)
        }

        btnBgSepia?.setOnClickListener {
            settingsService.setReaderBackground(SettingsService.READER_BG_SEPIA)
            updateBackgroundButtons()
            listener?.onReaderBackgroundChanged(SettingsService.READER_BG_SEPIA)
        }

        btnBgDark?.setOnClickListener {
            settingsService.setReaderBackground(SettingsService.READER_BG_DARK)
            updateBackgroundButtons()
            listener?.onReaderBackgroundChanged(SettingsService.READER_BG_DARK)
        }

        // Text alignment controls
        btnAlignLeft?.setOnClickListener {
            settingsService.setReaderTextAlign(SettingsService.READER_ALIGN_LEFT)
            updateAlignmentButtons()
            listener?.onReaderTextAlignChanged(SettingsService.READER_ALIGN_LEFT)
        }

        btnAlignCenter?.setOnClickListener {
            settingsService.setReaderTextAlign(SettingsService.READER_ALIGN_CENTER)
            updateAlignmentButtons()
            listener?.onReaderTextAlignChanged(SettingsService.READER_ALIGN_CENTER)
        }

        btnAlignRight?.setOnClickListener {
            settingsService.setReaderTextAlign(SettingsService.READER_ALIGN_RIGHT)
            updateAlignmentButtons()
            listener?.onReaderTextAlignChanged(SettingsService.READER_ALIGN_RIGHT)
        }

        btnAlignJustify?.setOnClickListener {
            settingsService.setReaderTextAlign(SettingsService.READER_ALIGN_JUSTIFY)
            updateAlignmentButtons()
            listener?.onReaderTextAlignChanged(SettingsService.READER_ALIGN_JUSTIFY)
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
     * @param triggerButton Optional button that triggered the panel (for positioning)
     */
    fun show(panel: View, triggerButton: View? = null) {
        if (isVisible) return // Already visible

        // Update settings values to current state
        updateSettingsValues()

        // Show panel with enhanced animation
        isVisible = true
        listener?.onSettingsPanelVisibilityChanged(true)

        bubbleAnimator.animateSettingsPanelShow(panel, triggerButton) {
            // Animation complete
        }
    }

    /**
     * Hide settings panel with animation
     *
     * @param panel The settings panel view to hide
     */
    fun hide(panel: View) {
        if (!isVisible) return // Already hidden

        isVisible = false

        bubbleAnimator.animateSettingsPanelHide(panel) {
            listener?.onSettingsPanelVisibilityChanged(false)
        }
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
        adBlockSwitch?.isChecked = settingsService.isAdBlockEnabled()
        javascriptSwitch?.isChecked = settingsService.isJavaScriptEnabled()
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
        return settingsService.isAdBlockEnabled()
    }

    /**
     * Get current JavaScript setting
     *
     * @return true if JavaScript is enabled, false otherwise
     */
    fun isJavaScriptEnabled(): Boolean {
        return settingsService.isJavaScriptEnabled()
    }

    /**
     * Force update settings values (useful when settings change externally)
     */
    fun refreshSettingsValues() {
        updateSettingsValues()
        updateReaderModeValues()
    }

    /**
     * Set reader mode state and update UI accordingly
     */
    fun setReaderMode(isReaderMode: Boolean) {
        this.isReaderMode = isReaderMode
        updatePanelSections()
        if (isReaderMode) {
            updateReaderModeValues()
        }
    }

    /**
     * Update which panel sections are visible based on reader mode state
     */
    private fun updatePanelSections() {
        if (isReaderMode) {
            browserSettingsSection?.visibility = View.GONE
            readerSettingsSection?.visibility = View.VISIBLE
        } else {
            browserSettingsSection?.visibility = View.VISIBLE
            readerSettingsSection?.visibility = View.GONE
        }
    }

    /**
     * Update all reader mode values to reflect current settings
     */
    private fun updateReaderModeValues() {
        updateFontSizeDisplay()
        updateFontSizeButtons()
        updateBackgroundButtons()
        updateAlignmentButtons()
    }

    /**
     * Update font size display
     */
    private fun updateFontSizeDisplay() {
        val fontSize = settingsService.getReaderFontSize()
        fontSizeDisplay?.text = "${fontSize}sp"
    }

    /**
     * Update font size button states
     */
    private fun updateFontSizeButtons() {
        val fontSize = settingsService.getReaderFontSize()
        val textColor = ContextCompat.getColor(context, android.R.color.black)
        val backgroundColor = ContextCompat.getColor(context, android.R.color.white)

        btnFontDecrease?.let { button ->
            button.setBackgroundColor(backgroundColor)
            button.setTextColor(textColor)
            button.isEnabled = fontSize > 12
            button.alpha = if (fontSize > 12) 1.0f else 0.5f
        }

        btnFontIncrease?.let { button ->
            button.setBackgroundColor(backgroundColor)
            button.setTextColor(textColor)
            button.isEnabled = fontSize < 32
            button.alpha = if (fontSize < 32) 1.0f else 0.5f
        }
    }

    /**
     * Update background button states
     */
    private fun updateBackgroundButtons() {
        val currentBg = settingsService.getReaderBackground()
        val strokeWidth = 4

        // White button
        btnBgWhite?.let { button ->
            val whiteColor = ContextCompat.getColor(context, android.R.color.white)
            val textColor = ContextCompat.getColor(context, android.R.color.black)
            button.setBackgroundColor(whiteColor)
            button.setTextColor(textColor)
            button.strokeWidth = if (currentBg == SettingsService.READER_BG_WHITE) strokeWidth else 1
            button.strokeColor = ContextCompat.getColorStateList(
                context,
                if (currentBg == SettingsService.READER_BG_WHITE) R.color.primary else android.R.color.darker_gray
            )
        }

        // Sepia button
        btnBgSepia?.let { button ->
            val sepiaColor = ContextCompat.getColor(context, R.color.read_mode_background_sepia)
            val textColor = ContextCompat.getColor(context, R.color.read_mode_text_sepia)
            button.setBackgroundColor(sepiaColor)
            button.setTextColor(textColor)
            button.strokeWidth = if (currentBg == SettingsService.READER_BG_SEPIA) strokeWidth else 1
            button.strokeColor = ContextCompat.getColorStateList(
                context,
                if (currentBg == SettingsService.READER_BG_SEPIA) R.color.primary else android.R.color.darker_gray
            )
        }

        // Dark button
        btnBgDark?.let { button ->
            val darkColor = ContextCompat.getColor(context, R.color.read_mode_background_dark)
            val textColor = ContextCompat.getColor(context, R.color.read_mode_text_dark)
            button.setBackgroundColor(darkColor)
            button.setTextColor(textColor)
            button.strokeWidth = if (currentBg == SettingsService.READER_BG_DARK) strokeWidth else 1
            button.strokeColor = ContextCompat.getColorStateList(
                context,
                if (currentBg == SettingsService.READER_BG_DARK) R.color.primary else android.R.color.darker_gray
            )
        }
    }

    /**
     * Update alignment button states
     */
    private fun updateAlignmentButtons() {
        val currentAlign = settingsService.getReaderTextAlign()
        val backgroundColor = ContextCompat.getColor(context, android.R.color.white)
        val textColor = ContextCompat.getColor(context, android.R.color.black)
        val strokeWidth = 4

        // Helper function to update alignment button
        fun updateAlignButton(button: MaterialButton?, isSelected: Boolean) {
            button?.let {
                it.setBackgroundColor(backgroundColor)
                it.setTextColor(textColor)
                it.strokeWidth = if (isSelected) strokeWidth else 1
                it.strokeColor = ContextCompat.getColorStateList(
                    context,
                    if (isSelected) R.color.primary else android.R.color.darker_gray
                )
            }
        }

        updateAlignButton(btnAlignLeft, currentAlign == SettingsService.READER_ALIGN_LEFT)
        updateAlignButton(btnAlignCenter, currentAlign == SettingsService.READER_ALIGN_CENTER)
        updateAlignButton(btnAlignRight, currentAlign == SettingsService.READER_ALIGN_RIGHT)
        updateAlignButton(btnAlignJustify, currentAlign == SettingsService.READER_ALIGN_JUSTIFY)
    }
}