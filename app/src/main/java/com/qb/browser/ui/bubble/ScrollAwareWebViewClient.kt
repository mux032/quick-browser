package com.qb.browser.ui.bubble

import android.content.Context
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * WebViewClient that detects scrolling to show/hide the toolbar
 */
class ScrollAwareWebViewClient(
    context: Context,
    onPageUrlChanged: (String) -> Unit,
    private val onHtmlContentLoaded: (String) -> Unit,
    private val onScrollDown: () -> Unit,
    private val onScrollUp: () -> Unit
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
                    
                    // Get HTML content for summarization
                    webView.evaluateJavascript("""
                        (function() {
                            return document.documentElement.outerHTML;
                        })()
                    """.trimIndent()) { html ->
                        if (html != null && html.length > 50) {
                            // Process the HTML on a background thread
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    // The result is a JSON string, so we need to parse it
                                    val unescapedHtml = html.substring(1, html.length - 1)
                                        .replace("\\\"", "\"")
                                        .replace("\\n", "\n")
                                        .replace("\\\\", "\\")
                                    
                                    // Pass the HTML content to the callback for background summarization
                                    android.util.Log.d("ScrollAwareWebViewClient", "Captured HTML content for URL: $url (${unescapedHtml.length} chars)")
                                    onHtmlContentLoaded(unescapedHtml)
                                } catch (e: Exception) {
                                    // Log the error but don't crash
                                    android.util.Log.e("ScrollAwareWebViewClient", "Error processing HTML", e)
                                }
                            }
                        } else {
                            android.util.Log.w("ScrollAwareWebViewClient", "HTML content too short or null for URL: $url")
                        }
                    }
                    
                    // Inject JavaScript to monitor scrolling in real-time with improved responsiveness
                    val js = """
                        (function() {
                            // Variables for scroll tracking
                            var lastScrollY = window.scrollY || document.documentElement.scrollTop;
                            var lastScrollDirection = null;
                            var scrollThreshold = 3; // Lower threshold for more sensitivity
                            var consecutiveThreshold = 2; // Number of consecutive scrolls in same direction to trigger
                            var consecutiveCount = 0;
                            var lastNotifiedDirection = null;
                            
                            // Use requestAnimationFrame for smoother performance
                            var ticking = false;
                            
                            // Main scroll handler
                            window.addEventListener('scroll', function() {
                                if (!ticking) {
                                    window.requestAnimationFrame(function() {
                                        var currentScrollY = window.scrollY || document.documentElement.scrollTop;
                                        var scrollDelta = currentScrollY - lastScrollY;
                                        
                                        // Determine scroll direction
                                        if (Math.abs(scrollDelta) > scrollThreshold) {
                                            var currentDirection = scrollDelta > 0 ? 'down' : 'up';
                                            
                                            // Check if we're continuing in the same direction
                                            if (currentDirection === lastScrollDirection) {
                                                consecutiveCount++;
                                            } else {
                                                consecutiveCount = 1;
                                                lastScrollDirection = currentDirection;
                                            }
                                            
                                            // Only notify when we have enough consecutive scrolls in the same direction
                                            // or when direction changes from the last notification
                                            if ((consecutiveCount >= consecutiveThreshold && 
                                                currentDirection !== lastNotifiedDirection) || 
                                                (currentDirection !== lastNotifiedDirection && 
                                                Math.abs(scrollDelta) > scrollThreshold * 3)) {
                                                
                                                if (currentDirection === 'down') {
                                                    window.ScrollDetector.onScrollDown();
                                                } else {
                                                    window.ScrollDetector.onScrollUp();
                                                }
                                                lastNotifiedDirection = currentDirection;
                                            }
                                            
                                            lastScrollY = currentScrollY;
                                        }
                                        
                                        ticking = false;
                                    });
                                    
                                    ticking = true;
                                }
                            }, { passive: true });
                            
                            // Also detect touch events for more responsive mobile scrolling
                            var touchStartY = 0;
                            
                            document.addEventListener('touchstart', function(e) {
                                touchStartY = e.touches[0].clientY;
                            }, { passive: true });
                            
                            document.addEventListener('touchmove', function(e) {
                                var touchY = e.touches[0].clientY;
                                var touchDelta = touchStartY - touchY;
                                
                                // Detect significant touch movement
                                if (Math.abs(touchDelta) > 10) {
                                    if (touchDelta > 0) {
                                        // Swiping up = scrolling down
                                        window.ScrollDetector.onScrollDown();
                                    } else {
                                        // Swiping down = scrolling up
                                        window.ScrollDetector.onScrollUp();
                                    }
                                    touchStartY = touchY;
                                }
                            }, { passive: true });
                        })();
                    """.trimIndent()
                    
                    webView.evaluateJavascript(js, null)
                } catch (e: Exception) {
                    android.util.Log.e("ScrollAwareWebViewClient", "Error in onPageFinished", e)
                }
            }
        }
    }
}