package com.qb.browser.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.qb.browser.R
import com.qb.browser.util.SummarizationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import android.widget.LinearLayout
import android.view.LayoutInflater
import android.widget.Button
import android.widget.Toast

/**
 * Activity for displaying article summaries in a full window
 */
class SummaryActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var progressBar: ProgressBar
    private lateinit var summaryContainer: LinearLayout
    private lateinit var scrollView: NestedScrollView
    private lateinit var buttonShare: Button
    
    private val summarizationManager by lazy { SummarizationManager.getInstance(this) }
    private var summaryPoints: List<String> = emptyList()
    private var htmlContent: String = ""
    
    companion object {
        private const val TAG = "SummaryActivity"
        private const val EXTRA_HTML_CONTENT = "extra_html_content"
        
        fun createIntent(context: Context, htmlContent: String): Intent {
            return Intent(context, SummaryActivity::class.java).apply {
                putExtra(EXTRA_HTML_CONTENT, htmlContent)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_summary)
            
            // Get HTML content from intent
            htmlContent = intent.getStringExtra(EXTRA_HTML_CONTENT) ?: ""
            if (htmlContent.isEmpty() || htmlContent.length < 100) {
                Toast.makeText(this, R.string.summary_error, Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            
            // Initialize views
            try {
                toolbar = findViewById(R.id.toolbar)
                progressBar = findViewById(R.id.progress_summarizing)
                summaryContainer = findViewById(R.id.summary_container)
                scrollView = findViewById(R.id.scroll_view)
                buttonShare = findViewById(R.id.button_share)
                
                // Set up toolbar
                setSupportActionBar(toolbar)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.title = getString(R.string.summary_title)
                
                // Set up button listeners
                buttonShare.setOnClickListener {
                    shareSummary()
                }
                
                // Start summarization
                summarizeContent()
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing views", e)
                Toast.makeText(this, R.string.summary_error, Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            try {
                Toast.makeText(this, R.string.summary_error, Toast.LENGTH_SHORT).show()
            } catch (e2: Exception) {
                // If even showing a toast fails, just finish silently
            }
            finish()
        }
    }
    
    private fun summarizeContent() {
        lifecycleScope.launch {
            try {
                // Show progress
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.VISIBLE
                    summaryContainer.visibility = View.GONE
                }
                
                // Validate HTML content
                if (htmlContent.length < 100) {
                    withContext(Dispatchers.Main) {
                        showError(getString(R.string.summary_not_article))
                    }
                    return@launch
                }
                
                // Clean HTML content first to remove any script tags or other unwanted elements
                val cleanedHtml = try {
                    withContext(Dispatchers.IO) {
                        val doc = Jsoup.parse(htmlContent)
                        // Remove all script, style, and other non-content elements
                        doc.select("script, style, noscript, iframe, object, embed, header, footer, nav, aside").remove()
                        // Extract only the text content to avoid any HTML tags
                        doc.text()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error cleaning HTML", e)
                    withContext(Dispatchers.Main) {
                        showError(getString(R.string.summary_error))
                    }
                    return@launch
                }
                
                // Validate cleaned text
                if (cleanedHtml.length < 100) {
                    withContext(Dispatchers.Main) {
                        showError(getString(R.string.summary_not_article))
                    }
                    return@launch
                }
                
                // Get summary
                try {
                    val startTime = System.currentTimeMillis()
                    summaryPoints = summarizationManager.summarizeContent(cleanedHtml)
                    val duration = System.currentTimeMillis() - startTime
                    
                    Log.d(TAG, "Summarization took $duration ms, found ${summaryPoints.size} points")
                    
                    // Update UI
                    withContext(Dispatchers.Main) {
                        if (summaryPoints.isNotEmpty()) {
                            displaySummary(summaryPoints)
                        } else {
                            // If we couldn't generate a summary, try a simpler approach
                            try {
                                val fallbackSummary = generateFallbackSummary(cleanedHtml)
                                if (fallbackSummary.isNotEmpty()) {
                                    summaryPoints = fallbackSummary
                                    displaySummary(fallbackSummary)
                                } else {
                                    showError(getString(R.string.summary_not_article))
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error generating fallback summary", e)
                                showError(getString(R.string.summary_error))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during summarization", e)
                    withContext(Dispatchers.Main) {
                        // Try fallback if main summarization fails
                        try {
                            val fallbackSummary = generateFallbackSummary(cleanedHtml)
                            if (fallbackSummary.isNotEmpty()) {
                                summaryPoints = fallbackSummary
                                displaySummary(fallbackSummary)
                            } else {
                                showError(getString(R.string.summary_error))
                            }
                        } catch (e2: Exception) {
                            Log.e(TAG, "Fallback summarization also failed", e2)
                            showError(getString(R.string.summary_error))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in summarizeContent", e)
                try {
                    withContext(Dispatchers.Main) {
                        showError(getString(R.string.summary_error))
                    }
                } catch (e2: Exception) {
                    Log.e(TAG, "Error showing error message", e2)
                }
            }
        }
    }
    
    /**
     * Generate a very simple fallback summary when the main summarization fails
     */
    private fun generateFallbackSummary(html: String): List<String> {
        try {
            val doc = Jsoup.parse(html)
            
            // Remove non-content elements
            doc.select("script, style, nav, footer, header, aside, iframe, form, button").remove()
            
            // Remove all elements with common ad-related class names
            doc.select("[class*=ad], [class*=Ad], [class*=AD], [class*=banner], [class*=popup], [class*=cookie], [id*=ad], [id*=Ad]").remove()
            
            // Remove social media widgets
            doc.select("[class*=social], [class*=share], [class*=follow], [class*=subscribe]").remove()
            
            // Get paragraphs
            val paragraphs = doc.select("p, h1, h2, h3, li")
                .map { it.text() }
                .filter { it.length > 40 && it.length < 250 } // More strict length filtering
                .filter { !it.contains("click here", ignoreCase = true) } // Filter out navigation text
                .filter { !it.contains("cookie", ignoreCase = true) } // Filter out cookie notices
                .filter { !it.contains("subscribe", ignoreCase = true) } // Filter out subscription prompts
                .filter { !it.contains("copyright", ignoreCase = true) } // Filter out copyright notices
                .filter { !it.contains("<") && !it.contains(">") } // Filter out any remaining HTML
                .take(30) // Take more paragraphs to have a better selection
            
            if (paragraphs.size < 5) {
                return emptyList()
            }
            
            // Take first paragraph (likely the introduction) and a few others based on length and position
            val firstParagraph = paragraphs.firstOrNull() ?: return emptyList()
            
            // Take some paragraphs from the beginning (likely important context)
            val earlyParagraphs = paragraphs.drop(1).take(5).take(2)
            
            // Take some paragraphs from the middle (likely the main content)
            val middleStart = (paragraphs.size / 3).coerceAtLeast(3)
            val middleEnd = (paragraphs.size * 2 / 3).coerceAtMost(paragraphs.size - 1)
            val middleParagraphs = paragraphs.subList(middleStart, middleEnd).take(4)
            
            // Take some paragraphs from the end (likely the conclusion)
            val endParagraphs = paragraphs.takeLast(3).take(2)
            
            // Combine all selected paragraphs
            val result = mutableListOf(firstParagraph)
            result.addAll(earlyParagraphs)
            result.addAll(middleParagraphs)
            result.addAll(endParagraphs)
            
            // Ensure we have at most 10 paragraphs
            val limitedResult = result.take(10)
            
            // Clean and format the paragraphs
            return limitedResult.map { paragraph ->
                // Clean the paragraph
                var cleanedParagraph = paragraph
                    .replace("\\s+".toRegex(), " ") // Normalize whitespace
                    .replace("&nbsp;", " ")
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&#39;", "'")
                    .replace("<[^>]*>".toRegex(), "") // Remove any remaining HTML tags
                    .replace("\\{\\{[^}]*\\}\\}".toRegex(), "") // Remove template expressions
                    .replace("\\[[0-9]+\\]".toRegex(), "") // Remove citation numbers
                    .trim()
                
                // Truncate very long paragraphs
                if (cleanedParagraph.length > 200) {
                    val truncated = cleanedParagraph.substring(0, 197) + "..."
                    cleanedParagraph = truncated
                }
                
                // Ensure the paragraph starts with a capital letter
                if (cleanedParagraph.isNotEmpty() && cleanedParagraph[0].isLowerCase()) {
                    cleanedParagraph = cleanedParagraph[0].uppercaseChar() + cleanedParagraph.substring(1)
                }
                
                // Add period if missing
                if (!cleanedParagraph.endsWith(".") && !cleanedParagraph.endsWith("!") && !cleanedParagraph.endsWith("?")) {
                    cleanedParagraph = "$cleanedParagraph."
                }
                
                cleanedParagraph
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating fallback summary", e)
            return emptyList()
        }
    }
    
    private fun displaySummary(points: List<String>) {
        progressBar.visibility = View.GONE
        summaryContainer.visibility = View.VISIBLE
        
        // Clear previous content
        summaryContainer.removeAllViews()
        
        // Add bullet points
        points.forEach { point ->
            val bulletPoint = LayoutInflater.from(this).inflate(
                R.layout.item_summary_point, 
                summaryContainer, 
                false
            ) as TextView
            
            bulletPoint.text = "• $point"
            
            summaryContainer.addView(bulletPoint)
        }
    }
    
    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        
        // Show error message
        val errorText = TextView(this)
        errorText.text = message
        errorText.setPadding(16, 16, 16, 16)
        
        summaryContainer.removeAllViews()
        summaryContainer.addView(errorText)
        summaryContainer.visibility = View.VISIBLE
    }
    
    private fun shareSummary() {
        if (summaryPoints.isEmpty()) return
        
        val summaryText = buildString {
            append("Article Summary:\n\n")
            summaryPoints.forEach { point ->
                append("• $point\n")
            }
        }
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, summaryText)
            type = "text/plain"
        }
        
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}