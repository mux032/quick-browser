package com.quick.browser.presentation.ui.browser

import android.content.Context
import android.graphics.Color
import android.view.View
import android.webkit.WebView
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.button.MaterialButton
import com.quick.browser.R
import com.quick.browser.service.SummarizationService
import com.quick.browser.utils.Logger
import kotlinx.coroutines.*
import org.jsoup.Jsoup

/**
 * Manages the summarization functionality for BubbleView.
 *
 * This class handles all summary-related operations including UI management,
 * content extraction, summarization processing, and error handling. It provides
 * a clean interface for toggling between web view and summary view.
 *
 * Responsibilities:
 * - Summary mode toggle and UI state management
 * - HTML content extraction and cleaning
 * - AI-powered summarization processing
 * - Summary results display and error handling
 * - Background summarization for performance
 *
 * @param context Android context for accessing resources and services
 * @param bubbleAnimator Enhanced animator for smooth mode transitions
 */
class BubbleSummaryManager(
    private val context: Context,
    private val summarizationService: SummarizationService,
    private val bubbleAnimator: BubbleAnimator? = null
) {

    companion object {
        private const val TAG = "BubbleSummaryManager"
        private const val MIN_CONTENT_LENGTH = 100
    }

    // UI components - will be initialized when views are set up
    private var summaryContainer: FrameLayout? = null
    private var summaryContent: LinearLayout? = null
    private var summaryProgress: ProgressBar? = null
    private var btnSummarize: MaterialButton? = null
    private var webView: WebView? = null

    // Summary state
    private var isSummaryMode = false
    private var isSummarizationInProgress = false
    private var cachedHtmlContent: String? = null

    // Summarization service is now injected via constructor

    // Callback interface for BubbleView to respond to summary events
    interface SummaryManagerListener {
        fun onSummaryModeChanged(isSummaryMode: Boolean)
        fun onSummarizationStarted()
        fun onSummarizationCompleted(success: Boolean)
        fun onSummarizationError(message: String)
        fun onSummaryScrollDown()
        fun onSummaryScrollUp()
    }

    private var listener: SummaryManagerListener? = null

    /**
     * Set the listener for summary manager events
     */
    fun setListener(listener: SummaryManagerListener?) {
        this.listener = listener
    }

    /**
     * Initialize the summary manager with the required UI components
     *
     * @param summaryContainer The container for summary content
     * @param summaryContent The content layout for summary points
     * @param summaryProgress The progress indicator for summarization
     * @param btnSummarize The button to toggle summary mode
     * @param webView The WebView instance to extract content from
     */
    fun initialize(
        summaryContainer: FrameLayout,
        summaryContent: LinearLayout,
        summaryProgress: ProgressBar,
        btnSummarize: MaterialButton,
        webView: WebView
    ) {
        this.summaryContainer = summaryContainer
        this.summaryContent = summaryContent
        this.summaryProgress = summaryProgress
        this.btnSummarize = btnSummarize
        this.webView = webView

        setupSummaryViews()
    }

    /**
     * Set up summary views with proper styling and background
     */
    private fun setupSummaryViews() {
        // Set background color for summary container and content
        summaryContainer?.setBackgroundColor(Color.WHITE)
        summaryContent?.setBackgroundColor(Color.WHITE)
        
        // Set up scroll listener for the summary container
        summaryContainer?.let { container ->
            // If it's a NestedScrollView, we can set up a scroll listener
            if (container is NestedScrollView) {
                container.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                    // Detect scroll direction
                    if (scrollY > oldScrollY) {
                        // Scrolling down
                        listener?.onSummaryScrollDown()
                    } else if (scrollY < oldScrollY) {
                        // Scrolling up
                        listener?.onSummaryScrollUp()
                    }
                }
            }
        }
    }

    /**
     * Toggle between web view and summary view
     */
    fun toggleSummaryMode() {
        if (isSummaryMode) {
            showWebViewOnly()
        } else {
            showSummaryView()
        }
    }

    /**
     * Show only the web view, hide summary
     */
    fun showWebViewOnly() {
        if (!isSummaryMode) return // Already in web view mode

        isSummaryMode = false

        // Update button appearance
        btnSummarize?.apply {
            setIconResource(R.drawable.ic_summarize)
            setIconTint(ContextCompat.getColorStateList(context, R.color.colorPrimary))
            contentDescription = context.getString(R.string.summarize)
        }

        // Use enhanced animation if available
        if (bubbleAnimator != null && summaryContainer != null && webView != null) {
            bubbleAnimator.animateFromSummaryMode(summaryContainer!!, webView!!) {
                Toast.makeText(context, R.string.showing_web_view, Toast.LENGTH_SHORT).show()
                listener?.onSummaryModeChanged(false)
            }
        } else {
            // Fallback to direct visibility change
            webView?.visibility = View.VISIBLE
            summaryContainer?.visibility = View.GONE
            Toast.makeText(context, R.string.showing_web_view, Toast.LENGTH_SHORT).show()
            listener?.onSummaryModeChanged(false)
        }
    }

    /**
     * Show the summary view and hide the web view
     */
    fun showSummaryView() {
        if (webView?.visibility != View.VISIBLE) {
            Toast.makeText(context, R.string.summary_error, Toast.LENGTH_SHORT).show()
            return
        }

        if (isSummaryMode) return // Already in summary mode

        isSummaryMode = true
        summaryProgress?.visibility = View.VISIBLE
        summaryContent?.removeAllViews()

        // Update button appearance
        btnSummarize?.apply {
            setIconResource(R.drawable.ic_web_page)
            setIconTint(ContextCompat.getColorStateList(context, R.color.colorPrimary))
            contentDescription = context.getString(R.string.show_web_view)
        }

        // Use enhanced animation if available
        if (bubbleAnimator != null && summaryContainer != null && webView != null) {
            bubbleAnimator.animateToSummaryMode(webView!!, summaryContainer!!) {
                Toast.makeText(context, R.string.summarizing, Toast.LENGTH_SHORT).show()
                listener?.onSummaryModeChanged(true)
                listener?.onSummarizationStarted()
                summarizeContent()
            }
        } else {
            // Fallback to direct visibility change
            webView?.visibility = View.GONE
            summaryContainer?.visibility = View.VISIBLE
            Toast.makeText(context, R.string.summarizing, Toast.LENGTH_SHORT).show()
            listener?.onSummaryModeChanged(true)
            listener?.onSummarizationStarted()
            summarizeContent()
        }
    }

    /**
     * Summarize the current page content
     */
    private fun summarizeContent() {
        try {
            if (cachedHtmlContent != null && cachedHtmlContent!!.length > MIN_CONTENT_LENGTH) {
                processSummarization(cachedHtmlContent!!)
            } else {
                extractContentFromWebView { htmlContent ->
                    if (htmlContent != null) {
                        cachedHtmlContent = htmlContent
                        processSummarization(htmlContent)
                    } else {
                        showSummaryError(context.getString(R.string.summary_error))
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error in summarizeContent", e)
            showSummaryError(context.getString(R.string.summary_error))
        }
    }

    /**
     * Extract HTML content from the WebView
     */
    private fun extractContentFromWebView(callback: (String?) -> Unit) {
        webView?.evaluateJavascript("(function() { return document.documentElement.outerHTML; })()") { html ->
            try {
                if (html != null && html.length > 50) {
                    val unescapedHtml = html.substring(1, html.length - 1)
                        .replace("\\\"", "\"")
                        .replace("\\n", "\n")
                        .replace("\\\\", "\\")
                    callback(unescapedHtml)
                } else {
                    callback(null)
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error processing HTML for summary", e)
                callback(null)
            }
        }
    }

    /**
     * Process the HTML content for summarization
     */
    private fun processSummarization(htmlContent: String) {
        isSummarizationInProgress = true
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Add a timeout for the entire summarization process
                withTimeout(30000) { // 30 second timeout
                    val cleanedHtml = withContext(Dispatchers.IO) {
                        try {
                            cleanHtmlContent(htmlContent)
                        } catch (e: Exception) {
                            Logger.e(TAG, "Error cleaning HTML", e)
                            null
                        }
                    }

                    if (cleanedHtml == null || cleanedHtml.length < MIN_CONTENT_LENGTH) {
                        showSummaryError(context.getString(R.string.summary_not_article))
                        listener?.onSummarizationCompleted(false)
                        return@withTimeout
                    }

                    val summaryPoints = withContext(Dispatchers.Default) {
                        summarizationService.summarizeContent(cleanedHtml)
                    }

                    if (summaryPoints.isNotEmpty()) {
                        displaySummaryPoints(summaryPoints)
                        listener?.onSummarizationCompleted(true)
                    } else {
                        showSummaryError(context.getString(R.string.summary_not_article))
                        listener?.onSummarizationCompleted(false)
                    }
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error processing summarization", e)
                showSummaryError(context.getString(R.string.summary_error))
                listener?.onSummarizationError(e.message ?: "Unknown error")
            } catch (e: TimeoutCancellationException) {
                Logger.e(TAG, "Summarization timed out", e)
                showSummaryError(context.getString(R.string.summary_timeout))
                listener?.onSummarizationError("Summarization timed out")
            } finally {
                isSummarizationInProgress = false
            }
        }
    }

    /**
     * Clean HTML content by removing unwanted elements and extracting text
     */
    private fun cleanHtmlContent(htmlContent: String): String? {
        val doc = Jsoup.parse(htmlContent)
        doc.select("script, style, noscript, iframe, object, embed, header, footer, nav, aside").remove()
        return doc.text()
    }

    /**
     * Display the summary points in the UI
     */
    private fun displaySummaryPoints(points: List<String>) {
        summaryProgress?.visibility = View.GONE
        summaryContent?.let { container ->
            for (point in points) {
                val bulletPoint = createSummaryPointView(point)
                container.addView(bulletPoint)
            }
        }
    }

    /**
     * Create a styled TextView for a summary point
     */
    private fun createSummaryPointView(point: String): TextView {
        return TextView(context).apply {
            text = "• $point"
            setPadding(16, 16, 16, 16)
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
        }
    }

    /**
     * Show an error in the summary view
     */
    private fun showSummaryError(message: String) {
        summaryProgress?.visibility = View.GONE
        summaryContent?.let { container ->
            val errorText = TextView(context).apply {
                text = message
                setPadding(16, 16, 16, 16)
                textSize = 16f
                setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            }
            container.addView(errorText)
        }
    }

    /**
     * Start background summarization of the HTML content
     * This is useful for pre-caching summaries without showing UI
     */
    fun startBackgroundSummarization(htmlContent: String) {
        if (isSummarizationInProgress || htmlContent.length < MIN_CONTENT_LENGTH) return

        isSummarizationInProgress = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Add a timeout for background summarization
                withTimeout(30000) { // 30 second timeout
                    val cleanedText = cleanHtmlContent(htmlContent)
                    if (cleanedText != null && cleanedText.length > MIN_CONTENT_LENGTH) {
                        summarizationService.summarizeContent(cleanedText)
                    }
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Background summarization failed", e)
            } catch (e: TimeoutCancellationException) {
                Logger.e(TAG, "Background summarization timed out", e)
            } finally {
                isSummarizationInProgress = false
            }
        }
    }

    /**
     * Cache HTML content for faster summarization
     */
    fun cacheHtmlContent(htmlContent: String) {
        if (htmlContent.length > MIN_CONTENT_LENGTH) {
            cachedHtmlContent = htmlContent
        }
    }

    /**
     * Check if currently in summary mode
     */
    fun isSummaryMode(): Boolean {
        return isSummaryMode
    }

    /**
     * Check if summarization is currently in progress
     */
    fun isSummarizationInProgress(): Boolean {
        return isSummarizationInProgress
    }

    /**
     * Clear cached content to force re-summarization
     */
    fun clearCache() {
        cachedHtmlContent = null
    }

    /**
     * Get the current cached HTML content
     */
    fun getCachedContent(): String? {
        return cachedHtmlContent
    }

    /**
     * Force exit summary mode (useful when bubble is collapsed)
     */
    fun forceExitSummaryMode() {
        if (isSummaryMode) {
            isSummaryMode = false
            webView?.visibility = View.VISIBLE
            summaryContainer?.visibility = View.GONE

            btnSummarize?.apply {
                setIconResource(R.drawable.ic_summarize)
                setIconTint(ContextCompat.getColorStateList(context, R.color.colorPrimary))
                contentDescription = context.getString(R.string.summarize)
            }

            listener?.onSummaryModeChanged(false)
        }
    }
}