package org.example.github

import io.ktor.client.statement.*
import io.ktor.http.*
import org.example.ResponseStatus

/**
 * Parses an HTTP response from GitHub and maps it to a [ResponseStatus].
 *
 * @param response The HTTP response from GitHub to map.
 * @return A [ResponseStatus] that represents the response.
 */
fun parseGitHubResponse(response: HttpResponse): ResponseStatus {
    return when {
        response.status == HttpStatusCode.Unauthorized -> ResponseStatus.Unauthorized
        response.status == HttpStatusCode.Forbidden -> ResponseStatus.Forbidden
        response.status == HttpStatusCode.Conflict -> ResponseStatus.Conflict
        response.status == HttpStatusCode.NotFound -> ResponseStatus.NotFound
        response.status == HttpStatusCode.UnprocessableEntity -> ResponseStatus.UnprocessableEntity
        !response.status.isSuccess() -> ResponseStatus.Unexpected(response.status)
        else -> ResponseStatus.Success
    }
}
