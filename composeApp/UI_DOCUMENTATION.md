# UI de Learnify - Documentación

## 📱 Pantalla de Subida de Documentos

### Arquitectura MVVM

```
┌─────────────────┐
│  UploadScreen   │ ← Composable UI
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ UploadViewModel │ ← Lógica de presentación
└────────┬────────┘
         │
         ▼
┌──────────────────────┐
│ExtractPdfContentUseCase│ ← Caso de uso
└──────────┬──────────┘
           │
           ▼
    ┌─────────────┐
    │PdfExtractor │ ← Implementación de plataforma
    └─────────────┘
```

## 🎨 Estados de la UI

### UploadUiState

```kotlin
sealed interface UploadUiState {
    data object Idle              // Estado inicial
    data object SelectingFile     // File picker abierto
    data object ExtractingContent // Procesando PDF
    data class Success(result)    // Extracción exitosa
    data class Error(message)     // Error ocurrió
}
```

### Flujo de Estados

```
Idle → SelectingFile → ExtractingContent → Success
                                        ↘ Error
```

## 🔄 Flujo de Funcionamiento

### 1. Usuario Presiona "Seleccionar PDF"

```kotlin
// En UploadScreen
Button(onClick = {
    viewModel.onSelectFileClick()  // Cambiar estado a SelectingFile
    onFilePickerRequest()          // Abrir file picker de plataforma
})
```

### 2. MainActivity Detecta el Cambio de Estado

```kotlin
// En MainActivity (Android)
LaunchedEffect(uiState) {
    if (uiState is UploadUiState.SelectingFile) {
        fileSelectedCallback = { uri ->
            viewModel.onFileSelected(uri)
        }
        filePickerLauncher.launch(arrayOf("application/pdf"))
    }
}
```

### 3. Usuario Selecciona un PDF

```kotlin
// El launcher retorna con URI
filePickerLauncher = registerForActivityResult(...) { uri ->
    fileSelectedCallback?.invoke(uri.toString())
}
```

### 4. ViewModel Procesa el PDF

```kotlin
fun onFileSelected(fileUri: String) {
    viewModelScope.launch {
        _uiState.value = ExtractingContent

        val result = extractPdfContentUseCase(fileUri)

        result.onSuccess { extraction ->
            _uiState.value = Success(extraction)
        }.onFailure { error ->
            _uiState.value = Error(error.message)
        }
    }
}
```

### 5. UI Muestra el Resultado

```kotlin
when (val state = uiState) {
    is Success -> SuccessContent(state.result)
    is Error -> ErrorContent(state.message)
    is ExtractingContent -> LoadingContent()
    is Idle -> IdleContent()
}
```

## 📱 Componentes de UI

### IdleContent
- **Propósito**: Pantalla de bienvenida
- **Elementos**:
  - Icono de documento grande
  - Título: "Sube tu documento PDF"
  - Descripción explicativa
  - Botón principal: "Seleccionar PDF"

### LoadingContent
- **Propósito**: Indicador de progreso
- **Elementos**:
  - CircularProgressIndicator animado
  - Texto: "Procesando documento..."
  - Subtexto: "Extrayendo contenido del PDF"

### SuccessContent
- **Propósito**: Mostrar resultado y continuar
- **Elementos**:
  - Icono de éxito (checkmark)
  - Card con estadísticas:
    - Total de páginas
    - Caracteres extraídos
  - Card con vista previa del texto
  - Botón primario: "Crear Ruta de Aprendizaje"
  - Botón secundario: "Subir Otro Documento"

### ErrorContent
- **Propósito**: Mostrar error y reintentar
- **Elementos**:
  - Emoji de error ❌
  - Mensaje de error
  - Botón: "Intentar de Nuevo"

## 🎯 Integración con Koin

### Inyección del ViewModel

```kotlin
// En App.kt
val uploadViewModel = koinViewModel<UploadViewModel>()

// En AppModule.kt
val appModule = module {
    viewModelOf(::UploadViewModel)
    single { ExtractPdfContentUseCase(get()) }
}
```

### Módulos de Plataforma

```kotlin
// platformModule.android.kt
actual val platformModule = module {
    single<PdfExtractor> {
        AndroidPdfExtractor(androidContext())
    }
}
```

## 📝 Personalización

### Tema y Colores

La UI usa Material Design 3 con:
- `MaterialTheme.colorScheme.primary`
- `MaterialTheme.colorScheme.primaryContainer`
- `MaterialTheme.colorScheme.error`

### Modificar Textos

Todos los textos están hardcodeados en español. Para internacionalización:
1. Crear recursos de strings
2. Usar Compose Resources
3. Cambiar textos por referencias

## 🚀 Uso

### Básico

```kotlin
@Composable
fun MyApp() {
    val viewModel = koinViewModel<UploadViewModel>()

    UploadScreen(
        viewModel = viewModel,
        onFilePickerRequest = { /* abrir picker */ },
        onContinue = { result ->
            /* navegar a siguiente pantalla */
        }
    )
}
```

### Con Navegación

```kotlin
NavHost {
    composable("upload") {
        UploadScreen(
            viewModel = koinViewModel(),
            onFilePickerRequest = { openFilePicker() },
            onContinue = { result ->
                navController.navigate("learning_path/${result.id}")
            }
        )
    }
}
```

## 🔧 Testing

### ViewModel Tests

```kotlin
@Test
fun `cuando se selecciona archivo, estado cambia a extracting`() {
    val viewModel = UploadViewModel(mockExtractUseCase)

    viewModel.onFileSelected("file://test.pdf")

    assertEquals(
        UploadUiState.ExtractingContent,
        viewModel.uiState.value
    )
}
```

### UI Tests

```kotlin
@Test
fun `cuando estado es idle, muestra boton de seleccionar`() {
    composeTestRule.setContent {
        UploadScreen(viewModel, {}, {})
    }

    composeTestRule
        .onNodeWithText("Seleccionar PDF")
        .assertIsDisplayed()
}
```

## 📊 Métricas de UI

- **Estados totales**: 5
- **Pantallas**: 4 (Idle, Loading, Success, Error)
- **Componentes reutilizables**: InfoRow, Cards
- **Animaciones**: CircularProgressIndicator
- **Material Design 3**: ✅
- **Responsive**: ✅
- **Dark mode**: Soportado automáticamente

## 🎨 Mejoras Futuras

- [ ] Animaciones de transición entre estados
- [ ] Soporte para drag & drop de archivos
- [ ] Vista previa del PDF antes de procesar
- [ ] Historial de documentos procesados
- [ ] Progreso granular (porcentaje de páginas procesadas)
- [ ] Soporte para múltiples archivos
- [ ] Internacionalización (i18n)
- [ ] Temas personalizados
