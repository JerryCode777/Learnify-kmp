package org.example.learnify.presentation.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.example.learnify.presentation.strings.LocalAppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    topicId: String,
    topicTitle: String,
    topicContent: String,
    viewModel: QuizViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val strings = LocalAppStrings.current

    LaunchedEffect(topicId) {
        viewModel.generateQuiz(topicId, topicContent, questionCount = 8)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.quizTitle(topicTitle)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, strings.navigationBack)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is QuizUiState.Loading -> LoadingContent()
                is QuizUiState.Active -> ActiveQuizContent(
                    state = state,
                    onAnswerSelected = viewModel::onAnswerSelected,
                    onSubmitAnswer = viewModel::onSubmitAnswer,
                    onNextQuestion = viewModel::onNextQuestion
                )
                is QuizUiState.Completed -> CompletedQuizContent(
                    state = state,
                    onRetry = viewModel::resetQuiz,
                    onExit = onNavigateBack
                )
                is QuizUiState.Error -> ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.generateQuiz(topicId, topicContent, 8) }
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = strings.quizGenerating,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun ActiveQuizContent(
    state: QuizUiState.Active,
    onAnswerSelected: (Int) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = strings.quizQuestionCount(state.currentQuestionIndex, state.totalQuestions),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = strings.quizScore(state.score, state.totalQuestions),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Question card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QuestionMark,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = state.currentQuestion.text,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Options
        state.currentQuestion.options.forEachIndexed { index, option ->
            AnswerOptionCard(
                option = option,
                index = index,
                isSelected = state.selectedAnswer == index,
                isCorrect = state.isAnswerSubmitted && index == state.currentQuestion.correctAnswer,
                isIncorrect = state.isAnswerSubmitted && state.selectedAnswer == index && index != state.currentQuestion.correctAnswer,
                isAnswerSubmitted = state.isAnswerSubmitted,
                onClick = { if (!state.isAnswerSubmitted) onAnswerSelected(index) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Explanation (shown after submission)
        if (state.isAnswerSubmitted) {
            Spacer(modifier = Modifier.height(24.dp))

            val isCorrect = state.selectedAnswer == state.currentQuestion.correctAnswer

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCorrect)
                        MaterialTheme.colorScheme.tertiaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (isCorrect)
                            MaterialTheme.colorScheme.onTertiaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isCorrect) strings.quizCorrect else strings.quizIncorrect,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isCorrect)
                                MaterialTheme.colorScheme.onTertiaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.currentQuestion.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isCorrect)
                                MaterialTheme.colorScheme.onTertiaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action button
        if (!state.isAnswerSubmitted) {
            Button(
                onClick = onSubmitAnswer,
                enabled = state.selectedAnswer != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.quizSubmitAnswer)
            }
        } else {
            Button(
                onClick = onNextQuestion,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (state.currentQuestionIndex < state.totalQuestions - 1)
                        strings.quizNextQuestion
                    else
                        strings.quizViewResults
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    if (state.currentQuestionIndex < state.totalQuestions - 1)
                        Icons.AutoMirrored.Filled.ArrowForward
                    else
                        Icons.Default.EmojiEvents,
                    contentDescription = null
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AnswerOptionCard(
    option: String,
    index: Int,
    isSelected: Boolean,
    isCorrect: Boolean,
    isIncorrect: Boolean,
    isAnswerSubmitted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val optionLetter = ('A' + index).toString()

    val borderColor = when {
        isCorrect -> MaterialTheme.colorScheme.tertiary
        isIncorrect -> MaterialTheme.colorScheme.error
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    val backgroundColor = when {
        isCorrect -> MaterialTheme.colorScheme.tertiaryContainer
        isIncorrect -> MaterialTheme.colorScheme.errorContainer
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        border = BorderStroke(
            width = if (isSelected || isCorrect || isIncorrect) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Option letter badge
            Surface(
                shape = MaterialTheme.shapes.small,
                color = borderColor,
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = optionLetter,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = option,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            if (isAnswerSubmitted && (isCorrect || isIncorrect)) {
                Icon(
                    imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = borderColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun CompletedQuizContent(
    state: QuizUiState.Completed,
    onRetry: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Result icon
        Icon(
            imageVector = if (state.isPassed) Icons.Default.EmojiEvents else Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = if (state.isPassed)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = if (state.isPassed) strings.quizCongrats else strings.quizKeepPracticing,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (state.isPassed)
                strings.quizCompletedMessage
            else
                strings.quizFailedMessage,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Score card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = strings.quizFinalScore,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "${state.score}/${state.totalQuestions}",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                val percentageInt = state.percentage.toInt()
                Text(
                    text = "$percentageInt%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { state.percentage / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Pass/Fail info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (state.isPassed)
                    MaterialTheme.colorScheme.tertiaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (state.isPassed) Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (state.isPassed)
                        MaterialTheme.colorScheme.onTertiaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (state.isPassed)
                        strings.quizPassedMessage
                    else
                        strings.quizFailedMessageDetailed,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.isPassed)
                        MaterialTheme.colorScheme.onTertiaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Action buttons
        Button(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.quizContinueLearning)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.quizRetry)
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current

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
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.quizRetryShort)
            }
        }
    }
}
