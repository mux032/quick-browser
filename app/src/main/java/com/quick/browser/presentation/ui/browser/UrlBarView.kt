package com.quick.browser.presentation.ui.browser

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import com.google.android.material.button.MaterialButton
import com.quick.browser.R

/**
 * A dedicated view for the URL bar component in the bubble.
 * 
 * This view handles all UI components and interactions for the URL bar,
 * separating it from the main BubbleView class to improve maintainability.
 */
class UrlBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var urlBarContainer: View
    private lateinit var urlBarIcon: ImageView
    private lateinit var urlBarText: EditText
    private lateinit var btnUrlBarShare: MaterialButton
    private lateinit var btnUrlBarSettings: MaterialButton

    init {
        initializeViews()
    }

    private fun initializeViews() {
        // Inflate the URL bar layout
        LayoutInflater.from(context).inflate(R.layout.url_bar_layout, this, true)
        
        // Initialize UI components
        urlBarContainer = findViewById(R.id.url_bar_container)
        urlBarIcon = findViewById(R.id.url_bar_icon)
        urlBarText = findViewById(R.id.url_bar_text)
        btnUrlBarShare = findViewById(R.id.btn_url_bar_share)
        btnUrlBarSettings = findViewById(R.id.btn_url_bar_settings)
    }

    // ================== PUBLIC INTERFACE METHODS ==================

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
     * Update URL bar text
     */
    fun updateUrlBarText(url: String) {
        if (urlBarText.text.toString() != url) {
            urlBarText.setText(url)
        }
    }

    /**
     * Get URL bar text
     */
    fun getUrlBarText(): String {
        return urlBarText.text.toString()
    }

    /**
     * Set URL bar text listener
     */
    fun setOnUrlTextListener(listener: (String) -> Unit) {
        urlBarText.setOnEditorActionListener { _, _, _ ->
            val inputUrl = urlBarText.text.toString().trim()
            if (inputUrl.isNotEmpty()) {
                listener(inputUrl)
            }
            true
        }
    }

    /**
     * Set focus change listener
     */
    fun setOnFocusChangeListener(listener: (Boolean) -> Unit) {
        urlBarText.setOnFocusChangeListener { _, hasFocus ->
            listener(hasFocus)
        }
    }

    /**
     * Set click listener for the URL bar
     */
    fun setOnUrlBarClickListener(listener: () -> Unit) {
        urlBarText.setOnClickListener {
            listener()
            urlBarText.requestFocus()
            urlBarText.selectAll()
        }
    }

    /**
     * Set click listener for the share button
     */
    fun setOnShareButtonClickListener(listener: () -> Unit) {
        btnUrlBarShare.setOnClickListener {
            listener()
        }
    }

    /**
     * Set click listener for the settings button
     */
    fun setOnSettingsButtonClickListener(listener: () -> Unit) {
        btnUrlBarSettings.setOnClickListener {
            listener()
        }
    }

    /**
     * Show keyboard
     */
    fun showKeyboard() {
        urlBarText.post {
            urlBarText.requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(urlBarText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    /**
     * Hide keyboard
     */
    fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(urlBarText.windowToken, 0)
        urlBarText.clearFocus()
    }

    /**
     * Show URL bar
     */
    fun show() {
        urlBarContainer.visibility = View.VISIBLE
    }

    /**
     * Hide URL bar
     */
    fun hide() {
        urlBarContainer.visibility = View.GONE
    }

    /**
     * Check if URL bar is visible
     */
    fun isVisible(): Boolean {
        return urlBarContainer.visibility == View.VISIBLE
    }

    // ================== GETTERS FOR VIEW REFERENCES ==================

    fun getUrlBarContainer(): View = urlBarContainer
    fun getUrlBarIcon(): ImageView = urlBarIcon
    fun getUrlBarTextEditText(): EditText = urlBarText
    fun getBtnUrlBarShare(): MaterialButton = btnUrlBarShare
    fun getBtnUrlBarSettings(): MaterialButton = btnUrlBarSettings
}