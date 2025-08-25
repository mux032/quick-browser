package com.quick.browser.utils

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Centralized error handling utility for the application.
 * Provides consistent error logging, reporting, and user feedback.
 * 
 * Usage examples:
 * 
 * 1. Basic try-catch with extension function:
 *    val result = runCatching { riskyOperation() }
 *        .onError(tag = "MyClass") { "Failed to perform operation" }
 * 
 * 2. With fallback value:
 *    val result = runCatching { riskyOperation() }
 *        .getOrDefault("fallback", tag = "MyClass") { "Failed to get data" }
 * 
 * 3. In coroutines:
 *    lifecycleScope.launch {
 *        val result = withErrorHandling(tag = "MyClass") {
 *            riskyAsyncOperation()
 *        }
 *    }
 * 
 * 4. With user feedback:
 *    runCatching { riskyOperation() }
 *        .onError(tag = "MyClass", context = this, view = myView) { "Operation failed" }
 */
object ErrorHandler {
    private const val TAG = "ErrorHandler"
    
    /**
     * Log an error with consistent formatting
     * 
     * @param tag The tag for the log message
     * @param message The error message
     * @param throwable The exception that was thrown (optional)
     * @param level The log level (default: ERROR)
     */
    fun logError(
        tag: String, 
        message: String, 
        throwable: Throwable? = null,
        level: LogLevel = LogLevel.ERROR
    ) {
        val fullTag = "$TAG:$tag"
        when (level) {
            LogLevel.DEBUG -> Log.d(fullTag, message, throwable)
            LogLevel.INFO -> Log.i(fullTag, message, throwable)
            LogLevel.WARNING -> Log.w(fullTag, message, throwable)
            LogLevel.ERROR -> Log.e(fullTag, message, throwable)
        }
    }
    
    /**
     * Show a toast message to the user
     * 
     * @param context The context to use for showing the toast
     * @param message The message to show
     * @param length The duration of the toast (default: SHORT)
     */
    fun showToast(
        context: Context, 
        message: String, 
        length: Int = Toast.LENGTH_SHORT
    ) {
        Toast.makeText(context, message, length).show()
    }
    
    /**
     * Show a snackbar message to the user
     * 
     * @param view The view to attach the snackbar to
     * @param message The message to show
     * @param length The duration of the snackbar (default: SHORT)
     * @param actionText The text for the action button (optional)
     * @param action The action to perform when the button is clicked (optional)
     */
    fun showSnackbar(
        view: View, 
        message: String, 
        length: Int = Snackbar.LENGTH_SHORT,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        val snackbar = Snackbar.make(view, message, length)
        if (actionText != null && action != null) {
            snackbar.setAction(actionText) { action() }
        }
        snackbar.show()
    }
    
    /**
     * Execute a block of code and handle any exceptions that are thrown
     * 
     * @param tag The tag for error logging
     * @param errorMessage The error message to log
     * @param showError Whether to show an error message to the user (default: false)
     * @param context The context to use for showing error messages (required if showError is true)
     * @param view The view to attach snackbars to (optional, used if context is provided)
     * @param block The code to execute
     * @return The result of the block, or null if an exception was thrown
     */
    inline fun <T> handleExceptions(
        tag: String,
        errorMessage: String,
        showError: Boolean = false,
        context: Context? = null,
        view: View? = null,
        block: () -> T
    ): T? {
        return try {
            block()
        } catch (e: Exception) {
            // Don't catch cancellation exceptions to allow coroutines to cancel properly
            if (e is CancellationException) throw e
            
            // Log the error
            logError(tag, errorMessage, e)
            
            // Show error to user if requested
            if (showError && context != null) {
                if (view != null) {
                    showSnackbar(view, "$errorMessage: ${e.localizedMessage}")
                } else {
                    showToast(context, "$errorMessage: ${e.localizedMessage}")
                }
            }
            
            null
        }
    }
    
    /**
     * Execute a block of code and handle any exceptions that are thrown with a fallback value
     * 
     * @param tag The tag for error logging
     * @param errorMessage The error message to log
     * @param fallback The fallback value to return if an exception is thrown
     * @param showError Whether to show an error message to the user (default: false)
     * @param context The context to use for showing error messages (required if showError is true)
     * @param view The view to attach snackbars to (optional, used if context is provided)
     * @param block The code to execute
     * @return The result of the block, or the fallback value if an exception was thrown
     */
    inline fun <T> handleExceptionsWithFallback(
        tag: String,
        errorMessage: String,
        fallback: T,
        showError: Boolean = false,
        context: Context? = null,
        view: View? = null,
        block: () -> T
    ): T {
        return try {
            block()
        } catch (e: Exception) {
            // Don't catch cancellation exceptions to allow coroutines to cancel properly
            if (e is CancellationException) throw e
            
            // Log the error
            logError(tag, errorMessage, e)
            
            // Show error to user if requested
            if (showError && context != null) {
                if (view != null) {
                    showSnackbar(view, "$errorMessage: ${e.localizedMessage}")
                } else {
                    showToast(context, "$errorMessage: ${e.localizedMessage}")
                }
            }
            
            fallback
        }
    }
    
    /**
     * Log levels for error logging
     */
    enum class LogLevel {
        DEBUG, INFO, WARNING, ERROR
    }
}

/**
 * Extension function for Result to handle errors with logging
 */
inline fun <T> Result<T>.onError(
    tag: String,
    context: Context? = null,
    view: View? = null,
    showError: Boolean = false,
    crossinline errorMessageProvider: (Throwable) -> String
): Result<T> {
    onFailure { throwable ->
        if (throwable is CancellationException) throw throwable
        
        val errorMessage = errorMessageProvider(throwable)
        ErrorHandler.logError(tag, errorMessage, throwable)
        
        if (showError && context != null) {
            val userMessage = "$errorMessage: ${throwable.localizedMessage}"
            if (view != null) {
                ErrorHandler.showSnackbar(view, userMessage)
            } else {
                ErrorHandler.showToast(context, userMessage)
            }
        }
    }
    return this
}

/**
 * Extension function for Result to get value or default with error handling
 */
inline fun <T> Result<T>.getOrDefault(
    defaultValue: T,
    tag: String,
    context: Context? = null,
    view: View? = null,
    showError: Boolean = false,
    crossinline errorMessageProvider: (Throwable) -> String
): T {
    return onError(tag, context, view, showError, errorMessageProvider)
        .getOrDefault(defaultValue)
}

/**
 * Extension function to run code with error handling and return a Result
 */
inline fun <T> runCatching(block: () -> T): Result<T> {
    return kotlin.runCatching(block)
}

/**
 * Extension function for CoroutineScope to run suspending code with error handling
 */
suspend inline fun <T> withErrorHandling(
    tag: String,
    errorMessage: String,
    context: Context? = null,
    view: View? = null,
    showError: Boolean = false,
    crossinline block: suspend () -> T
): T? = withContext(Dispatchers.Default) {
    try {
        block()
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        
        ErrorHandler.logError(tag, errorMessage, e)
        
        if (showError && context != null) {
            withContext(Dispatchers.Main) {
                if (view != null) {
                    ErrorHandler.showSnackbar(view, "$errorMessage: ${e.localizedMessage}")
                } else {
                    ErrorHandler.showToast(context, "$errorMessage: ${e.localizedMessage}")
                }
            }
        }
        
        null
    }
}

/**
 * Extension function for CoroutineScope to run suspending code with error handling and fallback
 */
suspend inline fun <T> withErrorHandlingAndFallback(
    tag: String,
    errorMessage: String,
    fallback: T,
    context: Context? = null,
    view: View? = null,
    showError: Boolean = false,
    crossinline block: suspend () -> T
): T = withContext(Dispatchers.Default) {
    try {
        block()
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        
        ErrorHandler.logError(tag, errorMessage, e)
        
        if (showError && context != null) {
            withContext(Dispatchers.Main) {
                if (view != null) {
                    ErrorHandler.showSnackbar(view, "$errorMessage: ${e.localizedMessage}")
                } else {
                    ErrorHandler.showToast(context, "$errorMessage: ${e.localizedMessage}")
                }
            }
        }
        
        fallback
    }
}

/**
 * Extension function to safely execute a block of code with error handling
 */
inline fun <T> T.safeExecute(
    tag: String,
    errorMessage: String,
    showError: Boolean = false,
    context: Context? = null,
    view: View? = null,
    block: T.() -> Unit
): T {
    return apply {
        ErrorHandler.handleExceptions(tag, errorMessage, showError, context, view) {
            block()
        }
    }
}

/**
 * Extension function to safely execute a suspending block of code with error handling
 */
suspend inline fun <T> T.safeExecuteAsync(
    tag: String,
    errorMessage: String,
    showError: Boolean = false,
    context: Context? = null,
    view: View? = null,
    crossinline block: suspend T.() -> Unit
): T {
    return apply {
        withErrorHandling(tag, errorMessage, context, view, showError) {
            block()
        }
    }
}