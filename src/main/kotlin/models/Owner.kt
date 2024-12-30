package org.example.models

import kotlinx.serialization.Serializable


/**
 * Represents an owner of a GitHub repository.
 *
 * Used to deserialize data about an owner of a repository.
 *
 * @property login The unique username of the repository owner.
 */
@Serializable
data class Owner(val login: String)