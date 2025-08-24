package com.quick.browser.domain.model

/**
 * Domain model for app settings
 */
data class Settings(
    val id: Int = 1,
    val size: String = "medium",
    val animationSpeed: String = "medium",
    val savePositions: Boolean = true,
    val blockAds: Boolean = true,
    val defaultColor: String = "#2196F3",
    val javascriptEnabled: Boolean = true,
    val darkTheme: Boolean = false,
    val bubbleSize: Float = 1.0f,
    val expandedBubbleSize: Float = 1.5f,
    val animSpeed: Float = 1.0f,
    val saveHistory: Boolean = true,
    val encryptData: Boolean = true,
    val bubblePositionRight: Boolean = false
)