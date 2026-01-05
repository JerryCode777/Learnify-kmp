package org.example.learnify.presentation.upload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.learnify.domain.model.DocumentJson
import org.example.learnify.domain.model.LearningPath
import org.example.learnify.domain.usecase.ExtractPdfToJsonUseCase
import org.example.learnify.domain.usecase.ProcessDocumentInChunksUseCase
import org.example.learnify.util.FilePicker
import org.example.learnify.util.currentTimeMillis

class UploadViewModel(
    private val extractPdfToJsonUseCase: ExtractPdfToJsonUseCase,
    private val processDocumentInChunksUseCase: ProcessDocumentInChunksUseCase,
    private val filePicker: FilePicker
) : ViewModel() {

    private val _uiState = MutableStateFlow<UploadUiState>(UploadUiState.Idle)
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    private var currentDocumentJson: DocumentJson? = null
    private var currentFilename: String = ""

    fun onFileSelected(fileUri: String) {
        viewModelScope.launch {
            _uiState.value = UploadUiState.ExtractingContent
            Napier.d("Iniciando extracción COMPLETA del archivo: $fileUri")

            // Extraer el nombre del archivo de la URI
            currentFilename = fileUri.substringAfterLast("/").removeSuffix(".pdf") + ".pdf"

            // PASO 1: Extracción completa a JSON
            val extractionResult = extractPdfToJsonUseCase(fileUri, currentFilename)

            extractionResult.onSuccess { documentJson ->
                Napier.i("Extracción completa exitosa: ${documentJson.metadata.totalPages} páginas, ${documentJson.metadata.totalCharacters} caracteres")
                currentDocumentJson = documentJson

                // Crear PdfExtractionResult para compatibilidad con UI
                val extraction = org.example.learnify.domain.model.PdfExtractionResult(
                    text = documentJson.pages.joinToString("\n\n") { it.content },
                    totalPages = documentJson.metadata.totalPages,
                    pages = documentJson.pages.map { pageJson ->
                        org.example.learnify.domain.model.PageContent(
                            pageNumber = pageJson.pageNumber,
                            text = pageJson.content
                        )
                    }
                )

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
        val documentJson = currentDocumentJson

        if (documentJson == null) {
            _uiState.value = UploadUiState.Error("No hay documento cargado")
            return
        }

        viewModelScope.launch {
            // PASO 2: Procesamiento exhaustivo por chunks
            _uiState.value = UploadUiState.GeneratingLearningPath
            Napier.d("Iniciando procesamiento por chunks del documento completo...")

            val result = processDocumentInChunksUseCase(
                documentJson = documentJson,
                onProgress = { current, total, message ->
                    val percentage = (current.toFloat() / total.toFloat())
                    val percentageInt = (percentage * 100).toInt()
                    Napier.i("Progreso: $current/$total ($percentageInt%) - $message")

                    // Actualizar UI con progreso
                    _uiState.value = UploadUiState.ProcessingChunks(
                        currentChunk = current,
                        totalChunks = total,
                        message = message,
                        percentage = percentage
                    )
                }
            )

            result.onSuccess { learningPath ->
                Napier.i("Ruta de aprendizaje generada: ${learningPath.topics.size} temas totales")
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
        viewModelScope.launch {
            _uiState.value = UploadUiState.SelectingFile
            Napier.d("Abriendo file picker...")

            val selectedFile = filePicker.pickPdfFile()

            if (selectedFile != null) {
                Napier.d("Archivo seleccionado desde file picker: $selectedFile")
                onFileSelected(selectedFile)
            } else {
                Napier.w("No se seleccionó ningún archivo")
                _uiState.value = UploadUiState.Idle
            }
        }
    }

    fun resetState() {
        _uiState.value = UploadUiState.Idle
        currentDocumentJson = null
        currentFilename = ""
    }

    fun onErrorDismissed() {
        _uiState.value = UploadUiState.Idle
    }

    private fun generateDocumentId(): String {
        return "doc_${currentTimeMillis()}"
    }
}
