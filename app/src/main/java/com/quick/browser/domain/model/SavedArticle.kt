package com.quick.browser.domain.model

/**
 * Domain model for saved articles
 *
 * @property url The URL of the saved article
 * @property title The title of the saved article
 * @property content The content of the saved article
 * @property savedDate The timestamp when the article was saved
 * @property author The author of the article, if available
 * @property siteName The name of the site where the article was published, if available
 * @property publishDate The date when the article was published, if available
 * @property excerpt A short excerpt or summary of the article, if available
 * @property folderId The ID of the folder this article belongs to (0 for no folder)
 */
data class SavedArticle(
    val url: String,
    val title: String,
    val content: String,
    val savedDate: Long,
    val author: String? = null,
    val siteName: String? = null,
    val publishDate: String? = null,
    val excerpt: String? = null,
    val tagId: Long = 0 // 0 means no tag
)