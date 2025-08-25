package com.quick.browser.presentation.ui.reader

import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.quick.browser.R
import com.quick.browser.service.SettingsService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Activity to display saved articles in reader mode
 */
@AndroidEntryPoint
class OfflineReaderActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsService: SettingsService

    private lateinit var webView: WebView

    companion object {
        const val EXTRA_ARTICLE_TITLE = "article_title"
        const val EXTRA_ARTICLE_CONTENT = "article_content"
        const val EXTRA_ARTICLE_BYLINE = "article_byline"
        const val EXTRA_ARTICLE_SITE_NAME = "article_site_name"
        const val EXTRA_ARTICLE_PUBLISH_DATE = "article_publish_date"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_reader)

        setupToolbar()
        setupWebView()
        loadArticle()
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = intent.getStringExtra(EXTRA_ARTICLE_TITLE) ?: getString(R.string.offline_reader)
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