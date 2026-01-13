package org.example.learnify

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
import org.example.learnify.presentation.strings.ProvideAppStrings
import org.example.learnify.ui.theme.LearnifyTheme
import org.koin.compose.koinInject

enum class Screen {
    Welcome,
    Dashboard,
    Upload,
    LearningPath,
    Quiz
}

@Composable
fun App(
    onFilePickerRequest: () -> Unit = {},
    savedLearningPaths: List<LearningPath> = emptyList(), // Inyectar learning paths guardados
    onSaveLearningPath: (LearningPath) -> Unit = {}, // Callback para guardar
    onDeleteLearningPath: (LearningPath) -> Unit = {}, // Callback para eliminar
    isFirstLaunch: Boolean = false, // Si es la primera vez que abre la app
    onWelcomeCompleted: () -> Unit = {} // Callback cuando termina el welcome
) {
    Napier.i("🔍 App.kt recibió isFirstLaunch = $isFirstLaunch")

    // FORZAR SIEMPRE WELCOME PARA DEBUGGING
    val initialScreen = Screen.Welcome  // Forzado temporalmente
    // val initialScreen = if (isFirstLaunch) Screen.Welcome else Screen.Dashboard  // Original
    var currentScreen by remember { mutableStateOf(initialScreen) }
    var currentLearningPath by remember { mutableStateOf<LearningPath?>(null) }
    var currentQuizTopic by remember { mutableStateOf<Topic?>(null) }
    // Guardar todas las rutas de aprendizaje - inicializar con las guardadas
    var learningPaths by remember { mutableStateOf(savedLearningPaths) }

    Napier.i("🚀 App iniciada con ${savedLearningPaths.size} Learning Paths guardados")
    Napier.i("📺 Pantalla inicial: $initialScreen")

    if (isFirstLaunch) {
        Napier.i("👋 Primera vez - mostrando pantalla de bienvenida")
    } else {
        Napier.i("🏠 NO es primera vez - yendo a Dashboard")
    }

    LearnifyTheme {
        ProvideAppStrings {
            when (currentScreen) {
                Screen.Welcome -> {
                    org.example.learnify.presentation.welcome.WelcomeScreen(
                        onGetStarted = {
                            Napier.i("✅ Welcome completado - navegando a Dashboard")
                            onWelcomeCompleted()
                            currentScreen = Screen.Dashboard
                        }
                    )
                }

                Screen.Dashboard -> {
                    DashboardScreen(
                        learningPaths = learningPaths,
                        onCreateNew = {
                            currentScreen = Screen.Upload
                        },
                        onSelectLearningPath = { learningPath ->
                            currentLearningPath = learningPath
                            currentScreen = Screen.LearningPath
                        },
                        onShowWelcome = {
                            Napier.i("ℹ️ Navegando a Welcome desde Dashboard")
                            currentScreen = Screen.Welcome
                        },
                        onDeleteLearningPath = { learningPath ->
                            Napier.i("🗑️ Eliminando Learning Path: ${learningPath.title}")
                            // Eliminar del estado local
                            learningPaths = learningPaths.filter { it.id != learningPath.id }
                            // Eliminar del almacenamiento persistente
                            onDeleteLearningPath(learningPath)
                        }
                    )
                }

                Screen.Upload -> {
                    val uploadViewModel = koinInject<UploadViewModel>()

                    UploadScreen(
                        viewModel = uploadViewModel,
                        onFilePickerRequest = onFilePickerRequest,
                        onNavigateBack = {
                            Napier.i("🏠 Navegando de vuelta al Dashboard")
                            uploadViewModel.resetState() // Limpiar estado del upload
                            currentScreen = Screen.Dashboard
                        },
                        onContinue = { learningPath ->
                            Napier.i("Navegando a ruta de aprendizaje: ${learningPath.title}")
                            currentLearningPath = learningPath
                            // Agregar a la lista de rutas de aprendizaje
                            learningPaths = learningPaths + learningPath
                            // Guardar en base de datos/almacenamiento persistente
                            onSaveLearningPath(learningPath)
                            Napier.i("💾 Learning Path guardado: ${learningPath.title}")
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
}
