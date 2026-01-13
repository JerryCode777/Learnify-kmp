package org.example.learnify.domain.usecase

import io.github.aakira.napier.Napier
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout
import org.example.learnify.data.remote.GeminiApiClient
import org.example.learnify.domain.model.*

/**
 * Procesa un documento completo en chunks manejables
 * Similar al enfoque de batch_content_extractor.py
 */
class ProcessDocumentInChunksUseCase(
    private val geminiApiClient: GeminiApiClient
) {
    companion object {
        // Páginas por chunk - cada 15 páginas se procesa un chunk
        // Reducido de 20 a 15 para hacer requests más pequeñas y rápidas
        private const val PAGES_PER_CHUNK = 15
        // Límite suave de caracteres por chunk para evitar payloads gigantes
        private const val MAX_CHARS_PER_CHUNK = 120000

        // Reintentos por chunk ante rate limiting
        private const val MAX_CHUNK_RETRIES = 3
        private const val BASE_RETRY_DELAY_MS = 15000L
        private const val MAX_RETRY_DELAY_MS = 120000L

        // Palabras clave para detectar páginas irrelevantes (bibliografía, índices, etc.)
        private val IRRELEVANT_PAGE_KEYWORDS = listOf(
            "bibliografía", "bibliography", "referencias", "references",
            "índice", "index", "tabla de contenido", "table of contents",
            "glosario", "glossary", "apéndice", "appendix",
            "agradecimientos", "acknowledgments", "sobre el autor", "about the author"
        )
    }

    /**
     * Procesa el documento completo en chunks y genera la ruta de aprendizaje
     */
    suspend operator fun invoke(
        documentJson: DocumentJson,
        onProgress: (current: Int, total: Int, message: String) -> Unit = { _, _, _ -> },
        onChunkSuccess: (topics: List<Topic>) -> Unit = {},
        chunkTimeoutMs: Long = 90000L,
        pagesOverride: List<PageJson>? = null
    ): Result<LearningPath> {
        return try {
            Napier.i("Iniciando procesamiento por chunks del documento: ${documentJson.filename}")
            Napier.i("Total de páginas: ${documentJson.metadata.totalPages}")
            Napier.i("Total de caracteres: ${documentJson.metadata.totalCharacters}")

            // Paso 1: Filtrar páginas irrelevantes (bibliografía, índices, etc.)
            val relevantPages = pagesOverride ?: filterRelevantPages(documentJson.pages)
            Napier.i("Páginas relevantes después de filtrado: ${relevantPages.size} de ${documentJson.pages.size}")

            // Paso 2: Dividir en chunks controlando páginas y tamaño de texto
            val chunks = createChunks(relevantPages)
            Napier.i("Documento dividido en ${chunks.size} chunks de ~$PAGES_PER_CHUNK páginas cada uno")

            // Paso 2: Procesar cada chunk con Gemini
            val allTopics = mutableListOf<Topic>()
            val failedChunks = mutableListOf<String>()

            chunks.forEachIndexed { index, chunk ->
                currentCoroutineContext().ensureActive()

                val progress = "Procesando chunk ${index + 1} de ${chunks.size} (págs ${chunk.startPage}-${chunk.endPage})"
                onProgress(index + 1, chunks.size, progress)
                Napier.i(progress)

                val result = processChunkWithRetry(
                    chunk = chunk,
                    documentId = documentJson.documentId,
                    onProgress = { message ->
                        onProgress(index + 1, chunks.size, message)
                    },
                    chunkTimeoutMs = chunkTimeoutMs
                )

                result.onSuccess { topics ->
                    allTopics.addAll(topics)
                    onChunkSuccess(topics)
                    Napier.i("✅ Chunk ${index + 1}: ${topics.size} temas generados exitosamente")
                }.onFailure { error ->
                    val errorMsg = "Chunk ${index + 1} (pág ${chunk.startPage}-${chunk.endPage}): ${error.message}"
                    failedChunks.add(errorMsg)
                    Napier.e("❌ Error procesando chunk ${index + 1}: ${error.message}", error)
                    // Continuar con los demás chunks
                }
            }

            // Paso 3: Combinar resultados
            if (allTopics.isEmpty()) {
                val errorDetails = if (failedChunks.isNotEmpty()) {
                    "Todos los chunks fallaron:\n${failedChunks.joinToString("\n")}"
                } else {
                    "No se pudieron generar temas del documento"
                }
                Napier.e("❌ Procesamiento fallido: $errorDetails")
                return Result.failure(Exception(errorDetails))
            }

            // Log de resumen
            if (failedChunks.isNotEmpty()) {
                Napier.w("⚠️ Algunos chunks fallaron (${failedChunks.size}/${chunks.size}): ${failedChunks.joinToString("; ")}")
            }

            val learningPath = LearningPath(
                id = generateId(),
                documentId = documentJson.documentId,
                title = extractTitle(documentJson),
                description = buildString {
                    append("Complete learning path generated from ${documentJson.metadata.totalPages} pages")
                    if (failedChunks.isNotEmpty()) {
                        append(". ${failedChunks.size} section(s) were omitted due to processing errors.")
                    }
                },
                topics = allTopics,
                createdAt = currentTimeMillis()
            )

            Napier.i("Procesamiento completado: ${allTopics.size} temas totales generados")
            Result.success(learningPath)

        } catch (e: Exception) {
            Napier.e("Error en procesamiento por chunks", e)
            Result.failure(e)
        }
    }

    /**
     * Filtra páginas irrelevantes como bibliografía, índices, etc.
     * Filtrado SUAVE - solo elimina páginas claramente irrelevantes
     */
    private fun filterRelevantPages(pages: List<PageJson>): List<PageJson> {
        val filtered = pages.filterIndexed { index, page ->
            val content = page.content.lowercase()
            val trimmedContent = page.content.trim()

            // Si una página es muy corta (menos de 50 caracteres), probablemente no tiene contenido útil
            val isTooShort = trimmedContent.length < 50

            // Solo filtrar si la página completa está dedicada a bibliografía/índice
            // No filtrar si solo menciona estas palabras
            val isFullyIrrelevant = IRRELEVANT_PAGE_KEYWORDS.any { keyword ->
                val keywordLower = keyword.lowercase()
                // La página empieza con la palabra clave Y es corta (probablemente solo el título)
                content.trimStart().startsWith(keywordLower) && trimmedContent.length < 200
            }

            // Mantener la página si NO es demasiado corta Y NO es completamente irrelevante
            val shouldKeep = !isTooShort && !isFullyIrrelevant

            if (!shouldKeep) {
                Napier.d("Filtrando página ${page.pageNumber}: ${if (isFullyIrrelevant) "página de $IRRELEVANT_PAGE_KEYWORDS" else "muy corta (${trimmedContent.length} chars)"}")
            }

            shouldKeep
        }

        // Si el filtrado elimina más del 70% de las páginas, algo está mal
        // En ese caso, devolver todas las páginas
        if (filtered.size < pages.size * 0.3) {
            Napier.w("Filtrado muy agresivo: ${filtered.size}/${pages.size} páginas. Usando todas las páginas.")
            return pages
        }

        return filtered
    }

    /**
     * Divide las páginas en chunks de hasta 15 páginas y con límite suave de caracteres.
     */
    private fun createChunks(pages: List<PageJson>): List<ContentChunk> {
        val chunks = mutableListOf<ContentChunk>()
        var currentPages = mutableListOf<PageJson>()
        var currentChars = 0

        fun flushChunk() {
            if (currentPages.isEmpty()) return
            val totalChars = currentPages.sumOf { it.content.length }
            chunks.add(
                ContentChunk(
                    chunkIndex = chunks.size,
                    startPage = currentPages.first().pageNumber,
                    endPage = currentPages.last().pageNumber,
                    pages = currentPages.toList(),
                    totalCharacters = totalChars
                )
            )
            Napier.d(
                "Chunk ${chunks.size}: páginas ${currentPages.first().pageNumber}-${currentPages.last().pageNumber}, $totalChars caracteres"
            )
            currentPages = mutableListOf()
            currentChars = 0
        }

        for (page in pages) {
            val pageChars = page.content.length
            val wouldExceedChars = currentChars + pageChars > MAX_CHARS_PER_CHUNK
            val wouldExceedPages = currentPages.size >= PAGES_PER_CHUNK

            if (currentPages.isNotEmpty() && (wouldExceedChars || wouldExceedPages)) {
                flushChunk()
            }

            currentPages.add(page)
            currentChars += pageChars

            // Si una sola pagina ya supera el limite, se manda sola
            if (currentPages.size == 1 && currentChars > MAX_CHARS_PER_CHUNK) {
                flushChunk()
            }
        }

        flushChunk()

        return chunks
    }

    /**
     * Procesa un chunk individual con Gemini
     */
    private suspend fun processChunk(
        chunk: ContentChunk,
        documentId: String
    ): Result<List<Topic>> {
        // Construir contenido del chunk
        val chunkContent = buildChunkContent(chunk)

        // Procesar con Gemini
        val result = geminiApiClient.generateLearningPathFromChunk(
            chunkContent = chunkContent,
            chunkIndex = chunk.chunkIndex,
            startPage = chunk.startPage,
            endPage = chunk.endPage
        )

        return result.map { response ->
            response.topics.mapIndexed { index, topicResponse ->
                Topic(
                    id = generateId(),
                    title = topicResponse.title,
                    content = "${topicResponse.description}\n\n${topicResponse.content}",
                    pageNumbers = (chunk.startPage..chunk.endPage).toList(),
                    order = chunk.chunkIndex * 100 + index // Mantener orden global
                )
            }
        }
    }

    private suspend fun processChunkWithRetry(
        chunk: ContentChunk,
        documentId: String,
        onProgress: (String) -> Unit,
        chunkTimeoutMs: Long
    ): Result<List<Topic>> {
        var attempt = 0
        var delayMs = BASE_RETRY_DELAY_MS

        while (true) {
            val result = try {
                withTimeout(chunkTimeoutMs) {
                    processChunk(chunk, documentId)
                }
            } catch (e: TimeoutCancellationException) {
                Result.failure(e)
            }
            if (result.isSuccess) {
                return result
            }

            val error = result.exceptionOrNull()
            val shouldRetry = (error is GeminiApiClient.RateLimitException ||
                error is TimeoutCancellationException) && attempt < MAX_CHUNK_RETRIES
            if (shouldRetry) {
                val delaySeconds = delayMs / 1000
                val reason = if (error is TimeoutCancellationException) {
                    "Timeout"
                } else {
                    "Rate limit"
                }
                val message = "$reason detectado. Reintentando chunk en ${delaySeconds}s (intento ${attempt + 1}/$MAX_CHUNK_RETRIES)..."
                Napier.w(message)
                onProgress(message)
                kotlinx.coroutines.delay(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(MAX_RETRY_DELAY_MS)
                attempt++
                continue
            }

            return result
        }
    }

    /**
     * Construye el contenido formateado del chunk
     */
    private fun buildChunkContent(chunk: ContentChunk): String {
        return buildString {
            appendLine("=== DOCUMENTO: PÁGINAS ${chunk.startPage}-${chunk.endPage} ===")
            appendLine()

            chunk.pages.forEach { page ->
                appendLine("--- PÁGINA ${page.pageNumber} ---")
                appendLine(page.content)
                appendLine()
            }
        }
    }

    /**
     * Extrae el título del documento del contenido de las primeras páginas
     */
    private fun extractTitle(documentJson: DocumentJson): String {
        // Intentar extraer título de las primeras páginas
        val firstPages = documentJson.pages.take(3)
        val firstPageContent = firstPages.firstOrNull()?.content ?: ""

        // Tomar la primera línea no vacía como título (simplificado)
        val title = firstPageContent
            .lines()
            .firstOrNull { it.trim().isNotEmpty() }
            ?.take(100) // Máximo 100 caracteres
            ?: documentJson.filename.removeSuffix(".pdf")

        return title
    }

    private fun generateId(): String {
        return com.benasher44.uuid.uuid4().toString()
    }

    private fun currentTimeMillis(): Long {
        return org.example.learnify.util.currentTimeMillis()
    }
}
