package com.quick.browser.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.quick.browser.data.local.dao.TagDao
import com.quick.browser.data.mapper.toDomain
import com.quick.browser.data.mapper.toEntity
import com.quick.browser.domain.model.Tag
import com.quick.browser.domain.repository.TagRepository
import com.quick.browser.utils.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Repository implementation for managing tags
 *
 * This class implements the TagRepository interface and provides concrete
 * implementations for accessing and modifying tags in the database.
 * It handles conversion between entity and domain models.
 *
 * @param tagDao The DAO for accessing tags in the database
 */
class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao
) : TagRepository {

    /**
     * Get all tags as a flow
     *
     * @return A Flow of lists of tags
     */
    override fun getAllTags(): Flow<List<Tag>> {
        return tagDao.getAllTags().map { list ->
            list.map { it.toDomain() }
        }
    }

    /**
     * Get a tag by its ID
     *
     * @param id The ID of the tag to retrieve
     * @return The tag or null if not found
     */
    override suspend fun getTagById(id: Long): Tag? {
        return tagDao.getTagById(id)?.toDomain()
    }

    /**
     * Get a tag by its name
     *
     * @param name The name of the tag to retrieve
     * @return The tag or null if not found
     */
    override suspend fun getTagByName(name: String): Tag? {
        return tagDao.getTagByName(name)?.toDomain()
    }

    /**
     * Create a new tag
     *
     * @param name The name of the tag to create
     * @return The created tag or null if creation failed
     */
    override suspend fun createTag(name: String): Tag {
        val tagEntity = com.quick.browser.data.local.entity.Tag(name = name)
        return try {
            val id = tagDao.insertTag(tagEntity)
            tagEntity.copy(id = id).toDomain()
        } catch (e: SQLiteConstraintException) {
            Logger.w(TAG, "Tag with name '$name' already exists", e)
            // If the tag already exists, return the existing tag
            tagDao.getTagByName(name)!!.toDomain()
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to create tag", e)
            throw e
        }
    }

    /**
     * Update an existing tag
     *
     * @param tag The tag to update
     */
    override suspend fun updateTag(tag: Tag) {
        try {
            tagDao.updateTag(tag.toEntity())
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to update tag", e)
            throw e
        }
    }

    /**
     * Delete a tag
     *
     * @param tag The tag to delete
     */
    override suspend fun deleteTag(tag: Tag) {
        try {
            tagDao.deleteTag(tag.toEntity())
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to delete tag", e)
            throw e
        }
    }

    /**
     * Delete a tag by its ID
     *
     * @param id The ID of the tag to delete
     */
    override suspend fun deleteTagById(id: Long) {
        try {
            tagDao.deleteTagById(id)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to delete tag by ID", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "TagRepositoryImpl"
    }
}