package com.quick.browser.presentation.ui.browser

import android.graphics.Bitmap
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quick.browser.domain.model.WebPage
import com.quick.browser.domain.usecase.IncrementVisitCountUseCase
import com.quick.browser.domain.usecase.SaveWebPageUseCase
import com.quick.browser.domain.usecase.UpdateOfflineStatusUseCase
import com.quick.browser.utils.ErrorHandler
import com.quick.browser.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * WebViewModel manages the web content state for bubbles in the application.
 * It handles loading URLs, managing web page states, and coordinating between
 * bubbles and their web content.
 */
@HiltViewModel
class WebViewModel @Inject constructor(
    private val saveWebPageUseCase: SaveWebPageUseCase,
    private val updateOfflineStatusUseCase: UpdateOfflineStatusUseCase,
    private val incrementVisitCountUseCase: IncrementVisitCountUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState

    /**
     * Loads a URL in the specified bubble
     * @param bubbleId The ID of the bubble
     * @param url The URL to load
     */
    fun loadUrl(bubbleId: String, url: String) {
        viewModelScope.launch {
            try {
                Logger.d("WebViewModel", "Loading URL for bubble $bubbleId: $url")

                val timestamp = System.currentTimeMillis()
                val webPage = WebPage(
                    url = url,
                    title = url,
                    timestamp = timestamp,
                    content = "",
                    isAvailableOffline = false,
                    visitCount = 1
                )

                // Set the parent bubble ID to associate this web page with the bubble
                val updatedWebPage = webPage.copy(parentBubbleId = bubbleId)

                updateWebPage(updatedWebPage)

                Logger.d("WebViewModel", "Successfully loaded URL for bubble $bubbleId: $url")
            } catch (e: Exception) {
                ErrorHandler.logError(
                    tag = "WebViewModel",
                    message = "Error loading URL for bubble $bubbleId: $url",
                    throwable = e
                )
            }
        }
    }

    /**
     * Updates the web page state
     * @param webPage The updated web page data
     */
    fun updateWebPage(webPage: WebPage) {
        viewModelScope.launch {
            try {
                Logger.d(
                    "WebViewModel",
                    "Updating web page for URL: ${webPage.url}, parentBubbleId: ${webPage.parentBubbleId}"
                )
                val currentPages = _uiState.value.webPages.toMutableMap()

                // Store the web page with a composite key that includes both URL and bubble ID
                // This ensures each bubble can have its own version of a web page
                val key = if (webPage.parentBubbleId != null) {
                    "${webPage.parentBubbleId}_${webPage.url}"
                } else {
                    webPage.url
                }

                currentPages[key] = webPage
                _uiState.value = _uiState.value.copy(webPages = currentPages)
                Logger.d("WebViewModel", "Web page updated successfully, total pages: ${currentPages.size}")
            } catch (e: Exception) {
                Logger.e("WebViewModel", "Error updating web page: ${e.message}", e)
            }
        }
    }

    /**
     * Closes a tab/web page
     * @param bubbleId The ID of the bubble whose web page should be closed
     */
    fun closeTab(bubbleId: String) {
        viewModelScope.launch {
            try {
                Logger.d("WebViewModel", "Closing tab for bubble ID: $bubbleId")
                val currentPages = _uiState.value.webPages.toMutableMap()

                // Find web pages associated with this bubble ID using the composite key format
                val pagesToRemove = currentPages.entries.filter { entry ->
                    entry.key.startsWith("${bubbleId}_") || entry.value.parentBubbleId == bubbleId
                }

                Logger.d("WebViewModel", "Found ${pagesToRemove.size} pages to remove for bubble ID: $bubbleId")

                // Remove the pages
                pagesToRemove.forEach { entry ->
                    currentPages.remove(entry.key)
                    Logger.d("WebViewModel", "Removed web page with key: ${entry.key}")
                }

                _uiState.value = _uiState.value.copy(webPages = currentPages)
                Logger.d("WebViewModel", "Closed tab for bubble ID: $bubbleId, remaining pages: ${currentPages.size}")
            } catch (e: Exception) {
                Logger.e("WebViewModel", "Error closing tab for bubble ID: $bubbleId", e)
            }
        }
    }

    /**
     * Updates the title of a web page
     * @param url The URL of the page
     * @param title The new title
     */
    fun updateTitle(url: String, title: String) {
        // Don't update if title is empty or same as URL (which means no real title was extracted)
        if (title.isEmpty() || title == url) {
            Logger.d("WebViewModel", "Skipping title update for $url - title is empty or same as URL")
            return
        }

        viewModelScope.launch {
            try {
                val currentPages = _uiState.value.webPages.toMutableMap()

                // Find all pages with this URL (might be multiple if opened in different bubbles)
                val matchingPages = currentPages.entries.filter {
                    it.value.url == url || it.key.endsWith("_$url")
                }

                if (matchingPages.isNotEmpty()) {
                    // Update all matching pages
                    matchingPages.forEach { entry ->
                        val currentPage = entry.value
                        // Only update if the current title is the URL or empty
                        if (currentPage.title == url || currentPage.title.isEmpty()) {
                            Logger.d("WebViewModel", "Updating title for $url from '${currentPage.title}' to '$title'")
                            val updatedPage = currentPage.copy(title = title)
                            currentPages[entry.key] = updatedPage
                        } else {
                            Logger.d(
                                "WebViewModel",
                                "Skipping title update - page already has a title: ${currentPage.title}"
                            )
                        }
                    }
                    _uiState.value = _uiState.value.copy(webPages = currentPages)
                } else {
                    // If the page doesn't exist yet, create it with the title
                    Logger.d("WebViewModel", "Creating new page with title: $title for URL: $url")
                    val webPage = WebPage(
                        url = url,
                        title = title,
                        timestamp = System.currentTimeMillis(),
                        content = "",
                        isAvailableOffline = false,
                        visitCount = 1
                    )
                    // We don't know the bubble ID here, but we'll set it when loadUrl is called
                    currentPages[url] = webPage
                    _uiState.value = _uiState.value.copy(webPages = currentPages)
                }
            } catch (e: Exception) {
                Logger.e("WebViewModel", "Error updating title for URL: $url", e)
            }
        }
    }

    /**
     * Updates the progress of a web page
     * @param url The URL of the web page
     * @param progress The progress value (0-100)
     */
    fun updateProgress(url: String, progress: Int) {
        viewModelScope.launch {
            try {
                val currentPages = _uiState.value.webPages.toMutableMap()

                // Find all pages with this URL (might be multiple if opened in different bubbles)
                val matchingPages = currentPages.entries.filter {
                    it.value.url == url || it.key.endsWith("_$url")
                }

                matchingPages.forEach { entry ->
                    val currentPage = entry.value
                    val updatedContent = currentPage.content + "<div>Progress updated: $progress</div>" // Placeholder logic
                    val updatedPage = currentPage.copy(content = updatedContent)
                    currentPages[entry.key] = updatedPage
                }

                _uiState.value = _uiState.value.copy(webPages = currentPages)
            } catch (e: Exception) {
                Logger.e("WebViewModel", "Error updating progress for URL: $url", e)
            }
        }
    }

    /**
     * Updates the favicon of a web page
     * @param url The URL of the web page
     * @param favicon The new favicon bitmap
     * @param bubbleId The ID of the bubble (optional)
     */
    fun updateFavicon(url: String, favicon: Bitmap, bubbleId: String? = null) {
        viewModelScope.launch {
            try {
                Logger.d("WebViewModel", "Updating favicon for URL: $url, bubbleId: $bubbleId, favicon dimensions: ${favicon.width}x${favicon.height}")
                val currentPages = _uiState.value.webPages.toMutableMap()

                // Find all pages with this URL (might be multiple if opened in different bubbles)
                val matchingPages = if (bubbleId != null) {
                    // If bubbleId is provided, only update pages for that bubble
                    currentPages.entries.filter {
                        (it.value.url == url && it.value.parentBubbleId == bubbleId) ||
                                it.key == "${bubbleId}_$url"
                    }
                } else {
                    // Otherwise check all pages with this URL
                    currentPages.entries.filter {
                        it.value.url == url || it.key.endsWith("_$url")
                    }
                }

                if (matchingPages.isNotEmpty()) {
                    // Update all matching pages
                    matchingPages.forEach { entry ->
                        val currentPage = entry.value
                        // Update the favicon in the WebPage object
                        val updatedPage = currentPage.copy(favicon = favicon)
                        currentPages[entry.key] = updatedPage
                        Logger.d("WebViewModel", "Updated favicon for existing page with key: ${entry.key}")
                    }
                    Logger.d("WebViewModel", "Favicon updated successfully for ${matchingPages.size} pages with URL: $url")
                } else {
                    // If the page doesn't exist yet, create it with the favicon
                    val webPage = WebPage(
                        url = url,
                        title = url,
                        timestamp = System.currentTimeMillis(),
                        content = "",
                        isAvailableOffline = false,
                        visitCount = 1,
                        favicon = favicon,
                        parentBubbleId = bubbleId
                    )
                    
                    // Set the parent bubble ID if provided
                    val key = if (bubbleId != null) {
                        "${bubbleId}_$url"
                    } else {
                        url
                    }
                    
                    currentPages[key] = webPage
                    Logger.d("WebViewModel", "Created new page with favicon for URL: $url, bubbleId: $bubbleId, stored with key: $key")
                }
                _uiState.value = _uiState.value.copy(webPages = currentPages)
                Logger.d("WebViewModel", "WebViewModel now has ${currentPages.size} web pages")
            } catch (e: Exception) {
                Logger.e("WebViewModel", "Error updating favicon for URL: $url, bubbleId: $bubbleId", e)
            }
        }
    }


    suspend fun evaluateHtml(webView: WebView): String = suspendCancellableCoroutine { cont ->
        webView.evaluateJavascript("document.body.innerHTML") { html ->
            cont.resume(html ?: "")
        }
    }

    /**
     * Saves the current page content from WebView for offline use
     * @param webView The WebView instance
     * @param url The URL of the page
     * @param title The title of the page
     */
    fun saveCurrentPageFromWebView(webView: WebView, url: String, title: String, bubbleId: String? = null) {
        viewModelScope.launch {
            try {
                val content = withContext(Dispatchers.Main) {
                    evaluateHtml(webView)
                }

                val webPage = WebPage(
                    url = url,
                    title = title,
                    timestamp = System.currentTimeMillis(),
                    content = content,
                    isAvailableOffline = true,
                    visitCount = 1,
                    parentBubbleId = bubbleId
                )

                saveWebPageUseCase(webPage).also { result ->
                    if (result is com.quick.browser.domain.result.Result.Failure<*>) {
                        Logger.e("WebViewModel", "Failed to save web page: ${result.error}")
                    }
                }
                updateWebPage(webPage)
                Logger.d("WebViewModel", "Saved page content from WebView for URL: $url, bubbleId: $bubbleId")
            } catch (e: Exception) {
                Logger.e("WebViewModel", "Error saving page content from WebView: ${e.message}", e)
            }
        }
    }

    // In-memory cache for summaries since they're not stored in the database
    private val summaryCache = mutableMapOf<String, List<String>>()

    /**
     * Updates the summary for a web page
     * @param url The URL of the page
     * @param summary The list of summary points
     */
    fun updateSummary(url: String, summary: List<String>, bubbleId: String? = null) {
        viewModelScope.launch {
            try {
                Logger.d("WebViewModel", "Updating summary for URL: $url, bubbleId: $bubbleId")

                // Store in the in-memory cache
                summaryCache[url] = summary

                // Update the WebPage objects if they exist
                val currentPages = _uiState.value.webPages.toMutableMap()

                // Find all pages with this URL (might be multiple if opened in different bubbles)
                val matchingPages = if (bubbleId != null) {
                    // If bubbleId is provided, only update pages for that bubble
                    currentPages.entries.filter {
                        (it.value.url == url && it.value.parentBubbleId == bubbleId) ||
                                it.key == "${bubbleId}_$url"
                    }
                } else {
                    // Otherwise update all pages with this URL
                    currentPages.entries.filter {
                        it.value.url == url || it.key.endsWith("_$url")
                    }
                }

                if (matchingPages.isNotEmpty()) {
                    // Update all matching pages
                    matchingPages.forEach { entry ->
                        val currentPage = entry.value
                        // Create a new instance with the summary
                        val updatedPage = currentPage.copy(summary = summary)
                        currentPages[entry.key] = updatedPage
                    }
                    _uiState.value = _uiState.value.copy(webPages = currentPages)
                    Logger.d("WebViewModel", "Summary updated successfully for ${matchingPages.size} pages with URL: $url")
                } else {
                    // If the page doesn't exist yet, create it with the summary
                    val webPage = WebPage(
                        url = url,
                        title = url,
                        timestamp = System.currentTimeMillis(),
                        content = "",
                        isAvailableOffline = false,
                        visitCount = 1,
                        summary = summary,
                        parentBubbleId = bubbleId
                    )

                    // Set the parent bubble ID if provided
                    if (bubbleId != null) {
                        currentPages["${bubbleId}_$url"] = webPage
                    } else {
                        currentPages[url] = webPage
                    }

                    _uiState.value = _uiState.value.copy(webPages = currentPages)
                    Logger.d("WebViewModel", "Created new page with summary for URL: $url")
                }
            } catch (e: Exception) {
                Logger.e("WebViewModel", "Error updating summary for URL: $url", e)
            }
        }
    }

    /**
     * Gets the summary for a web page if available
     * @param url The URL of the page
     * @return The list of summary points or null if not available
     */
    fun getSummary(url: String, bubbleId: String? = null): List<String>? {
        try {
            // First check the in-memory cache
            val cachedSummary = summaryCache[url]
            if (cachedSummary != null && cachedSummary.isNotEmpty()) {
                return cachedSummary
            }

            // Then check all WebPage objects with this URL
            val currentPages = _uiState.value.webPages

            // Find matching pages
            val matchingPages = if (bubbleId != null) {
                // If bubbleId is provided, only check pages for that bubble
                currentPages.entries.filter {
                    (it.value.url == url && it.value.parentBubbleId == bubbleId) ||
                            it.key == "${bubbleId}_$url"
                }
            } else {
                // Otherwise check all pages with this URL
                currentPages.entries.filter {
                    it.value.url == url || it.key.endsWith("_$url")
                }
            }

            // Return the first non-empty summary found
            for (entry in matchingPages) {
                val pageSummary = entry.value.summary
                if (pageSummary.isNotEmpty()) {
                    // Update the cache for future use
                    summaryCache[url] = pageSummary
                    return pageSummary
                }
            }

            return null
        } catch (e: Exception) {
            Logger.e("WebViewModel", "Error retrieving summary for URL: $url, bubbleId: $bubbleId", e)
            return null
        }
    }
}