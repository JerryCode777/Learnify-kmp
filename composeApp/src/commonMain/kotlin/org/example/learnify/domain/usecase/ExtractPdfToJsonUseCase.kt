package org.example.learnify.domain.usecase

import io.github.aakira.napier.Napier
import org.example.learnify.domain.model.*
import org.example.learnify.util.PdfExtractor

/**
 * Extrae TODO el contenido de un PDF a formato JSON estructurado
 * Similar a batch_content_extractor.py
 */
class ExtractPdfToJsonUseCase(
    private val pdfExtractor: PdfExtractor
) {
    suspend operator fun invoke(
        fileUri: String,
        filename: String,
        onProgress: ((currentPage: Int, totalPages: Int) -> Unit)? = null
    ): Result<DocumentJson> {
        return try {
            Napier.i("Iniciando extracción ${if (onProgress != null) "INCREMENTAL" else "completa"} a JSON: $filename")

            // Extraer todo el contenido del PDF
            val extraction = if (onProgress != null) {
                // Extracción incremental con progreso
                pdfExtractor.extractTextWithProgress(
                    fileUri = fileUri,
                    batchSize = 20,
                    onProgress = { current, total, _ ->
                        onProgress(current, total)
                    }
                ).getOrThrow()
            } else {
                // Extracción completa sin progreso
                pdfExtractor.extractText(fileUri).getOrThrow()
            }

            Napier.i("Extracción completada: ${extraction.totalPages} páginas, ${extraction.text.length} caracteres")

            // Contar palabras totales
            val totalWords = extraction.text.split(Regex("\\s+")).size

            // Construir JSON estructurado
            val pages = extraction.pages.map { pageContent ->
                PageJson(
                    pageNumber = pageContent.pageNumber,
                    content = pageContent.text,
                    wordCount = pageContent.text.split(Regex("\\s+")).size
                )
            }

            val documentJson = DocumentJson(
                documentId = generateDocumentId(),
                filename = filename,
                pages = pages,
                metadata = DocumentMetadata(
                    totalPages = extraction.totalPages,
                    totalCharacters = extraction.text.length,
                    totalWords = totalWords,
                    extractedAt = currentTimeMillis()
                )
            )

            Napier.i("JSON generado: ${pages.size} páginas, ${documentJson.metadata.totalCharacters} caracteres totales")

            Result.success(documentJson)

        } catch (e: Exception) {
            Napier.e("Error extrayendo PDF a JSON", e)
            Result.failure(e)
        }
    }

    private fun generateDocumentId(): String {
        return "doc_${currentTimeMillis()}"
    }

    private fun currentTimeMillis(): Long {
        return org.example.learnify.util.currentTimeMillis()
    }
}
