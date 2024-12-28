package org.example.github

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.example.ResponseStatus
import org.example.config.loadConfig


suspend fun createPullRequest(
    client: HttpClient,
    repoOwner: String,
    repoName: String,
    title: String,
    body: String,
    headBranch: String,
    baseBranch: String
): Boolean {
    val createPRUrl = "https://api.github.com/repos/$repoOwner/$repoName/pulls"

    val config = loadConfig().getOrElse {
        println("Failed to load config.json. Error: ${it.message}")
        return false
    }

    try {
        val createPRResponse = client.post(createPRUrl) {
            header("Authorization", "Bearer ${config.githubToken}")
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "title" to title,
                    "body" to body,
                    "head" to headBranch,
                    "base" to baseBranch
                )
            )
        }

        return when (val status = parseGitHubResponse(createPRResponse)) {
            is ResponseStatus.Success -> {
                println("Pull request created successfully. HTTP status: ${createPRResponse.status}")
                val prJson = createPRResponse.bodyAsText()
                println("Response: $prJson")
                true
            }

            is ResponseStatus.Forbidden -> {
                println("Forbidden: you do not have permissions to create a pull request")
                println("Error: ${createPRResponse.status}")
                false
            }

            is ResponseStatus.UnprocessableEntity -> {
                val errorBody = createPRResponse.bodyAsText()
                println("Validation error. HTTP status: ${createPRResponse.status}")
                println("Error: $errorBody")
                false
            }

            is ResponseStatus.Unexpected -> {
                println("Unexpected status. HTTP status: ${status.code}")
                false
            }

            else -> {
                val errorBody = createPRResponse.bodyAsText()
                println("Error occurred while creating pull request. HTTP status: ${createPRResponse.status}")
                println("Error: $errorBody")
                false
            }
        }
    } catch (e: Exception) {
        println("Exception occurred while creating pull request: ${e.message}")
        return false
    }
}