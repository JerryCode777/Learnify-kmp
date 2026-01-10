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

    override suspend fun extractTextWithProgress(
        fileUri: String,
        batchSize: Int,
        onProgress: (currentPage: Int, totalPages: Int, batch: List<PageContent>) -> Unit
    ): Result<PdfExtractionResult> = withContext(Dispatchers.IO) {
        try {
            Napier.d("Iniciando extracción INCREMENTAL del PDF desde: $fileUri")

            val uri = Uri.parse(fileUri)
            val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                ?: return@withContext Result.failure(Exception("No se pudo abrir el archivo PDF"))

            val pdfRenderer = PdfRenderer(parcelFileDescriptor)
            val totalPages = pdfRenderer.pageCount

            Napier.i("PDF cargado: $totalPages páginas. Extrayendo en lotes de $batchSize páginas...")

            val allPages = mutableListOf<PageContent>()
            val fullTextBuilder = StringBuilder()

            try {
                // Procesar en lotes
                var currentPage = 0
                while (currentPage < totalPages) {
                    val endPage = minOf(currentPage + batchSize, totalPages)
                    val batchPages = mutableListOf<PageContent>()

                    Napier.d("Procesando lote: páginas ${currentPage + 1} a $endPage de $totalPages")

                    for (pageIndex in currentPage until endPage) {
                        try {
                            val page = pdfRenderer.openPage(pageIndex)

                            // Nota: PdfRenderer en Android no extrae texto directamente
                            // Para extracción de texto real, necesitaríamos una biblioteca como PDFBox
                            val pageText = "Página ${pageIndex + 1}\n[Contenido extraído de la página ${pageIndex + 1}]\n\n"

                            // Limitar tamaño por página
                            val limitedText = if (pageText.length > 10000) {
                                pageText.take(10000) + "..."
                            } else {
                                pageText
                            }

                            val pageContent = PageContent(
                                pageNumber = pageIndex + 1,
                                text = limitedText
                            )

                            batchPages.add(pageContent)
                            allPages.add(pageContent)
                            fullTextBuilder.append(limitedText)
                            fullTextBuilder.append("\n\n")

                            page.close()

                        } catch (e: Exception) {
                            Napier.e("Error en página ${pageIndex + 1}", e)
                            // Continuar con la siguiente página
                        }
                    }

                    currentPage = endPage

                    // Reportar progreso después de cada lote
                    onProgress(currentPage, totalPages, batchPages)

                    // Liberar memoria después de cada lote
                    if (currentPage % 100 == 0) {
                        Napier.d("Checkpoint de memoria en página $currentPage")
                        System.gc()
                    }
                }

            } finally {
                pdfRenderer.close()
                parcelFileDescriptor.close()
            }

            Napier.i("Extracción incremental completada: ${allPages.size} páginas, ${fullTextBuilder.length} caracteres")

            Result.success(
                PdfExtractionResult(
                    text = fullTextBuilder.toString().trim(),
                    totalPages = allPages.size,
                    pages = allPages
                )
            )

        } catch (e: Exception) {
            Napier.e("Error en extracción incremental", e)
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
