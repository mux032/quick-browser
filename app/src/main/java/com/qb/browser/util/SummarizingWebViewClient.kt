package com.qb.browser.util

import android.content.Context
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * WebViewClient that automatically captures HTML content for summarization
 */
class SummarizingWebViewClient(
    context: Context,
    onPageUrlChanged: (String) -> Unit,
    private val onPageLoaded: (String) -> Unit
) : WebViewClientEx(context, onPageUrlChanged) {
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        
        // Capture HTML content for summarization in the background
        view?.let { webView ->
            // Use JavaScript to get the HTML content, but do it after a short delay
            // to ensure the page is fully rendered and interactive first
            coroutineScope.launch {
                try {
                    // Delay summarization to prioritize user interaction
                    kotlinx.coroutines.delay(1000)
                    
                    // JavaScript evaluation must be done on the main thread
                    // We're still in the main coroutine scope here
                    webView.evaluateJavascript("(function() { return document.documentElement.outerHTML; })()") { html ->
                        if (html != null && html.length > 50) {
                            // Process the HTML on a background thread
                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    // The result is a JSON string, so we need to parse it
                                    val unescapedHtml = html.substring(1, html.length - 1)
                                        .replace("\\\"", "\"")
                                        .replace("\\n", "\n")
                                        .replace("\\\\", "\\")
                                    
                                    // Pass the HTML content to the callback for background summarization
                                    android.util.Log.d("SummarizingWebViewClient", "Captured HTML content for URL: $url (${unescapedHtml.length} chars)")
                                    onPageLoaded(unescapedHtml)
                                } catch (e: Exception) {
                                    // Log the error but don't crash
                                    android.util.Log.e("SummarizingWebViewClient", "Error processing HTML", e)
                                }
                            }
                        } else {
                            android.util.Log.w("SummarizingWebViewClient", "HTML content too short or null for URL: $url")
                        }
                    }
                } catch (e: Exception) {
                    // Log the error but don't crash
                    android.util.Log.e("SummarizingWebViewClient", "Error in onPageFinished", e)
                }
            }
        }
    }
}