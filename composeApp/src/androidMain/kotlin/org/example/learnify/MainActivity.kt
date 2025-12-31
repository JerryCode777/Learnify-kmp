package org.example.learnify

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.example.learnify.presentation.upload.UploadViewModel
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {

    private var fileSelectedCallback: ((String) -> Unit)? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            // Dar permisos persistentes al URI
            try {
                contentResolver.takePersistableUriPermission(
                    selectedUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                Napier.e("Error taking persistent URI permission", e)
            }

            Napier.d("File selected: $selectedUri")
            fileSelectedCallback?.invoke(selectedUri.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Configurar Napier para logging
        Napier.base(DebugAntilog())

        setContent {
            val uploadViewModel = koinViewModel<UploadViewModel>()

            // Observar el estado para detectar cuando se solicita el file picker
            val uiState by uploadViewModel.uiState.collectAsState()

            LaunchedEffect(uiState) {
                if (uiState is org.example.learnify.presentation.upload.UploadUiState.SelectingFile) {
                    // Guardar callback y abrir picker
                    fileSelectedCallback = { uri ->
                        uploadViewModel.onFileSelected(uri)
                    }
                    filePickerLauncher.launch(arrayOf("application/pdf"))
                }
            }

            App(
                onFilePickerRequest = {
                    Napier.d("File picker request from App")
                }
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}