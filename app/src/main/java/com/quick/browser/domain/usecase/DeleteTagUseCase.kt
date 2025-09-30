package com.quick.browser.domain.usecase

import com.quick.browser.domain.model.Tag
import com.quick.browser.domain.repository.TagRepository
import javax.inject.Inject

/**
 * Use case for deleting a tag
 */
class DeleteTagUseCase @Inject constructor(
    private val tagRepository: TagRepository
) {
    suspend operator fun invoke(tag: Tag): Result<Unit> {
        return try {
            tagRepository.deleteTag(tag)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}