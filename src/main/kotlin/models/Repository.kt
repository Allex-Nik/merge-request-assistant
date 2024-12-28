package org.example.models

import kotlinx.serialization.Serializable


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