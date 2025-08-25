package com.quick.browser.utils

import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.view.children

/**
 * Recursively finds a view by its tag
 *
 * This extension function searches through the view hierarchy to find
 * a view with the specified tag. It performs a depth-first search,
 * checking the current view first, then recursively searching through
 * all child views if the current view is a ViewGroup.
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
 *
 * This makes the view visible and takes up space in the layout.
 */
fun View.show() {
    this.visibility = View.VISIBLE
}

/**
 * Hides a view by setting its visibility to GONE
 *
 * This makes the view invisible and does not take up space in the layout.
 */
fun View.hide() {
    this.visibility = View.GONE
}

/**
 * Makes a view invisible by setting its visibility to INVISIBLE
 *
 * This makes the view invisible but still takes up space in the layout.
 */
fun View.invisible() {
    this.visibility = View.INVISIBLE
}

/**
 * Checks if a view is visible
 *
 * @return True if the view is visible (visibility == VISIBLE), false otherwise
 */
fun View.isVisible(): Boolean {
    return this.visibility == View.VISIBLE
}

/**
 * Checks if a view is hidden (gone)
 *
 * @return True if the view is hidden (visibility == GONE), false otherwise
 */
fun View.isGone(): Boolean {
    return this.visibility == View.GONE
}

/**
 * Toggle visibility of a view between VISIBLE and GONE
 *
 * If the view is currently visible, it will be hidden.
 * If the view is currently hidden or invisible, it will be made visible.
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
 *
 * This extension function applies the show() method to all views
 * in the iterable collection.
 */
fun Iterable<View>.show() {
    forEach { it.show() }
}

/**
 * Set multiple views gone
 *
 * This extension function applies the hide() method to all views
 * in the iterable collection.
 */
fun Iterable<View>.hide() {
    forEach { it.hide() }
}

/**
 * Execute a block of code when the view is laid out
 *
 * This extension function executes the provided action immediately if
 * the view is already laid out, or waits for the next layout pass
 * to execute the action.
 *
 * @param action The block of code to execute when the view is laid out
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
 *
 * This extension function waits for the next layout pass to execute
 * the provided action, regardless of whether the view is already laid out.
 *
 * @param action The block of code to execute when the next layout pass is completed
 */
inline fun View.doOnNextLayout(crossinline action: (view: View) -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            viewTreeObserver.removeOnGlobalLayoutListener(this)
            action(this@doOnNextLayout)
        }
    })
}