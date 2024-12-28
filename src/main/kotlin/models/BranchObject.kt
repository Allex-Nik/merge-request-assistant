package org.example.models

import kotlinx.serialization.Serializable

@Serializable
data class BranchObject(
    val sha: String,
    val type: String,
    val url: String
)