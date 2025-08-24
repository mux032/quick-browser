package com.quick.browser.util

import android.view.View
import android.view.ViewGroup
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