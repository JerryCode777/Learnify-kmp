package org.example.learnify.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.example.learnify.data.remote.model.*

class GeminiApiClient(
    private val apiKey: String
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
        encodeDefaults = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Napier.d(message, tag = "Gemini-API")
                }
            }
            level = LogLevel.INFO
        }
    }

    private companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        const val MODEL = "gemini-3-flash-preview"
    }

    suspend fun generateLearningPath(documentContent: String): Result<LearningPathResponse> {
        val prompt = buildLearningPathPrompt(documentContent)

        return try {
            val response = generateContent(prompt)
            val learningPath = parseResponse<LearningPathResponse>(response)
            Result.success(learningPath)
        } catch (e: Exception) {
            Napier.e("Error generating learning path", e)
            Result.failure(e)
        }
    }

    suspend fun generateQuiz(topicContent: String, questionCount: Int = 5): Result<QuizResponse> {
        val prompt = buildQuizPrompt(topicContent, questionCount)

        return try {
            val response = generateContent(prompt)
            val quiz = parseResponse<QuizResponse>(response)
            Result.success(quiz)
        } catch (e: Exception) {
            Napier.e("Error generating quiz", e)
            Result.failure(e)
        }
    }

    private suspend fun generateContent(prompt: String): GeminiResponse {
        val request = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(text = prompt))
                )
            ),
            generationConfig = GenerationConfig(
                temperature = 0.7,
                responseMimeType = "application/json"
            )
        )

        val response: HttpResponse = client.post("$BASE_URL/models/$MODEL:generateContent") {
            url {
                parameters.append("key", apiKey)
            }
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        return response.body()
    }

    private inline fun <reified T> parseResponse(response: GeminiResponse): T {
        val candidate = response.candidates.firstOrNull()
            ?: throw IllegalStateException("No candidates in response")

        val text = candidate.content.parts.firstOrNull()?.text
            ?: throw IllegalStateException("No text in response")

        Napier.d("Parsing response: $text")

        return try {
            json.decodeFromString<T>(text)
        } catch (e: Exception) {
            Napier.e("Error parsing JSON response", e)
            throw IllegalStateException("Failed to parse Gemini response: ${e.message}", e)
        }
    }

    private fun buildLearningPathPrompt(content: String): String {
        // Limitar el contenido para no exceder el límite de tokens
        val trimmedContent = if (content.length > 15000) {
            content.take(15000) + "..."
        } else {
            content
        }

        return """
Analiza el siguiente contenido educativo y crea una ruta de aprendizaje completa y estructurada.

CONTENIDO:
$trimmedContent

INSTRUCCIONES:
1. Identifica los temas principales en orden lógico de aprendizaje (del más básico al más avanzado)
2. Para cada tema, proporciona:
   - Un título claro y descriptivo
   - Una descripción breve del tema
   - Contenido educativo detallado (teoría, conceptos, explicaciones)
   - 3-5 puntos clave que el estudiante debe aprender
   - Tiempo estimado de estudio en minutos

3. La ruta debe ser progresiva: cada tema debe construir sobre los anteriores

FORMATO DE RESPUESTA (JSON):
{
  "title": "Título general de la ruta de aprendizaje",
  "description": "Descripción general de qué aprenderá el estudiante",
  "topics": [
    {
      "title": "Nombre del tema",
      "description": "Descripción breve del tema",
      "content": "Contenido educativo detallado con explicaciones",
      "keyPoints": ["Punto clave 1", "Punto clave 2", "Punto clave 3"],
      "estimatedMinutes": 30
    }
  ]
}

Responde SOLO con el JSON, sin texto adicional.
        """.trimIndent()
    }

    private fun buildQuizPrompt(topicContent: String, questionCount: Int): String {
        return """
Crea un quiz de $questionCount preguntas de opción múltiple basado en el siguiente contenido educativo:

CONTENIDO:
$topicContent

INSTRUCCIONES:
1. Crea $questionCount preguntas que evalúen la comprensión del contenido
2. Cada pregunta debe tener exactamente 4 opciones
3. Las opciones deben ser plausibles pero solo una correcta
4. Incluye una explicación clara de por qué la respuesta es correcta

FORMATO DE RESPUESTA (JSON):
{
  "questions": [
    {
      "question": "Texto de la pregunta",
      "options": ["Opción A", "Opción B", "Opción C", "Opción D"],
      "correctAnswer": 0,
      "explanation": "Explicación de por qué la opción correcta es la adecuada"
    }
  ]
}

Responde SOLO con el JSON, sin texto adicional.
        """.trimIndent()
    }

    fun close() {
        client.close()
    }
}
