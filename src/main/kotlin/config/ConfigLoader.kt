package org.example.config

import kotlinx.serialization.json.Json
import org.example.models.Config
import java.io.File

/**
 * Read config.json and load the token.
 * If the file does not exist or parsing was not successful, throw exception and return.
 */
fun loadConfig(): Result<Config> {
    return try {
        val configFile = File("src/main/resources/config.json")
        if (!configFile.exists()) {
            throw IllegalStateException("config.json file was not found: ${configFile.absolutePath}")
        }
        val jsonContent = configFile.readText()
        val config = Json.decodeFromString(Config.serializer(), jsonContent)
        Result.success(config)
    } catch (e: Exception) {
        Result.failure(e)
    }
}