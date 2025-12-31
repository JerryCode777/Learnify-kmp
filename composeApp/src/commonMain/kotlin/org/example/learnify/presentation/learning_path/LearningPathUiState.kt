package org.example.learnify.presentation.learning_path

import org.example.learnify.domain.model.LearningPath
import org.example.learnify.domain.model.Topic

sealed interface LearningPathUiState {
    data object Loading : LearningPathUiState
    data class Success(
        val learningPath: LearningPath,
        val currentTopicIndex: Int = 0,
        val completedTopics: Set<String> = emptySet()
    ) : LearningPathUiState {
        val currentTopic: Topic
            get() = learningPath.topics[currentTopicIndex]

        val progress: Float
            get() = completedTopics.size.toFloat() / learningPath.topics.size.toFloat()

        val isFirstTopic: Boolean
            get() = currentTopicIndex == 0

        val isLastTopic: Boolean
            get() = currentTopicIndex == learningPath.topics.size - 1

        val completedCount: Int
            get() = completedTopics.size

        val totalCount: Int
            get() = learningPath.topics.size
    }
    data class Error(val message: String) : LearningPathUiState
}
