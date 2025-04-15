package com.qb.browser.ui

import android.content.Context
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.qb.browser.util.AdBlocker

class WebViewClientEx(
    private val context: Context,
    private val onUrlChange: (String) -> Unit
) : WebViewClient() {

    private val adBlocker = AdBlocker.getInstance(context)

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        url?.let { onUrlChange(it) }
    }

    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
        return url?.let { adBlocker.shouldBlockRequest(it) } ?: super.shouldInterceptRequest(view, url)
    }
}