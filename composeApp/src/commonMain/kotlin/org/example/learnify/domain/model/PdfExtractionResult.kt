package org.example.learnify.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PdfExtractionResult(
    val text: String,
    val totalPages: Int,
    val pages: List<PageContent> = emptyList(),
    val wasTruncated: Boolean = false,
    val originalTotalPages: Int? = null
)

@Serializable
data class PageContent(
    val pageNumber: Int,
    val text: String
)
