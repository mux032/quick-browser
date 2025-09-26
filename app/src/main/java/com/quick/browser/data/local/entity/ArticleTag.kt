package com.quick.browser.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Entity representing the relationship between articles and tags
 *
 * This class defines the structure of the article_tags table in the database.
 * It creates a many-to-many relationship between SavedArticle and Tag entities.
 *
 * @property articleUrl The URL of the article (part of composite primary key)
 * @property tagId The ID of the tag (part of composite primary key)
 */
@Entity(
    tableName = "article_tags",
    primaryKeys = ["articleUrl", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = com.quick.browser.data.local.entity.SavedArticle::class,
            parentColumns = ["url"],
            childColumns = ["articleUrl"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = com.quick.browser.data.local.entity.Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["articleUrl"]),
        Index(value = ["tagId"])
    ]
)
data class ArticleTag(
    val articleUrl: String,
    val tagId: Long
)