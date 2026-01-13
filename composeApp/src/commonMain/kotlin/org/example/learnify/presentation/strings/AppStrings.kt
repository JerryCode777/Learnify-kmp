package org.example.learnify.presentation.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

private val SpanishStrings = AppStrings(
    appTitle = "Learnify",
    dashboardTitle = "Learnify - Mis Rutas de Aprendizaje",
    dashboardNewDocument = "Nuevo Documento",
    dashboardWelcomeTitle = "¡Bienvenido a Learnify!",
    dashboardWelcomeSubtitle = "Sube un documento PDF para generar automáticamente una ruta de aprendizaje personalizada con IA.",
    dashboardUploadFirstDocument = "Subir Primer Documento",
    dashboardLearningPathsCount = { count ->
        "$count ruta${if (count != 1) "s" else ""} de aprendizaje"
    },
    dashboardTopicsCount = { count ->
        "$count temas"
    },
    dashboardCompletionPlaceholder = "0% completado",
    uploadTitle = "Subir Documento",
    uploadHeadline = "Sube tu documento PDF",
    uploadDescription = "Selecciona un documento PDF para crear tu ruta de aprendizaje personalizada",
    uploadSelectPdf = "Seleccionar PDF",
    uploadProcessingMessage = "Procesando documento...",
    uploadPleaseWait = "Por favor espera...",
    uploadLearningPathReadyTitle = "¡Ruta de Aprendizaje Lista!",
    uploadLearningPathCardTitle = "Tu ruta de aprendizaje",
    uploadTotalTopicsLabel = "Total de temas",
    uploadEstimatedTimeLabel = "Tiempo estimado",
    uploadTopicsToLearnTitle = "Temas a aprender",
    uploadMoreTopicsSuffix = { remaining ->
        "...y $remaining temas más"
    },
    uploadStartLearning = "Comenzar a Aprender",
    uploadUploadAnother = "Subir Otro Documento",
    uploadSuccessTitle = "¡Documento procesado!",
    uploadTotalPagesLabel = "Total de páginas",
    uploadCharactersLabel = "Caracteres extraídos",
    uploadPreviewTitle = "Vista previa del contenido",
    uploadCreateLearningPath = "Crear Ruta de Aprendizaje",
    uploadErrorTitle = "Error al procesar documento",
    uploadRetry = "Intentar de Nuevo",
    uploadExtractingTitle = "Extrayendo Contenido",
    uploadExtractingPage = { current, total ->
        "Página $current de $total"
    },
    uploadExtractingInfo = "Procesando en lotes para optimizar memoria",
    uploadExtractingFootnote = "Estamos extrayendo el texto de tu PDF página por página. Esto puede tomar unos minutos para documentos grandes.",
    uploadProcessingTitle = "Procesando Documento",
    uploadProcessingChunk = { current, total ->
        "Chunk $current de $total"
    },
    uploadProcessingFootnote = "Estamos analizando cada sección de tu documento con IA para crear una ruta de aprendizaje exhaustiva. Este proceso puede tomar varios minutos.",
    uploadPartialTopicsTitle = "Temas listos",
    uploadPartialTopicsCount = { count ->
        "$count tema${if (count != 1) "s" else ""} generado${if (count != 1) "s" else ""}"
    },
    uploadPartialTopicsMore = { remaining ->
        "...y $remaining tema${if (remaining != 1) "s" else ""} más"
    },
    uploadCancelProcessing = "Cancelar procesamiento",
    uploadCanceledTitle = "Procesamiento cancelado",
    uploadCanceledMessage = "Puedes reintentar el procesamiento cuando quieras.",
    uploadEstimatedTimeRemaining = { minutes ->
        "Tiempo estimado restante: ~$minutes min"
    },
    learningPathTopicIndicator = { current, total ->
        "Tema ${current + 1} de $total"
    },
    learningPathViewAllTopics = "Ver todos los temas",
    learningPathCompletedCount = { completed, total ->
        "$completed de $total temas completados"
    },
    learningPathQuizButton = "Hacer Quiz de este Tema",
    learningPathPrevious = "Anterior",
    learningPathNext = "Siguiente",
    learningPathComplete = "Completar",
    learningPathCompletedCheck = "✓ Completado",
    quizTitle = { topicTitle ->
        "Quiz: $topicTitle"
    },
    quizGenerating = "Generando quiz...",
    quizQuestionCount = { current, total ->
        "Pregunta ${current + 1} de $total"
    },
    quizScore = { score, total ->
        "Puntuación: $score/$total"
    },
    quizCorrect = "¡Correcto!",
    quizIncorrect = "Incorrecto",
    quizSubmitAnswer = "Enviar Respuesta",
    quizNextQuestion = "Siguiente Pregunta",
    quizViewResults = "Ver Resultados",
    quizCongrats = "¡Felicitaciones!",
    quizKeepPracticing = "Sigue practicando",
    quizCompletedMessage = "Has completado el quiz exitosamente",
    quizFailedMessage = "No alcanzaste el puntaje mínimo",
    quizFinalScore = "Puntuación Final",
    quizPassedMessage = "Aprobado - Necesitas 70% para aprobar",
    quizFailedMessageDetailed = "No aprobado - Necesitas al menos 70% para aprobar",
    quizContinueLearning = "Continuar Aprendiendo",
    quizRetry = "Reintentar Quiz",
    quizRetryShort = "Reintentar",
    navigationBack = "Volver",
    navigationNext = "Siguiente",
    accessibilityCompleted = "Completado",
    accessibilityViewTopics = "Ver todos los temas"
)

private val EnglishStrings = AppStrings(
    appTitle = "Learnify",
    dashboardTitle = "Learnify - My Learning Paths",
    dashboardNewDocument = "New Document",
    dashboardWelcomeTitle = "Welcome to Learnify!",
    dashboardWelcomeSubtitle = "Upload a PDF to generate a personalized learning path with AI.",
    dashboardUploadFirstDocument = "Upload First Document",
    dashboardLearningPathsCount = { count ->
        "$count learning path${if (count != 1) "s" else ""}"
    },
    dashboardTopicsCount = { count ->
        "$count topics"
    },
    dashboardCompletionPlaceholder = "0% completed",
    uploadTitle = "Upload Document",
    uploadHeadline = "Upload your PDF",
    uploadDescription = "Select a PDF document to build your personalized learning path",
    uploadSelectPdf = "Select PDF",
    uploadProcessingMessage = "Processing document...",
    uploadPleaseWait = "Please wait...",
    uploadLearningPathReadyTitle = "Learning Path Ready!",
    uploadLearningPathCardTitle = "Your learning path",
    uploadTotalTopicsLabel = "Total topics",
    uploadEstimatedTimeLabel = "Estimated time",
    uploadTopicsToLearnTitle = "Topics to learn",
    uploadMoreTopicsSuffix = { remaining ->
        "...and $remaining more topics"
    },
    uploadStartLearning = "Start Learning",
    uploadUploadAnother = "Upload Another Document",
    uploadSuccessTitle = "Document processed!",
    uploadTotalPagesLabel = "Total pages",
    uploadCharactersLabel = "Characters extracted",
    uploadPreviewTitle = "Content preview",
    uploadCreateLearningPath = "Create Learning Path",
    uploadErrorTitle = "Error processing document",
    uploadRetry = "Try Again",
    uploadExtractingTitle = "Extracting Content",
    uploadExtractingPage = { current, total ->
        "Page $current of $total"
    },
    uploadExtractingInfo = "Processing in batches to optimize memory",
    uploadExtractingFootnote = "We are extracting text page by page. This can take a few minutes for large documents.",
    uploadProcessingTitle = "Processing Document",
    uploadProcessingChunk = { current, total ->
        "Chunk $current of $total"
    },
    uploadProcessingFootnote = "We are analyzing each section with AI to build a complete learning path. This can take several minutes.",
    uploadPartialTopicsTitle = "Topics ready",
    uploadPartialTopicsCount = { count ->
        "$count topic${if (count != 1) "s" else ""} generated"
    },
    uploadPartialTopicsMore = { remaining ->
        "...and $remaining more topic${if (remaining != 1) "s" else ""}"
    },
    uploadCancelProcessing = "Cancel processing",
    uploadCanceledTitle = "Processing canceled",
    uploadCanceledMessage = "You can retry the processing when ready.",
    uploadEstimatedTimeRemaining = { minutes ->
        "Estimated time remaining: ~$minutes min"
    },
    learningPathTopicIndicator = { current, total ->
        "Topic ${current + 1} of $total"
    },
    learningPathViewAllTopics = "View all topics",
    learningPathCompletedCount = { completed, total ->
        "$completed of $total topics completed"
    },
    learningPathQuizButton = "Take a Quiz on this Topic",
    learningPathPrevious = "Previous",
    learningPathNext = "Next",
    learningPathComplete = "Complete",
    learningPathCompletedCheck = "✓ Completed",
    quizTitle = { topicTitle ->
        "Quiz: $topicTitle"
    },
    quizGenerating = "Generating quiz...",
    quizQuestionCount = { current, total ->
        "Question ${current + 1} of $total"
    },
    quizScore = { score, total ->
        "Score: $score/$total"
    },
    quizCorrect = "Correct!",
    quizIncorrect = "Incorrect",
    quizSubmitAnswer = "Submit Answer",
    quizNextQuestion = "Next Question",
    quizViewResults = "View Results",
    quizCongrats = "Congratulations!",
    quizKeepPracticing = "Keep practicing",
    quizCompletedMessage = "You completed the quiz successfully",
    quizFailedMessage = "You did not reach the minimum score",
    quizFinalScore = "Final Score",
    quizPassedMessage = "Passed - You need 70% to pass",
    quizFailedMessageDetailed = "Not passed - You need at least 70% to pass",
    quizContinueLearning = "Continue Learning",
    quizRetry = "Retry Quiz",
    quizRetryShort = "Retry",
    navigationBack = "Back",
    navigationNext = "Next",
    accessibilityCompleted = "Completed",
    accessibilityViewTopics = "View all topics"
)

val LocalAppStrings = staticCompositionLocalOf { SpanishStrings }

@Composable
fun ProvideAppStrings(content: @Composable () -> Unit) {
    val languageTag = remember { currentLanguageTag() }
    val strings = remember(languageTag) {
        if (languageTag.lowercase().startsWith("es")) {
            SpanishStrings
        } else {
            EnglishStrings
        }
    }
    CompositionLocalProvider(LocalAppStrings provides strings, content = content)
}

data class AppStrings(
    val appTitle: String,
    val dashboardTitle: String,
    val dashboardNewDocument: String,
    val dashboardWelcomeTitle: String,
    val dashboardWelcomeSubtitle: String,
    val dashboardUploadFirstDocument: String,
    val dashboardLearningPathsCount: (Int) -> String,
    val dashboardTopicsCount: (Int) -> String,
    val dashboardCompletionPlaceholder: String,
    val uploadTitle: String,
    val uploadHeadline: String,
    val uploadDescription: String,
    val uploadSelectPdf: String,
    val uploadProcessingMessage: String,
    val uploadPleaseWait: String,
    val uploadLearningPathReadyTitle: String,
    val uploadLearningPathCardTitle: String,
    val uploadTotalTopicsLabel: String,
    val uploadEstimatedTimeLabel: String,
    val uploadTopicsToLearnTitle: String,
    val uploadMoreTopicsSuffix: (Int) -> String,
    val uploadStartLearning: String,
    val uploadUploadAnother: String,
    val uploadSuccessTitle: String,
    val uploadTotalPagesLabel: String,
    val uploadCharactersLabel: String,
    val uploadPreviewTitle: String,
    val uploadCreateLearningPath: String,
    val uploadErrorTitle: String,
    val uploadRetry: String,
    val uploadExtractingTitle: String,
    val uploadExtractingPage: (Int, Int) -> String,
    val uploadExtractingInfo: String,
    val uploadExtractingFootnote: String,
    val uploadProcessingTitle: String,
    val uploadProcessingChunk: (Int, Int) -> String,
    val uploadProcessingFootnote: String,
    val uploadPartialTopicsTitle: String,
    val uploadPartialTopicsCount: (Int) -> String,
    val uploadPartialTopicsMore: (Int) -> String,
    val uploadCancelProcessing: String,
    val uploadCanceledTitle: String,
    val uploadCanceledMessage: String,
    val uploadEstimatedTimeRemaining: (Int) -> String,
    val learningPathTopicIndicator: (Int, Int) -> String,
    val learningPathViewAllTopics: String,
    val learningPathCompletedCount: (Int, Int) -> String,
    val learningPathQuizButton: String,
    val learningPathPrevious: String,
    val learningPathNext: String,
    val learningPathComplete: String,
    val learningPathCompletedCheck: String,
    val quizTitle: (String) -> String,
    val quizGenerating: String,
    val quizQuestionCount: (Int, Int) -> String,
    val quizScore: (Int, Int) -> String,
    val quizCorrect: String,
    val quizIncorrect: String,
    val quizSubmitAnswer: String,
    val quizNextQuestion: String,
    val quizViewResults: String,
    val quizCongrats: String,
    val quizKeepPracticing: String,
    val quizCompletedMessage: String,
    val quizFailedMessage: String,
    val quizFinalScore: String,
    val quizPassedMessage: String,
    val quizFailedMessageDetailed: String,
    val quizContinueLearning: String,
    val quizRetry: String,
    val quizRetryShort: String,
    val navigationBack: String,
    val navigationNext: String,
    val accessibilityCompleted: String,
    val accessibilityViewTopics: String
)
