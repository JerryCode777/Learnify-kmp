package org.example.learnify.presentation.quiz

import org.example.learnify.domain.model.Question
import org.example.learnify.domain.model.Quiz

sealed interface QuizUiState {
    data object Loading : QuizUiState

    data class Active(
        val quiz: Quiz,
        val currentQuestionIndex: Int,
        val currentQuestion: Question,
        val selectedAnswer: Int?,
        val isAnswerSubmitted: Boolean,
        val score: Int,
        val answeredQuestions: Int,
        val totalQuestions: Int,
        val progress: Float
    ) : QuizUiState

    data class Completed(
        val quiz: Quiz,
        val score: Int,
        val totalQuestions: Int,
        val percentage: Float,
        val isPassed: Boolean
    ) : QuizUiState

    data class Error(val message: String) : QuizUiState
}
