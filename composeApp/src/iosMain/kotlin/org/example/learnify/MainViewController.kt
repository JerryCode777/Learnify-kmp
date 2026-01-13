package org.example.learnify

import androidx.compose.ui.window.ComposeUIViewController
import io.github.aakira.napier.Napier
import org.example.learnify.di.appModule
import org.example.learnify.di.platformModule
import org.example.learnify.util.LearningPathStorage
import org.koin.core.context.startKoin

private var koinInitialized = false

fun MainViewController() = ComposeUIViewController {
    if (!koinInitialized) {
        startKoin {
            modules(appModule, platformModule)
        }
        koinInitialized = true
    }

    // Verificar si es la primera vez que abre la app
    val isFirstLaunch = LearningPathStorage.isFirstLaunch()

    // Cargar Learning Paths guardados al iniciar
    val savedLearningPaths = LearningPathStorage.loadAllLearningPaths()
    Napier.i("🚀 MainViewController cargado con ${savedLearningPaths.size} Learning Paths")

    if (isFirstLaunch) {
        Napier.i("👋 Primera vez - mostrando Welcome Screen")
    }

    App(
        savedLearningPaths = savedLearningPaths,
        onSaveLearningPath = { learningPath ->
            Napier.i("💾 Guardando Learning Path: ${learningPath.title}")
            LearningPathStorage.saveLearningPath(learningPath)
        },
        onDeleteLearningPath = { learningPath ->
            Napier.i("🗑️ Eliminando Learning Path: ${learningPath.title}")
            LearningPathStorage.deleteLearningPath(learningPath.id)
        },
        isFirstLaunch = isFirstLaunch,
        onWelcomeCompleted = {
            Napier.i("✅ Usuario completó el Welcome - guardando preferencia")
            LearningPathStorage.markWelcomeAsSeen()
        }
    )
}