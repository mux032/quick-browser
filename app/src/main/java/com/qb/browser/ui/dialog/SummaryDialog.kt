package com.qb.browser.ui.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.qb.browser.R
import com.qb.browser.util.SummarizationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

/**
 * Dialog for displaying article summaries
 */
class SummaryDialog private constructor(
    context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val htmlContent: String
) : Dialog(context) {

    private lateinit var progressBar: ProgressBar
    private lateinit var summaryContainer: LinearLayout
    private lateinit var scrollView: NestedScrollView
    private lateinit var buttonClose: Button
    private lateinit var buttonShare: Button
    
    private val summarizationManager = SummarizationManager.getInstance(context)
    private var summaryPoints: List<String> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_summary, null)
        setContentView(view)
        
        // Initialize views
        progressBar = view.findViewById(R.id.progress_summarizing)
        summaryContainer = view.findViewById(R.id.summary_container)
        scrollView = view.findViewById(R.id.scroll_view)
        buttonClose = view.findViewById(R.id.button_close)
        buttonShare = view.findViewById(R.id.button_share)
        
        // Set up button listeners
        buttonClose.setOnClickListener {
            dismiss()
        }
        
        buttonShare.setOnClickListener {
            shareSummary()
        }
        
        // Start summarization
        summarizeContent()
    }
    
    private fun summarizeContent() {
        lifecycleOwner.lifecycleScope.launch {
            try {
                // Show progress
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.VISIBLE
                    summaryContainer.visibility = View.GONE
                }
                
                // Get summary
                val startTime = System.currentTimeMillis()
                summaryPoints = summarizationManager.summarizeContent(htmlContent)
                val duration = System.currentTimeMillis() - startTime
                
                Log.d("SummaryDialog", "Summarization took $duration ms, found ${summaryPoints.size} points")
                
                // Update UI
                withContext(Dispatchers.Main) {
                    if (summaryPoints.isNotEmpty()) {
                        displaySummary(summaryPoints)
                    } else {
                        // If we couldn't generate a summary, try a simpler approach
                        val fallbackSummary = generateFallbackSummary(htmlContent)
                        if (fallbackSummary.isNotEmpty()) {
                            summaryPoints = fallbackSummary
                            displaySummary(fallbackSummary)
                        } else {
                            showError(context.getString(R.string.summary_not_article))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SummaryDialog", "Error summarizing content", e)
                withContext(Dispatchers.Main) {
                    showError(context.getString(R.string.summary_error))
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
            doc.select("script, style, nav, footer, header, aside").remove()
            
            // Get paragraphs
            val paragraphs = doc.select("p")
                .map { it.text() }
                .filter { it.length > 50 && it.length < 300 }
                .take(20)
            
            if (paragraphs.size < 5) {
                return emptyList()
            }
            
            // Take first paragraph and a few others based on length
            val firstParagraph = paragraphs.firstOrNull() ?: return emptyList()
            val otherParagraphs = paragraphs.drop(1)
                .sortedByDescending { it.length }
                .take(9)
            
            val result = mutableListOf(firstParagraph)
            result.addAll(otherParagraphs)
            
            return result.map { 
                if (it.endsWith(".") || it.endsWith("!") || it.endsWith("?")) it else "$it."
            }
        } catch (e: Exception) {
            Log.e("SummaryDialog", "Error generating fallback summary", e)
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
            val bulletPoint = LayoutInflater.from(context).inflate(
                android.R.layout.simple_list_item_1, 
                summaryContainer, 
                false
            ) as TextView
            
            bulletPoint.text = "• $point"
            bulletPoint.setPadding(16, 8, 16, 8)
            
            summaryContainer.addView(bulletPoint)
        }
    }
    
    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        
        // Show error message
        val errorText = TextView(context)
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
        
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share)))
    }
    
    companion object {
        fun show(
            context: Context,
            lifecycleOwner: LifecycleOwner,
            htmlContent: String
        ): SummaryDialog {
            val dialog = SummaryDialog(context, lifecycleOwner, htmlContent)
            dialog.show()
            return dialog
        }
    }
}