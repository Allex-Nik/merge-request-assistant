package org.example.models

import kotlinx.serialization.Serializable

@Serializable
data class FileInfo(
    val sha: String
)