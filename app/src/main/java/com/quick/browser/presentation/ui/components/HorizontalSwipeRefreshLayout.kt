package com.quick.browser.presentation.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.WebView
import android.widget.ImageView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlin.math.abs

class HorizontalSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {

    var webView: WebView? = null
    private var backArrow: ImageView? = null
    private var forwardArrow: ImageView? = null

    private var startX = 0f
    private var startY = 0f
    private var isHorizontalSwipe = false
    private val swipeSlop = 100

    fun setArrowImageViews(back: ImageView, forward: ImageView) {
        this.backArrow = back
        this.forwardArrow = forward
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.x
                startY = ev.y
                isHorizontalSwipe = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (isHorizontalSwipe) {
                    return true // Already determined it's a horizontal swipe, so intercept
                }
                val dx = ev.x - startX
                val dy = ev.y - startY
                if (abs(dx) > swipeSlop && abs(dx) > abs(dy)) {
                    // Horizontal swipe detected
                    isHorizontalSwipe = true
                    // Check if we can handle it
                    val canGoBack = webView?.canGoBack() ?: false
                    val canGoForward = webView?.canGoForward() ?: false
                    if ((dx > 0 && canGoBack) || (dx < 0 && canGoForward)) {
                        return true // We can handle this, so intercept
                    }
                }
            }
        }
        // For other cases, let the parent (SwipeRefreshLayout) decide
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!isHorizontalSwipe) {
            return super.onTouchEvent(ev)
        }

        when (ev.action) {
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - startX
                if (dx > 0) { // Swiping right (back)
                    if (webView?.canGoBack() == true) {
                        backArrow?.visibility = VISIBLE
                        forwardArrow?.visibility = GONE
                        val alpha = (dx / (swipeSlop * 2)).coerceIn(0f, 1f) // make it take longer to become fully visible
                        backArrow?.alpha = alpha
                        backArrow?.translationX = dx / 3
                    }
                } else { // Swiping left (forward)
                    if (webView?.canGoForward() == true) {
                        forwardArrow?.visibility = VISIBLE
                        backArrow?.visibility = GONE
                        val alpha = (abs(dx) / (swipeSlop * 2)).coerceIn(0f, 1f)
                        forwardArrow?.alpha = alpha
                        forwardArrow?.translationX = dx / 3
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                val dx = ev.x - startX
                if (abs(dx) > swipeSlop) {
                    if (dx > 0) {
                        webView?.let {
                            if (it.canGoBack()) {
                                it.goBack()
                            }
                        }
                    } else {
                        webView?.let {
                            if (it.canGoForward()) {
                                it.goForward()
                            }
                        }
                    }
                }
                // Reset after touch up
                hideArrows()
                isHorizontalSwipe = false
            }
            MotionEvent.ACTION_CANCEL -> {
                hideArrows()
                isHorizontalSwipe = false
            }
        }
        return true // Consume the touch event if it was a horizontal swipe
    }

    private fun hideArrows() {
        backArrow?.animate()?.alpha(0f)?.setDuration(150)?.start()
        forwardArrow?.animate()?.alpha(0f)?.setDuration(150)?.withEndAction {
            backArrow?.visibility = GONE
            forwardArrow?.visibility = GONE
            backArrow?.translationX = 0f
            forwardArrow?.translationX = 0f
        }?.start()
    }
}