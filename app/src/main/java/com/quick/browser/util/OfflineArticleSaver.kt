package com.quick.browser.util

import android.content.Context
import android.widget.Toast
import com.quick.browser.R
import com.quick.browser.data.AppDatabase
import com.quick.browser.data.SavedArticleRepository
import com.quick.browser.manager.ReadabilityExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Utility class for handling offline article saving functionality
 */
class OfflineArticleSaver(private val context: Context) {
    
    private val repository = SavedArticleRepository(
        AppDatabase.getInstance(context).savedArticleDao(),
        ReadabilityExtractor(context)
    )
    
    private val webPageDao = AppDatabase.getInstance(context).webPageDao()
    
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
                
                // Save the article
                val success = repository.saveArticle(url)
                
                // Update the WebPage's isAvailableOffline field
                if (success) {
                    webPageDao.updateOfflineStatus(url, true)
                }
                
                launch(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.article_saved_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        onSuccess()
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.article_save_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                        onError(context.getString(R.string.article_save_failed))
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
}