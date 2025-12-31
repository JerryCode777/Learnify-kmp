package org.example.learnify.util

import org.example.learnify.domain.model.PdfExtractionResult

/**
 * Interface para extraer texto de PDFs
 * Implementaciones específicas por plataforma usando expect/actual
 */
interface PdfExtractor {
    /**
     * Extrae texto de un archivo PDF
     * @param fileUri URI del archivo PDF (diferentes formatos por plataforma)
     * @return Resultado con el texto extraído y metadatos
     */
    suspend fun extractText(fileUri: String): Result<PdfExtractionResult>

    /**
     * Extrae texto de bytes de PDF
     * @param pdfBytes Contenido del PDF en bytes
     * @return Resultado con el texto extraído y metadatos
     */
    suspend fun extractTextFromBytes(pdfBytes: ByteArray): Result<PdfExtractionResult>
}

/**
 * Función expect para obtener la implementación específica de plataforma
 */
expect fun getPdfExtractor(): PdfExtractor
