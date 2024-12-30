package org.example.models

import kotlinx.serialization.Serializable


/**
 * Class describing a GitHub repository.
 *
 * Used to deserialize data about a repository.
 *
 * @property name The name of the repository.
 * @property owner The owner of the repository.
 * @property html_url The URL of the repository.
 * @property private Indicates if the repository is private.
 */
@Serializable
data class Repository(
    val name: String,
    val owner: Owner,
    val html_url: String,
    val private: Boolean
)