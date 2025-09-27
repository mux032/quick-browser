package com.quick.browser.domain.repository

import com.quick.browser.domain.model.Tag
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing tags
 *
 * This interface defines the contract for tag-related operations.
 * It provides methods for accessing and modifying tags in the data layer.
 */
interface TagRepository {

    /**
     * Get all tags as a flow
     *
     * @return A Flow of lists of tags
     */
    fun getAllTags(): Flow<List<Tag>>

    /**
     * Get a tag by its ID
     *
     * @param id The ID of the tag to retrieve
     * @return The tag or null if not found
     */
    suspend fun getTagById(id: Long): Tag?

    /**
     * Get a tag by its name
     *
     * @param name The name of the tag to retrieve
     * @return The tag or null if not found
     */
    suspend fun getTagByName(name: String): Tag?

    /**
     * Create a new tag
     *
     * @param name The name of the tag to create
     * @return The created tag
     */
    suspend fun createTag(name: String): Tag

    /**
     * Update an existing tag
     *
     * @param tag The tag to update
     */
    suspend fun updateTag(tag: Tag)

    /**
     * Delete a tag
     *
     * @param tag The tag to delete
     */
    suspend fun deleteTag(tag: Tag)

    /**
     * Delete a tag by its ID
     *
     * @param id The ID of the tag to delete
     */
    suspend fun deleteTagById(id: Long)
}