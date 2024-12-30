package org.example.models

import kotlinx.serialization.Serializable


/**
 * Represents information about a branch in a GitHub repository.
 *
 * Used to deserialize the data about a branch from the GitHub responses.
 *
 * @property ref The name of the branch reference.
 * @property node_id A unique identifier for the branch name.
 * @property url The URL of the API endpoint for the branch
 * @property `object` The associated [BranchObject] containing details about the branch.
 */
@Serializable
data class BranchInfo(
    val ref: String,
    val node_id: String,
    val url: String,
    val `object`: BranchObject
)
