package org.example.learnify.data.remote.model

import kotlinx.serialization.Serializable

/**
 * Respuesta de Gemini para generación de ruta de aprendizaje
 */
@Serializable
data class LearningPathResponse(
    val title: String,
    val description: String,
    val topics: List<TopicResponse>
)

@Serializable
data class TopicResponse(
    val title: String,
    val description: String,
    val content: String,
    val keyPoints: List<String>,
    val estimatedMinutes: Int = 30
)
