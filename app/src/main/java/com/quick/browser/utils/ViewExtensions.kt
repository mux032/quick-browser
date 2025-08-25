package com.quick.browser.utils

import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.view.children

/**
 * Recursively finds a view by its tag
 *
 * @param tag The tag to search for
 * @return The view with the specified tag, or null if not found
 */
fun View.findViewByTag(tag: String): View? {
    if (this.tag == tag) {
        return this
    }
    
    if (this is ViewGroup) {
        for (child in this.children) {
            val result = child.findViewByTag(tag)
            if (result != null) {
                return result
            }
        }
    }
    
    return null
}

/**
 * Shows a view by setting its visibility to VISIBLE
 */
fun View.show() {
    this.visibility = View.VISIBLE
}

/**
 * Hides a view by setting its visibility to GONE
 */
fun View.hide() {
    this.visibility = View.GONE
}

/**
 * Makes a view invisible by setting its visibility to INVISIBLE
 */
fun View.invisible() {
    this.visibility = View.INVISIBLE
}

/**
 * Checks if a view is visible
 *
 * @return True if the view is visible, false otherwise
 */
fun View.isVisible(): Boolean {
    return this.visibility == View.VISIBLE
}

/**
 * Checks if a view is hidden (gone)
 *
 * @return True if the view is hidden, false otherwise
 */
fun View.isGone(): Boolean {
    return this.visibility == View.GONE
}

/**
 * Toggle visibility of a view between VISIBLE and GONE
 */
fun View.toggleVisibility() {
    if (this.isVisible()) {
        this.hide()
    } else {
        this.show()
    }
}

/**
 * Set multiple views visible
 */
fun Iterable<View>.show() {
    forEach { it.show() }
}

/**
 * Set multiple views gone
 */
fun Iterable<View>.hide() {
    forEach { it.hide() }
}

/**
 * Execute a block of code when the view is laid out
 */
inline fun View.doOnLayout(crossinline action: (view: View) -> Unit) {
    if (isLaidOut && !isLayoutRequested) {
        action(this)
    } else {
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                action(this@doOnLayout)
            }
        })
    }
}

/**
 * Execute a block of code when the next layout pass is completed
 */
inline fun View.doOnNextLayout(crossinline action: (view: View) -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            viewTreeObserver.removeOnGlobalLayoutListener(this)
            action(this@doOnNextLayout)
        }
    })
}