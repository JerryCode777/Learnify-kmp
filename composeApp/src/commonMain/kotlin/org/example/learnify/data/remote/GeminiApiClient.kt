package org.example.learnify.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
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

        install(HttpTimeout) {
            requestTimeoutMillis = 60_000  // 60 segundos (reducido de 120s)
            connectTimeoutMillis = 15_000  // 15 segundos (reducido de 30s)
            socketTimeoutMillis = 60_000   // 60 segundos (reducido de 120s)
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
        // Usando Gemini 2.5 Flash (Pro agotó su cuota)
        // Flash es más rápido y tiene cuota disponible
        const val MODEL = "gemini-2.5-flash"
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

    /**
     * Genera ruta de aprendizaje para un chunk específico del documento
     * Usado en procesamiento por chunks
     */
    suspend fun generateLearningPathFromChunk(
        chunkContent: String,
        chunkIndex: Int,
        startPage: Int,
        endPage: Int
    ): Result<LearningPathResponse> {
        val prompt = buildChunkPrompt(chunkContent, chunkIndex, startPage, endPage)

        return try {
            val response = generateContent(prompt)
            val learningPath = parseResponse<LearningPathResponse>(response)
            Result.success(learningPath)
        } catch (e: Exception) {
            Napier.e("Error generating learning path from chunk $chunkIndex", e)
            Result.failure(e)
        }
    }

    suspend fun generateQuiz(topicContent: String, questionCount: Int = 8): Result<QuizResponse> {
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

    private suspend fun generateContent(prompt: String, retryCount: Int = 0): GeminiResponse {
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

        try {
            val response: HttpResponse = client.post("$BASE_URL/models/$MODEL:generateContent") {
                url {
                    parameters.append("key", apiKey)
                }
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            // Verificar el código de estado HTTP
            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()

                // Manejo específico para error 429 (Too Many Requests)
                if (response.status.value == 429) {
                    // Verificar si es cuota excedida (no reintentable) o rate limit temporal
                    if (errorBody.contains("quota", ignoreCase = true) ||
                        errorBody.contains("exceeded your current quota", ignoreCase = true)) {
                        Napier.e("❌ HTTP 429: Cuota diaria de Gemini API excedida")
                        throw RateLimitException(
                            """Cuota de Gemini API excedida.
                            |
                            |Tu cuenta ha alcanzado el límite diario de peticiones.
                            |Por favor espera hasta que se restablezca tu cuota (generalmente 24 horas)
                            |o verifica tu plan en: https://console.cloud.google.com/apis/api/generativelanguage.googleapis.com/quotas
                            """.trimMargin()
                        )
                    }

                    if (retryCount < 3) {
                        // Backoff exponencial: 10s, 20s, 40s
                        val delaySeconds = 10 * (1 shl retryCount)
                        Napier.w("⚠️ HTTP 429 (Rate Limit). Reintento ${retryCount + 1}/3 en ${delaySeconds}s...")
                        kotlinx.coroutines.delay(delaySeconds * 1000L)
                        return generateContent(prompt, retryCount + 1)
                    } else {
                        Napier.e("❌ HTTP 429: Rate limit excedido después de 3 reintentos")
                        throw RateLimitException("Límite de peticiones excedido. Por favor espera unos minutos e intenta nuevamente.")
                    }
                }

                Napier.e("HTTP error ${response.status.value}: $errorBody")
                throw IllegalStateException("HTTP ${response.status.value}: ${response.status.description}\n$errorBody")
            }

            return response.body()
        } catch (e: RateLimitException) {
            // Re-lanzar RateLimitException sin envolver
            throw e
        } catch (e: io.ktor.client.plugins.HttpRequestTimeoutException) {
            val errorMsg = "La petición a Gemini tardó demasiado tiempo (>60s). Verifica tu conexión a internet o intenta más tarde."
            Napier.e(errorMsg, e)
            throw IllegalStateException(errorMsg, e)
        } catch (e: io.ktor.client.network.sockets.ConnectTimeoutException) {
            val errorMsg = "No se pudo conectar a la API de Gemini. Verifica tu conexión a internet."
            Napier.e(errorMsg, e)
            throw IllegalStateException(errorMsg, e)
        } catch (e: io.ktor.client.network.sockets.SocketTimeoutException) {
            val errorMsg = "Timeout de conexión con Gemini API. La API podría estar experimentando problemas."
            Napier.e(errorMsg, e)
            throw IllegalStateException(errorMsg, e)
        } catch (e: Exception) {
            Napier.e("Error calling Gemini API: ${e::class.simpleName} - ${e.message}", e)
            throw e
        }
    }

    // Excepción personalizada para rate limiting
    class RateLimitException(message: String) : Exception(message)

    private inline fun <reified T> parseResponse(response: GeminiResponse): T {
        // Verificar si hay un error en la respuesta
        response.error?.let { error ->
            val errorMessage = "Gemini API error (${error.code}): ${error.message}"
            Napier.e(errorMessage)
            throw IllegalStateException(errorMessage)
        }

        // Verificar si hay candidates
        if (response.candidates.isNullOrEmpty()) {
            Napier.e("No candidates in Gemini response")
            throw IllegalStateException("No candidates in response. This may be due to content filtering or API limits.")
        }

        val candidate = response.candidates.firstOrNull()
            ?: throw IllegalStateException("No candidates in response")

        val text = candidate.content.parts.firstOrNull()?.text
            ?: throw IllegalStateException("No text in response")

        Napier.d("Parsing response: ${text.take(200)}...")

        return try {
            json.decodeFromString<T>(text)
        } catch (e: Exception) {
            Napier.e("Error parsing JSON response", e)
            Napier.e("Response text: $text")
            throw IllegalStateException("Failed to parse Gemini response: ${e.message}", e)
        }
    }

    private fun buildLearningPathPrompt(content: String): String {
        // Aumentar límite significativamente para documentos grandes
        // Gemini 3 Flash soporta hasta ~1M tokens (~750K caracteres)
        val trimmedContent = if (content.length > 500000) {
            Napier.w("Contenido muy grande (${content.length} chars), truncando a 500K")
            content.take(500000) + "\n\n[CONTENIDO TRUNCADO - Documento muy extenso]"
        } else {
            content
        }

        // Calcular número recomendado de temas basado en longitud del contenido
        // Aproximadamente 1 tema por cada 10,000 caracteres
        val recommendedTopics = (trimmedContent.length / 10000).coerceIn(10, 50)

        Napier.i("Generando ruta de aprendizaje: ${trimmedContent.length} caracteres, ~$recommendedTopics temas recomendados")

        return """
Analyze the following educational content and create a learning path in the form of a QUALITY SUMMARY.

CONTENT (${trimmedContent.length} characters):
$trimmedContent

CRITICAL INSTRUCTIONS:
1. This is an extensive document. You must create a SUMMARIZED learning path with at least $recommendedTopics main topics.

2. Identify ALL important concepts, chapters and sections of the content.

3. For each topic, provide:
   - A clear and descriptive title that reflects the specific content
   - A brief description of the topic (1-2 sentences)
   - Summarized educational content with theory, concepts and clear explanations (120-220 words per topic)
   - 4-6 key points that the student should master
   - Estimated study time in minutes (realistic according to complexity)

4. The path should be PROGRESSIVE: organize topics in logical learning order, from basic to advanced.

5. DO NOT omit important content. Cover all main topics of the document.

6. If the document has clearly defined sections, chapters or parts, respect that structure.

RESPONSE FORMAT (JSON):
{
  "title": "Descriptive title of the learning path",
  "description": "Complete description of what the student will learn (2-3 sentences)",
  "topics": [
    {
      "title": "Specific topic name",
      "description": "Description of the topic scope and objectives",
      "content": "DETAILED educational content with complete explanations, examples and context (minimum 200 words)",
      "keyPoints": ["Key point 1", "Key point 2", "Key point 3", "Key point 4"],
      "estimatedMinutes": 45
    }
  ]
}

IMPORTANT: Generate AT LEAST $recommendedTopics topics. Prioritize clarity and correct sequence.

Respond ONLY with JSON, no additional text.
        """.trimIndent()
    }

    private fun buildQuizPrompt(topicContent: String, questionCount: Int): String {
        return """
Create a COMPREHENSIVE quiz of $questionCount multiple choice questions based on the following educational content:

TOPIC CONTENT:
$topicContent

CRITICAL INSTRUCTIONS:

1. **COMPLETE COVERAGE**:
   - The $questionCount questions must cover ALL the topic content
   - Distribute questions among key concepts, definitions, examples and applications
   - Don't focus on just one aspect of the topic

2. **QUESTION TYPES** (vary between these types):
   - Definitions and fundamental concepts
   - Application of knowledge to practical situations
   - Analysis of specific examples mentioned in the content
   - Comparison between related concepts
   - Identification of characteristics or properties
   - Causes, effects and relationships between concepts

3. **QUESTION QUALITY**:
   - Clear, specific and unambiguous questions
   - Based on REAL information from the content (don't make up data)
   - Varied difficulty level (easy, medium, hard)
   - Each question should assess real understanding, not superficial memorization

4. **ANSWER OPTIONS**:
   - Exactly 4 options per question
   - All options should be plausible and related to the topic
   - The correct answer should be clearly supported by the content
   - Incorrect options should be common mistakes or related concepts

5. **EXPLANATIONS**:
   - Explain why the correct answer is correct (reference to content)
   - If possible, mention why the other options are incorrect
   - The explanation should reinforce learning

RESPONSE FORMAT (JSON):
{
  "questions": [
    {
      "question": "Specific and clear question based on content",
      "options": [
        "Option A - plausible and related",
        "Option B - plausible and related",
        "Option C - plausible and related",
        "Option D - plausible and related"
      ],
      "correctAnswer": 0,
      "explanation": "Detailed explanation of why this answer is correct, referencing the topic content. Optionally, mention why the other options are incorrect."
    }
  ]
}

🎯 OBJECTIVE: Create a quiz that truly assesses whether the student understood the complete topic.

✅ A GOOD QUIZ:
- Covers all important aspects of the content
- Has questions of different difficulty levels
- Incorrect options are plausible but clearly wrong
- Explanations reinforce learning

❌ AVOID:
- Trivial or too obvious questions
- Questions that can be answered by elimination without knowing the topic
- Making up information that isn't in the content
- Repeating the same question with different words

Generate EXACTLY $questionCount questions.

Respond ONLY with JSON, no additional text.
        """.trimIndent()
    }

    private fun buildChunkPrompt(
        chunkContent: String,
        chunkIndex: Int,
        startPage: Int,
        endPage: Int
    ): String {
        // Calcular número de temas basado en el número de páginas
        val pageCount = endPage - startPage + 1
        val recommendedTopics = (pageCount / 5).coerceIn(4, 12)

        Napier.i("Generando chunk $chunkIndex: páginas $startPage-$endPage ($pageCount páginas), ~$recommendedTopics temas esperados")

        return """
Analyze the following fragment of an educational document (PAGES $startPage-$endPage) and create a HIGH-QUALITY SUMMARY in the form of topics.

DOCUMENT CONTENT (${chunkContent.length} characters):
$chunkContent

CONTEXT:
- This is chunk #$chunkIndex of a larger document
- Contains pages $startPage to $endPage (total: $pageCount pages)
- Your task is to SUMMARIZE clearly, maintaining the correct sequence of concepts

⚠️ CRITICAL RULES - READ CAREFULLY:

1. **QUALITY SUMMARY**: Don't copy literal text. Condense ideas, but preserve key definitions and important concepts.

2. **COHERENCE AND SEQUENCE**:
   - Respect the logical order of the document content
   - Connect topics from basic to advanced

3. **GENERATE ~ $recommendedTopics TOPICS**:
   - Divide the content into logical and well-delimited topics
   - Each topic should have a clear summary (120-220 words)

4. **FOR EACH TOPIC, INCLUDE**:
   - **Title**: Specific and descriptive
   - **Description**: What exactly this topic covers (2-3 sentences)
   - **Content (CRITICAL)**:
     * Deep and faithful summary of the text
     * Include definitions, explanations, examples and relevant formulas
     * Avoid filler; prioritize clarity and accuracy
   - **KeyPoints**: 4-6 key points
   - **EstimatedMinutes**: Realistic study time (15-60 minutes)

RESPONSE FORMAT (JSON):
{
  "title": "Section: Pages $startPage-$endPage",
  "description": "Structured summary of pages $startPage to $endPage of the document",
  "topics": [
    {
      "title": "Specific topic title",
      "description": "Description of the topic scope",
      "content": "Deep and structured summary of the topic, maintaining main ideas, relevant definitions and key examples.",
      "keyPoints": [
        "Point 1 with specific details from the text",
        "Point 2 with concrete information",
        "Point 3 with exact data",
        "Point 4 with specific examples",
        "Point 5 with key definitions"
      ],
      "estimatedMinutes": 45
    }
  ]
}

🎯 MAIN OBJECTIVE: Your job is to SUMMARIZE with high quality and correct sequence. The student should understand the main content without reading the complete PDF.

✅ SUCCESS CRITERIA: If a student reads your topics, they understand the concepts and correct order without reading the complete PDF.

❌ WHAT YOU SHOULD NOT DO:
- Vague summaries without concepts or definitions
- Lose the logical sequence of content
- Omit important examples or relevant formulas

Respond ONLY with JSON, no additional text.
        """.trimIndent()
    }

    fun close() {
        client.close()
    }
}
