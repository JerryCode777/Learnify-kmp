package org.example.learnify.presentation.upload

import androidx.compose.runtime.*
import io.github.aakira.napier.Napier
import org.example.learnify.domain.model.LearningPath
import org.koin.compose.koinInject

/**
 * Versión integrada del UploadScreen que maneja automáticamente
 * la selección de archivos y la extracción
 */
@Composable
fun UploadScreenIntegrated(
    onFilePickerRequest: (onFileSelected: (String) -> Unit) -> Unit,
    onContinue: (LearningPath) -> Unit
) {
    val viewModel = koinInject<UploadViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    // Callback cuando se selecciona un archivo
    val fileSelectedCallback: (String) -> Unit = remember {
        { uri ->
            Napier.d("File selected in integrated screen: $uri")
            viewModel.onFileSelected(uri)
        }
    }

    UploadScreen(
        viewModel = viewModel,
        onFilePickerRequest = {
            Napier.d("Requesting file picker...")
            onFilePickerRequest(fileSelectedCallback)
        },
        onContinue = onContinue
    )
}
