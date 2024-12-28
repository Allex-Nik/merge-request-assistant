package org.example.models

import kotlinx.serialization.Serializable

/**
 * Class describing the GitHub-token that is read from config.json.
 */
@Serializable
data class Config(val githubToken: String)