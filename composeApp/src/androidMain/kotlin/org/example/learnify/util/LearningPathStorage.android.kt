package org.example.learnify.util

import android.content.Context
import android.content.SharedPreferences
import io.github.aakira.napier.Napier
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.learnify.domain.model.LearningPath

/**
 * Almacenamiento persistente de Learning Paths usando SharedPreferences
 */
class LearningPathStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Verifica si es la primera vez que el usuario abre la app
     */
    fun isFirstLaunch(): Boolean {
        val hasSeenWelcome = prefs.getBoolean(HAS_SEEN_WELCOME_KEY, false)
        Napier.i("🔍 Storage: hasSeenWelcome = $hasSeenWelcome")
        // TEMPORAL: Forzar que siempre sea primera vez para testing
        return true  // Cambiado temporalmente para debugging
        // return !hasSeenWelcome  // Versión original
    }

    /**
     * Marca que el usuario ya vio la pantalla de bienvenida
     */
    fun markWelcomeAsSeen() {
        prefs.edit().putBoolean(HAS_SEEN_WELCOME_KEY, true).apply()
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

            // Guardar en SharedPreferences
            prefs.edit().putString(LEARNING_PATHS_KEY, jsonString).apply()

            Napier.i("💾 Learning Path guardado en SharedPreferences: ${learningPath.title}")
        } catch (e: Exception) {
            Napier.e("❌ Error guardando Learning Path", e)
        }
    }

    /**
     * Carga todos los Learning Paths guardados
     */
    fun loadAllLearningPaths(): List<LearningPath> {
        return try {
            val jsonString = prefs.getString(LEARNING_PATHS_KEY, null)

            if (jsonString != null) {
                val paths = json.decodeFromString<List<LearningPath>>(jsonString)
                Napier.i("📚 Cargados ${paths.size} Learning Paths desde SharedPreferences")
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
            prefs.edit().putString(LEARNING_PATHS_KEY, jsonString).apply()

            Napier.i("🗑️ Learning Path eliminado: $learningPathId")
        } catch (e: Exception) {
            Napier.e("❌ Error eliminando Learning Path", e)
        }
    }

    /**
     * Limpia todos los Learning Paths guardados
     */
    fun clearAll() {
        prefs.edit().remove(LEARNING_PATHS_KEY).apply()
        Napier.i("🧹 Todos los Learning Paths eliminados")
    }

    companion object {
        private const val PREFS_NAME = "learnify_prefs"
        private const val LEARNING_PATHS_KEY = "saved_learning_paths"
        private const val HAS_SEEN_WELCOME_KEY = "has_seen_welcome"
    }
}
