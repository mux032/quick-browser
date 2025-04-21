package com.qb.browser.util

import android.content.Context
import android.content.Intent
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LifecycleCoroutineScope
import java.util.regex.Pattern
import com.qb.browser.Constants
import com.qb.browser.db.WebPageDao
import com.qb.browser.manager.BubbleManager
import com.qb.browser.model.WebPage
import com.qb.browser.service.BubbleService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

/**
 * BubbleIntentProcessor handles the processing of intents received by the BubbleService. It
 * extracts URLs from various types of sharing intents and processes them accordingly.
 */
class BubbleIntentProcessor(
        private val context: Context,
        private val bubbleManager: BubbleManager,
        private val webPageDao: WebPageDao,
        private val lifecycleScope: LifecycleCoroutineScope
) {
    companion object {
        private const val TAG = "BubbleIntentProcessor"
    }
    
    fun processIntent(intent: Intent) {
        try {
            Log.e(TAG, "processIntent | Received intent: ${intent.action}, data: ${intent.extras}")
            when (intent.action) {
                Constants.ACTION_CREATE_BUBBLE -> handleCreateBubble(intent)
                Constants.ACTION_OPEN_URL -> handleOpenUrl(intent)
                Constants.ACTION_CLOSE_BUBBLE -> handleCloseBubble(intent)
                Constants.ACTION_TOGGLE_BUBBLES -> handleToggleBubbles()
                Constants.ACTION_ACTIVATE_BUBBLE -> handleActivateBubble(intent)
                Constants.ACTION_SAVE_OFFLINE -> handleSaveOffline(intent)
                Constants.ACTION_SAVE_POSITION -> handleSavePosition(intent)
                Intent.ACTION_SEND -> handleSharedContent(intent)
                Intent.ACTION_VIEW -> handleViewAction(intent)
                else -> Log.w(TAG, "Unsupported intent action: ${intent.action}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing intent", e)
        }
    }

    private fun handleCreateBubble(intent: Intent) {
        val sharedUrl = intent.getStringExtra(Constants.EXTRA_URL)
        Log.e(TAG, "handleCreateBubble | Received intent: ${intent.action}, sharedURL: ${sharedUrl}")
        
        if (sharedUrl != null && isValidUrl(sharedUrl)) {
            Log.e(TAG, "Creating bubble with URL: $sharedUrl")
            
            // Save to history
            saveToHistory(sharedUrl)
            
            // Create a new bubble with the shared URL
            bubbleManager.createOrUpdateBubbleWithNewUrl(sharedUrl)
        } else {
            Log.e(TAG, "No valid URL provided.")
        }
    }
    
    /**
     * Saves a URL to the browsing history
     */
    private fun saveToHistory(url: String) {
        lifecycleScope.launch {
            try {
                // Check if the page already exists in history
                val existingPage = webPageDao.getPageByUrl(url)
                
                if (existingPage != null) {
                    // Update existing page timestamp and increment visit count
                    val updatedPage = existingPage.copy(
                        timestamp = System.currentTimeMillis(),
                        visitCount = existingPage.visitCount + 1
                    )
                    webPageDao.updatePage(updatedPage)
                    webPageDao.incrementVisitCount(url)
                    Log.d(TAG, "Updated existing page in history: $url")
                } else {
                    // Try to get the title from HTML asynchronously
                    val htmlTitle = try {
                        extractTitleFromHtmlAsync(url)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error extracting HTML title asynchronously", e)
                        null
                    }
                    
                    // Create new history entry with the best available title
                    val title = htmlTitle ?: extractTitleFromUrl(url)
                    
                    val newPage = WebPage(
                        url = url,
                        title = title,
                        timestamp = System.currentTimeMillis(),
                        visitCount = 1
                    )
                    webPageDao.insertPage(newPage)
                    Log.d(TAG, "Added new page to history with title '$title': $url")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving to history", e)
            }
        }
    }
    
    private fun handleOpenUrl(intent: Intent) {
        val url = intent.getStringExtra(Constants.EXTRA_URL)
        if (url != null && isValidUrl(url)) {
            // Save to history
            saveToHistory(url)
            
            bubbleManager.createOrUpdateBubbleWithNewUrl(url)
        } else {
            Log.w(TAG, "Invalid or missing URL for handleOpenUrl")
        }
    }

    private fun handleCloseBubble(intent: Intent) {
        val bubbleId = intent.getStringExtra(Constants.EXTRA_BUBBLE_ID)
        if (bubbleId != null) {
            bubbleManager.removeBubble(bubbleId)
        }
        Log.d(TAG, "Bubble closed via intent")
    }

    private fun handleActivateBubble(intent: Intent) {
        val bubbleId = intent.getStringExtra(Constants.EXTRA_BUBBLE_ID)
        if (bubbleId != null) {
            val currentBubbles = bubbleManager.bubbles.value
            val bubble = currentBubbles[bubbleId]
            
            if (bubble != null) {
                // The BubbleView now handles expansion directly
                // Just log that the bubble was activated
                Log.d(TAG, "Bubble activated with ID: $bubbleId, URL: ${bubble.url}")
            } else {
                Log.w(TAG, "No bubble found to activate with ID: $bubbleId")
            }
        } else {
            Log.w(TAG, "No bubble ID provided in handleActivateBubble")
        }
    }

    private fun handleToggleBubbles() {
        // Optional: implement show/hide behavior if needed. Here’s a basic toggle logic idea.
        val bubbles = bubbleManager.bubbles.value
        if (bubbles.isNotEmpty()) {
            // We now have multiple bubbles, so we'll just log this action
            // The main bubble will show all bubbles in its expanded state
            Log.d(TAG, "Toggle bubbles requested with ${bubbles.size} bubbles")
        } else {
            Log.d(TAG, "Toggle bubbles requested, but no bubbles exist.")
        }
    }

    private fun handleSaveOffline(intent: Intent) {
        val url = intent.getStringExtra(Constants.EXTRA_URL)
        if (url != null && isValidUrl(url)) {
            // Save to DB for offline availability
            lifecycleScope.launch {
                try {
                    val existingPage = webPageDao.getPageByUrl(url)
                    
                    if (existingPage != null) {
                        // Use existing page but mark as available offline
                        val pageToSave = existingPage.copy(isAvailableOffline = true)
                        webPageDao.insertPage(pageToSave)
                        Log.d(TAG, "Existing page marked for offline: $url")
                    } else {
                        // Try to get the title from HTML asynchronously
                        val htmlTitle = try {
                            extractTitleFromHtmlAsync(url)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error extracting HTML title for offline page", e)
                            null
                        }
                        
                        // Create new page with the best available title
                        val title = htmlTitle ?: extractTitleFromUrl(url)
                        
                        val pageToSave = WebPage(
                            url = url,
                            title = title,
                            timestamp = System.currentTimeMillis(),
                            content = "", // Can be filled with offline HTML content
                            isAvailableOffline = true,
                            visitCount = 1
                        )
                        webPageDao.insertPage(pageToSave)
                        Log.d(TAG, "New page saved for offline with title '$title': $url")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving page for offline access", e)
                }
            }
        } else {
            Log.w(TAG, "Invalid URL or null passed to handleSaveOffline")
        }
    }

    private fun handleSavePosition(intent: Intent) {
        val x = intent.getIntExtra(BubbleService.EXTRA_X, 0)
        val y = intent.getIntExtra(BubbleService.EXTRA_Y, 0)
        val settingsManager = SettingsManager.getInstance(context)
        settingsManager.setLastBubblePosition(x, y)
        Log.d(TAG, "Saved bubble position: x=$x, y=$y")
    }

    /**
     * Handles the ACTION_SEND intent to create or update a bubble with a new URL.
     * @param intent The incoming intent containing the shared content.
     */
    private fun handleSharedContent(intent: Intent) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        Log.d(TAG, "handleSharedContent | Received shared text: $sharedText")
        
        if (sharedText != null) {
            // Save to history
            saveToHistory(sharedText)
            
            // Create a new bubble with the extracted URL
            Log.d(TAG, "Creating bubble with extracted URL: $sharedText")
            bubbleManager.createOrUpdateBubbleWithNewUrl(sharedText)
            
        } else {
            Log.w(TAG, "Missing shared text for handleSharedContent")
        }
    }
    
    /**
     * Extracts a URL from text that might contain other content
     */
    private fun extractUrl(text: String): String? {
        // Simple URL extraction using regex
        val urlPattern = "(https?://(?:www\\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s]{2,}|www\\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s]{2,}|https?://(?:www\\.|(?!www))[a-zA-Z0-9]+\\.[^\\s]{2,}|www\\.[a-zA-Z0-9]+\\.[^\\s]{2,})"
        val pattern = Pattern.compile(urlPattern)
        val matcher = pattern.matcher(text)
        
        return if (matcher.find()) {
            matcher.group()
        } else {
            null
        }
    }

    /**
     * Handles the ACTION_VIEW intent to create or update a bubble with a new URL.
     * @param intent The incoming intent containing the URL.
     */
    private fun handleViewAction(intent: Intent) {
        val data = intent.data
        if (data != null && isValidUrl(data.toString())) {
            val url = data.toString()
            
            // Save to history
            saveToHistory(url)
            
            bubbleManager.createOrUpdateBubbleWithNewUrl(url)
        }
    }

    /**
     * Validates the URL format using Android's built-in Patterns class.
     * @param url The URL string to validate.
     * @return True if the URL is valid, false otherwise.
     */
    private fun isValidUrl(url: String): Boolean {
        // First try with Android's built-in pattern
        if (Patterns.WEB_URL.matcher(url).matches()) {
            return true
        }
        
        // If that fails, try a more lenient approach for URLs that might have special characters
        // or don't strictly match the pattern but are still valid URLs
        val lowerUrl = url.lowercase()
        return lowerUrl.startsWith("http://") || 
               lowerUrl.startsWith("https://") || 
               lowerUrl.startsWith("www.") ||
               lowerUrl.contains(".")
    }
    
    /**
     * Extracts a user-friendly title from a URL.
     * @param url The URL to extract a title from.
     * @return A user-friendly title based on the URL.
     */
    private fun extractTitleFromUrl(url: String): String {
        try {
            // First try to fetch the actual title from the HTML (synchronously)
            val htmlTitle = try {
                // This is a blocking call, but it's okay for this context
                // In a real app, you might want to use a more sophisticated approach
                val connection = org.jsoup.Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(3000) // Short timeout to avoid blocking too long
                    .followRedirects(true)
                val document = connection.get()
                document.title()
            } catch (e: Exception) {
                Log.d(TAG, "Failed to fetch HTML title, falling back to URL parsing: ${e.message}")
                null
            }
            
            // If we successfully got a title from HTML, use it
            if (!htmlTitle.isNullOrBlank()) {
                Log.d(TAG, "Using HTML title: $htmlTitle")
                return htmlTitle
            }
            
            // Otherwise fall back to extracting from URL
            Log.d(TAG, "Falling back to URL-based title extraction")
            
            // Remove protocol (http://, https://, etc.)
            var title = url.replace(Regex("^(https?://|www\\.)"), "")
            
            // Remove trailing slashes
            title = title.replace(Regex("/+$"), "")
            
            // Remove query parameters and fragments
            title = title.split("?")[0].split("#")[0]
            
            // Split by path segments and take the domain
            val parts = title.split("/")
            if (parts.isNotEmpty()) {
                // Use domain as base
                title = parts[0]
                
                // If there's a specific page path, add it for context
                if (parts.size > 1 && parts.last().isNotEmpty()) {
                    // Replace hyphens and underscores with spaces
                    val pageName = parts.last()
                        .replace("-", " ")
                        .replace("_", " ")
                        .split(".")
                        .first() // Remove file extension if present
                    
                    // Capitalize first letter of each word
                    val formattedPageName = pageName.split(" ")
                        .filter { it.isNotEmpty() }
                        .joinToString(" ") { word ->
                            word.replaceFirstChar { 
                                if (it.isLowerCase()) it.titlecase() else it.toString() 
                            }
                        }
                    
                    if (formattedPageName.isNotEmpty()) {
                        title = "$formattedPageName - $title"
                    }
                }
            }
            
            return title
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting title from URL: $url", e)
            return "Web Page" // Fallback title
        }
    }
    
    /**
     * Asynchronously extracts the title from a webpage's HTML.
     * This method should be called from a coroutine context.
     * 
     * @param url The URL to extract the title from
     * @return The title from the HTML or null if it couldn't be extracted
     */
    suspend fun extractTitleFromHtmlAsync(url: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching HTML title for URL: $url")
            val connection = org.jsoup.Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(5000)
                .followRedirects(true)
            
            val document = connection.get()
            val title = document.title()
            
            if (title.isNotBlank()) {
                Log.d(TAG, "Successfully extracted HTML title: $title")
                return@withContext title
            } else {
                // Try to find the first h1 tag if title is empty
                val h1Title = document.select("h1").firstOrNull()?.text()
                if (!h1Title.isNullOrBlank()) {
                    Log.d(TAG, "Using h1 as title: $h1Title")
                    return@withContext h1Title
                }
                
                // If still no title, try meta tags
                val metaTitle = document.select("meta[property=og:title], meta[name=twitter:title]").firstOrNull()?.attr("content")
                if (!metaTitle.isNullOrBlank()) {
                    Log.d(TAG, "Using meta title: $metaTitle")
                    return@withContext metaTitle
                }
            }
            
            Log.d(TAG, "No title found in HTML")
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting title from HTML: ${e.message}", e)
            return@withContext null
        }
    }
}
