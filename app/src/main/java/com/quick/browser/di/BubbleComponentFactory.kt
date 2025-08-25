package com.quick.browser.di

import android.content.Context
import com.quick.browser.presentation.ui.browser.BubbleView
import com.quick.browser.presentation.ui.browser.BubbleWebViewManager
import com.quick.browser.presentation.ui.browser.WebViewClientEx
import com.quick.browser.service.AdBlockingService
import com.quick.browser.service.SettingsService
import com.quick.browser.service.SummarizationService
import com.quick.browser.utils.security.SecurityPolicyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory for creating bubble-related UI components with proper dependency injection
 */
@Singleton
class BubbleComponentFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsService: SettingsService,
    private val adBlockingService: AdBlockingService,
    private val summarizationService: SummarizationService
) {

    fun createBubbleWebViewManager(
        bubbleId: String,
        bubbleView: BubbleView
    ): BubbleWebViewManager {
        return BubbleWebViewManager(
            context = context,
            bubbleId = bubbleId,
            bubbleView = bubbleView,
            settingsService = settingsService,
            adBlockingService = adBlockingService,
            securityPolicyManager = SecurityPolicyManager(context)
        )
    }

    fun createWebViewClientEx(
        onPageUrlChanged: (String) -> Unit
    ): WebViewClientEx {
        return WebViewClientEx(
            context = context,
            onPageUrlChanged = onPageUrlChanged,
            settingsService = settingsService,
            adBlockingService = adBlockingService
        )
    }
}