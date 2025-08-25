package com.quick.browser.utils

import android.util.Log

/**
 * Logger utility for consistent logging throughout the application
 */
object Logger {
    private const val DEFAULT_TAG = "QuickBrowser"

    fun d(tag: String = DEFAULT_TAG, message: String) {
        Log.d(tag, message)
    }

    fun d(tag: String, message: String, throwable: Throwable) {
        Log.d(tag, message, throwable)
    }

    fun i(tag: String = DEFAULT_TAG, message: String) {
        Log.i(tag, message)
    }

    fun i(tag: String, message: String, throwable: Throwable) {
        Log.i(tag, message, throwable)
    }

    fun w(tag: String = DEFAULT_TAG, message: String) {
        Log.w(tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable) {
        Log.w(tag, message, throwable)
    }

    fun e(tag: String = DEFAULT_TAG, message: String) {
        Log.e(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
    }
}