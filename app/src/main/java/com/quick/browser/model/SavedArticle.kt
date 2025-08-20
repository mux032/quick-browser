package com.quick.browser.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a saved article for offline reading
 */
@Entity(tableName = "saved_articles")
data class SavedArticle(
    @PrimaryKey
    val url: String,
    val title: String,
    val content: String,
    val byline: String?,
    val siteName: String?,
    val publishDate: String?,
    val savedDate: Long,
    val excerpt: String?
)