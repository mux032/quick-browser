package com.quick.browser

/**
 * Centralized constants for the entire application.
 * All constants should be defined here to avoid duplication and inconsistency.
 */
object Constants {
    // Action constants
    const val ACTION_SEND = "com.quick.browser.ACTION_SEND"
    const val ACTION_CREATE_BUBBLE = "com.quick.browser.action.CREATE_BUBBLE"
    const val ACTION_OPEN_URL = "com.quick.browser.action.OPEN_URL"
    const val ACTION_CLOSE_BUBBLE = "com.quick.browser.action.CLOSE_BUBBLE"
    const val ACTION_TOGGLE_BUBBLES = "com.quick.browser.action.TOGGLE_BUBBLES"
    const val ACTION_ACTIVATE_BUBBLE = "com.quick.browser.action.ACTIVATE_BUBBLE"

    // Intent extra constants
    const val EXTRA_URL = "extra_url"
    const val EXTRA_BUBBLE_ID = "extra_bubble_id"
}