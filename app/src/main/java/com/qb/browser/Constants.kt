// app/src/main/java/com/qb/browser/Constants.kt
package com.qb.browser

/**
 * Centralized constants for the entire application.
 * All constants should be defined here to avoid duplication and inconsistency.
 */
object Constants {
    // Action constants
    const val ACTION_SEND = "com.qb.browser.ACTION_SEND"
    const val ACTION_CREATE_BUBBLE = "com.qb.browser.action.CREATE_BUBBLE"
    const val ACTION_OPEN_URL = "com.qb.browser.action.OPEN_URL"
    const val ACTION_CLOSE_BUBBLE = "com.qb.browser.action.CLOSE_BUBBLE"
    const val ACTION_TOGGLE_BUBBLES = "com.qb.browser.action.TOGGLE_BUBBLES"
    const val ACTION_ACTIVATE_BUBBLE = "com.qb.browser.action.ACTIVATE_BUBBLE"
    const val ACTION_SAVE_OFFLINE = "com.qb.browser.SAVE_OFFLINE"
    const val ACTION_SAVE_POSITION = "com.qb.browser.ACTION_SAVE_POSITION"

    // Intent extra constants
    const val EXTRA_URL = "extra_url"
    const val EXTRA_BUBBLE_ID = "extra_bubble_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_HTML_CONTENT = "extra_html_content"
    const val EXTRA_X = "extra_x"
    const val EXTRA_Y = "extra_y"
    const val EXTRA_IS_OFFLINE = "is_offline"
    const val EXTRA_PAGE_ID = "page_id"
    const val EXTRA_PAGE_TITLE = "page_title"

    // Font size constants
    const val MIN_FONT_SIZE = 12
    const val MAX_FONT_SIZE = 24
    const val DEFAULT_FONT_SIZE = 16

    // Theme constants
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    const val THEME_SEPIA = "sepia"

    // Notification constants
    const val NOTIFICATION_ID = 1
    const val CHANNEL_ID = "bubble_browser_channel"
    const val CHANNEL_NAME = "Bubble Browser Service"
    const val CHANNEL_DESCRIPTION = "Keeps QB Browser bubbles running"

    // Bubble position constants
    const val BUBBLE_SIZE = 64 // dp
    const val EDGE_MARGIN = 16 // dp
    const val SNAP_THRESHOLD = 32 // dp
    const val BUBBLE_SPACING = 80 // dp

    // Request codes
    const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
}