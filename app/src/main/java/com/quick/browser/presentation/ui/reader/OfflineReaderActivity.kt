package com.quick.browser.presentation.ui.reader

import android.graphics.Rect
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.quick.browser.R
import com.quick.browser.domain.repository.ArticleRepository
import com.quick.browser.domain.model.SavedArticle
import com.quick.browser.service.SettingsService
import com.quick.browser.utils.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Activity to display saved articles in reader mode
 */
@AndroidEntryPoint
class OfflineReaderActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsService: SettingsService
    
    @Inject
    lateinit var articleRepository: ArticleRepository

    private lateinit var webView: WebView
    private lateinit var searchView: SearchView
    private lateinit var searchCard: MaterialCardView
    private var isSearchBarExplicitlyOpened = false

    companion object {
        const val EXTRA_ARTICLE_TITLE = "article_title"
        const val EXTRA_ARTICLE_CONTENT = "article_content"
        const val EXTRA_ARTICLE_BYLINE = "article_byline"
        const val EXTRA_ARTICLE_SITE_NAME = "article_site_name"
        const val EXTRA_ARTICLE_PUBLISH_DATE = "article_publish_date"
        const val EXTRA_ARTICLE_URL = "article_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_reader)

        setupToolbar()
        setupWebView()
        setupSearch()
        loadArticle()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false) // Hide default title
        
        // Ensure toolbar sits below the status bar on all devices
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.updatePadding(top = statusBarHeight)
            insets
        }
        
        // Set up custom toolbar buttons
        val searchButton = toolbar.findViewById<ImageButton>(R.id.toolbar_search)
        searchButton.setOnClickListener {
            showSearchBar()
        }
    }

    private fun setupWebView() {
        webView = findViewById(R.id.webview_offline_reader)

        // Configure WebView settings for reader mode
        webView.settings.apply {
            javaScriptEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            textZoom = 100
        }
    }

    private fun loadArticle() {
        // Check if we're loading by URL (for saved articles)
        val articleUrl = intent.getStringExtra(EXTRA_ARTICLE_URL)
        if (articleUrl != null) {
            loadSavedArticleByUrl(articleUrl)
            return
        }
        
        // Load article from intent extras (traditional approach)
        val title = intent.getStringExtra(EXTRA_ARTICLE_TITLE) ?: "Untitled"
        val content = intent.getStringExtra(EXTRA_ARTICLE_CONTENT) ?: ""
        val byline = intent.getStringExtra(EXTRA_ARTICLE_BYLINE)
        val siteName = intent.getStringExtra(EXTRA_ARTICLE_SITE_NAME)
        val publishDate = intent.getStringExtra(EXTRA_ARTICLE_PUBLISH_DATE)

        if (content.isEmpty()) {
            Toast.makeText(this, "No content to display", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val htmlContent = createStyledHtml(title, content, byline, siteName, publishDate)
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun loadSavedArticleByUrl(url: String) {
        // Use coroutine to fetch saved article from database
        lifecycleScope.launch {
            try {
                // Get the saved article by URL from the repository
                val savedArticle = articleRepository.getSavedArticleByUrl(url)
                
                if (savedArticle != null) {
                    // Load the article content using the traditional approach
                    val htmlContent = createStyledHtml(
                        title = savedArticle.title,
                        content = savedArticle.content,
                        byline = savedArticle.author,
                        siteName = savedArticle.siteName,
                        publishDate = savedArticle.publishDate
                    )
                    withContext(Dispatchers.Main) {
                        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@OfflineReaderActivity, "Article not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                Logger.e("OfflineReaderActivity", "Error loading saved article by URL", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OfflineReaderActivity, "Error loading article: ${e.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun createStyledHtml(
        title: String,
        content: String,
        byline: String?,
        siteName: String?,
        publishDate: String?
    ): String {
        // Get current reader mode settings
        val readerBackground = settingsService.getReaderBackground()
        val fontSize = settingsService.getReaderFontSize()
        val textAlign = settingsService.getReaderTextAlign()

        // Define color schemes for different backgrounds
        val colors = when (readerBackground) {
            SettingsService.Companion.READER_BG_DARK -> arrayOf("#121212", "#E0E0E0", "#90CAF9", "#B0B0B0", "#1E1E1E", "#616161")
            SettingsService.Companion.READER_BG_SEPIA -> arrayOf("#F4F1E8", "#5D4E37", "#8B4513", "#8B7355", "#EAE7DC", "#D2B48C")
            else -> arrayOf("#FFFFFF", "#212121", "#1976D2", "#757575", "#F5F5F5", "#9E9E9E")
        }

        // Map text alignment values
        val textAlignStyle = when (textAlign) {
            SettingsService.Companion.READER_ALIGN_LEFT -> "left"
            SettingsService.Companion.READER_ALIGN_CENTER -> "center"
            SettingsService.Companion.READER_ALIGN_RIGHT -> "right"
            SettingsService.Companion.READER_ALIGN_JUSTIFY -> "justify"
            else -> "left"
        }

        // Create responsive HTML with embedded CSS
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
                        background-color: ${colors[0]};
                        color: ${colors[1]};
                        margin: 0;
                        padding: 20px;
                        font-size: ${fontSize}px;
                        line-height: 1.6;
                        text-align: $textAlignStyle;
                    }
                    h1, h2, h3, h4, h5, h6 {
                        color: ${colors[2]};
                        margin-top: 1.5em;
                        margin-bottom: 0.5em;
                    }
                    h1 {
                        font-size: 1.8em;
                        border-bottom: 1px solid ${colors[3]};
                        padding-bottom: 0.3em;
                    }
                    h2 {
                        font-size: 1.5em;
                    }
                    h3 {
                        font-size: 1.3em;
                    }
                    p {
                        margin-top: 0;
                        margin-bottom: 1em;
                        text-align: $textAlignStyle;
                    }
                    a {
                        color: ${colors[2]};
                        text-decoration: none;
                    }
                    a:hover {
                        text-decoration: underline;
                    }
                    img {
                        max-width: 100%;
                        height: auto;
                        display: block;
                        margin: 1em auto;
                        border-radius: 4px;
                    }
                    blockquote {
                        border-left: 4px solid ${colors[4]};
                        margin: 1.5em 0;
                        padding: 0.5em 1em;
                        color: ${colors[5]};
                        font-style: italic;
                    }
                    pre, code {
                        background-color: ${colors[4]};
                        border-radius: 4px;
                        padding: 0.2em 0.4em;
                        font-family: 'Courier New', Courier, monospace;
                        overflow-x: auto;
                    }
                    pre {
                        padding: 1em;
                        overflow-x: auto;
                        white-space: pre-wrap;
                        word-wrap: break-word;
                    }
                    pre code {
                        background-color: transparent;
                        padding: 0;
                    }
                    hr {
                        border: 0;
                        border-top: 1px solid ${colors[3]};
                        margin: 2em 0;
                    }
                    ul, ol {
                        padding-left: 1.5em;
                        margin: 1em 0;
                    }
                    li {
                        margin-bottom: 0.5em;
                    }
                    table {
                        border-collapse: collapse;
                        width: 100%;
                        margin: 1em 0;
                    }
                    th, td {
                        border: 1px solid ${colors[3]};
                        padding: 0.5em;
                        text-align: left;
                    }
                    th {
                        background-color: ${colors[4]};
                    }
                    .byline {
                        color: ${colors[5]};
                        font-style: italic;
                        margin-bottom: 1em;
                    }
                    .article-meta {
                        color: ${colors[5]};
                        font-size: 0.9em;
                        margin-bottom: 1em;
                    }
                    .site-name {
                        font-weight: bold;
                    }
                    .publish-date {
                        margin-left: 1em;
                    }
                </style>
            </head>
            <body>
                <article>
                    <header>
                        <h1>$title</h1>
                        ${if (byline != null) "<p class='byline'>By $byline</p>" else ""}
                        ${if (siteName != null || publishDate != null) """
                        <div class='article-meta'>
                            ${if (siteName != null) "<span class='site-name'>$siteName</span>" else ""}
                            ${if (publishDate != null) "<span class='publish-date'>$publishDate</span>" else ""}
                        </div>
                        """ else ""}
                    </header>
                    <main>
                        $content
                    </main>
                </article>
            </body>
            </html>
        """.trimIndent()
    }

    private fun setupSearch() {
        // Initialize views
        searchView = findViewById(R.id.search_view)
        searchCard = findViewById(R.id.search_card)
        
        // Set up search functionality
        setupSearchView()
        
        // Listen for keyboard visibility changes
        setupKeyboardVisibilityListener()
    }
    
    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Hide keyboard when Enter is pressed but keep search bar visible
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(searchView.windowToken, 0)
                
                // Find the query in the WebView
                if (!query.isNullOrEmpty()) {
                    webView.findAllAsync(query)
                    webView.findNext(true)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Find the query in the WebView
                if (!newText.isNullOrEmpty()) {
                    webView.findAllAsync(newText)
                } else {
                    webView.clearMatches()
                }
                return true
            }
        })
        
        // Handle close button click
        searchView.setOnCloseListener {
            closeSearchBar()
            true
        }
        
        // Also handle Enter key from the search view's text field
        searchView.findViewById<androidx.appcompat.widget.SearchView.SearchAutoComplete>(
            androidx.appcompat.R.id.search_src_text
        ).setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Hide keyboard when Enter is pressed but keep search bar visible
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(searchView.windowToken, 0)
                
                // Find the query in the WebView
                val query = searchView.query.toString()
                if (query.isNotEmpty()) {
                    webView.findAllAsync(query)
                    webView.findNext(true)
                }
                true
            } else {
                false
            }
        }
    }
    
    private fun showSearchBar() {
        // Mark that the search bar was explicitly opened
        isSearchBarExplicitlyOpened = true
        
        // Show the search card
        searchCard.visibility = View.VISIBLE
        
        // Post the focus and keyboard show to ensure the view is properly laid out
        searchCard.post {
            // Focus on the search view and show keyboard
            searchView.requestFocus()
            searchView.isIconified = false
            
            // Show keyboard with a delay to ensure proper layout
            searchView.post {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }
    
    private fun closeSearchBar() {
        // Reset the explicit open flag
        isSearchBarExplicitlyOpened = false
        
        // Hide the search card
        searchCard.visibility = View.GONE
        
        // Clear search query
        searchView.setQuery("", false)
        
        // Hide keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchView.windowToken, 0)
        
        // Clear WebView matches
        webView.clearMatches()
    }
    
    private fun hideSearchBar() {
        // Hide keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchView.windowToken, 0)
        
        // Only hide the search bar if it's not focused (user explicitly closed it) or if it wasn't explicitly opened
        if ((!searchView.hasFocus() && !isSearchBarExplicitlyOpened) || searchView.query.isNullOrEmpty()) {
            searchCard.visibility = View.GONE
            isSearchBarExplicitlyOpened = false
            // Clear search query
            searchView.setQuery("", false)
            // Clear WebView matches
            webView.clearMatches()
        }
    }
    
    private fun setupKeyboardVisibilityListener() {
        // Listen for keyboard visibility changes
        window.decorView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            window.decorView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = window.decorView.height
            val keypadHeight = screenHeight - rect.bottom
            
            // Update search card bottom margin to position it above keyboard
            val layoutParams = searchCard.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            layoutParams.bottomMargin = if (keypadHeight > screenHeight * 0.15) {
                // Keyboard is visible, position above it
                keypadHeight + 32 // Add some padding
            } else {
                // Keyboard is hidden, use default margin
                32
            }
            searchCard.layoutParams = layoutParams
            
            // If keyboard is hidden AND search view is not focused AND search bar wasn't explicitly opened, hide it
            if (keypadHeight < screenHeight * 0.15 && !searchView.hasFocus() && !isSearchBarExplicitlyOpened) {
                if (searchCard.visibility == View.VISIBLE && searchView.query.isNullOrEmpty()) {
                    searchCard.visibility = View.GONE
                }
            }
        }
    }
    
    override fun onBackPressed() {
        // If search bar is visible, close it instead of closing the activity
        if (searchCard.visibility == View.VISIBLE) {
            closeSearchBar()
        } else {
            super.onBackPressed()
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}