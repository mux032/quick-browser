package com.quick.browser.utils

/**
 * Centralized constants for the entire application
 *
 * All constants should be defined here to avoid duplication and inconsistency.
 * This object contains action constants, intent extra keys, and other application-wide constants.
 */
object Constants {
    // Action constants
    /** Action to send data to the application */
    const val ACTION_SEND = "com.quick.browser.ACTION_SEND"
    
    /** Action to create a new bubble */
    const val ACTION_CREATE_BUBBLE = "com.quick.browser.action.CREATE_BUBBLE"
    
    /** Action to open a URL in a bubble */
    const val ACTION_OPEN_URL = "com.quick.browser.action.OPEN_URL"
    
    /** Action to close a bubble */
    const val ACTION_CLOSE_BUBBLE = "com.quick.browser.action.CLOSE_BUBBLE"
    
    /** Action to toggle bubble visibility */
    const val ACTION_TOGGLE_BUBBLES = "com.quick.browser.action.TOGGLE_BUBBLES"
    
    /** Action to activate a bubble */
    const val ACTION_ACTIVATE_BUBBLE = "com.quick.browser.action.ACTIVATE_BUBBLE"

    // Intent extra constants
    /** Extra key for URL data */
    const val EXTRA_URL = "extra_url"
    
    /** Extra key for bubble ID */
    const val EXTRA_BUBBLE_ID = "extra_bubble_id"
}