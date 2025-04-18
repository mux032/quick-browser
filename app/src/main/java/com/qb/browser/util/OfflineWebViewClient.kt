package com.qb.browser.util

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

/**
 * Custom WebViewClient for handling offline resources
 */
class OfflineWebViewClient(
    private val context: Context,
    private val pageId: String,
    private val baseUri: Uri
) : WebViewClient() {

    private val offlineManager = OfflinePageManager.getInstance(context)
    private val contentTypes = mapOf(
        "html" to "text/html",
        "css" to "text/css",
        "js" to "application/javascript",
        "png" to "image/png",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "gif" to "image/gif",
        "svg" to "image/svg+xml",
        "ico" to "image/x-icon",
        "json" to "application/json",
        "woff" to "font/woff",
        "woff2" to "font/woff2",
        "ttf" to "font/ttf",
        "otf" to "font/otf",
        "eot" to "application/vnd.ms-fontobject"
    )

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        request?.url?.let { uri ->
            // Only intercept if we're already viewing an offline page
            if (uri.scheme == "file" || uri.toString().startsWith(baseUri.toString())) {
                // Try to load from the offline storage
                return handleOfflineResource(uri)
            }
        }
        return super.shouldInterceptRequest(view, request)
    }

    private fun handleOfflineResource(uri: Uri): WebResourceResponse? {
        try {
            // Parse the resource path from the URI
            val filePath = getOfflineFilePath(uri)
            val file = File(filePath)
            
            if (file.exists()) {
                // Determine content type from extension
                val extension = file.extension.lowercase()
                val mimeType = contentTypes[extension] ?: "application/octet-stream"
                
                // Create input stream from file
                val inputStream = FileInputStream(file)
                
                // Return the resource response
                return WebResourceResponse(
                    mimeType,
                    "UTF-8",
                    inputStream
                )
            }
        } catch (e: FileNotFoundException) {
            // If file not found, just continue to default handling
        } catch (e: Exception) {
            // Log other exceptions
            android.util.Log.e("OfflineWebViewClient", "Error loading offline resource: ${e.message}")
        }
        
        // If we can't handle the resource, return a blank response
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            "".byteInputStream()
        )
    }

    private fun getOfflineFilePath(uri: Uri): String {
        // Get the base offline directory for this page
        val baseDir = offlineManager.getOfflinePageDir(pageId)
        
        // Extract the relative path from the URI
        val path = when {
            uri.scheme == "file" -> {
                // Get the path relative to the base directory
                uri.path?.substringAfterLast("/") ?: ""
            }
            else -> {
                // For resources referenced in HTML
                uri.lastPathSegment ?: ""
            }
        }
        
        // Construct the file path
        return "$baseDir/$path"
    }
}