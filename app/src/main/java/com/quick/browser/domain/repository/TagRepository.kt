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
     * @return The created tag or null if creation failed
     */
    suspend fun createTag(name: String): Tag?

    /**
     * Update an existing tag
     *
     * @param tag The tag to update
     * @return True if the tag was updated successfully, false otherwise
     */
    suspend fun updateTag(tag: Tag): Boolean

    /**
     * Delete a tag
     *
     * @param tag The tag to delete
     * @return True if the tag was deleted successfully, false otherwise
     */
    suspend fun deleteTag(tag: Tag): Boolean

    /**
     * Delete a tag by its ID
     *
     * @param id The ID of the tag to delete
     * @return True if the tag was deleted successfully, false otherwise
     */
    suspend fun deleteTagById(id: Long): Boolean

    /**
     * Check if a tag with the given name already exists
     *
     * @param name The name to check
     * @return True if a tag with the given name exists, false otherwise
     */
    suspend fun tagExists(name: String): Boolean
}