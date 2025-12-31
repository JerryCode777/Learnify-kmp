package org.example.learnify.util

import io.github.aakira.napier.Napier
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.example.learnify.domain.model.PageContent
import org.example.learnify.domain.model.PdfExtractionResult
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfURL
import platform.PDFKit.PDFDocument
import platform.darwin.NSInteger

class IosPdfExtractor : PdfExtractor {

    override suspend fun extractText(fileUri: String): Result<PdfExtractionResult> = withContext(Dispatchers.IO) {
        try {
            val url = NSURL.URLWithString(fileUri)
                ?: return@withContext Result.failure(Exception("URL inválida: $fileUri"))

            val pdfDocument = PDFDocument(url)
                ?: return@withContext Result.failure(Exception("No se pudo cargar el PDF desde: $fileUri"))

            val result = extractFromPdfDocument(pdfDocument)
            Result.success(result)
        } catch (e: Exception) {
            Napier.e("Error extrayendo texto del PDF en iOS", e)
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun extractTextFromBytes(pdfBytes: ByteArray): Result<PdfExtractionResult> = withContext(Dispatchers.IO) {
        try {
            // Convertir ByteArray a NSData
            val nsData = pdfBytes.usePinned { pinned ->
                NSData.create(
                    bytes = pinned.addressOf(0),
                    length = pdfBytes.size.toULong()
                )
            }

            val pdfDocument = PDFDocument(nsData)
                ?: return@withContext Result.failure(Exception("No se pudo crear PDF desde bytes"))

            val result = extractFromPdfDocument(pdfDocument)
            Result.success(result)
        } catch (e: Exception) {
            Napier.e("Error extrayendo texto de bytes PDF en iOS", e)
            Result.failure(e)
        }
    }

    private fun extractFromPdfDocument(pdfDocument: PDFDocument): PdfExtractionResult {
        val totalPages = pdfDocument.pageCount.toInt()
        val pages = mutableListOf<PageContent>()
        val fullTextBuilder = StringBuilder()

        for (pageIndex in 0 until totalPages) {
            val page = pdfDocument.pageAtIndex(pageIndex.toULong())
            val pageText = page?.string ?: ""

            val pageContent = PageContent(
                pageNumber = pageIndex + 1,
                text = pageText
            )

            pages.add(pageContent)
            fullTextBuilder.append(pageText)
            fullTextBuilder.append("\n\n")
        }

        return PdfExtractionResult(
            text = fullTextBuilder.toString().trim(),
            totalPages = totalPages,
            pages = pages
        )
    }
}

actual fun getPdfExtractor(): PdfExtractor {
    return IosPdfExtractor()
}
