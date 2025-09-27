package com.quick.browser.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a saved article for offline reading
 *
 * This class defines the structure of the saved articles table in the database.
 * It includes the content of articles saved for offline reading, along with
 * metadata such as author, publication date, and save date.
 *
 * @property url The URL of the article (primary key)
 * @property title The title of the article
 * @property content The content of the article
 * @property byline The author or byline of the article
 * @property siteName The name of the website where the article was published
 * @property publishDate The date when the article was published
 * @property savedDate The timestamp when the article was saved
 * @property excerpt A short excerpt or summary of the article
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