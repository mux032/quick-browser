package com.quick.browser.domain.model

import android.graphics.Bitmap

/**
 * Domain model for a browser bubble
 *
 * @property id The unique identifier for the bubble
 * @property url The URL currently loaded in the bubble
 * @property title The title of the page currently loaded in the bubble
 * @property x The x-coordinate position of the bubble on screen
 * @property y The y-coordinate position of the bubble on screen
 * @property width The width of the bubble
 * @property height The height of the bubble
 * @property isExpanded Whether the bubble is currently expanded
 * @property isPinned Whether the bubble is pinned (cannot be closed)
 * @property createdAt The timestamp when the bubble was created
 * @property favicon The favicon for the current page, if available
 */
data class Bubble(
    val id: String,
    val url: String,
    val title: String = "",
    val x: Int = 0,
    val y: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val isExpanded: Boolean = false,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val favicon: Bitmap? = null
)