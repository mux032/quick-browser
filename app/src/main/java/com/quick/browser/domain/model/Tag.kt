package com.quick.browser.domain.model

/**
 * Domain model for tags
 *
 * @property id The unique identifier for the tag
 * @property name The name of the tag
 * @property createdAt The timestamp when the tag was created
 * @property updatedAt The timestamp when the tag was last updated
 */
data class Tag(
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)