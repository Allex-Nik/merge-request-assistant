package org.example.github

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

                if (errorBody.contains("already exists")) {
                    println("A pull request with head $headBranch and base $baseBranch already exists")
                    val pullsUrl = "https://api.github.com/repos/$repoOwner/$repoName/pulls"
                    val response = client.get(pullsUrl) {
                        header("Authorization", "Bearer ${config.githubToken}")
                    }
                    val json = Json { ignoreUnknownKeys = true }
                    val pullRequests = json.parseToJsonElement(response.bodyAsText()).jsonArray
                    for (pr in pullRequests) {
                        val prObject = pr.jsonObject
                        val headRef = prObject["head"]?.jsonObject?.get("ref")?.jsonPrimitive?.content
                        val baseRef = prObject["base"]?.jsonObject?.get("ref")?.jsonPrimitive?.content
                        val htmlUrl = prObject["html_url"]?.jsonPrimitive?.content

                        if (headRef == headBranch && baseRef == baseBranch) {
                            println("Link: $htmlUrl")
                        }
                    }
                } else {
                    println("Validation error. HTTP status: ${createPRResponse.status}")
                    println("Error: $errorBody")
                }
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

suspend fun interactiveCreatePullRequest(
    client: HttpClient,
    repoOwner: String,
    repoName: String,
    title: String,
    body: String,
    headBranch: String,
    baseBranch: String
): Boolean {
    while (true) {
        val result = createPullRequest(client, repoOwner, repoName, title, body, headBranch, baseBranch)
        if (result) {
            return true
        }

        println("Do you want to retry? (yes/no)")
        val choice = readlnOrNull()?.lowercase()
        if (choice == "yes") {
            continue
        }
        return false
    }
}