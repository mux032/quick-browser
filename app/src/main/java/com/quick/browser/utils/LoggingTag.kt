package com.quick.browser.utils

/**
 * Interface for classes that need to provide a logging tag
 */
interface LoggingTag {
    val tag: String
        get() = this::class.java.simpleName
}