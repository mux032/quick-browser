package com.quick.browser.domain.model

/**
 * Domain model for saved articles
 */
data class SavedArticle(
    val url: String,
    val title: String,
    val content: String,
    val savedDate: Long,
    val author: String? = null,
    val siteName: String? = null,
    val publishDate: String? = null,
    val excerpt: String? = null
)