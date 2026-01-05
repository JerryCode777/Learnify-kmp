package org.example.learnify

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import io.github.aakira.napier.Napier
import org.example.learnify.domain.model.LearningPath
import org.example.learnify.domain.model.Topic
import org.example.learnify.presentation.dashboard.DashboardScreen
import org.example.learnify.presentation.learning_path.LearningPathScreen
import org.example.learnify.presentation.learning_path.LearningPathViewModel
import org.example.learnify.presentation.quiz.QuizScreen
import org.example.learnify.presentation.quiz.QuizViewModel
import org.example.learnify.presentation.upload.UploadScreen
import org.example.learnify.presentation.upload.UploadViewModel
import org.koin.compose.koinInject

enum class Screen {
    Dashboard,
    Upload,
    LearningPath,
    Quiz
}

@Composable
fun App(
    onFilePickerRequest: () -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf(Screen.Dashboard) }
    var currentLearningPath by remember { mutableStateOf<LearningPath?>(null) }
    var currentQuizTopic by remember { mutableStateOf<Topic?>(null) }
    // Guardar todas las rutas de aprendizaje generadas en esta sesión
    var learningPaths by remember { mutableStateOf<List<LearningPath>>(emptyList()) }

    MaterialTheme {
        when (currentScreen) {
            Screen.Dashboard -> {
                DashboardScreen(
                    learningPaths = learningPaths,
                    onCreateNew = {
                        currentScreen = Screen.Upload
                    },
                    onSelectLearningPath = { learningPath ->
                        currentLearningPath = learningPath
                        currentScreen = Screen.LearningPath
                    }
                )
            }

            Screen.Upload -> {
                val uploadViewModel = koinInject<UploadViewModel>()

                UploadScreen(
                    viewModel = uploadViewModel,
                    onFilePickerRequest = onFilePickerRequest,
                    onContinue = { learningPath ->
                        Napier.i("Navegando a ruta de aprendizaje: ${learningPath.title}")
                        currentLearningPath = learningPath
                        // Agregar a la lista de rutas de aprendizaje
                        learningPaths = learningPaths + learningPath
                        currentScreen = Screen.LearningPath
                    }
                )
            }

            Screen.LearningPath -> {
                val learningPathViewModel = koinInject<LearningPathViewModel>()

                currentLearningPath?.let { learningPath ->
                    LearningPathScreen(
                        learningPath = learningPath,
                        viewModel = learningPathViewModel,
                        onNavigateBack = {
                            // Volver al dashboard en lugar de Upload
                            currentScreen = Screen.Dashboard
                        },
                        onStartQuiz = { topicId ->
                            // Buscar el tema correspondiente
                            val topic = learningPath.topics.find { it.id == topicId }
                            if (topic != null) {
                                currentQuizTopic = topic
                                currentScreen = Screen.Quiz
                                Napier.i("Navegando a quiz para tema: ${topic.title}")
                            }
                        }
                    )
                }
            }

            Screen.Quiz -> {
                val quizViewModel = koinInject<QuizViewModel>()

                currentQuizTopic?.let { topic ->
                    QuizScreen(
                        topicId = topic.id,
                        topicTitle = topic.title,
                        topicContent = topic.content,
                        viewModel = quizViewModel,
                        onNavigateBack = {
                            currentScreen = Screen.LearningPath
                        }
                    )
                }
            }
        }
    }
}