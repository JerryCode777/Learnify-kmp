package org.example.learnify

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import io.github.aakira.napier.Napier
import org.example.learnify.domain.model.LearningPath
import org.example.learnify.presentation.learning_path.LearningPathScreen
import org.example.learnify.presentation.learning_path.LearningPathViewModel
import org.example.learnify.presentation.upload.UploadScreen
import org.example.learnify.presentation.upload.UploadViewModel
import org.koin.compose.viewmodel.koinViewModel

enum class Screen {
    Upload,
    LearningPath
}

@Composable
fun App(
    onFilePickerRequest: () -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf(Screen.Upload) }
    var currentLearningPath by remember { mutableStateOf<LearningPath?>(null) }

    MaterialTheme {
        when (currentScreen) {
            Screen.Upload -> {
                val uploadViewModel = koinViewModel<UploadViewModel>()

                UploadScreen(
                    viewModel = uploadViewModel,
                    onFilePickerRequest = onFilePickerRequest,
                    onContinue = { learningPath ->
                        Napier.i("Navegando a ruta de aprendizaje: ${learningPath.title}")
                        currentLearningPath = learningPath
                        currentScreen = Screen.LearningPath
                    }
                )
            }

            Screen.LearningPath -> {
                val learningPathViewModel = koinViewModel<LearningPathViewModel>()

                currentLearningPath?.let { learningPath ->
                    LearningPathScreen(
                        learningPath = learningPath,
                        viewModel = learningPathViewModel,
                        onNavigateBack = {
                            currentScreen = Screen.Upload
                        },
                        onStartQuiz = { topicId ->
                            // TODO: Navegar a pantalla de quiz
                            Napier.i("Iniciar quiz para tema: $topicId")
                        }
                    )
                }
            }
        }
    }
}