package org.example.learnify.presentation.learning_path

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.learnify.domain.model.LearningPath

class LearningPathViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<LearningPathUiState>(LearningPathUiState.Loading)
    val uiState: StateFlow<LearningPathUiState> = _uiState.asStateFlow()

    fun loadLearningPath(learningPath: LearningPath) {
        Napier.d("Cargando ruta de aprendizaje: ${learningPath.title}")
        _uiState.value = LearningPathUiState.Success(
            learningPath = learningPath,
            currentTopicIndex = 0,
            completedTopics = emptySet()
        )
    }

    fun onNextTopic() {
        val currentState = _uiState.value as? LearningPathUiState.Success ?: return

        if (!currentState.isLastTopic) {
            Napier.d("Navegando al siguiente tema")
            _uiState.update {
                currentState.copy(
                    currentTopicIndex = currentState.currentTopicIndex + 1
                )
            }
        }
    }

    fun onPreviousTopic() {
        val currentState = _uiState.value as? LearningPathUiState.Success ?: return

        if (!currentState.isFirstTopic) {
            Napier.d("Navegando al tema anterior")
            _uiState.update {
                currentState.copy(
                    currentTopicIndex = currentState.currentTopicIndex - 1
                )
            }
        }
    }

    fun onTopicCompleted() {
        val currentState = _uiState.value as? LearningPathUiState.Success ?: return

        val topicId = currentState.currentTopic.id
        Napier.i("Tema completado: $topicId")

        _uiState.update {
            currentState.copy(
                completedTopics = currentState.completedTopics + topicId
            )
        }
    }

    fun onNavigateToTopic(index: Int) {
        val currentState = _uiState.value as? LearningPathUiState.Success ?: return

        if (index in currentState.learningPath.topics.indices) {
            Napier.d("Navegando al tema $index")
            _uiState.update {
                currentState.copy(currentTopicIndex = index)
            }
        }
    }

    fun getProgress(): Float {
        return when (val state = _uiState.value) {
            is LearningPathUiState.Success -> state.progress
            else -> 0f
        }
    }
}
