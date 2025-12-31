package org.example.learnify.presentation.upload

import org.example.learnify.domain.model.LearningPath
import org.example.learnify.domain.model.PdfExtractionResult

sealed interface UploadUiState {
    data object Idle : UploadUiState
    data object SelectingFile : UploadUiState
    data object ExtractingContent : UploadUiState
    data class Success(val result: PdfExtractionResult) : UploadUiState
    data object GeneratingLearningPath : UploadUiState
    data class LearningPathGenerated(val learningPath: LearningPath) : UploadUiState
    data class Error(val message: String) : UploadUiState
}
