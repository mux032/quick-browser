package com.quick.browser.data.repository

import com.quick.browser.data.local.dao.TagDao
import com.quick.browser.domain.model.Tag
import com.quick.browser.domain.repository.TagRepository
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
            list.map { entityToDomain(it) }
        }
    }

    /**
     * Get a tag by its ID
     *
     * @param id The ID of the tag to retrieve
     * @return The tag or null if not found
     */
    override suspend fun getTagById(id: Long): Tag? {
        val entity = tagDao.getTagById(id)
        return entity?.let { entityToDomain(it) }
    }

    /**
     * Get a tag by its name
     *
     * @param name The name of the tag to retrieve
     * @return The tag or null if not found
     */
    override suspend fun getTagByName(name: String): Tag? {
        val entity = tagDao.getTagByName(name)
        return entity?.let { entityToDomain(it) }
    }

    /**
     * Create a new tag
     *
     * @param name The name of the tag to create
     * @return The created tag or null if creation failed
     */
    override suspend fun createTag(name: String): Tag? {
        return try {
            // Check if tag with this name already exists
            if (tagExists(name)) {
                return null
            }

            // Create new tag entity
            val tagEntity = com.quick.browser.data.local.entity.Tag(
                name = name
            )
            
            // Insert the tag entity into the database
            val id = tagDao.insertTag(tagEntity)
            
            // Return the tag with the assigned ID
            return if (id != -1L) {
                entityToDomain(tagEntity.copy(id = id))
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Update an existing tag
     *
     * @param tag The tag to update
     * @return True if the tag was updated successfully, false otherwise
     */
    override suspend fun updateTag(tag: Tag): Boolean {
        return try {
            val tagEntity = domainToEntity(tag)
            tagDao.updateTag(tagEntity)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Delete a tag
     *
     * @param tag The tag to delete
     * @return True if the tag was deleted successfully, false otherwise
     */
    override suspend fun deleteTag(tag: Tag): Boolean {
        return try {
            val tagEntity = domainToEntity(tag)
            tagDao.deleteTag(tagEntity)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Delete a tag by its ID
     *
     * @param id The ID of the tag to delete
     * @return True if the tag was deleted successfully, false otherwise
     */
    override suspend fun deleteTagById(id: Long): Boolean {
        return try {
            tagDao.deleteTagById(id)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Check if a tag with the given name already exists
     *
     * @param name The name to check
     * @return True if a tag with the given name exists, false otherwise
     */
    override suspend fun tagExists(name: String): Boolean {
        return try {
            val count = tagDao.getTagCountByName(name)
            count > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Convert a tag entity to a domain model
     *
     * @param entity The entity to convert
     * @return The domain model representation of the tag
     */
    private fun entityToDomain(entity: com.quick.browser.data.local.entity.Tag): Tag {
        return Tag(
            id = entity.id,
            name = entity.name,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * Convert a domain model to a tag entity
     *
     * @param domain The domain model to convert
     * @return The entity representation of the tag
     */
    private fun domainToEntity(domain: Tag): com.quick.browser.data.local.entity.Tag {
        return com.quick.browser.data.local.entity.Tag(
            id = domain.id,
            name = domain.name,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}