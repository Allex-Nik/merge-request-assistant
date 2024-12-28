package org.example.github

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import org.example.ResponseStatus
import org.example.config.loadConfig
import org.example.models.BranchInfo
import org.example.models.Repository
import java.util.*

suspend fun getBaseBranchSha(
    client: HttpClient,
    repoOwner: String,
    repoName: String,
    baseBranch: String
): Result<String> {
    val branchInfoUrl = "https://api.github.com/repos/$repoOwner/$repoName/git/refs/heads/$baseBranch"

    val config = loadConfig().getOrElse {
        return Result.failure(Exception("Failed to load config.json. Error: ${it.message}"))
    }

    return try {
        val branchInfoResponse = client.get(branchInfoUrl) {
            header("Authorization", "Bearer ${config.githubToken}")
            header("Accept", ContentType.Application.Json)
        }

//        println(branchInfoResponse.bodyAsText())
        when (val status = parseGitHubResponse(branchInfoResponse)) {
            is ResponseStatus.Success -> {
                val branchInfo = Json.decodeFromString<BranchInfo>(branchInfoResponse.bodyAsText())
                val branchSha = branchInfo.`object`.sha
                Result.success(branchSha)
            }

            is ResponseStatus.NotFound -> Result.failure(
                Exception(
                    "Base branch $baseBranch not found. HTTP status: ${branchInfoResponse.status}"
                )
            )

            is ResponseStatus.Conflict -> Result.failure(
                Exception(
                    "Conflict occurred while fetching branch. HTTP status: ${branchInfoResponse.status}"
                )
            )

            is ResponseStatus.Unexpected -> Result.failure(
                Exception(
                    "Unexpected response when fetching branch. HTTP status: ${status.code}"
                )
            )

            else -> Result.failure(Exception("Unknown response status ${branchInfoResponse.status}"))
        }


    } catch (e: Exception) {
        Result.failure(Exception("Exception occurred while obtaining the base branch SHA: ${e.message}"))
    }
}

suspend fun createBranch(
    client: HttpClient,
    repoOwner: String,
    repoName: String,
    branchName: String,
    baseBranch: String): Boolean {

    val branchShaResult = getBaseBranchSha(client, repoOwner, repoName, baseBranch)
    val branchSha = branchShaResult.getOrElse {
        println("Failed to fetch base branch SHA: ${it.message}")
        return false
    }

    val config = loadConfig().getOrElse {
        println("Failed to load config.json. Error: ${it.message}")
        return false
    }

    try {
        val createBranchUrl = "https://api.github.com/repos/$repoOwner/$repoName/git/refs"
        val createBranchResponse = client.post(createBranchUrl) {
            header("Authorization", "Bearer ${config.githubToken}")
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "ref" to "refs/heads/$branchName",
                    "sha" to branchSha
                )
            )
        }

        return when (val status = parseGitHubResponse(createBranchResponse)) {
            is ResponseStatus.Success -> {
                println("Branch $branchName created successfully. HTTP status: ${createBranchResponse.status}")
                true
            }

            is ResponseStatus.Conflict -> {
                println("Conflict: branch $branchName already exists. HTTP status: ${createBranchResponse.status}")
                false
            }

            is ResponseStatus.UnprocessableEntity -> {
                println("Unprocessable entity. HTTP status: ${createBranchResponse.status}")
                false
            }

            is ResponseStatus.Unexpected -> {
                println("Unexpected status: ${status.code}")
                false
            }

            else -> {
                println("Error occurred while creating branch. HTTP status: ${createBranchResponse.status}")
                false
            }
        }
    } catch (e: Exception) {
        println("Exception occurred while creating branch: ${e.message}")
        return false
    }
}

suspend fun addFileToBranch(
    client: HttpClient,
    repoOwner: String,
    repoName: String,
    branchName: String,
    filePath: String,
    content: String): Boolean {
    val addFileUrl = "https://api.github.com/repos/$repoOwner/$repoName/contents/$filePath"

    val config = loadConfig().getOrElse {
        println("Failed to load config.json. Error: ${it.message}")
        return false
    }

    try {
        val encodedContent = Base64.getEncoder().encodeToString(content.toByteArray())
        //    println(encodedContent)
        val addFileResponse = client.put(addFileUrl) {
            header("Authorization", "Bearer ${config.githubToken}")
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "message" to "Add $filePath",
                    "content" to encodedContent,
                    "branch" to branchName
                )
            )
        }
        return when (val status = parseGitHubResponse(addFileResponse)) {
            is ResponseStatus.Success -> {
                println("File added successfully. HTTP status: ${addFileResponse.status}")
                true
            }

            is ResponseStatus.Conflict -> {
                println("Conflict: file $filePath already exists. HTTP status: ${addFileResponse.status}")
                false
            }

            is ResponseStatus.UnprocessableEntity -> {
                val errorBody = addFileResponse.bodyAsText()
                println("Unprocessable entity: possibly invalid branch or content. HTTP status: ${addFileResponse.status}")
                println("Error: $errorBody")
                false
            }

            else -> {
                val errorBody = addFileResponse.bodyAsText()
                println("Error occurred while adding file. HTTP status: ${addFileResponse.status}")
                println("Error: $errorBody")
                false
            }
        }
    } catch (e: Exception) {
        println("Exception occurred while adding file: ${e.message}")
        return false
    }
}

suspend fun getRepositories(client: HttpClient, token: String): Result<List<Repository>> {
    return try {
        val response = client.get("https://api.github.com/user/repos") {
            header("Authorization", "Bearer $token")
        }
        //        println(response.bodyAsText())

        when (val status = parseGitHubResponse(response)) {
            is ResponseStatus.Success -> {
                val json = Json { ignoreUnknownKeys = true }
                val repositories = json.decodeFromString<List<Repository>>(response.bodyAsText())
                Result.success(repositories)
            }

            is ResponseStatus.Unauthorized -> {
                Result.failure(Exception("Invalid GitHub token: Unauthorized. Please check your token in config.json file."))
            }

            is ResponseStatus.Forbidden -> {
                Result.failure(Exception("Insufficient permissions for your GitHub token. Please check the permissions."))
            }

            is ResponseStatus.Unexpected -> {
                Result.failure(Exception("Error occurred while fetching repositories. HTTP status: ${status.code}."))
            }

            else -> {
                Result.failure(Exception("Unknown status: ${response.status}."))
            }
        }
    } catch (e: Exception) {
        Result.failure(Exception("Exception occurred while fetching repositories: ${e.message}"))
    }
}

suspend fun interactiveGetRepositories(client: HttpClient): List<Repository>? {
    var currentConfig = loadConfig().getOrElse {
        println("Failed to load config.json. Error: ${it.message}")
        return null
    }

    while (true) {
        val repositoriesResult = getRepositories(client, currentConfig.githubToken)
        if (repositoriesResult.isFailure) {
            println("Failed to fetch repositories: ${repositoriesResult.exceptionOrNull()?.message}")
            println("Do you want to retry? (yes/no)")
            val choice = readlnOrNull()?.lowercase()
            if (choice != "yes") {
                return null
            }

            currentConfig = loadConfig().getOrElse {
                println("Failed to load config.json. Error: ${it.message}")
                return null
            }

            continue
        }
        val repositories = repositoriesResult.getOrNull() ?: emptyList()

        // Check if the list of repositories is empty
        if (repositories.isEmpty()) {
            println("No repositories found for your user. Make sure your account has repositories.")
            println("Do you want to retry? (yes/no)")
            val choice = readlnOrNull()?.lowercase()
            if (choice != "yes") {
                return null
            }
            continue
        }
        return repositories
    }
}