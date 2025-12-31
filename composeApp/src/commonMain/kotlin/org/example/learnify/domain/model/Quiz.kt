package org.example.learnify.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Quiz(
    val id: String,
    val topicId: String,
    val questions: List<Question>
)

@Serializable
data class Question(
    val id: String,
    val text: String,
    val options: List<String>,
    val correctAnswer: Int,
    val explanation: String
)

@Serializable
data class QuizResult(
    val quizId: String,
    val score: Int,
    val totalQuestions: Int,
    val completedAt: Long
)
