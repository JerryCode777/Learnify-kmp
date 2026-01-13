package org.example.learnify.presentation.upload

import org.example.learnify.domain.model.LearningPath
import org.example.learnify.domain.model.PdfExtractionResult
import org.example.learnify.domain.model.Topic

sealed interface UploadUiState {
    data object Idle : UploadUiState
    data object SelectingFile : UploadUiState
    data object ExtractingContent : UploadUiState
    data class ExtractingPages(
        val currentPage: Int,
        val totalPages: Int,
        val percentage: Float
    ) : UploadUiState
    data class Success(val result: PdfExtractionResult) : UploadUiState
    data object GeneratingLearningPath : UploadUiState
    data class ProcessingChunks(
        val currentChunk: Int,
        val totalChunks: Int,
        val message: String,
        val percentage: Float,
        val partialTopics: List<Topic> = emptyList()
    ) : UploadUiState
    data class Canceled(val message: String) : UploadUiState
    data class LearningPathGenerated(val learningPath: LearningPath) : UploadUiState
    data class Error(val message: String) : UploadUiState
}
