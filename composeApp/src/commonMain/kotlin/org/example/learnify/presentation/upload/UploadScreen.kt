package org.example.learnify.presentation.upload

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.example.learnify.domain.model.PdfExtractionResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    viewModel: UploadViewModel,
    onFilePickerRequest: () -> Unit,
    onContinue: (org.example.learnify.domain.model.LearningPath) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subir Documento") },
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
                is UploadUiState.Idle -> {
                    IdleContent(
                        onSelectFileClick = {
                            viewModel.onSelectFileClick()
                            onFilePickerRequest()
                        }
                    )
                }

                is UploadUiState.SelectingFile -> {
                    IdleContent(
                        onSelectFileClick = {
                            onFilePickerRequest()
                        }
                    )
                }

                is UploadUiState.ExtractingContent -> {
                    LoadingContent(message = "Procesando documento...")
                }

                is UploadUiState.Success -> {
                    SuccessContent(
                        result = state.result,
                        onContinue = { viewModel.onGenerateLearningPath() },
                        onUploadAnother = { viewModel.resetState() }
                    )
                }

                is UploadUiState.GeneratingLearningPath -> {
                    LoadingContent(message = "Preparando procesamiento por chunks...")
                }

                is UploadUiState.ProcessingChunks -> {
                    ProcessingChunksContent(
                        currentChunk = state.currentChunk,
                        totalChunks = state.totalChunks,
                        message = state.message,
                        percentage = state.percentage
                    )
                }

                is UploadUiState.LearningPathGenerated -> {
                    LearningPathGeneratedContent(
                        learningPath = state.learningPath,
                        onContinue = { onContinue(state.learningPath) },
                        onUploadAnother = { viewModel.resetState() }
                    )
                }

                is UploadUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.resetState() }
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleContent(
    onSelectFileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Sube tu documento PDF",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Selecciona un documento PDF para crear tu ruta de aprendizaje personalizada",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onSelectFileClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Upload,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Seleccionar PDF",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun LoadingContent(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            strokeWidth = 6.dp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Por favor espera...",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LearningPathGeneratedContent(
    learningPath: org.example.learnify.domain.model.LearningPath,
    onContinue: () -> Unit,
    onUploadAnother: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "¡Ruta de Aprendizaje Lista!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = learningPath.title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Tu ruta de aprendizaje",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow(label = "Total de temas", value = "${learningPath.topics.size}")
                Spacer(modifier = Modifier.height(8.dp))
                val totalMinutes = learningPath.topics.sumOf { 30 } // estimado
                InfoRow(label = "Tiempo estimado", value = "${totalMinutes} min")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Temas a aprender",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                learningPath.topics.take(5).forEachIndexed { index, topic ->
                    Text(
                        text = "${index + 1}. ${topic.title}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (index < 4 && index < learningPath.topics.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                if (learningPath.topics.size > 5) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "...y ${learningPath.topics.size - 5} temas más",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Comenzar a Aprender")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onUploadAnother,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Subir Otro Documento")
        }
    }
}

@Composable
private fun SuccessContent(
    result: PdfExtractionResult,
    onContinue: () -> Unit,
    onUploadAnother: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "¡Documento procesado!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                InfoRow(label = "Total de páginas", value = "${result.totalPages}")
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(label = "Caracteres extraídos", value = "${result.text.length}")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Vista previa del contenido",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = result.text.take(300) + if (result.text.length > 300) "..." else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Crear Ruta de Aprendizaje")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onUploadAnother,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Subir Otro Documento")
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "❌",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Error al procesar documento",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Intentar de Nuevo")
        }
    }
}

@Composable
private fun ProcessingChunksContent(
    currentChunk: Int,
    totalChunks: Int,
    message: String,
    percentage: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animación de progreso circular
        Box(
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { percentage },
                modifier = Modifier.size(120.dp),
                strokeWidth = 8.dp
            )

            Text(
                text = "${(percentage * 100).toInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Título principal
        Text(
            text = "Procesando Documento",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Información de chunks
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Chunk $currentChunk de $totalChunks",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { percentage },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Mensaje informativo
        Text(
            text = "Estamos analizando cada sección de tu documento con IA para crear una ruta de aprendizaje exhaustiva. Este proceso puede tomar varios minutos.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Estimación de tiempo (opcional)
        if (totalChunks > 0 && currentChunk > 0) {
            val estimatedTimeRemaining = ((totalChunks - currentChunk) * 30) // ~30 segundos por chunk
            if (estimatedTimeRemaining > 0) {
                Text(
                    text = "Tiempo estimado restante: ~${estimatedTimeRemaining / 60} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
