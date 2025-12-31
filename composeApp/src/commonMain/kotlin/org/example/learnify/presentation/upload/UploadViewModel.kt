package org.example.learnify.presentation.upload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.learnify.domain.model.LearningPath
import org.example.learnify.domain.usecase.ExtractPdfContentUseCase
import org.example.learnify.domain.usecase.GenerateLearningPathUseCase
import org.example.learnify.util.currentTimeMillis

class UploadViewModel(
    private val extractPdfContentUseCase: ExtractPdfContentUseCase,
    private val generateLearningPathUseCase: GenerateLearningPathUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<UploadUiState>(UploadUiState.Idle)
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    private var currentDocumentId: String? = null
    private var currentContent: String? = null

    fun onFileSelected(fileUri: String) {
        viewModelScope.launch {
            _uiState.value = UploadUiState.ExtractingContent
            Napier.d("Iniciando extracción del archivo: $fileUri")

            val result = extractPdfContentUseCase(fileUri)

            result.onSuccess { extraction ->
                Napier.i("Extracción exitosa: ${extraction.totalPages} páginas")
                currentContent = extraction.text
                currentDocumentId = generateDocumentId()
                _uiState.value = UploadUiState.Success(extraction)
            }.onFailure { error ->
                Napier.e("Error en extracción", error)
                _uiState.value = UploadUiState.Error(
                    error.message ?: "Error desconocido al procesar el PDF"
                )
            }
        }
    }

    fun onGenerateLearningPath() {
        val content = currentContent
        val documentId = currentDocumentId

        if (content == null || documentId == null) {
            _uiState.value = UploadUiState.Error("No hay documento cargado")
            return
        }

        viewModelScope.launch {
            _uiState.value = UploadUiState.GeneratingLearningPath
            Napier.d("Generando ruta de aprendizaje...")

            val result = generateLearningPathUseCase(documentId, content)

            result.onSuccess { learningPath ->
                Napier.i("Ruta de aprendizaje generada: ${learningPath.topics.size} temas")
                _uiState.value = UploadUiState.LearningPathGenerated(learningPath)
            }.onFailure { error ->
                Napier.e("Error generando ruta de aprendizaje", error)
                _uiState.value = UploadUiState.Error(
                    error.message ?: "Error al generar la ruta de aprendizaje"
                )
            }
        }
    }

    fun onSelectFileClick() {
        _uiState.value = UploadUiState.SelectingFile
    }

    fun resetState() {
        _uiState.value = UploadUiState.Idle
        currentDocumentId = null
        currentContent = null
    }

    fun onErrorDismissed() {
        _uiState.value = UploadUiState.Idle
    }

    private fun generateDocumentId(): String {
        return "doc_${currentTimeMillis()}"
    }
}
