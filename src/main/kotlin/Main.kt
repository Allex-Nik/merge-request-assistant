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
 */
suspend fun main() {
//    println("GitHub token: ${config.githubToken}")

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

//    val userInfo: String = client.get("https://api.github.com/user").bodyAsText()
//    println("User info: $userInfo")

    val client = createHttpClient()
    try {
        // Get the list of repositories
        val selectedRepository = selectRepository(client) ?: return

        val finalBranchName = interactiveCreateBranch(
            client,
            selectedRepository.owner.login,
            selectedRepository.name,
            headBranch,
            baseBranch
        )
        if (finalBranchName == null) {
            return
        }

        val fileAdded = interactiveAddFileToBranch(
            client,
            selectedRepository.owner.login,
            selectedRepository.name,
            finalBranchName,
            fileName,
            content
        )
        if (!fileAdded) {
            return
        }

        val prCreated = interactiveCreatePullRequest(
            client,
            selectedRepository.owner.login,
            selectedRepository.name,
            title = prTitle,
            body = prDescription,
            headBranch = finalBranchName,
            baseBranch = baseBranch
        )
        if (!prCreated) {
            return
        }

        println("All steps completed successfully.")
    } finally {
        client.close()
    }
}

fun createHttpClient() = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
    install(DefaultRequest) {
        accept(ContentType.Application.Json)
    }
}

suspend fun selectRepository(client: HttpClient): Repository? {
    val repositories = interactiveGetRepositories(client) ?: return null

    // Print the list of the repositories
    println("Available repositories: ${repositories.size}")
    repositories.forEachIndexed { index, repository ->
        println("${index + 1}. Name: ${repository.name}, URL: ${repository.html_url}, Private: ${repository.private}")
    }

    // Ask the user to choose a repository and save the choice
    println("Enter the number of the repository: ")
    val selectedIndex = readlnOrNull()?.toIntOrNull()

    // Check the input
    if (selectedIndex == null || selectedIndex !in 1..repositories.size) {
        println("Invalid selected repository index. Try again.")
        return null
    }

    return repositories[selectedIndex - 1].also { println("Selected repository: ${it.name}") }
}

fun handleInput(message: String, default: String): String {
    println("$message. Default: $default")
    return readlnOrNull()
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: default
}