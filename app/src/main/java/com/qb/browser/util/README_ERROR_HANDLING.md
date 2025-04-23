# Error Handling in Bubble Browser

This document describes the error handling patterns used in the Bubble Browser application.

## ErrorHandler

The `ErrorHandler` class provides a centralized way to handle errors consistently throughout the application. It offers several approaches to error handling, from simple try-catch blocks to more sophisticated Result-based and coroutine-based error handling.

## Basic Usage

### 1. Using the Object Methods Directly

```kotlin
// Simple error logging
ErrorHandler.logError(
    tag = "MyClass",
    message = "Something went wrong",
    throwable = exception
)

// Handle exceptions with null fallback
val result = ErrorHandler.handleExceptions(
    tag = "MyClass",
    errorMessage = "Failed to perform operation",
    block = {
        // Your code that might throw an exception
        riskyOperation()
    }
)

// Handle exceptions with custom fallback
val result = ErrorHandler.handleExceptionsWithFallback(
    tag = "MyClass",
    errorMessage = "Failed to perform operation",
    fallback = defaultValue,
    block = {
        // Your code that might throw an exception
        riskyOperation()
    }
)
```

### 2. Using Extension Functions (Recommended)

```kotlin
// Using Result-based error handling
val result = runCatching {
    riskyOperation()
}.onError(tag = "MyClass") { 
    "Failed to perform operation: ${it.message}" 
}.getOrNull()

// With fallback value
val result = runCatching {
    riskyOperation()
}.getOrDefault(defaultValue, tag = "MyClass") { 
    "Failed to perform operation: ${it.message}" 
}

// With user feedback
runCatching {
    riskyOperation()
}.onError(
    tag = "MyClass",
    context = this,
    view = myView,
    showError = true
) { 
    "Operation failed" 
}
```

### 3. In Coroutines

```kotlin
// In a coroutine with error handling
lifecycleScope.launch {
    val result = withErrorHandling(
        tag = "MyClass",
        errorMessage = "Failed to perform async operation"
    ) {
        // Suspending code that might throw
        riskyAsyncOperation()
    }
    
    // Process result (might be null if an error occurred)
    result?.let { processResult(it) }
}

// With fallback value
lifecycleScope.launch {
    val result = withErrorHandlingAndFallback(
        tag = "MyClass",
        errorMessage = "Failed to perform async operation",
        fallback = defaultValue
    ) {
        // Suspending code that might throw
        riskyAsyncOperation()
    }
    
    // Process result (will be defaultValue if an error occurred)
    processResult(result)
}
```

## Best Practices

1. **Use Descriptive Tags**: Always use a meaningful tag (usually the class name) for error logging.

2. **Provide Helpful Error Messages**: Error messages should be descriptive and help identify what operation failed.

3. **Consider User Feedback**: Use the `showError`, `context`, and `view` parameters when appropriate to provide user feedback.

4. **Prefer Extension Functions**: The extension functions provide a more concise and readable way to handle errors.

5. **Use Result-Based Handling for Complex Flows**: For complex error handling flows, use the Result-based approach for better composability.

6. **Use Coroutine Extensions for Async Code**: For suspending functions, use the coroutine-specific error handling extensions.

## Examples

### Example 1: Loading Data from Network

```kotlin
suspend fun loadData(): Data = 
    withErrorHandlingAndFallback(
        tag = "DataRepository",
        errorMessage = "Failed to load data from network",
        fallback = Data.Empty
    ) {
        api.fetchData()
    }
```

### Example 2: Processing User Input

```kotlin
fun processUserInput(input: String): Boolean {
    return runCatching {
        validateInput(input)
        saveInput(input)
        true
    }.onError(
        tag = "InputProcessor",
        context = this,
        view = rootView,
        showError = true
    ) { 
        "Invalid input: ${it.message}" 
    }.getOrDefault(false)
}
```

### Example 3: Background Task in Activity

```kotlin
private fun performBackgroundTask() {
    lifecycleScope.launch {
        // Show loading indicator
        progressBar.visibility = View.VISIBLE
        
        val result = withErrorHandling(
            tag = "MyActivity",
            errorMessage = "Background task failed",
            context = this@MyActivity,
            view = rootView,
            showError = true
        ) {
            repository.performLongRunningTask()
        }
        
        // Hide loading indicator
        progressBar.visibility = View.GONE
        
        // Process result
        result?.let { updateUI(it) }
    }
}
```