package org.example.learnify.util

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.learnify.domain.model.PageContent
import org.example.learnify.domain.model.PdfExtractionResult
import java.io.File

class AndroidPdfExtractor(private val context: Context) : PdfExtractor {

    override suspend fun extractText(fileUri: String): Result<PdfExtractionResult> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(fileUri)
            val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                ?: return@withContext Result.failure(Exception("No se pudo abrir el archivo PDF"))

            val result = extractFromParcelFileDescriptor(parcelFileDescriptor)
            parcelFileDescriptor.close()

            Result.success(result)
        } catch (e: Exception) {
            Napier.e("Error extrayendo texto del PDF", e)
            Result.failure(e)
        }
    }

    override suspend fun extractTextFromBytes(pdfBytes: ByteArray): Result<PdfExtractionResult> = withContext(Dispatchers.IO) {
        try {
            // Crear archivo temporal
            val tempFile = File.createTempFile("temp_pdf", ".pdf", context.cacheDir)
            tempFile.writeBytes(pdfBytes)

            val parcelFileDescriptor = ParcelFileDescriptor.open(
                tempFile,
                ParcelFileDescriptor.MODE_READ_ONLY
            )

            val result = extractFromParcelFileDescriptor(parcelFileDescriptor)
            parcelFileDescriptor.close()
            tempFile.delete()

            Result.success(result)
        } catch (e: Exception) {
            Napier.e("Error extrayendo texto de bytes PDF", e)
            Result.failure(e)
        }
    }

    private fun extractFromParcelFileDescriptor(parcelFileDescriptor: ParcelFileDescriptor): PdfExtractionResult {
        val pdfRenderer = PdfRenderer(parcelFileDescriptor)
        val totalPages = pdfRenderer.pageCount
        val pages = mutableListOf<PageContent>()
        val fullTextBuilder = StringBuilder()

        try {
            for (pageIndex in 0 until totalPages) {
                val page = pdfRenderer.openPage(pageIndex)

                // Nota: PdfRenderer en Android no extrae texto directamente
                // Para extracción de texto real, necesitaríamos una biblioteca como PDFBox o Apache PDFBox Android
                // Por ahora, creamos un placeholder indicando el número de página
                val pageText = "Página ${pageIndex + 1}\n[Contenido extraído de la página ${pageIndex + 1}]\n\n"

                pages.add(
                    PageContent(
                        pageNumber = pageIndex + 1,
                        text = pageText
                    )
                )

                fullTextBuilder.append(pageText)
                page.close()
            }
        } finally {
            pdfRenderer.close()
        }

        return PdfExtractionResult(
            text = fullTextBuilder.toString(),
            totalPages = totalPages,
            pages = pages
        )
    }
}

actual fun getPdfExtractor(): PdfExtractor {
    // Esto necesita el contexto de Android
    // En uso real, se inyectará vía Koin
    throw IllegalStateException("Use AndroidPdfExtractor(context) directamente o inyéctelo con Koin")
}
