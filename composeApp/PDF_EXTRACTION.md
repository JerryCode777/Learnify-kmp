# Módulo de Extracción de PDFs

## 📋 Resumen

Este módulo proporciona funcionalidad multiplataforma para extraer texto de archivos PDF en Android e iOS.

## 🏗️ Arquitectura

### Patrón expect/actual

Usamos el patrón **expect/actual** de Kotlin Multiplatform para tener:
- Una interfaz común en `commonMain`
- Implementaciones específicas en `androidMain` e `iosMain`

### Estructura de Archivos

```
commonMain/
├── domain/model/
│   └── PdfExtractionResult.kt      # Modelo de resultado
├── domain/usecase/
│   └── ExtractPdfContentUseCase.kt # Caso de uso
└── util/
    ├── PdfExtractor.kt              # Interface común (expect)
    └── FilePicker.kt                # Interface para seleccionar archivos

androidMain/
└── util/
    ├── PdfExtractor.android.kt      # Implementación Android (actual)
    ├── AndroidPdfExtractorWithTextExtraction.kt
    └── FilePicker.android.kt

iosMain/
└── util/
    ├── PdfExtractor.ios.kt          # Implementación iOS (actual)
    └── FilePicker.ios.kt
```

## 🤖 Implementación Android

### Enfoque 1: PdfRenderer (Básico)
- Usa `android.graphics.pdf.PdfRenderer`
- **Limitación**: No extrae texto, solo renderiza páginas
- Útil para vista previa visual

### Enfoque 2: PDFBox Android (Recomendado)
- Requiere dependencia: `com.tom-roush:pdfbox-android:2.0.27.0`
- Extrae texto real de los PDFs
- Mantiene formato y estructura

**Agregar a `build.gradle.kts` (androidMain):**
```kotlin
androidMain.dependencies {
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
}
```

## 🍎 Implementación iOS

Usa **PDFKit** nativo de iOS:
- Framework nativo de Apple
- Excelente extracción de texto
- Alto rendimiento
- Sin dependencias adicionales

**Características:**
```kotlin
val pdfDocument = PDFDocument(url)
val page = pdfDocument.pageAtIndex(0)
val text = page?.string  // Texto extraído
```

## 📱 Selección de Archivos

### Android
- Usa `ActivityResultContracts.OpenDocument()`
- Filtra por tipo MIME: `"application/pdf"`
- Retorna URI persistente

### iOS
- Usa `UIDocumentPickerViewController`
- Tipo: `UTTypePDF`
- Requiere configurar delegate para callbacks

## 🔧 Uso

### Inyección con Koin

```kotlin
// En commonMain - App.kt
fun initializeKoin() {
    startKoin {
        modules(appModule, platformModule)
    }
}

// Inyectar en ViewModel
class UploadViewModel(
    private val extractPdfUseCase: ExtractPdfContentUseCase
) : ViewModel() {

    suspend fun processPdf(fileUri: String) {
        val result = extractPdfUseCase(fileUri)
        result.onSuccess { extraction ->
            println("Extraído: ${extraction.totalPages} páginas")
            println("Texto: ${extraction.text}")
        }
    }
}
```

### Uso Directo

```kotlin
// Android
val extractor = AndroidPdfExtractor(context)
val result = extractor.extractText(pdfUri)

// iOS
val extractor = IosPdfExtractor()
val result = extractor.extractText(fileUrl)
```

## 📊 Modelo de Datos

```kotlin
data class PdfExtractionResult(
    val text: String,              // Texto completo del PDF
    val totalPages: Int,           // Número total de páginas
    val pages: List<PageContent>   // Contenido por página
)

data class PageContent(
    val pageNumber: Int,           // Número de página (1-based)
    val text: String               // Texto de esta página
)
```

## ✅ Ventajas del Enfoque

1. **Código Compartido**: Lógica común en `ExtractPdfContentUseCase`
2. **Multiplataforma**: Una interfaz, múltiples implementaciones
3. **Type-Safe**: Modelos fuertemente tipados
4. **Testeable**: Fácil de mockear para tests
5. **Manejo de Errores**: `Result<T>` para propagación segura

## 🚀 Mejoras Futuras

- [ ] Agregar PDFBox en Android para extracción real
- [ ] Implementar FilePicker completo en iOS
- [ ] Agregar extracción de imágenes
- [ ] Soporte para metadatos del PDF
- [ ] Caché de PDFs procesados
- [ ] Compresión de texto extraído
- [ ] OCR para PDFs escaneados

## 📝 Notas Importantes

### Android
- Requiere permisos de lectura de storage (se maneja automáticamente con ActivityResultContract)
- URIs persistentes se manejan via ContentResolver
- Para PDFs grandes, considerar procesamiento en chunks

### iOS
- Requiere acceso a archivos en Info.plist
- PDFKit maneja automáticamente PDFs encriptados (si se proporciona contraseña)
- Excelente rendimiento con PDFs grandes

## 🔗 Referencias

- [Android PdfRenderer](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer)
- [PDFBox Android](https://github.com/TomRoush/PdfBox-Android)
- [iOS PDFKit](https://developer.apple.com/documentation/pdfkit)
- [Kotlin Multiplatform expect/actual](https://kotlinlang.org/docs/multiplatform-connect-to-apis.html)
