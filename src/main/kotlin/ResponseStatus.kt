package org.example

import io.ktor.http.*


sealed class ResponseStatus {
    data object Success : ResponseStatus()
    data object Unauthorized : ResponseStatus()
    data object Forbidden : ResponseStatus()
    data object Conflict : ResponseStatus()
    data object NotFound : ResponseStatus()
    data object UnprocessableEntity : ResponseStatus()
    data class Unexpected(val code: HttpStatusCode) : ResponseStatus()
}
