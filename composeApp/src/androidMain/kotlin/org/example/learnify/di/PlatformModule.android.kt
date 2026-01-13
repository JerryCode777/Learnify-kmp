package org.example.learnify.di

import android.content.Context
import org.example.learnify.util.AndroidPdfExtractor
import org.example.learnify.util.FilePicker
import org.example.learnify.util.PdfExtractor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

// Placeholder FilePicker para Android que usa el sistema de callback existente
class AndroidFilePickerPlaceholder(private val context: Context) : FilePicker {
    override suspend fun pickPdfFile(): String? {
        // En Android, el file picker se maneja desde MainActivity con ActivityResultContract
        // Este placeholder se usa solo para satisfacer la inyección de dependencias
        // El flujo real usa el callback onFilePickerRequest
        return null
    }

    override val supportsDirectPicker: Boolean = false
}

actual val platformModule = module {
    // PDF Extractor para Android
    single<PdfExtractor> {
        AndroidPdfExtractor(androidContext())
    }

    // File Picker placeholder para Android
    single<FilePicker> {
        AndroidFilePickerPlaceholder(androidContext())
    }
}
