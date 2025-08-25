package com.quick.browser.presentation.ui.browser

import android.graphics.Bitmap

/**
 * Interface for BubbleView to interact with BubbleWebViewManager
 * 
 * This interface defines the contract between BubbleView and BubbleWebViewManager,
 * allowing the WebView manager to communicate back to the BubbleView for UI updates
 * and state changes.
 */
interface BubbleWebViewManagerInterface {
    
    /**
     * Called when the WebView URL changes
     * 
     * @param newUrl The new URL that the WebView navigated to
     */
    fun onWebViewUrlChanged(newUrl: String)
    
    /**
     * Called when HTML content is loaded and available for processing
     * 
     * @param htmlContent The HTML content of the loaded page
     */
    fun onWebViewHtmlContentLoaded(htmlContent: String)
    
    /**
     * Called when the user scrolls down in the WebView
     */
    fun onWebViewScrollDown()
    
    /**
     * Called when the user scrolls up in the WebView
     */
    fun onWebViewScrollUp()
    
    /**
     * Called when a favicon is received from the WebView
     * 
     * @param favicon The favicon bitmap
     */
    fun onWebViewFaviconReceived(favicon: Bitmap)
    
    /**
     * Called when a page title is received from the WebView
     * 
     * @param title The page title
     */
    fun onWebViewTitleReceived(title: String)
    
    /**
     * Called when the WebView loading progress changes
     * 
     * @param progress The loading progress (0-100)
     */
    fun onWebViewProgressChanged(progress: Int)
}