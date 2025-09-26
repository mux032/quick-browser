package com.quick.browser.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.quick.browser.data.local.entity.Tag
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Tag entities
 *
 * This interface provides methods for accessing and modifying tags in the database.
 * It includes operations for inserting, updating, deleting, and querying tags.
 */
@Dao
interface TagDao {

    /**
     * Get all tags ordered by name
     *
     * @return A Flow of lists of tags ordered by name
     */
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<Tag>>

    /**
     * Get a tag by its ID
     *
     * @param id The ID of the tag to retrieve
     * @return The tag or null if not found
     */
    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getTagById(id: Long): Tag?

    /**
     * Get a tag by its name
     *
     * @param name The name of the tag to retrieve
     * @return The tag or null if not found
     */
    @Query("SELECT * FROM tags WHERE name = :name")
    suspend fun getTagByName(name: String): Tag?

    /**
     * Insert a new tag
     *
     * @param tag The tag to insert
     * @return The ID of the inserted tag
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: Tag): Long

    /**
     * Update an existing tag
     *
     * @param tag The tag to update
     */
    @Update
    suspend fun updateTag(tag: Tag)

    /**
     * Delete a tag
     *
     * @param tag The tag to delete
     */
    @Delete
    suspend fun deleteTag(tag: Tag)

    /**
     * Delete a tag by its ID
     *
     * @param id The ID of the tag to delete
     */
    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTagById(id: Long)

    /**
     * Get the count of tags with a specific name
     *
     * @param name The name to check
     * @return The count of tags with the specified name
     */
    @Query("SELECT COUNT(*) FROM tags WHERE name = :name")
    suspend fun getTagCountByName(name: String): Int
}