package org.example.learnify.util

import io.github.aakira.napier.Napier
import kotlin.native.runtime.NativeRuntimeApi
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

    override suspend fun extractTextWithProgress(
        fileUri: String,
        batchSize: Int,
        onProgress: (currentPage: Int, totalPages: Int, batch: List<PageContent>) -> Unit
    ): Result<PdfExtractionResult> = withContext(Dispatchers.IO) {
        try {
            Napier.d("Iniciando extracción INCREMENTAL del PDF desde: $fileUri")

            // Limpiar la ruta
            val cleanPath = if (fileUri.startsWith("file://")) {
                fileUri.removePrefix("file://")
            } else {
                fileUri
            }

            // Crear URL y verificar archivo
            val url = NSURL.fileURLWithPath(cleanPath)
            val fileManager = platform.Foundation.NSFileManager.defaultManager

            if (!fileManager.fileExistsAtPath(cleanPath)) {
                return@withContext Result.failure(Exception("El archivo no existe en la ruta especificada"))
            }

            // Cargar documento PDF
            val pdfDocument = PDFDocument(url)
            if (pdfDocument == null) {
                return@withContext Result.failure(Exception("No se pudo cargar el PDF"))
            }

            val totalPages = pdfDocument.pageCount.toInt()
            Napier.i("PDF cargado: $totalPages páginas. Extrayendo en lotes de $batchSize páginas...")

            // IMPORTANTE: Limitar a un máximo de páginas para documentos muy grandes
            val maxPagesToExtract = minOf(totalPages, 200) // Límite: primeras 200 páginas
            if (totalPages > maxPagesToExtract) {
                Napier.w("⚠️ Documento muy grande ($totalPages páginas). Limitando a las primeras $maxPagesToExtract páginas para evitar problemas de memoria.")
            }

            val allPages = mutableListOf<PageContent>()
            val fullTextBuilder = StringBuilder()

            // Procesar en lotes
            var currentPage = 0
            while (currentPage < maxPagesToExtract) {
                val endPage = minOf(currentPage + batchSize, maxPagesToExtract)
                val batchPages = mutableListOf<PageContent>()

                Napier.d("Procesando lote: páginas ${currentPage + 1} a $endPage de $maxPagesToExtract")

                for (pageIndex in currentPage until endPage) {
                    try {
                        val page = pdfDocument.pageAtIndex(pageIndex.toULong())
                        val pageText = page?.string ?: ""

                        // Limitar tamaño por página más agresivamente
                        val limitedText = if (pageText.length > 5000) {
                            Napier.d("Página ${pageIndex + 1} muy larga (${pageText.length} chars), truncando a 5000")
                            pageText.take(5000) + "..."
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

                    } catch (e: Exception) {
                        Napier.e("Error en página ${pageIndex + 1}", e)
                        // Continuar con la siguiente página
                    }
                }

                currentPage = endPage

                // Reportar progreso después de cada lote
                onProgress(currentPage, maxPagesToExtract, batchPages)

                // Liberar memoria después de cada lote - más frecuente
                if (currentPage % 20 == 0) {
                    Napier.d("Liberando memoria en página $currentPage")
                    @OptIn(NativeRuntimeApi::class)
                    kotlin.native.runtime.GC.collect()
                }
            }

            // Liberar memoria final
            @OptIn(NativeRuntimeApi::class)
            kotlin.native.runtime.GC.collect()

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

    override suspend fun extractText(fileUri: String): Result<PdfExtractionResult> = withContext(Dispatchers.IO) {
        try {
            Napier.d("Iniciando extracción de PDF desde: $fileUri")

            // Verificar si la ruta comienza con file:// y removerla si es necesario
            val cleanPath = if (fileUri.startsWith("file://")) {
                fileUri.removePrefix("file://")
            } else {
                fileUri
            }

            Napier.d("Ruta limpia: $cleanPath")

            // Crear URL desde la ruta
            val url = NSURL.fileURLWithPath(cleanPath)
            Napier.d("URL creada: ${url.absoluteString}")

            // Verificar si el archivo existe
            val fileManager = platform.Foundation.NSFileManager.defaultManager
            val fileExists = fileManager.fileExistsAtPath(cleanPath)

            if (!fileExists) {
                Napier.e("El archivo no existe en la ruta: $cleanPath")
                return@withContext Result.failure(Exception("El archivo no existe en la ruta especificada"))
            }

            Napier.d("Archivo existe, intentando cargar PDF...")

            // Intentar cargar el documento PDF
            val pdfDocument = PDFDocument(url)
            if (pdfDocument == null) {
                Napier.e("No se pudo cargar el PDF desde: $cleanPath")

                // Intentar leer como data para verificar si es un PDF válido
                val data = platform.Foundation.NSData.dataWithContentsOfURL(url)
                if (data == null) {
                    return@withContext Result.failure(Exception("No se pudo leer el archivo. Puede que no tengas permisos de acceso."))
                }

                return@withContext Result.failure(Exception("El archivo no es un PDF válido o está corrupto."))
            }

            Napier.i("PDF cargado exitosamente, ${pdfDocument.pageCount} páginas")

            val result = extractFromPdfDocument(pdfDocument)
            Napier.i("Extracción completada: ${result.totalPages} páginas, ${result.text.length} caracteres")

            Result.success(result)
        } catch (e: Exception) {
            Napier.e("Error extrayendo texto del PDF en iOS", e)
            Result.failure(Exception("Error al procesar el PDF: ${e.message}", e))
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

    @OptIn(NativeRuntimeApi::class)
    private fun extractFromPdfDocument(pdfDocument: PDFDocument): PdfExtractionResult {
        val totalPages = pdfDocument.pageCount.toInt()
        val pages = mutableListOf<PageContent>()
        val fullTextBuilder = StringBuilder()

        Napier.d("Extrayendo texto de $totalPages páginas...")

        // Limitar a un máximo de páginas para evitar problemas de memoria en iOS
        val maxPages = minOf(totalPages, 500) // Límite de seguridad

        if (totalPages > maxPages) {
            Napier.w("Documento muy grande ($totalPages páginas), limitando a $maxPages páginas")
        }

        for (pageIndex in 0 until maxPages) {
            try {
                // Log cada 25 páginas para feedback
                if (pageIndex % 25 == 0) {
                    Napier.d("Procesando página ${pageIndex + 1} de $maxPages")
                }

                val page = pdfDocument.pageAtIndex(pageIndex.toULong())
                val pageText = page?.string ?: ""

                // Limitar el tamaño de texto por página para evitar memoria excesiva
                val limitedText = if (pageText.length > 10000) {
                    Napier.w("Página ${pageIndex + 1} muy larga (${pageText.length} chars), truncando")
                    pageText.take(10000) + "..."
                } else {
                    pageText
                }

                val pageContent = PageContent(
                    pageNumber = pageIndex + 1,
                    text = limitedText
                )

                pages.add(pageContent)
                fullTextBuilder.append(limitedText)
                fullTextBuilder.append("\n\n")

                // Liberar memoria cada 50 páginas
                if (pageIndex > 0 && pageIndex % 50 == 0) {
                    Napier.d("Checkpoint de memoria en página $pageIndex")
                    // Forzar colección de basura en iOS
                    kotlin.native.runtime.GC.collect()
                }

            } catch (e: Exception) {
                Napier.e("Error procesando página ${pageIndex + 1}", e)
                // Continuar con la siguiente página en lugar de fallar completamente
            }
        }

        val totalExtracted = pages.size
        Napier.i("Extracción completada: $totalExtracted de $totalPages páginas, ${fullTextBuilder.length} caracteres totales")

        return PdfExtractionResult(
            text = fullTextBuilder.toString().trim(),
            totalPages = totalExtracted,
            pages = pages
        )
    }
}

actual fun getPdfExtractor(): PdfExtractor {
    return IosPdfExtractor()
}
