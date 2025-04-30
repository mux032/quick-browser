package com.qb.browser.viewmodel

import android.webkit.WebView
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qb.browser.model.WebPage
import com.qb.browser.util.ErrorHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.webkit.ValueCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * WebViewModel manages the web content state for bubbles in the application.
 * It handles loading URLs, managing web page states, and coordinating between
 * bubbles and their web content.
 */
class WebViewModel : ViewModel() {
    private val _webPages = MutableStateFlow<Map<String, WebPage>>(emptyMap())
    val webPages: StateFlow<Map<String, WebPage>> = _webPages

    /**
     * Loads a URL in the specified bubble
     * @param bubbleId The ID of the bubble
     * @param url The URL to load
     */
    fun loadUrl(bubbleId: String, url: String) {
        viewModelScope.launch {
            try {
                Log.d("WebViewModel", "Loading URL for bubble $bubbleId: $url")
                
                val timestamp = System.currentTimeMillis()
                val webPage = WebPage(
                    url = url,
                    title = url,
                    timestamp = timestamp,
                    content = "",
                    isAvailableOffline = false,
                    visitCount = 1
                )
                
                updateWebPage(webPage)
                
                Log.d("WebViewModel", "Successfully loaded URL for bubble $bubbleId: $url")
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
            val currentPages = _webPages.value.toMutableMap()
            currentPages[webPage.url] = webPage
            _webPages.value = currentPages
        }
    }

    /**
     * Closes a tab/web page
     * @param bubbleId The ID of the bubble whose web page should be closed
     */
    fun closeTab(bubbleId: String) {
        viewModelScope.launch {
            val timestamp = bubbleId.toLongOrNull() ?: return@launch
            val currentPages = _webPages.value.toMutableMap()
            currentPages.entries.removeIf { it.value.timestamp == timestamp }
            _webPages.value = currentPages
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
            Log.d("WebViewModel", "Skipping title update for $url - title is empty or same as URL")
            return
        }
        
        viewModelScope.launch {
            try {
                val currentPages = _webPages.value.toMutableMap()
                val currentPage = currentPages[url]
                
                if (currentPage != null) {
                    // Only update if the current title is the URL or empty
                    if (currentPage.title == url || currentPage.title.isEmpty()) {
                        Log.d("WebViewModel", "Updating title for $url from '${currentPage.title}' to '$title'")
                        val updatedPage = currentPage.copy(title = title)
                        currentPage.copyTransientFields(updatedPage)
                        currentPages[url] = updatedPage
                        _webPages.value = currentPages
                    } else {
                        Log.d("WebViewModel", "Skipping title update - page already has a title: ${currentPage.title}")
                    }
                } else {
                    // If the page doesn't exist yet, create it with the title
                    Log.d("WebViewModel", "Creating new page with title: $title for URL: $url")
                    val webPage = WebPage(
                        url = url,
                        title = title,
                        timestamp = System.currentTimeMillis(),
                        content = "",
                        isAvailableOffline = false,
                        visitCount = 1
                    )
                    currentPages[url] = webPage
                    _webPages.value = currentPages
                }
            } catch (e: Exception) {
                Log.e("WebViewModel", "Error updating title for URL: $url", e)
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
            val currentPages = _webPages.value.toMutableMap()
            currentPages[url]?.let { currentPage -> 
                // Just update the transient progress field if needed
                currentPage.progress = progress
                
                // No need to modify content for progress updates
                // Don't create a new instance for just progress updates
                // as it's a transient field and doesn't need to trigger state updates
            }
        }
    }

    /**
     * Updates the favicon of a web page
     * @param url The URL of the web page
     * @param favicon The new favicon bitmap
     */
    fun updateFavicon(url: String, favicon: Bitmap) {
        viewModelScope.launch {
            try {
                Log.d("WebViewModel", "Updating favicon for URL: $url")
                val currentPages = _webPages.value.toMutableMap()
                currentPages[url]?.let { currentPage -> 
                    // Update the favicon in the WebPage object
                    val updatedPage = currentPage.copy(favicon = favicon)
                    currentPage.copyTransientFields(updatedPage)
                    currentPages[url] = updatedPage
                    Log.d("WebViewModel", "Favicon updated successfully for URL: $url")
                } ?: run {
                    // If the page doesn't exist yet, create it with the favicon
                    val webPage = WebPage(
                        url = url,
                        title = url,
                        timestamp = System.currentTimeMillis(),
                        content = "",
                        isAvailableOffline = false,
                        visitCount = 1,
                        favicon = favicon
                    )
                    currentPages[url] = webPage
                    Log.d("WebViewModel", "Created new page with favicon for URL: $url")
                }
                _webPages.value = currentPages
            } catch (e: Exception) {
                Log.e("WebViewModel", "Error updating favicon for URL: $url", e)
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
    fun saveCurrentPageFromWebView(webView: WebView, url: String, title: String) {
        viewModelScope.launch {
            val content = withContext(Dispatchers.Main) {
                evaluateHtml(webView)
            }
    
            val webPage = WebPage(
                url = url,
                title = title,
                timestamp = System.currentTimeMillis(),
                content = content,
                isAvailableOffline = true,
                visitCount = 1
            )
    
            updateWebPage(webPage)
        }
    }
    
    // In-memory cache for summaries since they're not stored in the database
    private val summaryCache = mutableMapOf<String, List<String>>()
    
    /**
     * Updates the summary for a web page
     * @param url The URL of the page
     * @param summary The list of summary points
     */
    fun updateSummary(url: String, summary: List<String>) {
        viewModelScope.launch {
            try {
                Log.d("WebViewModel", "Updating summary for URL: $url")
                
                // Store in the in-memory cache
                summaryCache[url] = summary
                
                // Update the WebPage object if it exists
                val currentPages = _webPages.value.toMutableMap()
                val currentPage = currentPages[url]
                
                if (currentPage != null) {
                    // Create a new instance with the summary
                    val updatedPage = currentPage.copy()
                    currentPage.copyTransientFields(updatedPage)
                    updatedPage.summary = summary
                    currentPages[url] = updatedPage
                    _webPages.value = currentPages
                    Log.d("WebViewModel", "Summary updated successfully for URL: $url")
                } else {
                    // If the page doesn't exist yet, create it with the summary
                    val webPage = WebPage(
                        url = url,
                        title = url,
                        timestamp = System.currentTimeMillis(),
                        content = "",
                        isAvailableOffline = false,
                        visitCount = 1
                    )
                    webPage.summary = summary
                    currentPages[url] = webPage
                    _webPages.value = currentPages
                    Log.d("WebViewModel", "Created new page with summary for URL: $url")
                }
            } catch (e: Exception) {
                Log.e("WebViewModel", "Error updating summary for URL: $url", e)
            }
        }
    }
    
    /**
     * Gets the summary for a web page if available
     * @param url The URL of the page
     * @return The list of summary points or null if not available
     */
    fun getSummary(url: String): List<String>? {
        try {
            // First check the in-memory cache
            val cachedSummary = summaryCache[url]
            if (cachedSummary != null && cachedSummary.isNotEmpty()) {
                return cachedSummary
            }
            
            // Then check the WebPage object
            val pageSummary = _webPages.value[url]?.summary
            if (pageSummary != null && pageSummary.isNotEmpty()) {
                // Update the cache for future use
                summaryCache[url] = pageSummary
                return pageSummary
            }
            
            return null
        } catch (e: Exception) {
            Log.e("WebViewModel", "Error retrieving summary for URL: $url", e)
            return null
        }
    }
}