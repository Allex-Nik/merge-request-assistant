package org.example.models

import kotlinx.serialization.Serializable

/**
 * Represents an object associated with a branch in a GitHub repository.
 *
 * @property sha The SHA hash of the commit that the branch points to.
 * @property type The type of the object.
 * @property url The URL of the API endpoint for this object.
 */
@Serializable
data class BranchObject(
    val sha: String,
    val type: String,
    val url: String
)