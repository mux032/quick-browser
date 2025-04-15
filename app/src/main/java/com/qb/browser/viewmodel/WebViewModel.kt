package com.qb.browser.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qb.browser.R
import com.qb.browser.service.OfflinePageService
import com.qb.browser.util.AdBlocker
import com.qb.browser.util.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * ViewModel responsible for WebView content and management
 */
class WebViewModel(private val application: Application) : AndroidViewModel(application) {
    
    private val settingsManager = SettingsManager.getInstance(application)
    private val adBlocker = AdBlocker.getInstance(application)
    
    // Maps to store page data by bubble ID
    private val progressMap = ConcurrentHashMap<String, MutableLiveData<Int>>()
    private val faviconMap = ConcurrentHashMap<String, MutableLiveData<Bitmap>>()
    private val titleMap = ConcurrentHashMap<String, MutableLiveData<String>>()
    private val allTitlesLiveData = MutableLiveData<Map<String, String>>()
    
    /**
     * Load a URL in the background for a specific bubble
     */
    private val backgroundWebViews = ConcurrentHashMap<String, WebView>()
    
    fun loadUrl(bubbleId: String, url: String) {
        viewModelScope.launch(Dispatchers.Main) {
            // Update progress to show loading
            getProgressLiveData(bubbleId).postValue(0)
            
            // Create a WebView for background loading
            val webView = WebView(application).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        favicon?.let {
                            getFaviconLiveData(bubbleId).postValue(it)
                        }
                    }
                    
                    override fun onPageFinished(view: WebView?, url: String?) {
                        getProgressLiveData(bubbleId).postValue(100)
                        view?.title?.let { title ->
                            getTitleLiveData(bubbleId).postValue(title)
                            updateAllTitles()
                        }
                    }
                }
                
                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onProgressChanged(view: WebView, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        getProgressLiveData(bubbleId).postValue(newProgress)
                    }
                    
                    override fun onReceivedIcon(view: WebView, icon: Bitmap) {
                        super.onReceivedIcon(view, icon)
                        getFaviconLiveData(bubbleId).postValue(icon)
                    }
                }
            }
            
            // Store the WebView and load the URL
            backgroundWebViews[bubbleId] = webView
            webView.loadUrl(url)
            
            // Set a default favicon until the page loads one
            val defaultIcon = BitmapFactory.decodeResource(application.resources, R.drawable.ic_globe)
            getFaviconLiveData(bubbleId).postValue(defaultIcon)
            
            // Extract domain for the title until page loads
            val domain = extractDomain(url)
            getTitleLiveData(bubbleId).postValue(domain)
            updateAllTitles()
        }
    }
    
    /**
     * Check if a resource should be blocked by ad blocker
     */
    fun shouldBlockResource(url: String): WebResourceResponse? {
        return if (settingsManager.isAdBlockingEnabled()) {
            adBlocker.shouldBlockRequest(url)
        } else {
            null
        }
    }
    
    /**
     * Close a page when its bubble is closed
     */
    fun closePage(bubbleId: String) {
        progressMap.remove(bubbleId)
        faviconMap.remove(bubbleId)
        titleMap.remove(bubbleId)
        backgroundWebViews.remove(bubbleId)?.destroy()
        updateAllTitles()
    }
    
    /**
     * Update favicon for a page
     */
    fun updateFavicon(bubbleId: String, favicon: Bitmap) {
        getFaviconLiveData(bubbleId).postValue(favicon)
    }
    
    /**
     * Update title for a page
     */
    fun updateTitle(bubbleId: String, title: String) {
        getTitleLiveData(bubbleId).postValue(title)
        updateAllTitles()
    }
    
    /**
     * Update progress for a page
     */
    fun updateProgress(bubbleId: String, progress: Int) {
        getProgressLiveData(bubbleId).postValue(progress)
    }
    
    /**
     * Get loading progress LiveData for a specific bubble
     */
    fun getLoadingProgress(bubbleId: String): LiveData<Int> {
        return getProgressLiveData(bubbleId)
    }
    
    /**
     * Get favicon LiveData for a specific bubble
     */
    fun getFavicon(bubbleId: String): LiveData<Bitmap> {
        return getFaviconLiveData(bubbleId)
    }
    
    /**
     * Get title LiveData for a specific bubble
     */
    fun getTitle(bubbleId: String): LiveData<String> {
        return getTitleLiveData(bubbleId)
    }
    
    /**
     * Get LiveData containing map of all page titles
     */
    fun getAllPageTitles(): LiveData<Map<String, String>> {
        return allTitlesLiveData
    }
    
    /**
     * Update the all titles LiveData
     */
    private fun updateAllTitles() {
        val titles = titleMap.mapValues { (_, liveData) -> liveData.value ?: "" }
                          .filter { (_, title) -> title.isNotEmpty() }
        allTitlesLiveData.postValue(titles)
    }
    
    /**
     * Get progress LiveData for a bubble, creating it if needed
     */
    private fun getProgressLiveData(bubbleId: String): MutableLiveData<Int> {
        return progressMap.getOrPut(bubbleId) { MutableLiveData(0) }
    }
    
    /**
     * Get favicon LiveData for a bubble, creating it if needed
     */
    private fun getFaviconLiveData(bubbleId: String): MutableLiveData<Bitmap> {
        return faviconMap.getOrPut(bubbleId) { MutableLiveData() }
    }
    
    /**
     * Get title LiveData for a bubble, creating it if needed
     */
    private fun getTitleLiveData(bubbleId: String): MutableLiveData<String> {
        return titleMap.getOrPut(bubbleId) { MutableLiveData("") }
    }
    
    /**
     * Extract domain from URL
     */
    private fun extractDomain(url: String): String {
        return try {
            val uri = android.net.Uri.parse(url)
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    }
    
    /**
     * Save a web page for offline reading
     */
    fun savePageForOffline(url: String, title: String) {
        // Start the service to save the page in the background
        OfflinePageService.startService(application, url, title)
    }
    
    /**
     * Save the currently displayed web page from a WebView
     * This captures the DOM content directly from the WebView
     */
    fun saveCurrentPageFromWebView(webView: WebView, url: String, title: String) {
        viewModelScope.launch {
            // Execute JavaScript to get the full HTML content
            webView.evaluateJavascript(
                "(function() { return document.documentElement.outerHTML; })();",
                { htmlContent ->
                    // The callback provides the HTML as a JSON string, need to unescape it
                    var html = htmlContent
                    if (html.startsWith("\"") && html.endsWith("\"")) {
                        html = html.substring(1, html.length - 1)
                        // Unescape the JSON string
                        html = html.replace("\\\"", "\"")
                            .replace("\\n", "\n")
                            .replace("\\t", "\t")
                            .replace("\\\\", "\\")
                    }
                    
                    // Get the base URL for resolving relative paths
                    val baseUrl = webView.url ?: url
                    
                    // Start the service with the captured HTML content
                    OfflinePageService.startServiceWithHtml(
                        application, baseUrl, title, html
                    )
                }
            )
        }
    }
}