package org.example

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import org.example.github.*
import org.example.models.Repository

/**
 * Stores default values as constants.
 */
object Constants {
    const val DEFAULT_BASE_BRANCH = "main"
    const val DEFAULT_HEAD_BRANCH = "hello"
    const val DEFAULT_FILE_NAME = "Hello.txt"
    const val DEFAULT_FILE_CONTENT = "Hello World"
    const val DEFAULT_PR_TITLE = "Add Hello.txt"
    const val DEFAULT_PR_DESCRIPTION = "Added Hello.txt with Hello world"
}

/**
 * The entrance point of the program.
 * Guides the user through the process of creating a pull request.
 */
suspend fun main() {

    val baseBranch = handleInput(
        "Enter the name of the base branch", Constants.DEFAULT_BASE_BRANCH)

    val headBranch = handleInput(
        "Enter the name of the head branch", Constants.DEFAULT_HEAD_BRANCH)

    val fileName = handleInput(
        "Enter the name of the file you want to add", Constants.DEFAULT_FILE_NAME)

    val content = handleInput(
        "Enter the content of the file you want to add", Constants.DEFAULT_FILE_CONTENT )

    val prTitle = handleInput(
        "Enter the title of your pull request", Constants.DEFAULT_PR_TITLE)

    val prDescription = handleInput(
        "Enter the description of your pull request", Constants.DEFAULT_PR_DESCRIPTION)

    val client = createHttpClient()

    client.use { httpClient ->
        // Get the list of repositories and let the user select one
        val selectedRepository = selectRepository(httpClient) ?: return

        // Create a new branch
        val finalBranchName = interactiveCreateBranch(
            httpClient,
            selectedRepository.owner.login,
            selectedRepository.name,
            headBranch,
            baseBranch
        )
        if (finalBranchName == null) return

        // Add a file to a new branch
        val fileAdded = interactiveAddFileToBranch(
            httpClient,
            selectedRepository.owner.login,
            selectedRepository.name,
            finalBranchName,
            fileName,
            content
        )
        if (!fileAdded) return

        // Create a pull request from the head branch to the base branch
        val prCreated = interactiveCreatePullRequest(
            httpClient,
            selectedRepository.owner.login,
            selectedRepository.name,
            title = prTitle,
            body = prDescription,
            headBranch = finalBranchName,
            baseBranch = baseBranch
        )
        if (!prCreated) return

        println("All steps completed successfully.")
    }
}

/**
 * Creates an HTTP client for interacting with the GitHub API.
 *
 * @return An instance of [HttpClient].
 */
fun createHttpClient() = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
    install(DefaultRequest) {
        accept(ContentType.Application.Json)
    }
}

/**
 * Prompts the user to select a repository from a list fetched from GitHub.
 *
 * @param client The HTTP client used to fetch the repositories.
 * @return The selected repositories, or null.
 */
suspend fun selectRepository(client: HttpClient): Repository? {
    val repositories = interactiveGetRepositories(client) ?: return null

    println("Available repositories: ${repositories.size}")
    repositories.forEachIndexed { index, repository ->
        println("${index + 1}. Name: ${repository.name}, URL: ${repository.html_url}, Private: ${repository.private}")
    }

    println("Enter the number of the repository: ")
    val selectedIndex = readlnOrNull()?.toIntOrNull()

    if (selectedIndex == null || selectedIndex !in 1..repositories.size) {
        println("Invalid selected repository index. Try again.")
        return null
    }

    return repositories[selectedIndex - 1].also { println("Selected repository: ${it.name}") }
}

/**
 * Handles user input to receive the necessary information to proceed.
 *
 * @param message The message that is displayed to the user.
 * @param default The default value to use.
 * @return The input of the user or the default value.
 */
fun handleInput(message: String, default: String): String {
    println("$message. Default: $default")
    return readlnOrNull()
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: default
}