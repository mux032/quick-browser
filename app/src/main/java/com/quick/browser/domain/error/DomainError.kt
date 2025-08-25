package com.quick.browser.domain.error

/**
 * Base class for all domain errors
 */
sealed class DomainError {
    /**
     * Represents a network error
     *
     * @param message The error message
     * @param cause The cause of the error, if available
     */
    data class NetworkError(val message: String, val cause: Throwable? = null) : DomainError()

    /**
     * Represents a database error
     *
     * @param message The error message
     * @param cause The cause of the error, if available
     */
    data class DatabaseError(val message: String, val cause: Throwable? = null) : DomainError()

    /**
     * Represents a validation error
     *
     * @param message The error message
     */
    data class ValidationError(val message: String) : DomainError()

    /**
     * Represents a general error
     *
     * @param message The error message
     * @param cause The cause of the error, if available
     */
    data class GeneralError(val message: String, val cause: Throwable? = null) : DomainError()
}