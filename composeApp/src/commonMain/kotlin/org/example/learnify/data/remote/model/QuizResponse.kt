package org.example.learnify.data.remote.model

import kotlinx.serialization.Serializable

/**
 * Respuesta de Gemini para generación de quiz
 */
@Serializable
data class QuizResponse(
    val questions: List<QuestionResponse>
)

@Serializable
data class QuestionResponse(
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,
    val explanation: String
)
