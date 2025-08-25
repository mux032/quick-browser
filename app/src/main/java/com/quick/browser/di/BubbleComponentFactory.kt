package com.quick.browser.di

import android.content.Context
import com.quick.browser.presentation.ui.browser.BubbleView
import com.quick.browser.presentation.ui.browser.BubbleWebViewManager
import com.quick.browser.presentation.ui.browser.WebViewClientEx
import com.quick.browser.utils.managers.AdBlocker
import com.quick.browser.utils.managers.SecurityPolicyManager
import com.quick.browser.utils.managers.SettingsManager
import com.quick.browser.utils.managers.SummarizationManager
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
            adBlocker = adBlocker,
            securityPolicyManager = SecurityPolicyManager(context)
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