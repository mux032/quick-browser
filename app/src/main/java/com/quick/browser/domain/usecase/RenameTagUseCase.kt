package com.quick.browser.domain.usecase

import com.quick.browser.domain.model.Tag
import com.quick.browser.domain.repository.TagRepository
import javax.inject.Inject

/**
 * Use case for renaming a tag
 */
class RenameTagUseCase @Inject constructor(
    private val tagRepository: TagRepository
) {
    suspend operator fun invoke(tag: Tag, newName: String): Result<Tag> {
        return try {
            // First check if a tag with the new name already exists
            val existingTag = tagRepository.getTagByName(newName)
            if (existingTag != null && existingTag.id != tag.id) {
                return Result.failure(Exception("A tag with this name already exists"))
            }
            
            val updatedTag = tag.copy(
                name = newName,
                updatedAt = System.currentTimeMillis()
            )
            tagRepository.updateTag(updatedTag)
            Result.success(updatedTag)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}