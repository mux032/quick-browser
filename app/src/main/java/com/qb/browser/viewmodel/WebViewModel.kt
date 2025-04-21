package com.qb.browser.viewmodel

import android.webkit.WebView
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qb.browser.model.WebPage
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
                Log.e("WebViewModel", "Error loading URL for bubble $bubbleId: $url", e)
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
        viewModelScope.launch {
            val currentPage = _webPages.value[url] ?: return@launch
            updateWebPage(currentPage.copy(title = title))
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
            currentPages[url]?.let { 
                it.content += "<div>Progress updated: $progress</div>" // Placeholder logic
                currentPages[url] = it.copy(content = it.content)
            }
            _webPages.value = currentPages
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
                currentPages[url]?.let { 
                    // Update the favicon in the WebPage object
                    currentPages[url] = it.copy(favicon = favicon)
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
}