package org.example.models

import kotlinx.serialization.Serializable

/**
 * Class describing the GitHub-token that is read from config.json.
 *
 * Used to deserialize the configuration data required for authentication.
 *
 * @property githubToken The token used to authenticate GitHub API requests.
 */
@Serializable
data class Config(val githubToken: String)