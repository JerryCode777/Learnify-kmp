package org.example.learnify.domain.model

import kotlinx.serialization.Serializable

/**
 * Estructura JSON completa del documento extraído
 * Similar a la salida de batch_content_extractor.py
 */
@Serializable
data class DocumentJson(
    val documentId: String,
    val filename: String,
    val pages: List<PageJson>,
    val metadata: DocumentMetadata
)

@Serializable
data class PageJson(
    val pageNumber: Int,
    val content: String,
    val wordCount: Int
)

@Serializable
data class DocumentMetadata(
    val totalPages: Int,
    val totalCharacters: Int,
    val totalWords: Int,
    val extractedAt: Long,
    val processingMethod: String = "PDFKit/PdfRenderer"
)

/**
 * Chunk de contenido para procesamiento con Gemini
 */
data class ContentChunk(
    val chunkIndex: Int,
    val startPage: Int,
    val endPage: Int,
    val pages: List<PageJson>,
    val totalCharacters: Int
)
