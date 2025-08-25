package com.quick.browser.presentation.ui.browser

import android.content.Context
import android.widget.Toast
import com.quick.browser.R
import com.quick.browser.domain.service.ArticleSavingService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Utility class for handling offline article saving functionality with UI feedback
 * This class handles the UI aspects of saving articles (toasts, callbacks) while
 * delegating the business logic to ArticleSavingService
 */
class OfflineArticleSaver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val articleSavingService: ArticleSavingService
) {
    
    /**
     * Save an article for offline reading with UI feedback
     *
     * @param url The URL of the article to save
     * @param scope The coroutine scope to launch the save operation
     * @param onSuccess Callback when save is successful
     * @param onError Callback when save fails
     */
    fun saveArticleForOfflineReading(
        url: String,
        scope: CoroutineScope,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        scope.launch {
            val success = articleSavingService.saveArticleForOfflineReading(
                url = url,
                scope = this,
                onSuccess = {
                    Toast.makeText(
                        context,
                        context.getString(R.string.article_saved_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    onSuccess()
                },
                onError = { errorMessage ->
                    Toast.makeText(
                        context,
                        getErrorMessage(errorMessage, R.string.article_extraction_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                    onError(errorMessage)
                }
            )
            
            if (!success) {
                // Article already exists
                Toast.makeText(
                    context,
                    context.getString(R.string.article_already_saved),
                    Toast.LENGTH_SHORT
                ).show()
                onError(context.getString(R.string.article_already_saved))
            }
        }
    }
    
    /**
     * Save the original web page content as an article if extraction fails with UI feedback
     *
     * @param url The URL of the page to save
     * @param title The title of the page
     * @param content The HTML content of the page
     * @param scope The coroutine scope to launch the save operation
     * @param onSuccess Callback when save is successful
     * @param onError Callback when save fails
     */
    fun saveOriginalPageAsArticle(
        url: String,
        title: String,
        content: String,
        scope: CoroutineScope,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        scope.launch {
            val success = articleSavingService.saveOriginalPageAsArticle(
                url = url,
                title = title,
                content = content,
                scope = this,
                onSuccess = {
                    Toast.makeText(
                        context,
                        context.getString(R.string.original_page_saved_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    onSuccess()
                },
                onError = { errorMessage ->
                    Toast.makeText(
                        context,
                        getErrorMessage(errorMessage, R.string.original_page_save_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                    onError(errorMessage)
                }
            )
            
            if (!success) {
                // Article already exists
                Toast.makeText(
                    context,
                    context.getString(R.string.article_already_saved),
                    Toast.LENGTH_SHORT
                ).show()
                onError(context.getString(R.string.article_already_saved))
            }
        }
    }
    
    private fun getErrorMessage(errorMessage: String, defaultResId: Int): String {
        return if (errorMessage == "Article already saved") {
            context.getString(R.string.article_already_saved)
        } else if (errorMessage == "Article extraction failed") {
            context.getString(R.string.article_extraction_failed)
        } else if (errorMessage == "Failed to save original page") {
            context.getString(R.string.original_page_save_failed)
        } else {
            errorMessage
        }
    }
}