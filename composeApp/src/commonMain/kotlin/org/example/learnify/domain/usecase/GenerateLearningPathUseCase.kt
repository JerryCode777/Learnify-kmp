package org.example.learnify.domain.usecase

import io.github.aakira.napier.Napier
import org.example.learnify.data.mapper.toDomain
import org.example.learnify.data.remote.GeminiApiClient
import org.example.learnify.domain.model.LearningPath

/**
 * Caso de uso para generar una ruta de aprendizaje a partir del contenido de un documento
 */
class GenerateLearningPathUseCase(
    private val geminiApiClient: GeminiApiClient
) {
    /**
     * Genera una ruta de aprendizaje personalizada
     *
     * @param documentId ID del documento
     * @param content Contenido extraído del PDF
     * @return Result con la ruta de aprendizaje generada
     */
    suspend operator fun invoke(
        documentId: String,
        content: String
    ): Result<LearningPath> {
        Napier.d("Generando ruta de aprendizaje para documento: $documentId")

        if (content.isBlank()) {
            return Result.failure(IllegalArgumentException("El contenido del documento está vacío"))
        }

        return try {
            val response = geminiApiClient.generateLearningPath(content)

            response.mapCatching { learningPathResponse ->
                val learningPath = learningPathResponse.toDomain(documentId)

                Napier.i("Ruta de aprendizaje generada exitosamente: ${learningPath.topics.size} temas")

                learningPath
            }
        } catch (e: Exception) {
            Napier.e("Error generando ruta de aprendizaje", e)
            Result.failure(e)
        }
    }

    /**
     * Valida que la ruta de aprendizaje generada sea válida
     */
    fun validateLearningPath(learningPath: LearningPath): Boolean {
        return learningPath.topics.isNotEmpty() &&
                learningPath.title.isNotBlank() &&
                learningPath.topics.all { it.content.isNotBlank() }
    }
}
