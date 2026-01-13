package org.example.learnify.presentation.learning_path

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.example.learnify.domain.model.LearningPath
import org.example.learnify.domain.model.Topic

class LearningPathViewModelTest {

    @Test
    fun `loads learning path into success state`() {
        val viewModel = LearningPathViewModel()
        val learningPath = sampleLearningPath()

        viewModel.loadLearningPath(learningPath)

        val state = viewModel.uiState.value
        assertTrue(state is LearningPathUiState.Success)
        assertEquals(learningPath.title, state.learningPath.title)
        assertEquals(0, state.currentTopicIndex)
    }

    @Test
    fun `next topic advances and previous topic returns`() {
        val viewModel = LearningPathViewModel()
        viewModel.loadLearningPath(sampleLearningPath())

        viewModel.onNextTopic()
        val stateAfterNext = viewModel.uiState.value as LearningPathUiState.Success
        assertEquals(1, stateAfterNext.currentTopicIndex)

        viewModel.onPreviousTopic()
        val stateAfterPrevious = viewModel.uiState.value as LearningPathUiState.Success
        assertEquals(0, stateAfterPrevious.currentTopicIndex)
    }

    @Test
    fun `marking topic as completed updates completed set`() {
        val viewModel = LearningPathViewModel()
        viewModel.loadLearningPath(sampleLearningPath())

        viewModel.onTopicCompleted()

        val state = viewModel.uiState.value as LearningPathUiState.Success
        assertEquals(setOf("topic-1"), state.completedTopics)
    }

    private fun sampleLearningPath(): LearningPath {
        return LearningPath(
            id = "path-1",
            documentId = "doc-1",
            title = "Ruta de prueba",
            description = "Descripcion",
            topics = listOf(
                Topic(
                    id = "topic-1",
                    title = "Tema 1",
                    content = "Contenido 1",
                    pageNumbers = listOf(1),
                    order = 1
                ),
                Topic(
                    id = "topic-2",
                    title = "Tema 2",
                    content = "Contenido 2",
                    pageNumbers = listOf(2),
                    order = 2
                )
            ),
            createdAt = 0L
        )
    }
}
