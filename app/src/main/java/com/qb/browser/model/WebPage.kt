package com.qb.browser.model

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a web page in history and offline storage
 */
@Entity(tableName = "web_pages")
data class WebPage(
    @PrimaryKey
    val url: String,
    var title: String,
    var timestamp: Long,
    var content: String = "",
    var isAvailableOffline: Boolean = false,
    var visitCount: Int = 1,
    var favicon: Bitmap? = null
)
