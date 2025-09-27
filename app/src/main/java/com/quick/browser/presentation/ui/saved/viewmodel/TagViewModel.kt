package com.quick.browser.presentation.ui.saved.viewmodel

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quick.browser.domain.model.Tag
import com.quick.browser.domain.usecase.CreateTagUseCase
import com.quick.browser.domain.usecase.DeleteTagUseCase
import com.quick.browser.domain.usecase.GetAllTagsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing tags in saved articles
 *
 * This ViewModel handles tag-related operations such as creating, updating,
 * deleting, and retrieving tags. It exposes UI state through StateFlow
 * and provides methods for UI interactions.
 */
@HiltViewModel
class TagViewModel @Inject constructor(
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val createTagUseCase: CreateTagUseCase,
    private val deleteTagUseCase: DeleteTagUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagUiState())
    val uiState: StateFlow<TagUiState> = _uiState

    init {
        loadTags()
    }

    /**
     * Load all tags from the repository
     */
    private fun loadTags() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                getAllTagsUseCase().collectLatest { tags ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        tags = tags,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load tags: ${e.message}"
                )
            }
        }
    }

    /**
     * Create a new tag
     *
     * @param name The name of the tag to create
     */
    fun createTag(name: String) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _uiState.value = _uiState.value.copy(error = "Tag name cannot be empty")
                return@launch
            }

            createTagUseCase(name).onSuccess {
                _uiState.value = _uiState.value.copy(successMessage = "Tag '$name' created successfully")
            }.onFailure { exception ->
                when (exception) {
                    is SQLiteConstraintException -> _uiState.value = _uiState.value.copy(error = "A tag with this name already exists")
                    else -> _uiState.value = _uiState.value.copy(error = "Error creating tag: ${exception.message}")
                }
            }
        }
    }

    /**
     * Delete a tag
     *
     * @param tag The tag to delete
     */
    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            deleteTagUseCase(tag).onSuccess {
                _uiState.value = _uiState.value.copy(successMessage = "Tag '${tag.name}' deleted successfully")
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(error = "Error deleting tag: ${exception.message}")
            }
        }
    }

    /**
     * Clear any success or error messages
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            error = null
        )
    }
}

/**
 * UI state for the TagViewModel
 *
 * @property isLoading Whether tags are currently being loaded
 * @property tags The list of tags
 * @property error Any error message
 * @property successMessage Any success message
 */
data class TagUiState(
    val isLoading: Boolean = false,
    val tags: List<Tag> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)