package com.qb.browser.di

import android.content.Context
import com.qb.browser.manager.AdBlocker
import com.qb.browser.manager.SettingsManager
import com.qb.browser.manager.SummarizationManager
import com.qb.browser.ui.bubble.BubbleView
import com.qb.browser.ui.bubble.BubbleWebViewManager
import com.qb.browser.ui.bubble.WebViewClientEx
import com.qb.browser.viewmodel.BubbleViewModel
import com.qb.browser.viewmodel.WebViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory for creating bubble-related UI components with proper dependency injection
 */
@Singleton
class BubbleComponentFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager,
    private val adBlocker: AdBlocker,
    private val summarizationManager: SummarizationManager
) {

    fun createBubbleWebViewManager(
        bubbleId: String,
        bubbleView: BubbleView
    ): BubbleWebViewManager {
        return BubbleWebViewManager(
            context = context,
            bubbleId = bubbleId,
            bubbleView = bubbleView,
            settingsManager = settingsManager,
            adBlocker = adBlocker
        )
    }

    fun createWebViewClientEx(
        onPageUrlChanged: (String) -> Unit
    ): WebViewClientEx {
        return WebViewClientEx(
            context = context,
            onPageUrlChanged = onPageUrlChanged,
            settingsManager = settingsManager,
            adBlocker = adBlocker
        )
    }
}