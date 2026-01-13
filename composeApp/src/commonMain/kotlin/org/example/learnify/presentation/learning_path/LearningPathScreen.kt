package org.example.learnify.presentation.learning_path

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.example.learnify.domain.model.LearningPath
import org.example.learnify.presentation.strings.LocalAppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningPathScreen(
    learningPath: LearningPath,
    viewModel: LearningPathViewModel,
    onNavigateBack: () -> Unit,
    onStartQuiz: (topicId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val strings = LocalAppStrings.current

    LaunchedEffect(learningPath) {
        viewModel.loadLearningPath(learningPath)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = learningPath.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        when (val state = uiState) {
                            is LearningPathUiState.Success -> {
                                Text(
                                    text = strings.learningPathTopicIndicator(state.currentTopicIndex, state.totalCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            else -> {}
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, strings.navigationBack)
                    }
                },
                actions = {
                    when (val state = uiState) {
                        is LearningPathUiState.Success -> {
                            IconButton(
                                onClick = { /* TODO: Mostrar lista de temas */ }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.List, strings.learningPathViewAllTopics)
                            }
                        }
                        else -> {}
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            when (val state = uiState) {
                is LearningPathUiState.Success -> {
                    NavigationBottomBar(
                        state = state,
                        onPrevious = { viewModel.onPreviousTopic() },
                        onNext = { viewModel.onNextTopic() },
                        onMarkComplete = { viewModel.onTopicCompleted() }
                    )
                }
                else -> {}
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is LearningPathUiState.Loading -> {
                    LoadingContent()
                }

                is LearningPathUiState.Success -> {
                    LearningContent(
                        state = state,
                        onStartQuiz = onStartQuiz
                    )
                }

                is LearningPathUiState.Error -> {
                    ErrorContent(message = state.message)
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LearningContent(
    state: LearningPathUiState.Success,
    onStartQuiz: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current

    val topic = state.currentTopic
    val isCompleted = topic.id in state.completedTopics

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Progress indicator
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = strings.learningPathCompletedCount(state.completedCount, state.totalCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Topic title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = topic.title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )

            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = strings.accessibilityCompleted,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Topic content card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = topic.content,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight.times(1.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quiz button
        ElevatedButton(
            onClick = { onStartQuiz(topic.id) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Quiz, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.learningPathQuizButton)
        }

        Spacer(modifier = Modifier.height(100.dp)) // Espacio para el bottom bar
    }
}

@Composable
private fun NavigationBottomBar(
    state: LearningPathUiState.Success,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onMarkComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current

    val isCompleted = state.currentTopic.id in state.completedTopics

    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous button
            OutlinedButton(
                onClick = onPrevious,
                enabled = !state.isFirstTopic
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings.learningPathPrevious
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.learningPathPrevious)
            }

            // Mark as complete button
            if (!isCompleted) {
                FilledTonalButton(
                    onClick = onMarkComplete
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.learningPathComplete)
                }
            } else {
                Text(
                    text = strings.learningPathCompletedCheck,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Next button
            Button(
                onClick = onNext,
                enabled = !state.isLastTopic
            ) {
                Text(strings.learningPathNext)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = strings.learningPathNext
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
