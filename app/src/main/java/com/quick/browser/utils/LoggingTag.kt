package com.quick.browser.utils

/**
 * Interface for classes that need to provide a logging tag
 *
 * This interface provides a convenient way for classes to automatically
 * generate a logging tag based on their class name. Classes implementing
 * this interface can use the default implementation which uses the simple
 * class name as the tag, or override it to provide a custom tag.
 */
interface LoggingTag {
    /**
     * The logging tag for this class
     *
     * By default, this property returns the simple name of the class.
     * Classes can override this property to provide a custom logging tag.
     */
    val tag: String
        get() = this::class.java.simpleName
}