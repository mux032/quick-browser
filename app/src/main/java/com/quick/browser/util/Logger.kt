package com.quick.browser.util

import android.util.Log
import com.quick.browser.QBApplication

/**
 * Centralized logging utility with configurable levels
 */
object Logger {
    private const val TAG_PREFIX = "QuickBrowser_"
    
    enum class LogLevel {
        VERBOSE, DEBUG, INFO, WARN, ERROR, NONE
    }
    
    // Set the minimum log level (can be changed based on build type)
    private val MIN_LOG_LEVEL = if (isDebugBuild()) LogLevel.VERBOSE else LogLevel.WARN
    
    private fun isDebugBuild(): Boolean {
        try {
            // Try to get the BuildConfig.DEBUG value through reflection
            val buildConfigClass = Class.forName("com.quick.browser.BuildConfig")
            val debugField = buildConfigClass.getField("DEBUG")
            return debugField.getBoolean(null)
        } catch (e: Exception) {
            // Fallback to checking if we're in debug mode through QBApplication
            return QBApplication.isDebugBuild()
        }
    }
    
    private fun shouldLog(level: LogLevel): Boolean {
        return when (MIN_LOG_LEVEL) {
            LogLevel.VERBOSE -> true
            LogLevel.DEBUG -> level != LogLevel.VERBOSE
            LogLevel.INFO -> level != LogLevel.VERBOSE && level != LogLevel.DEBUG
            LogLevel.WARN -> level == LogLevel.WARN || level == LogLevel.ERROR
            LogLevel.ERROR -> level == LogLevel.ERROR
            LogLevel.NONE -> false
        }
    }
    
    fun v(tag: String, message: String) {
        if (shouldLog(LogLevel.VERBOSE)) {
            Log.v(TAG_PREFIX + tag, message)
        }
    }
    
    fun d(tag: String, message: String) {
        if (shouldLog(LogLevel.DEBUG)) {
            Log.d(TAG_PREFIX + tag, message)
        }
    }
    
    fun i(tag: String, message: String) {
        if (shouldLog(LogLevel.INFO)) {
            Log.i(TAG_PREFIX + tag, message)
        }
    }
    
    fun w(tag: String, message: String) {
        if (shouldLog(LogLevel.WARN)) {
            Log.w(TAG_PREFIX + tag, message)
        }
    }
    
    fun w(tag: String, message: String, throwable: Throwable) {
        if (shouldLog(LogLevel.WARN)) {
            Log.w(TAG_PREFIX + tag, message, throwable)
        }
    }
    
    fun e(tag: String, message: String) {
        if (shouldLog(LogLevel.ERROR)) {
            Log.e(TAG_PREFIX + tag, message)
        }
    }
    
    fun e(tag: String, message: String, throwable: Throwable) {
        if (shouldLog(LogLevel.ERROR)) {
            Log.e(TAG_PREFIX + tag, message, throwable)
        }
    }
    
    /**
     * Structured logging for better debugging
     */
    fun logStructured(tag: String, message: String, vararg params: Pair<String, Any?>) {
        if (shouldLog(LogLevel.DEBUG)) {
            val paramString = params.joinToString(", ") { "${it.first}=${it.second}" }
            Log.d(TAG_PREFIX + tag, "$message | Params: $paramString")
        }
    }
}