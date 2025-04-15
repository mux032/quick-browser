package com.qb.browser.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

/**
 * Entity representing a web page in history and offline storage
 */
@Entity(tableName = "web_pages")
data class WebPage(
    @PrimaryKey
    val url: String,
    
    var title: String,
    var timestamp: Long,
    
    // Content for offline reading
    var content: String = "",
    
    // Availability flag
    var isAvailableOffline: Boolean = false,
    
    // Visit count for most visited pages
    var visitCount: Int = 1
)
