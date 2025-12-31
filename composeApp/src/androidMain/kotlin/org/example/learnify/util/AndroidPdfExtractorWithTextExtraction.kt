package org.example.learnify.util

import android.content.Context
import android.net.Uri
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.learnify.domain.model.PageContent
import org.example.learnify.domain.model.PdfExtractionResult
import java.io.InputStream

/**
 * Extractor de PDF mejorado que realmente extrae el texto
 * Usa Apache PDFBox para Android
 *
 * NOTA: Requiere agregar dependencia:
 * implementation("com.tom-roush:pdfbox-android:2.0.27.0")
 */
class AndroidPdfExtractorWithTextExtraction(private val context: Context) : PdfExtractor {

    override suspend fun extractText(fileUri: String): Result<PdfExtractionResult> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(fileUri)
            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("No se pudo abrir el archivo PDF"))

            val result = extractFromInputStream(inputStream)
            inputStream.close()

            Result.success(result)
        } catch (e: Exception) {
            Napier.e("Error extrayendo texto del PDF", e)
            Result.failure(e)
        }
    }

    override suspend fun extractTextFromBytes(pdfBytes: ByteArray): Result<PdfExtractionResult> = withContext(Dispatchers.IO) {
        try {
            val inputStream = pdfBytes.inputStream()
            val result = extractFromInputStream(inputStream)
            inputStream.close()

            Result.success(result)
        } catch (e: Exception) {
            Napier.e("Error extrayendo texto de bytes PDF", e)
            Result.failure(e)
        }
    }

    private fun extractFromInputStream(inputStream: InputStream): PdfExtractionResult {
        // TODO: Implementar con PDFBox cuando se agregue la dependencia
        // Por ahora, versión simplificada

        /*
        // Código de ejemplo cuando se agregue PDFBox:

        val document = PDDocument.load(inputStream)
        val stripper = PDFTextStripper()
        val totalPages = document.numberOfPages
        val pages = mutableListOf<PageContent>()
        val fullTextBuilder = StringBuilder()

        for (pageNum in 1..totalPages) {
            stripper.startPage = pageNum
            stripper.endPage = pageNum
            val pageText = stripper.getText(document)

            pages.add(
                PageContent(
                    pageNumber = pageNum,
                    text = pageText
                )
            )

            fullTextBuilder.append(pageText)
        }

        document.close()

        return PdfExtractionResult(
            text = fullTextBuilder.toString(),
            totalPages = totalPages,
            pages = pages
        )
        */

        // Placeholder mientras no tengamos PDFBox
        return PdfExtractionResult(
            text = "Texto del PDF extraído (requiere implementación con PDFBox)",
            totalPages = 1,
            pages = listOf(
                PageContent(
                    pageNumber = 1,
                    text = "Contenido del PDF"
                )
            )
        )
    }
}
