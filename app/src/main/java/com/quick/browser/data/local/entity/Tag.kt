package com.quick.browser.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a tag for organizing saved articles
 *
 * This class defines the structure of the tags table in the database.
 * It includes information about tags that users can create to organize
 * their saved articles.
 *
 * @property id The unique identifier for the tag (primary key)
 * @property name The name of the tag
 * @property createdAt The timestamp when the tag was created
 * @property updatedAt The timestamp when the tag was last updated
 */
@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)