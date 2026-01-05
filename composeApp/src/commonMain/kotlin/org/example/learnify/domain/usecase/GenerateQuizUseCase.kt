package org.example.learnify.domain.usecase

import io.github.aakira.napier.Napier
import org.example.learnify.data.mapper.toDomain
import org.example.learnify.data.remote.GeminiApiClient
import org.example.learnify.domain.model.Quiz

/**
 * Caso de uso para generar quizzes basados en el contenido de un tema
 */
class GenerateQuizUseCase(
    private val geminiApiClient: GeminiApiClient
) {
    /**
     * Genera un quiz para un tema específico
     *
     * @param topicId ID del tema
     * @param topicContent Contenido del tema
     * @param questionCount Número de preguntas a generar (por defecto 8)
     * @return Result con el quiz generado
     */
    suspend operator fun invoke(
        topicId: String,
        topicContent: String,
        questionCount: Int = 8
    ): Result<Quiz> {
        Napier.d("Generando quiz para tema: $topicId con $questionCount preguntas")

        if (topicContent.isBlank()) {
            return Result.failure(IllegalArgumentException("El contenido del tema está vacío"))
        }

        if (questionCount < 1 || questionCount > 20) {
            return Result.failure(IllegalArgumentException("El número de preguntas debe estar entre 1 y 20"))
        }

        return try {
            val response = geminiApiClient.generateQuiz(topicContent, questionCount)

            response.mapCatching { quizResponse ->
                val quiz = quizResponse.toDomain(topicId)

                Napier.i("Quiz generado exitosamente: ${quiz.questions.size} preguntas")

                quiz
            }
        } catch (e: Exception) {
            Napier.e("Error generando quiz", e)
            Result.failure(e)
        }
    }

    /**
     * Valida que el quiz generado sea válido
     */
    fun validateQuiz(quiz: Quiz): Boolean {
        return quiz.questions.isNotEmpty() &&
                quiz.questions.all { question ->
                    question.options.size == 4 &&
                            question.correctAnswer in 0..3 &&
                            question.text.isNotBlank()
                }
    }
}
