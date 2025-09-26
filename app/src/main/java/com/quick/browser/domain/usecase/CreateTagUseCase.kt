package com.quick.browser.domain.usecase

import com.quick.browser.domain.model.Tag
import com.quick.browser.domain.repository.TagRepository
import javax.inject.Inject

/**
 * Use case for creating a new tag
 */
class CreateTagUseCase @Inject constructor(
    private val tagRepository: TagRepository
) {
    suspend operator fun invoke(name: String): Result<Tag?> {
        return try {
            val tag = tagRepository.createTag(name)
            Result.success(tag)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}