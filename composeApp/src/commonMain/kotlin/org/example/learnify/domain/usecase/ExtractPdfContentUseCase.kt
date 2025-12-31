package org.example.learnify.domain.usecase

import io.github.aakira.napier.Napier
import org.example.learnify.domain.model.PdfExtractionResult
import org.example.learnify.util.PdfExtractor

/**
 * Caso de uso para extraer contenido de un PDF
 * Abstrae la lógica de extracción y manejo de errores
 */
class ExtractPdfContentUseCase(
    private val pdfExtractor: PdfExtractor
) {
    /**
     * Extrae el contenido de un PDF desde su URI
     */
    suspend operator fun invoke(fileUri: String): Result<PdfExtractionResult> {
        Napier.d("Iniciando extracción de PDF: $fileUri")

        return try {
            val result = pdfExtractor.extractText(fileUri)

            result.onSuccess { extractionResult ->
                Napier.i("PDF extraído exitosamente: ${extractionResult.totalPages} páginas")
            }.onFailure { error ->
                Napier.e("Error extrayendo PDF", error)
            }

            result
        } catch (e: Exception) {
            Napier.e("Excepción inesperada extrayendo PDF", e)
            Result.failure(e)
        }
    }

    /**
     * Extrae el contenido de un PDF desde bytes
     */
    suspend fun extractFromBytes(pdfBytes: ByteArray): Result<PdfExtractionResult> {
        Napier.d("Iniciando extracción de PDF desde bytes (${pdfBytes.size} bytes)")

        return try {
            val result = pdfExtractor.extractTextFromBytes(pdfBytes)

            result.onSuccess { extractionResult ->
                Napier.i("PDF desde bytes extraído exitosamente: ${extractionResult.totalPages} páginas")
            }.onFailure { error ->
                Napier.e("Error extrayendo PDF desde bytes", error)
            }

            result
        } catch (e: Exception) {
            Napier.e("Excepción inesperada extrayendo PDF desde bytes", e)
            Result.failure(e)
        }
    }

    /**
     * Valida que el contenido extraído sea válido
     */
    fun validateExtraction(result: PdfExtractionResult): Boolean {
        return result.text.isNotBlank() && result.totalPages > 0
    }
}
