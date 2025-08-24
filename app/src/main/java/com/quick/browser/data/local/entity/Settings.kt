package com.quick.browser.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Entity representing app settings */
@Entity(tableName = "settings")
data class Settings(
        val size: String = "medium",
        val animationSpeed: String = "medium",
        val savePositions: Boolean = true,
        val blockAds: Boolean = true,
        val defaultColor: String = "#2196F3",
        val javascriptEnabled: Boolean = true,
        val darkTheme: Boolean = false,
        val bubbleSize: Float = 1.0f,
        val expandedBubbleSize: Float = 1.5f, // Add this line
        val animSpeed: Float = 1.0f,
        val saveHistory: Boolean = true,
        val encryptData: Boolean = true,
        val bubblePositionRight: Boolean = false,
        @PrimaryKey val id: Int = 1 // Only one settings row in the database
)
