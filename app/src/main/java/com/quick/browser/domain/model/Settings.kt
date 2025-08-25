package com.quick.browser.domain.model

/**
 * Domain model for app settings
 *
 * @property id The unique identifier for the settings record
 * @property size The size of the browser bubble
 * @property animationSpeed The speed of animations in the app
 * @property savePositions Whether to save bubble positions
 * @property blockAds Whether to block ads
 * @property defaultColor The default color for the app
 * @property javascriptEnabled Whether JavaScript is enabled
 * @property darkTheme Whether dark theme is enabled
 * @property bubbleSize The size of the bubble as a float multiplier
 * @property expandedBubbleSize The size of the expanded bubble as a float multiplier
 * @property animSpeed The speed of animations as a float multiplier
 * @property saveHistory Whether to save browsing history
 * @property encryptData Whether to encrypt saved data
 * @property bubblePositionRight Whether the bubble should be positioned on the right side of the screen
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