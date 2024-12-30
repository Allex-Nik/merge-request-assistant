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

/**
 * Creates a pull request on GitHub.
 *
 * Send a POST request to the GitHub API to create a pull request in the specified repository.
 * If the operation is successful, print the details of the pull request.
 * Otherwise, print an error message and suggest to try again.
 *
 * @param client An instance of [HttpClient] used to make HTTP requests.
 * @param repoOwner The owner of the repository.
 * @param repoName The name of the repository.
 * @param title The title of the pull request.
 * @param body The description of the pull request.
 * @param headBranch The name of the branch from which the changes will be merged.
 * @param baseBranch The name of the branch to which the changes will be merged.
 * @return `true` if the pull request was successfully created, `false` otherwise.
 */
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
        // Send a POST request
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

        // Handle the response
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

                // If the PR already exists, gives the link to the PR and finishes the program
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

/**
 * Repeatedly attempts to create a pull request using [createPullRequest]
 * until the pull request is created or the users decides to stop trying.
 *
 * @param client An instance of [HttpClient] used to make HTTP requests.
 * @param repoOwner The owner of the repository.
 * @param repoName The name of the repository.
 * @param title The title of the pull request.
 * @param body The description of the pull request.
 * @param headBranch The name of the branch from which the changes will be merged.
 * @param baseBranch The name of the branch to which the changes will be merged.
 * @return `true` if the pull request was successfully created, `false` otherwise.
 */
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