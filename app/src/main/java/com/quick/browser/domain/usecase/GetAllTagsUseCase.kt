package com.quick.browser.domain.usecase

import com.quick.browser.domain.model.Tag
import com.quick.browser.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting all tags
 */
class GetAllTagsUseCase @Inject constructor(
    private val tagRepository: TagRepository
) {
    operator fun invoke(): Flow<List<Tag>> {
        return tagRepository.getAllTags()
    }
}