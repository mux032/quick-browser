package com.quick.browser.domain.result

/**
 * A generic class that holds a value or an error
 *
 * @param T The type of the value
 * @param E The type of the error
 */
sealed class Result<out T, out E> {
    /**
     * Represents a successful result with a value
     *
     * @param data The value
     */
    data class Success<out T>(val data: T) : Result<T, Nothing>()

    /**
     * Represents a failure result with an error
     *
     * @param error The error
     */
    data class Failure<out E>(val error: E) : Result<Nothing, E>()

    companion object {
        /**
         * Creates a successful result
         *
         * @param data The value
         * @return A successful result
         */
        fun <T> success(data: T): Result<T, Nothing> = Success(data)

        /**
         * Creates a failure result
         *
         * @param error The error
         * @return A failure result
         */
        fun <E> failure(error: E): Result<Nothing, E> = Failure(error)
    }
}