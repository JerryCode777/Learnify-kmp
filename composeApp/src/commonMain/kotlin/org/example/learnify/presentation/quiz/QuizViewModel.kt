package org.example.learnify.presentation.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.learnify.data.remote.GeminiApiClient
import org.example.learnify.domain.model.Question
import org.example.learnify.domain.model.Quiz
import org.example.learnify.domain.usecase.GenerateQuizUseCase

class QuizViewModel(
    private val generateQuizUseCase: GenerateQuizUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var currentQuiz: Quiz? = null
    private var currentQuestionIndex = 0
    private var score = 0
    private val answeredQuestions = mutableSetOf<Int>()

    fun generateQuiz(topicId: String, topicContent: String, questionCount: Int = 8) {
        viewModelScope.launch {
            _uiState.value = QuizUiState.Loading
            Napier.d("Generando quiz para tema: $topicId con $questionCount preguntas")

            val result = generateQuizUseCase(topicId, topicContent, questionCount)

            result.onSuccess { quiz ->
                currentQuiz = quiz
                currentQuestionIndex = 0
                score = 0
                answeredQuestions.clear()

                if (quiz.questions.isEmpty()) {
                    _uiState.value = QuizUiState.Error("No se pudieron generar preguntas")
                    return@onSuccess
                }

                updateActiveState(quiz.questions[0])
                Napier.i("Quiz generado con ${quiz.questions.size} preguntas")
            }.onFailure { error ->
                Napier.e("Error generando quiz", error)
                _uiState.value = QuizUiState.Error(
                    userFacingErrorMessage(error, "Error al generar el quiz")
                )
            }
        }
    }

    fun onAnswerSelected(answerIndex: Int) {
        val state = _uiState.value
        if (state !is QuizUiState.Active || state.isAnswerSubmitted) return

        _uiState.value = state.copy(selectedAnswer = answerIndex)
    }

    fun onSubmitAnswer() {
        val state = _uiState.value
        if (state !is QuizUiState.Active || state.selectedAnswer == null || state.isAnswerSubmitted) return

        val isCorrect = state.selectedAnswer == state.currentQuestion.correctAnswer

        if (isCorrect && currentQuestionIndex !in answeredQuestions) {
            score++
            answeredQuestions.add(currentQuestionIndex)
        }

        _uiState.value = state.copy(
            isAnswerSubmitted = true,
            score = score
        )
    }

    fun onNextQuestion() {
        val quiz = currentQuiz ?: return

        if (currentQuestionIndex < quiz.questions.size - 1) {
            currentQuestionIndex++
            updateActiveState(quiz.questions[currentQuestionIndex])
        } else {
            // Quiz completado
            val totalQuestions = quiz.questions.size
            val percentage = (score.toFloat() / totalQuestions.toFloat()) * 100f
            val isPassed = percentage >= 70f // 70% para aprobar

            _uiState.value = QuizUiState.Completed(
                quiz = quiz,
                score = score,
                totalQuestions = totalQuestions,
                percentage = percentage,
                isPassed = isPassed
            )
        }
    }

    fun resetQuiz() {
        val quiz = currentQuiz ?: return
        currentQuestionIndex = 0
        score = 0
        answeredQuestions.clear()
        updateActiveState(quiz.questions[0])
    }

    private fun updateActiveState(question: Question) {
        val quiz = currentQuiz ?: return
        val totalQuestions = quiz.questions.size
        val progress = (currentQuestionIndex + 1).toFloat() / totalQuestions.toFloat()

        _uiState.value = QuizUiState.Active(
            quiz = quiz,
            currentQuestionIndex = currentQuestionIndex,
            currentQuestion = question,
            selectedAnswer = null,
            isAnswerSubmitted = false,
            score = score,
            answeredQuestions = answeredQuestions.size,
            totalQuestions = totalQuestions,
            progress = progress
        )
    }

    private fun userFacingErrorMessage(error: Throwable, fallback: String): String {
        return when (error) {
            is GeminiApiClient.RateLimitException ->
                "Límite de la API alcanzado. Espera unos minutos e inténtalo de nuevo."
            else -> error.message ?: fallback
        }
    }
}
