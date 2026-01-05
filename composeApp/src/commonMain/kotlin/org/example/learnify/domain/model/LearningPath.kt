package org.example.learnify.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LearningPath(
    val id: String,
    val documentId: String,
    val title: String,
    val description: String = "",
    val topics: List<Topic>,
    val createdAt: Long
)

@Serializable
data class Topic(
    val id: String,
    val title: String,
    val content: String,
    val pageNumbers: List<Int>,
    val order: Int
)
