package org.example

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Class describing the GitHub-token that is read from config.json.
 */
@Serializable
data class Config(val githubToken: String)

/**
 * Class describing a GitHub repository.
 */
@Serializable
data class Repository(
    val name: String,
    val html_url: String,
    val private: Boolean
)

/**
 * Read config.json and load the token.
 * If the file does not exist or parsing was not successful, throw exception and return.
 */
fun loadConfig(): Config {
    val configFile = File("src/main/resources/config.json")
    if (!configFile.exists()) {
        throw IllegalStateException("config.json file was not found: ${configFile.absolutePath}")
    }
    val jsonContent = configFile.readText()
    return Json.decodeFromString(Config.serializer(), jsonContent)
}

/**
 * The entrance point of the program.
 */
suspend fun main() {

    // Load the token
    val config: Config
    try {
        config = loadConfig()
    } catch (e: Exception) {
        println("Failed to load config.json. Error: ${e.message}")
        return
    }

//    println("GitHub token: ${config.githubToken}")

    // Create a client
    val client = HttpClient(CIO) {
        install(io.ktor.client.plugins.DefaultRequest) {
            header("Authorization", "Bearer ${config.githubToken}")
            accept(ContentType.Application.Json)
        }
    }

//    val userInfo: String = client.get("https://api.github.com/user").bodyAsText()
//    println("User info: $userInfo")

    // Get the list of repositories
    val repositories: List<Repository> = try {
        val response = client.get("https://api.github.com/user/repos")

        when {
            response.status == HttpStatusCode.Unauthorized -> {
                println("Invalid GitHub token: Unauthorized. Please check your token in config.json file.")
                client.close()
                return
            }
            response.status == HttpStatusCode.Forbidden -> {
                println("Insufficient permissions for your GitHub token. Please check the permissions.")
                client.close()
                return
            }
            !response.status.isSuccess() -> {
                println("Error occured while fetching repositories. HTTP status: ${response.status}.")
                client.close()
                return
            }
        }

        val json = Json { ignoreUnknownKeys = true}
        json.decodeFromString<List<Repository>>(response.bodyAsText())
    } catch (e: Exception) {
        println("Exception occured while fetching repositories: ${e.message}")
        client.close()
        return
    }

    // Check if the list of repositories is empty
    if (repositories.isEmpty()) {
        println("No repositories found for your user. Make sure your account has repositories.")
        client.close()
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
        client.close()
        return
    }

    val selectedRepository = repositories[selectedIndex - 1]
    println("Selected repository: ${selectedRepository.name}")

    client.close()
}