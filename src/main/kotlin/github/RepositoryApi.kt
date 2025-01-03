package org.example.github

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import org.example.ResponseStatus
import org.example.config.loadConfig
import org.example.models.BranchInfo
import org.example.models.FileInfo
import org.example.models.Repository
import java.util.*

/**
 * Fetches the SHA of the base branch in the specified GitHub repository.
 *
 * @param client An instance of [HttpClient] used for making HTTP requests.
 * @param repoOwner The owner of the repository.
 * @param repoName The name of the repository.
 * @param baseBranch The name of the base branch.
 * @return A [Result] containing the SHA of the base branch if the request succeeds,
 * error message otherwise.
 */
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

        when (val status = parseGitHubResponse(branchInfoResponse)) {
            is ResponseStatus.Success -> {
                try {
                    val branchInfo = Json.decodeFromString<BranchInfo>(branchInfoResponse.bodyAsText())
                    val branchSha = branchInfo.`object`.sha
                    Result.success(branchSha)
                } catch (e: Exception) {
                    Result.failure(
                        Exception(
                            """Branch $baseBranch not found.
                                |Details: ${e.message}
                            """.trimMargin()
                        )
                    )
                }

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

/**
 * Creates a new branch in the specified GitHub repository.
 *
 * Calls [getBaseBranchSha] to get the SHA of the base branch,
 * and sends a request to the GitHub API to create a new branch.
 *
 * @param client An instance of [HttpClient] used for making HTTP requests.
 * @param repoOwner The owner of the repository.
 * @param repoName The name of the repository.
 * @param branchName The name of the new branch to create.
 * @param baseBranch The name of the base branch.
 */
suspend fun createBranch(
    client: HttpClient,
    repoOwner: String,
    repoName: String,
    branchName: String,
    baseBranch: String
): String? {

    val branchShaResult = getBaseBranchSha(client, repoOwner, repoName, baseBranch)

    if (branchShaResult.isFailure) {
        val errorMessage = branchShaResult.exceptionOrNull()?.message ?: "Unknown error"
        println(errorMessage)
        return null
    }
    val branchSha = branchShaResult.getOrElse {
        println("Failed to fetch base branch SHA: ${it.message}")
        return null
    }

    val config = loadConfig().getOrElse {
        println("Failed to load config.json. Error: ${it.message}")
        return null
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
                branchName
            }

            is ResponseStatus.Conflict -> {
                println("Conflict: HTTP status: ${createBranchResponse.status}")
                null
            }

            is ResponseStatus.UnprocessableEntity -> {
                println("Unprocessable entity. HTTP status: ${createBranchResponse.status}")
                val errorBody = createBranchResponse.bodyAsText()
                if (errorBody.contains("Reference already exists")) {
                    println("Branch already exists")
                    println("Do you want to use the existing branch? (yes/no)")
                    val useExisting = readlnOrNull()?.lowercase()
                    if (useExisting == "yes") return branchName

                    println("Do you want to retry with a different branch name? (yes/no)")
                    val retryNewName = readlnOrNull()?.lowercase()
                    if (retryNewName == "yes") {
                        println("Enter a new branch name: ")
                        val newBranchName = readlnOrNull()?.trim()
                        if (!newBranchName.isNullOrEmpty()) {
                            return createBranch(client, repoOwner, repoName, newBranchName, baseBranch)
                        } else {
                            println("Invalid branch name. Exiting.")
                            return null
                        }
                    }
                }
                null
            }

            is ResponseStatus.Unexpected -> {
                println("Unexpected status: ${status.code}")
                null
            }

            else -> {
                println("Error occurred while creating branch. HTTP status: ${createBranchResponse.status}")
                null
            }
        }
    } catch (e: Exception) {
        println("Exception occurred while creating branch: ${e.message}")
        return null
    }
}

/**
 * Repeatedly tries to create a new branch by calling [createBranch]
 * until it succeeds or the user decides to stop trying.
 *
 * @param client An instance of [HttpClient] used for making HTTP requests.
 * @param repoOwner The owner of the repository.
 * @param repoName The name of the repository.
 * @param branchName The name of the new branch to create.
 * @param baseBranch The name of the base branch.
 */
suspend fun interactiveCreateBranch(
    client: HttpClient,
    repoOwner: String,
    repoName: String,
    branchName: String,
    baseBranch: String
): String? {
    while (true) {
        val resultBranch = createBranch(client, repoOwner, repoName, branchName, baseBranch)
        if (resultBranch != null) {
            return resultBranch
        }

        println("Do you want to retry? (yes/no)")
        val choice = readlnOrNull()?.lowercase()
        if (choice == "yes") {
            continue
        }
        return null
    }
}

/**
 * Adds a file to the specified branch in the GitHub repository.
 *
 * @param client An instance of [HttpClient] used for making HTTP requests.
 * @param repoOwner The owner of the repository.
 * @param repoName The name of the repository.
 * @param branchName The name of the new branch to create.
 * @param filePath The path where the file should be added.
 * @param content The content of the file to be added.
 * @return `true` if the file was added successfully, `false` otherwise.
 */
suspend fun addFileToBranch(
    client: HttpClient,
    repoOwner: String,
    repoName: String,
    branchName: String,
    filePath: String,
    content: String
): Boolean {
    val config = loadConfig().getOrElse {
        println("Failed to load config.json. Error: ${it.message}")
        return false
    }
    val fileSha = checkIfFileExists(client, repoOwner, repoName, filePath, branchName)

    return if (fileSha != null) {
        println("File $filePath already exists in branch $branchName.")
        println("Do you want to replace the file? (yes/no)")
        val choice = readlnOrNull()?.lowercase()
        if (choice == "yes") {
            return replaceFileInBranch(client, repoOwner, repoName, branchName, filePath, content)
        } else {
            false
        }
    } else {

        try {
            val encodedContent = Base64.getEncoder().encodeToString(content.toByteArray())
            val addFileUrl = "https://api.github.com/repos/$repoOwner/$repoName/contents/$filePath"
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
}

/**
 * Checks if a file exists in the specified GitHub repository.
 *
 * Sends a request to the GitHub API to check if the file exists at the specified path.
 * If the file exists, returns its SHA.
 *
 * @param client An instance of [HttpClient] used for making HTTP requests.
 * @param repoOwner The owner of the repository.
 * @param repoName The name of the repository.
 * @param filePath The path to the file.
 */
suspend fun checkIfFileExists(
    client: HttpClient,
    repoOwner: String,
    repoName: String,
    filePath: String,
    branchName: String
): String? {
    val fileUrl = "https://api.github.com/repos/$repoOwner/$repoName/contents/$filePath?ref=$branchName"
    val config = loadConfig().getOrElse {
        println("Failed to load config.json. Error: ${it.message}")
        return null
    }

    return try {
        val response = client.get(fileUrl) {
            header("Authorization", "Bearer ${config.githubToken}")
        }

        if (response.status == HttpStatusCode.OK) {
            val json = Json { ignoreUnknownKeys = true }
            val fileInfo = json.decodeFromString<FileInfo>(response.bodyAsText())
            fileInfo.sha
        } else {
            null
        }
    } catch (e: Exception) {
        println("Exception occurred while checking file existence: ${e.message}")
        null
    }
}

/**
 * Replaces the content of an existing file in the specified branch of the GitHub repository.
 *
 * Retrieves the SHA of the current file, then updates the file content with the new content.
 *
 * @param client An instance of [HttpClient] used for making HTTP requests.
 * @param repoOwner The owner of the repository.
 * @param repoName The name of the repository.
 * @param branchName The name of the new branch to create.
 * @param filePath The path to the file.
 * @param newContent The new content.
 * @return `true` if the file was updated successfully, `false` otherwise.
 */
suspend fun replaceFileInBranch(
    client: HttpClient,
    repoOwner: String,
    repoName: String,
    branchName: String,
    filePath: String,
    newContent: String
): Boolean {
    val replaceFileUrl = "https://api.github.com/repos/$repoOwner/$repoName/contents/$filePath?ref=$branchName"

    val config = loadConfig().getOrElse {
        println("Failed to load config.json. Error: ${it.message}")
        return false
    }

    try {
        val replaceFileResponse = client.get(replaceFileUrl) {
            header("Authorization", "Bearer ${config.githubToken}")
            header("Accept", ContentType.Application.Json)
        }

        if (!replaceFileResponse.status.isSuccess()) {
            println("Failed to fetch the file details. HTTP status: ${replaceFileResponse.status}")
            return false
        }

        val json = Json { ignoreUnknownKeys = true }
        val fileInfo = json.decodeFromString<FileInfo>(replaceFileResponse.bodyAsText())
        val currentSha = fileInfo.sha

        val encodedContent = Base64.getEncoder().encodeToString(newContent.toByteArray())
        val updateFileResponse = client.put(replaceFileUrl) {
            header("Authorization", "Bearer ${config.githubToken}")
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "message" to "Update $filePath",
                    "content" to encodedContent,
                    "branch" to branchName,
                    "sha" to currentSha
                )
            )
        }
        return if (updateFileResponse.status == HttpStatusCode.OK) {
            println("File $filePath is updated successfully in branch $branchName")
            true
        } else {
            println("Failed to update $filePath. HTTP status: ${updateFileResponse.status}")
            println("Error: ${updateFileResponse.bodyAsText()}")
            false
        }
    } catch (e: Exception) {
        println("Exception occurred while updating file: ${e.message}")
        return false
    }
}

/**
 * Repeatedly tries to add a file to the specified branch in the GitHub repository.
 *
 * @param client An instance of [HttpClient] used for making HTTP requests.
 * @param repoOwner The owner of the repository.
 * @param repoName The name of the repository.
 * @param branchName The name of the new branch to create.
 * @param filePath The path where the file will be added.
 * @param content The content of the file to be added.
 * @return `true` if the file was added successfully, `false` otherwise.
 */
suspend fun interactiveAddFileToBranch(
    client: HttpClient,
    repoOwner: String,
    repoName: String,
    branchName: String,
    filePath: String,
    content: String
): Boolean {
    while (true) {
        val result = addFileToBranch(client, repoOwner, repoName, branchName, filePath, content)
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

/**
 * Fetches the list of repositories in the specified GitHub account.
 *
 * @param client An instance of [HttpClient] used for making HTTP requests.
 * @param token The GitHub token for authentication.
 * @return A [Result] containing the list of repositories, or an error.
 */
suspend fun getRepositories(client: HttpClient, token: String): Result<List<Repository>> {
    return try {
        val response = client.get("https://api.github.com/user/repos") {
            header("Authorization", "Bearer $token")
        }

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

/**
 * Repeatedly tries to get the list of repositories in the specified GitHub account.
 *
 * @param client An instance of [HttpClient] used for making HTTP requests.
 * @return A list of repositories, or null.
 */
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