package org.example

import io.ktor.http.*


/**
 * Represents possible statuses of a response from the GitHub API.
 */
sealed class ResponseStatus {
    /**
     * Indicates that the operation was successful.
     */
    data object Success : ResponseStatus()

    /**
     * Indicates that the operation was forbidden due to invalid or missing credentials.
     */
    data object Unauthorized : ResponseStatus()

    /**
     * Indicates that the operation was forbidden due to insufficient permissions.
     */
    data object Forbidden : ResponseStatus()

    /**
     * Indicates that a conflict occurred during the operation.
     */
    data object Conflict : ResponseStatus()

    /**
     * Indicates that the requested resource was not found.
     */
    data object NotFound : ResponseStatus()

    /**
     * Indicates that the result was unprocessable.
     */
    data object UnprocessableEntity : ResponseStatus()

    /**
     * Represents an unexpected status code.
     *
     * @property code The unexpected HTTP status code.
     */
    data class Unexpected(val code: HttpStatusCode) : ResponseStatus()
}
