package org.example.learnify.domain.usecase

import io.github.aakira.napier.Napier
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
        // Páginas por chunk - cada 20 páginas se procesa un chunk
        // Reducido de 25 a 20 para evitar exceder límites de tokens de Gemini
        private const val PAGES_PER_CHUNK = 20

        // Delay entre peticiones para evitar rate limiting (en milisegundos)
        // Gemini Pro: límite de requests por minuto → delay de 5 segundos = ~12 requests/minuto
        private const val DELAY_BETWEEN_CHUNKS_MS = 5000L

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
        onProgress: (current: Int, total: Int, message: String) -> Unit = { _, _, _ -> }
    ): Result<LearningPath> {
        return try {
            Napier.i("Iniciando procesamiento por chunks del documento: ${documentJson.filename}")
            Napier.i("Total de páginas: ${documentJson.metadata.totalPages}")
            Napier.i("Total de caracteres: ${documentJson.metadata.totalCharacters}")

            // Paso 1: Filtrar páginas irrelevantes (bibliografía, índices, etc.)
            val relevantPages = filterRelevantPages(documentJson.pages)
            Napier.i("Páginas relevantes después de filtrado: ${relevantPages.size} de ${documentJson.pages.size}")

            // Paso 2: Dividir en chunks de 25 páginas
            val chunks = createChunks(relevantPages)
            Napier.i("Documento dividido en ${chunks.size} chunks de ~$PAGES_PER_CHUNK páginas cada uno")

            // Paso 2: Procesar cada chunk con Gemini
            val allTopics = mutableListOf<Topic>()
            val failedChunks = mutableListOf<String>()

            chunks.forEachIndexed { index, chunk ->
                // Agregar delay entre chunks para evitar rate limiting (excepto el primero)
                if (index > 0) {
                    Napier.d("⏱️ Esperando ${DELAY_BETWEEN_CHUNKS_MS / 1000}s antes del siguiente chunk (evitar rate limit)...")
                    kotlinx.coroutines.delay(DELAY_BETWEEN_CHUNKS_MS)
                }

                val progress = "Procesando chunk ${index + 1} de ${chunks.size} (páginas ${chunk.startPage}-${chunk.endPage})"
                onProgress(index + 1, chunks.size, progress)
                Napier.i(progress)

                val result = processChunk(chunk, documentJson.documentId)

                result.onSuccess { topics ->
                    allTopics.addAll(topics)
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
                description = "Ruta de aprendizaje completa generada a partir de ${documentJson.metadata.totalPages} páginas",
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
     * Divide las páginas en chunks de 25 páginas cada uno
     */
    private fun createChunks(pages: List<PageJson>): List<ContentChunk> {
        val chunks = mutableListOf<ContentChunk>()

        // Dividir en chunks de PAGES_PER_CHUNK páginas
        for (i in pages.indices step PAGES_PER_CHUNK) {
            val chunkPages = pages.subList(
                i,
                minOf(i + PAGES_PER_CHUNK, pages.size)
            )

            if (chunkPages.isEmpty()) continue

            val totalChars = chunkPages.sumOf { it.content.length }

            chunks.add(
                ContentChunk(
                    chunkIndex = chunks.size,
                    startPage = chunkPages.first().pageNumber,
                    endPage = chunkPages.last().pageNumber,
                    pages = chunkPages,
                    totalCharacters = totalChars
                )
            )

            Napier.d("Chunk ${chunks.size}: páginas ${chunkPages.first().pageNumber}-${chunkPages.last().pageNumber}, $totalChars caracteres")
        }

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
