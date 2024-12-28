package org.example.github

import io.ktor.client.statement.*
import io.ktor.http.*
import org.example.ResponseStatus


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
