package org.example.models

import kotlinx.serialization.Serializable

@Serializable
data class BranchInfo(
    val ref: String,
    val node_id: String,
    val url: String,
    val `object`: BranchObject
)
