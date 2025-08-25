package com.quick.browser.presentation.navigation

import android.content.Context
import android.content.Intent
import com.quick.browser.presentation.ui.history.HistoryActivity
import com.quick.browser.presentation.ui.saved.SavedArticlesActivity
import com.quick.browser.presentation.ui.settings.SettingsActivity

/**
 * Navigation constants and helper functions for the Quick Browser application
 * 
 * This is a simplified navigation system for a View-based Android application
 * rather than a Compose-based one.
 */
object NavGraph {
    // Main navigation routes
    const val MAIN = "main"
    const val HISTORY = "history"
    const val SAVED_ARTICLES = "saved_articles"
    const val SETTINGS = "settings"
    
    // History-specific actions
    const val HISTORY_DETAILS = "history_details"
    const val HISTORY_SEARCH = "history_search"
    
    // Saved articles actions
    const val ARTICLE_READER = "article_reader"
    const val ARTICLE_DETAILS = "article_details"
    
    // Settings actions
    const val SETTINGS_GENERAL = "settings_general"
    const val SETTINGS_PRIVACY = "settings_privacy"
    const val SETTINGS_DISPLAY = "settings_display"
}

/**
 * Navigation helper class for View-based navigation
 */
class NavigationHelper(private val context: Context) {
    
    /**
     * Navigate to main activity
     */
    fun navigateToMain() {
        val intent = Intent(context, com.quick.browser.presentation.ui.main.MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        context.startActivity(intent)
    }
    
    /**
     * Navigate to history activity
     */
    fun navigateToHistory() {
        val intent = Intent(context, HistoryActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        context.startActivity(intent)
    }
    
    /**
     * Navigate to saved articles activity
     */
    fun navigateToSavedArticles() {
        val intent = Intent(context, SavedArticlesActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        context.startActivity(intent)
    }
    
    /**
     * Navigate to settings activity
     */
    fun navigateToSettings() {
        val intent = Intent(context, SettingsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        context.startActivity(intent)
    }
    
    /**
     * Navigate to reader mode with a specific URL
     */
    fun navigateToReaderMode(url: String) {
        // This would typically involve passing the URL as an extra to the reader activity
        // For now, we'll just navigate to the main activity with a reader mode flag
        val intent = Intent(context, com.quick.browser.presentation.ui.main.MainActivity::class.java).apply {
            putExtra("reader_mode_url", url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
    
    /**
     * Navigate back (finish current activity)
     */
    fun navigateBack() {
        if (context is android.app.Activity) {
            context.finish()
        }
    }
}