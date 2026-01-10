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
        // Usando Gemini 2.5 Pro - mayor capacidad y límites más altos
        const val MODEL = "gemini-2.5-pro"
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
                    if (retryCount < 3) {
                        // Backoff exponencial más agresivo: 30s, 60s, 120s
                        val delaySeconds = 30 * (1 shl retryCount) // 30, 60, 120
                        Napier.w("⚠️ HTTP 429 (Rate Limit). Reintento ${retryCount + 1}/3 en ${delaySeconds}s...")
                        Napier.w("   La API de Gemini tiene límites muy estrictos. Por favor ten paciencia...")
                        kotlinx.coroutines.delay(delaySeconds * 1000L)
                        return generateContent(prompt, retryCount + 1)
                    } else {
                        Napier.e("❌ HTTP 429: Rate limit excedido después de 3 reintentos (total: ${30 + 60 + 120}s esperados)")
                        throw RateLimitException("HTTP 429: Too Many Requests - La API de Gemini tiene límites muy estrictos. Por favor espera 5-10 minutos antes de intentar nuevamente.")
                    }
                }

                Napier.e("HTTP error ${response.status.value}: $errorBody")
                throw IllegalStateException("HTTP ${response.status.value}: ${response.status.description}")
            }

            return response.body()
        } catch (e: RateLimitException) {
            // Re-lanzar RateLimitException sin envolver
            throw e
        } catch (e: Exception) {
            Napier.e("Error calling Gemini API", e)
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
Analiza el siguiente contenido educativo y crea una ruta de aprendizaje COMPLETA y EXHAUSTIVA.

CONTENIDO (${trimmedContent.length} caracteres):
$trimmedContent

INSTRUCCIONES CRÍTICAS:
1. Este es un documento extenso. Debes crear una ruta de aprendizaje EXHAUSTIVA con al menos $recommendedTopics temas principales.

2. Identifica TODOS los conceptos, capítulos y secciones importantes del contenido.

3. Para cada tema, proporciona:
   - Un título claro y descriptivo que refleje el contenido específico
   - Una descripción breve del tema (1-2 oraciones)
   - Contenido educativo DETALLADO con teoría, conceptos y explicaciones completas (mínimo 200 palabras por tema)
   - 4-6 puntos clave que el estudiante debe dominar
   - Tiempo estimado de estudio en minutos (realista según la complejidad)

4. La ruta debe ser PROGRESIVA: organiza los temas en orden lógico de aprendizaje, del más básico al más avanzado.

5. NO omitas contenido importante. Cubre todos los temas principales del documento.

6. Si el documento tiene secciones, capítulos o partes claramente definidas, respeta esa estructura.

FORMATO DE RESPUESTA (JSON):
{
  "title": "Título descriptivo de la ruta de aprendizaje",
  "description": "Descripción completa de qué aprenderá el estudiante (2-3 oraciones)",
  "topics": [
    {
      "title": "Nombre específico del tema",
      "description": "Descripción del alcance y objetivos del tema",
      "content": "Contenido educativo DETALLADO con explicaciones completas, ejemplos y contexto (mínimo 200 palabras)",
      "keyPoints": ["Punto clave 1", "Punto clave 2", "Punto clave 3", "Punto clave 4"],
      "estimatedMinutes": 45
    }
  ]
}

IMPORTANTE: Genera AL MENOS $recommendedTopics temas. Mientras más temas generes mejor, siempre que sean relevantes y bien desarrollados.

Responde SOLO con el JSON, sin texto adicional.
        """.trimIndent()
    }

    private fun buildQuizPrompt(topicContent: String, questionCount: Int): String {
        return """
Crea un quiz EXHAUSTIVO de $questionCount preguntas de opción múltiple basado en el siguiente contenido educativo:

CONTENIDO DEL TEMA:
$topicContent

INSTRUCCIONES CRÍTICAS:

1. **COBERTURA COMPLETA**:
   - Las $questionCount preguntas deben cubrir TODO el contenido del tema
   - Distribuye las preguntas entre conceptos clave, definiciones, ejemplos y aplicaciones
   - No te enfoques solo en un aspecto del tema

2. **TIPOS DE PREGUNTAS** (varía entre estos tipos):
   - Definiciones y conceptos fundamentales
   - Aplicación de conocimientos a situaciones prácticas
   - Análisis de ejemplos específicos mencionados en el contenido
   - Comparación entre conceptos relacionados
   - Identificación de características o propiedades
   - Causas, efectos y relaciones entre conceptos

3. **CALIDAD DE LAS PREGUNTAS**:
   - Preguntas claras, específicas y sin ambigüedad
   - Basadas en información REAL del contenido (no inventes datos)
   - Nivel de dificultad variado (fácil, medio, difícil)
   - Cada pregunta debe evaluar comprensión real, no memorización superficial

4. **OPCIONES DE RESPUESTA**:
   - Exactamente 4 opciones por pregunta
   - Todas las opciones deben ser plausibles y relacionadas con el tema
   - La respuesta correcta debe estar claramente respaldada por el contenido
   - Las opciones incorrectas deben ser errores comunes o conceptos relacionados

5. **EXPLICACIONES**:
   - Explica por qué la respuesta correcta es correcta (referencia al contenido)
   - Si es posible, menciona por qué las otras opciones son incorrectas
   - La explicación debe reforzar el aprendizaje

FORMATO DE RESPUESTA (JSON):
{
  "questions": [
    {
      "question": "Pregunta específica y clara basada en el contenido",
      "options": [
        "Opción A - plausible y relacionada",
        "Opción B - plausible y relacionada",
        "Opción C - plausible y relacionada",
        "Opción D - plausible y relacionada"
      ],
      "correctAnswer": 0,
      "explanation": "Explicación detallada de por qué esta respuesta es correcta, haciendo referencia al contenido del tema. Opcionalmente, menciona por qué las otras opciones son incorrectas."
    }
  ]
}

🎯 OBJETIVO: Crear un quiz que realmente evalúe si el estudiante comprendió el tema completo.

✅ UN BUEN QUIZ:
- Cubre todos los aspectos importantes del contenido
- Tiene preguntas de diferentes niveles de dificultad
- Las opciones incorrectas son plausibles pero claramente incorrectas
- Las explicaciones refuerzan el aprendizaje

❌ EVITA:
- Preguntas triviales o demasiado obvias
- Preguntas que se respondan por eliminación sin conocer el tema
- Inventar información que no está en el contenido
- Repetir la misma pregunta con diferentes palabras

Genera EXACTAMENTE $questionCount preguntas.

Responde SOLO con el JSON, sin texto adicional.
        """.trimIndent()
    }

    private fun buildChunkPrompt(
        chunkContent: String,
        chunkIndex: Int,
        startPage: Int,
        endPage: Int
    ): String {
        // Calcular número de temas basado en el número de páginas
        // Con chunks de 25 páginas, esperamos aproximadamente 5-8 temas
        val pageCount = endPage - startPage + 1
        val recommendedTopics = (pageCount / 3).coerceIn(5, 20) // Aproximadamente 1 tema cada 3 páginas

        Napier.i("Generando chunk $chunkIndex: páginas $startPage-$endPage ($pageCount páginas), ~$recommendedTopics temas esperados")

        return """
Analiza el siguiente fragmento de un documento educativo (PÁGINAS $startPage-$endPage) y extrae TODO su contenido para crear una ruta de aprendizaje COMPLETA.

CONTENIDO DEL DOCUMENTO (${chunkContent.length} caracteres):
$chunkContent

CONTEXTO:
- Este es el chunk #$chunkIndex de un documento más grande
- Contiene las páginas $startPage a $endPage (total: $pageCount páginas)
- Tu tarea es EXTRAER TODO EL CONTENIDO, NO RESUMIRLO

⚠️ REGLAS CRÍTICAS - LEE CON ATENCIÓN:

1. **NO RESUMAS**: Debes COPIAR y EXTRAER todo el contenido textual de cada página. NO escribas resúmenes genéricos.

2. **CONTENIDO PALABRA POR PALABRA**:
   - Copia definiciones exactas del texto
   - Incluye TODOS los ejemplos mencionados
   - Transcribe explicaciones completas
   - Preserva nombres propios, fechas, lugares, números, fórmulas
   - Mantén citas y referencias importantes

3. **GENERA AL MENOS $recommendedTopics TEMAS**:
   - Divide el contenido en temas lógicos
   - Si una sección es muy extensa, divídela en múltiples temas
   - Cada tema debe tener MÍNIMO 500 palabras de contenido real

4. **PARA CADA TEMA, INCLUYE**:
   - **Título**: Específico y descriptivo
   - **Descripción**: Qué cubre exactamente este tema (2-3 oraciones)
   - **Content (CRÍTICO)**:
     * Copia TEXTUALMENTE toda la información relevante de las páginas
     * Incluye definiciones, explicaciones, ejemplos, casos de estudio
     * Transcribe fórmulas, ecuaciones, teoremas si los hay
     * Copia listas, enumeraciones, clasificaciones
     * Mínimo 500 palabras por tema (idealmente 800-1200 palabras)
   - **KeyPoints**: 6-8 puntos clave extraídos del contenido
   - **EstimatedMinutes**: Tiempo realista de estudio (20-90 minutos)

5. **EXTRACCIÓN EXHAUSTIVA**:
   - Lee TODAS las páginas del chunk
   - NO omitas secciones o párrafos
   - Si hay tablas, convierte el contenido a texto
   - Si hay diagramas, describe lo que representan
   - Captura TODA la información educativa

FORMATO DE RESPUESTA (JSON):
{
  "title": "Sección: Páginas $startPage-$endPage",
  "description": "Contenido completo extraído de las páginas $startPage a $endPage del documento",
  "topics": [
    {
      "title": "Título específico del tema",
      "description": "Descripción del alcance del tema",
      "content": "AQUÍ VA TODO EL CONTENIDO TEXTUAL DE LAS PÁGINAS. Copia definiciones completas, explicaciones detalladas, ejemplos específicos con todos sus detalles, casos de estudio, fórmulas, teoremas, clasificaciones, enumeraciones, y cualquier información educativa presente en el texto original. NO escribas resúmenes genéricos. COPIA el contenido real. Mínimo 500 palabras.",
      "keyPoints": [
        "Punto 1 con detalles específicos del texto",
        "Punto 2 con información concreta",
        "Punto 3 con datos exactos",
        "Punto 4 con ejemplos específicos",
        "Punto 5 con definiciones clave",
        "Punto 6 con conceptos importantes"
      ],
      "estimatedMinutes": 45
    }
  ]
}

🎯 OBJETIVO PRINCIPAL: Tu trabajo es COPIAR y ORGANIZAR el contenido del libro en temas, NO resumirlo. El estudiante debe poder aprender TODO el material leyendo tus temas, sin necesidad de leer el PDF original.

✅ CRITERIO DE ÉXITO: Si un estudiante lee tus temas y luego toma un examen sobre estas páginas del libro, debe poder responder TODAS las preguntas porque has extraído TODA la información.

❌ LO QUE NO DEBES HACER:
- Escribir "El texto habla sobre..." → EN SU LUGAR: Copia lo que dice el texto
- Hacer resúmenes genéricos → EN SU LUGAR: Extrae el contenido completo
- Omitir ejemplos o detalles → EN SU LUGAR: Incluye TODO
- Contenido corto (<500 palabras) → EN SU LUGAR: Extrae más información

Responde SOLO con el JSON, sin texto adicional.
        """.trimIndent()
    }

    fun close() {
        client.close()
    }
}
