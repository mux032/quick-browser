package com.quick.browser.domain.model

import android.graphics.Bitmap

/**
 * Domain model for a browser bubble
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