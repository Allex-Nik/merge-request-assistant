package org.example

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import org.example.github.*


/**
 * The entrance point of the program.
 */
suspend fun main() {

//    // Load the token
//    val config = loadConfig().getOrElse {
//        println("Failed to load config.json. Error: ${it.message}")
//        return
//    }

//    println("GitHub token: ${config.githubToken}")

    // Create a client
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
        install(DefaultRequest) {
//            header("Authorization", "Bearer ${config.githubToken}")
            accept(ContentType.Application.Json)
        }
    }

//    val userInfo: String = client.get("https://api.github.com/user").bodyAsText()
//    println("User info: $userInfo")

    try {
        // Get the list of repositories
        val repositories = interactiveGetRepositories(client)
        if (repositories == null) {
            println("Exiting program.")
            return
        }

        //    repositories.forEach { repo ->
        //        println("Name: ${repo.name}, URL: ${repo.html_url}, Private: ${repo.private}")
        //    }

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
            return
        }

        val selectedRepository = repositories[selectedIndex - 1]
        println("Selected repository: ${selectedRepository.name}")


        interactiveCreateBranch(
            client,
            selectedRepository.owner.login,
            selectedRepository.name,
            "test",
            "main"
        )

        interactiveAddFileToBranch(
            client,
            selectedRepository.owner.login,
            selectedRepository.name,
            "test",
            "Hello.txt",
            content = "Hello world"
        )

        interactiveCreatePullRequest(
            client,
            selectedRepository.owner.login,
            selectedRepository.name,
            title = "Add Hello.txt",
            body = "Added Hello.txt with Hello world",
            headBranch = "test",
            baseBranch = "main"
        )
    } finally {
        client.close()
    }
}