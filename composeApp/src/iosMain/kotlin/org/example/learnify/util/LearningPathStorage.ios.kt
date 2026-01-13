package org.example.learnify.util

import io.github.aakira.napier.Napier
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.learnify.domain.model.LearningPath
import platform.Foundation.NSUserDefaults

/**
 * Almacenamiento persistente de Learning Paths usando NSUserDefaults
 */
object LearningPathStorage {
    private const val LEARNING_PATHS_KEY = "saved_learning_paths"
    private const val HAS_SEEN_WELCOME_KEY = "has_seen_welcome"

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Verifica si es la primera vez que el usuario abre la app
     */
    fun isFirstLaunch(): Boolean {
        val hasSeenWelcome = NSUserDefaults.standardUserDefaults.boolForKey(HAS_SEEN_WELCOME_KEY)
        return !hasSeenWelcome
    }

    /**
     * Marca que el usuario ya vio la pantalla de bienvenida
     */
    fun markWelcomeAsSeen() {
        NSUserDefaults.standardUserDefaults.setBool(true, HAS_SEEN_WELCOME_KEY)
        NSUserDefaults.standardUserDefaults.synchronize()
        Napier.i("✅ Welcome marcado como visto")
    }

    /**
     * Guarda un Learning Path
     */
    fun saveLearningPath(learningPath: LearningPath) {
        try {
            val currentPaths = loadAllLearningPaths().toMutableList()

            // Evitar duplicados - reemplazar si ya existe
            currentPaths.removeAll { it.id == learningPath.id }
            currentPaths.add(learningPath)

            // Serializar a JSON
            val jsonString = json.encodeToString(currentPaths)

            // Guardar en NSUserDefaults
            NSUserDefaults.standardUserDefaults.setObject(jsonString, LEARNING_PATHS_KEY)
            NSUserDefaults.standardUserDefaults.synchronize()

            Napier.i("💾 Learning Path guardado en NSUserDefaults: ${learningPath.title}")
        } catch (e: Exception) {
            Napier.e("❌ Error guardando Learning Path", e)
        }
    }

    /**
     * Carga todos los Learning Paths guardados
     */
    fun loadAllLearningPaths(): List<LearningPath> {
        return try {
            val jsonString = NSUserDefaults.standardUserDefaults.stringForKey(LEARNING_PATHS_KEY)

            if (jsonString != null) {
                val paths = json.decodeFromString<List<LearningPath>>(jsonString)
                Napier.i("📚 Cargados ${paths.size} Learning Paths desde NSUserDefaults")
                paths
            } else {
                Napier.i("📚 No hay Learning Paths guardados")
                emptyList()
            }
        } catch (e: Exception) {
            Napier.e("❌ Error cargando Learning Paths", e)
            emptyList()
        }
    }

    /**
     * Elimina un Learning Path
     */
    fun deleteLearningPath(learningPathId: String) {
        try {
            val currentPaths = loadAllLearningPaths().toMutableList()
            currentPaths.removeAll { it.id == learningPathId }

            val jsonString = json.encodeToString(currentPaths)
            NSUserDefaults.standardUserDefaults.setObject(jsonString, LEARNING_PATHS_KEY)
            NSUserDefaults.standardUserDefaults.synchronize()

            Napier.i("🗑️ Learning Path eliminado: $learningPathId")
        } catch (e: Exception) {
            Napier.e("❌ Error eliminando Learning Path", e)
        }
    }

    /**
     * Limpia todos los Learning Paths guardados
     */
    fun clearAll() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(LEARNING_PATHS_KEY)
        NSUserDefaults.standardUserDefaults.synchronize()
        Napier.i("🧹 Todos los Learning Paths eliminados")
    }
}
