package com.quick.browser.utils

import android.util.Log

/**
 * Logger utility for consistent logging throughout the application
 *
 * This object provides a unified interface for logging messages at different
 * levels (debug, info, warning, error) with consistent formatting and tagging.
 * It wraps the Android Log class to provide a cleaner API.
 */
object Logger {
    private const val DEFAULT_TAG = "QuickBrowser"

    /**
     * Log a debug message
     *
     * @param tag The tag for the log message, defaults to "QuickBrowser"
     * @param message The debug message to log
     */
    fun d(tag: String = DEFAULT_TAG, message: String) {
        Log.d(tag, message)
    }

    /**
     * Log a debug message with throwable
     *
     * @param tag The tag for the log message
     * @param message The debug message to log
     * @param throwable The throwable to log
     */
    fun d(tag: String, message: String, throwable: Throwable) {
        Log.d(tag, message, throwable)
    }

    /**
     * Log an info message
     *
     * @param tag The tag for the log message, defaults to "QuickBrowser"
     * @param message The info message to log
     */
    fun i(tag: String = DEFAULT_TAG, message: String) {
        Log.i(tag, message)
    }

    /**
     * Log an info message with throwable
     *
     * @param tag The tag for the log message
     * @param message The info message to log
     * @param throwable The throwable to log
     */
    fun i(tag: String, message: String, throwable: Throwable) {
        Log.i(tag, message, throwable)
    }

    /**
     * Log a warning message
     *
     * @param tag The tag for the log message, defaults to "QuickBrowser"
     * @param message The warning message to log
     */
    fun w(tag: String = DEFAULT_TAG, message: String) {
        Log.w(tag, message)
    }

    /**
     * Log a warning message with throwable
     *
     * @param tag The tag for the log message
     * @param message The warning message to log
     * @param throwable The throwable to log
     */
    fun w(tag: String, message: String, throwable: Throwable) {
        Log.w(tag, message, throwable)
    }

    /**
     * Log an error message
     *
     * @param tag The tag for the log message, defaults to "QuickBrowser"
     * @param message The error message to log
     */
    fun e(tag: String = DEFAULT_TAG, message: String) {
        Log.e(tag, message)
    }

    /**
     * Log an error message with throwable
     *
     * @param tag The tag for the log message
     * @param message The error message to log
     * @param throwable The throwable to log
     */
    fun e(tag: String, message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
    }
}