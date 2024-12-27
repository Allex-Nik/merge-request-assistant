package org.example

import java.util.Base64
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
//import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Class describing the GitHub-token that is read from config.json.
 */
@Serializable
data class Config(val githubToken: String)

@Serializable
data class Owner(val login: String)

/**
 * Class describing a GitHub repository.
 */
@Serializable
data class Repository(
    val name: String,
    val owner: Owner,
    val html_url: String,
    val private: Boolean
)

@Serializable
data class BranchInfo(
    val ref: String,
    val node_id: String,
    val url: String,
    val `object`: BranchObject
)

@Serializable
data class BranchObject(
    val sha: String,
    val type: String,
    val url: String
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

suspend fun createBranch(client: HttpClient, repoOwner: String, repoName: String, branchName: String, baseBranch: String) {
    val branchInfoUrl = "https://api.github.com/repos/$repoOwner/$repoName/git/refs/heads/$baseBranch"

    try {
        val branchInfoResponse = client.get(branchInfoUrl) {
            header("Accept", ContentType.Application.Json)
        }

//        println(branchInfoResponse.bodyAsText())

        when {
            branchInfoResponse.status == HttpStatusCode.NotFound -> {
                println("Branch not found. Make sure the base branch exists. HTTP status: ${branchInfoResponse.status}")
                client.close()
                return
            }

            branchInfoResponse.status == HttpStatusCode.Conflict -> {
                println("Conflict occured while fetching branch. HTTP status: ${branchInfoResponse.status}.")
                println("Make sure the reference format is correct.")
            }

            !branchInfoResponse.status.isSuccess() -> {
                println("Error occurred while obtaining the base branch info. HTTP status: ${branchInfoResponse.status}")
                client.close()
                return
            }
        }

        val branchInfo = Json.decodeFromString<BranchInfo>(branchInfoResponse.bodyAsText())
        val branchSha = branchInfo.`object`.sha

        val createBranchUrl = "https://api.github.com/repos/$repoOwner/$repoName/git/refs"
        val createBranchResponse = client.post(createBranchUrl) {
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "ref" to "refs/heads/$branchName",
                    "sha" to branchSha
                )
            )
        }

        when (createBranchResponse.status) {
            HttpStatusCode.Created -> {
                println("Branch $branchName created successfully. HTTP status: ${createBranchResponse.status}")
            }

            HttpStatusCode.Conflict -> {
                println("Conflict: branch $branchName already exists. HTTP status: ${createBranchResponse.status}")
            }

            HttpStatusCode.UnprocessableEntity -> {
                val errorBody = createBranchResponse.bodyAsText()
                println("Unprocessable entity: ${createBranchResponse.status}")
                println(errorBody)
                client.close()
                return
            }

            else -> {
                println("Error occurred while creating branch. HTTP status: ${createBranchResponse.status}")
                client.close()
                return
            }
        }
    } catch (e: Exception) {
        println("Exception occurred while creating branch: ${e.message}")
    }
}

suspend fun addFileToBranch(client: HttpClient, repoOwner: String, repoName: String, branchName: String, filePath: String, content: String) {
    val addFileUrl = "https://api.github.com/repos/$repoOwner/$repoName/contents/$filePath"
    val encodedContent = Base64.getEncoder().encodeToString(content.toByteArray())
//    println(encodedContent)
    val addFileResponse = client.put(addFileUrl) {
        contentType(ContentType.Application.Json)
        setBody(
            mapOf(
                "message" to "Add $filePath",
                "content" to encodedContent,
                "branch" to branchName
            )
        )
    }
    when (addFileResponse.status) {
        HttpStatusCode.OK, HttpStatusCode.Created -> {
            println("File added successfully. HTTP status: ${addFileResponse.status}")
        }

        HttpStatusCode.Conflict -> {
            println("Conflict: file $filePath already exists. HTTP status: ${addFileResponse.status}")
        }

        HttpStatusCode.UnprocessableEntity -> {
            val errorBody = addFileResponse.bodyAsText()
            println("Unprocessable entity: possibly invalid branch or content. HTTP status: ${addFileResponse.status}")
            println("Error: $errorBody")
        }

        else -> {
            val errorBody = addFileResponse.bodyAsText()
            println("Error occurred while adding file. HTTP status: ${addFileResponse.status}")
            println("Error: $errorBody")
        }
    }
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
        install(ContentNegotiation) {
            json()
        }
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
//        println(response.bodyAsText())

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


    createBranch(client,
        selectedRepository.owner.login,
        selectedRepository.name,
        "test",
        "main")

    addFileToBranch(client,
        selectedRepository.owner.login,
        selectedRepository.name,
        "test",
        "Hello.txt",
        content = "Hello world"
    )

    client.close()
}