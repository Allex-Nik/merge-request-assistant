package org.example.models

import kotlinx.serialization.Serializable


/**
 * Represents data about a file in a GitHub repository.
 *
 * Used to deserialize information about a file obtained from the GitHub API.
 *
 * @property sha The SHA hash of the file, used to identify its version
 */
@Serializable
data class FileInfo(
    val sha: String
)