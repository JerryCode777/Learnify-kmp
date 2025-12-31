package org.example.learnify.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Document(
    val id: String,
    val title: String,
    val content: String,
    val uploadedAt: Long,
    val totalPages: Int
)
