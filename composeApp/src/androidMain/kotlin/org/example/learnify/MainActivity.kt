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
import org.example.learnify.util.LearningPathStorage
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {

    private var fileSelectedCallback: ((String) -> Unit)? = null
    private var fileSelectionCanceledCallback: (() -> Unit)? = null
    private lateinit var storage: LearningPathStorage

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
        } ?: run {
            Napier.d("File picker cancelado por el usuario")
            fileSelectionCanceledCallback?.invoke()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Configurar Napier para logging
        Napier.base(DebugAntilog())

        // Inicializar storage
        storage = LearningPathStorage(applicationContext)

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
                    fileSelectionCanceledCallback = {
                        uploadViewModel.onFileSelectionCanceled()
                    }
                    filePickerLauncher.launch(arrayOf("application/pdf"))
                }
            }

            // Verificar si es la primera vez que abre la app
            val isFirstLaunch = storage.isFirstLaunch()

            // Cargar Learning Paths guardados al iniciar
            val savedLearningPaths = storage.loadAllLearningPaths()

            Napier.i("🚀 MainActivity iniciada con ${savedLearningPaths.size} Learning Paths")
            Napier.i("🔍 isFirstLaunch = $isFirstLaunch")

            if (isFirstLaunch) {
                Napier.i("👋 Primera vez - mostrando Welcome Screen")
            } else {
                Napier.i("🏠 Ya vio el Welcome - yendo directo a Dashboard")
            }

            App(
                onFilePickerRequest = {
                    Napier.d("File picker request from App")
                },
                savedLearningPaths = savedLearningPaths,
                onSaveLearningPath = { learningPath ->
                    Napier.i("💾 Guardando Learning Path: ${learningPath.title}")
                    storage.saveLearningPath(learningPath)
                },
                onDeleteLearningPath = { learningPath ->
                    Napier.i("🗑️ Eliminando Learning Path: ${learningPath.title}")
                    storage.deleteLearningPath(learningPath.id)
                },
                isFirstLaunch = isFirstLaunch,
                onWelcomeCompleted = {
                    Napier.i("✅ Usuario completó el Welcome - guardando preferencia")
                    storage.markWelcomeAsSeen()
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
