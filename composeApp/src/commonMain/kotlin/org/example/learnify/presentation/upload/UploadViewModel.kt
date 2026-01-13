package org.example.learnify.presentation.upload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import com.benasher44.uuid.uuid4
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.learnify.domain.model.DocumentJson
import org.example.learnify.domain.model.LearningPath
import org.example.learnify.domain.model.Topic
import org.example.learnify.domain.usecase.ExtractPdfToJsonUseCase
import org.example.learnify.domain.usecase.ProcessDocumentInChunksUseCase
import org.example.learnify.util.FilePicker
import org.example.learnify.util.currentTimeMillis
import org.example.learnify.data.remote.GeminiApiClient
import kotlinx.coroutines.CancellationException

class UploadViewModel(
    private val extractPdfToJsonUseCase: ExtractPdfToJsonUseCase,
    private val processDocumentInChunksUseCase: ProcessDocumentInChunksUseCase,
    private val filePicker: FilePicker
) : ViewModel() {

    private val _uiState = MutableStateFlow<UploadUiState>(UploadUiState.Idle)
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    private var currentDocumentJson: DocumentJson? = null
    private var currentFilename: String = ""
    private var processingJob: Job? = null
    private val pagesPerPart = 200

    fun onFileSelected(fileUri: String) {
        viewModelScope.launch {
            try {
                _uiState.value = UploadUiState.ExtractingContent
                Napier.d("Iniciando extracción INCREMENTAL del archivo: $fileUri")

                // Extraer el nombre del archivo de la URI
                currentFilename = fileUri.substringAfterLast("/").removeSuffix(".pdf") + ".pdf"
                Napier.d("Nombre del archivo: $currentFilename")

                // PASO 1: Extracción incremental a JSON con progreso
                Napier.d("Llamando a extractPdfToJsonUseCase con progreso...")
                val extractionResult = extractPdfToJsonUseCase(
                    fileUri = fileUri,
                    filename = currentFilename,
                    onProgress = { currentPage, totalPages ->
                        val percentage = currentPage.toFloat() / totalPages.toFloat()
                        Napier.i("Extrayendo: $currentPage/$totalPages páginas (${(percentage * 100).toInt()}%)")
                        _uiState.value = UploadUiState.ExtractingPages(
                            currentPage = currentPage,
                            totalPages = totalPages,
                            percentage = percentage
                        )
                    }
                )

                extractionResult.onSuccess { documentJson ->
                    Napier.i("Extracción completa exitosa: ${documentJson.metadata.totalPages} páginas, ${documentJson.metadata.totalCharacters} caracteres")
                    currentDocumentJson = documentJson

                    // Verificar si el documento fue truncado
                    if (documentJson.metadata.wasTruncated && documentJson.metadata.originalTotalPages != null) {
                        val originalPages = documentJson.metadata.originalTotalPages
                        val extractedPages = documentJson.metadata.totalPages
                        Napier.w("⚠️ Documento truncado: $originalPages → $extractedPages páginas")

                        // Mostrar advertencia al usuario
                        _uiState.value = UploadUiState.Error(
                            """⚠️ Documento muy grande
                            |
                            |Tu PDF tiene $originalPages páginas, pero solo se pudieron extraer las primeras $extractedPages páginas por límites de memoria del dispositivo.
                            |
                            |Recomendación: Divide el PDF en partes más pequeñas (máximo 1000 páginas cada una) para procesar todo el contenido.
                            """.trimMargin()
                        )
                        return@launch
                    }

                    // Crear PdfExtractionResult para compatibilidad con UI
                    val extraction = org.example.learnify.domain.model.PdfExtractionResult(
                        text = documentJson.pages.joinToString("\n\n") { it.content },
                        totalPages = documentJson.metadata.totalPages,
                        pages = documentJson.pages.map { pageJson ->
                            org.example.learnify.domain.model.PageContent(
                                pageNumber = pageJson.pageNumber,
                                text = pageJson.content
                            )
                        },
                        wasTruncated = documentJson.metadata.wasTruncated,
                        originalTotalPages = documentJson.metadata.originalTotalPages
                    )

                    Napier.d("Actualizando UI a Success")
                    _uiState.value = UploadUiState.Success(extraction)
                }.onFailure { error ->
                    Napier.e("Error en extracción", error)
                    _uiState.value = UploadUiState.Error(
                        userFacingErrorMessage(
                            error,
                            "Error desconocido al procesar el PDF"
                        )
                    )
                }
            } catch (e: Exception) {
                Napier.e("Exception no capturada en onFileSelected", e)
                _uiState.value = UploadUiState.Error(
                    "Error inesperado: ${e.message}"
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

        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            var partialTopics: List<Topic> = emptyList()
            val allTopics = mutableListOf<Topic>()
            var learningPathTitle: String? = null
            val failedParts = mutableListOf<String>()

            // PASO 2: Procesamiento exhaustivo por chunks
            _uiState.value = UploadUiState.GeneratingLearningPath
            Napier.d("Iniciando procesamiento por chunks del documento completo...")

            val parts = documentJson.pages.chunked(pagesPerPart)
            val totalParts = parts.size

            for ((partIndex, partPages) in parts.withIndex()) {
                ensureActive()
                val partLabel = "Parte ${partIndex + 1}/$totalParts"
                val startPage = partPages.firstOrNull()?.pageNumber ?: (partIndex * pagesPerPart + 1)
                val endPage = partPages.lastOrNull()?.pageNumber ?: ((partIndex + 1) * pagesPerPart)

                Napier.i("📖 Procesando $partLabel (páginas $startPage-$endPage)")

                val result = processDocumentInChunksUseCase(
                    documentJson = documentJson,
                    onProgress = { current, total, message ->
                        val partProgress = current.toFloat() / total.toFloat()
                        val overallProgress = (partIndex + partProgress) / totalParts.toFloat()
                        val percentageInt = (overallProgress * 100).toInt()
                        val prefixedMessage = "$partLabel - $message"
                        Napier.i("Progreso: $percentageInt% - $prefixedMessage")

                        // Actualizar UI con progreso
                        _uiState.value = UploadUiState.ProcessingChunks(
                            currentChunk = current,
                            totalChunks = total,
                            message = prefixedMessage,
                            percentage = overallProgress,
                            partialTopics = partialTopics
                        )
                    },
                    onChunkSuccess = { topics ->
                        partialTopics = partialTopics + topics
                        val state = _uiState.value
                        if (state is UploadUiState.ProcessingChunks) {
                            _uiState.value = state.copy(partialTopics = partialTopics)
                        }
                    },
                    chunkTimeoutMs = 90000L,
                    pagesOverride = partPages
                )

                result.onSuccess { learningPath ->
                    if (learningPathTitle == null) {
                        learningPathTitle = learningPath.title
                    }
                    allTopics.addAll(learningPath.topics)
                    Napier.i("✅ $partLabel completada: ${learningPath.topics.size} temas generados")
                }.onFailure { error ->
                    // Si el usuario canceló, detener todo
                    if (error is CancellationException) {
                        Napier.w("❌ Usuario canceló el procesamiento en $partLabel")
                        _uiState.value = UploadUiState.Canceled("Procesamiento cancelado por el usuario")
                        return@launch
                    }

                    // Para otros errores, registrar y CONTINUAR con las siguientes partes
                    val errorMsg = "$partLabel (págs $startPage-$endPage): ${error.message}"
                    failedParts.add(errorMsg)
                    Napier.e("❌ Error en $partLabel - CONTINUANDO con siguiente parte", error)
                    Napier.e("   Error: ${error.message}")
                }
            }

            // Verificar resultados
            if (allTopics.isEmpty()) {
                val errorDetails = if (failedParts.isNotEmpty()) {
                    "Todas las partes fallaron:\n${failedParts.joinToString("\n")}"
                } else {
                    "No se pudieron generar temas del documento"
                }
                Napier.e("❌ Procesamiento completo fallido: $errorDetails")
                _uiState.value = UploadUiState.Error(errorDetails)
                return@launch
            }

            // Generate description with processed parts information
            val successfulParts = totalParts - failedParts.size
            val description = if (failedParts.isEmpty()) {
                "Complete summary in $totalParts parts"
            } else {
                "⚠️ Processed $successfulParts of $totalParts parts. " +
                "Some parts failed due to API limits."
            }

            val finalLearningPath = LearningPath(
                id = uuid4().toString(),
                documentId = documentJson.documentId,
                title = learningPathTitle ?: documentJson.filename.removeSuffix(".pdf"),
                description = description,
                topics = allTopics,
                createdAt = currentTimeMillis()
            )

            // Log de resultados
            if (failedParts.isEmpty()) {
                Napier.i("✅ Procesamiento completo exitoso: ${finalLearningPath.topics.size} temas en $totalParts partes")
            } else {
                Napier.w("⚠️ Procesamiento parcial: ${finalLearningPath.topics.size} temas generados")
                Napier.w("   Partes exitosas: $successfulParts/$totalParts")
                Napier.w("   Partes fallidas:")
                failedParts.forEach { Napier.w("   - $it") }
            }

            _uiState.value = UploadUiState.LearningPathGenerated(finalLearningPath)
        }
    }

    fun onCancelProcessing() {
        processingJob?.cancel()
        processingJob = null
        _uiState.value = UploadUiState.Canceled("Procesamiento cancelado por el usuario")
    }

    fun onSelectFileClick() {
        viewModelScope.launch {
            _uiState.value = UploadUiState.SelectingFile
            Napier.d("Abriendo file picker...")

            if (filePicker.supportsDirectPicker) {
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
    }

    fun onFileSelectionCanceled() {
        if (_uiState.value is UploadUiState.SelectingFile) {
            _uiState.value = UploadUiState.Idle
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

    private fun userFacingErrorMessage(error: Throwable, fallback: String): String {
        val message = error.message ?: ""
        return when {
            error is GeminiApiClient.RateLimitException ||
                message.contains("quota", ignoreCase = true) ||
                message.contains("429", ignoreCase = true) -> {
                "⚠️ Cuota de Gemini API excedida\n\n" +
                "Tu cuenta ha alcanzado el límite diario. Por favor:\n" +
                "• Espera 24 horas para que se restablezca\n" +
                "• O verifica tu plan en Google Cloud Console"
            }
            message.contains("timeout", ignoreCase = true) ||
                message.contains("tardó demasiado", ignoreCase = true) -> {
                "⏱️ Timeout de conexión\n\n" +
                "La petición tardó demasiado tiempo (>60s).\n" +
                "Verifica tu conexión a internet e intenta nuevamente."
            }
            message.contains("connect", ignoreCase = true) ||
                message.contains("no se pudo conectar", ignoreCase = true) -> {
                "🌐 Error de conexión\n\n" +
                "No se pudo conectar a la API de Gemini.\n" +
                "Verifica tu conexión a internet."
            }
            else -> message.ifEmpty { fallback }
        }
    }
}
