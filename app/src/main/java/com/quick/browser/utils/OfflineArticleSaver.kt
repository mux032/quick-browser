package com.quick.browser.utils

import android.content.Context
import android.widget.Toast
import com.quick.browser.R
import com.quick.browser.domain.repository.ArticleRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Utility class for handling offline article saving functionality
 */
class OfflineArticleSaver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ArticleRepository
) {
    
    /**
     * Save an article for offline reading
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
        scope.launch(Dispatchers.IO) {
            try {
                // Check if article is already saved
                if (repository.isArticleSaved(url)) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.article_already_saved),
                            Toast.LENGTH_SHORT
                        ).show()
                        onError(context.getString(R.string.article_already_saved))
                    }
                    return@launch
                }
                
                // Attempt to save the article by extracting readable content
                val success = repository.saveArticleByUrl(url)
                
                if (success) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.article_saved_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        onSuccess()
                    }
                } else {
                    // Extraction failed, notify the caller
                    launch(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.article_extraction_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                        onError(context.getString(R.string.article_extraction_failed))
                    }
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.article_save_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                    onError(e.message ?: context.getString(R.string.article_save_failed))
                }
            }
        }
    }
    
    /**
     * Save the original web page content as an article if extraction fails
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
        scope.launch(Dispatchers.IO) {
            try {
                // Check if article is already saved
                if (repository.isArticleSaved(url)) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.article_already_saved),
                            Toast.LENGTH_SHORT
                        ).show()
                        onError(context.getString(R.string.article_already_saved))
                    }
                    return@launch
                }
                
                // Save the original page content
                val success = repository.saveOriginalPageAsArticle(url, title, content)
                
                if (success) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.original_page_saved_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        onSuccess()
                    }
                } else {
                    launch(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.original_page_save_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                        onError(context.getString(R.string.original_page_save_failed))
                    }
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.original_page_save_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                    onError(e.message ?: context.getString(R.string.original_page_save_failed))
                }
            }
        }
    }
}