package org.example.learnify.di

import org.example.learnify.util.FilePicker
import org.example.learnify.util.IosFilePicker
import org.example.learnify.util.IosPdfExtractor
import org.example.learnify.util.PdfExtractor
import org.koin.dsl.module

actual val platformModule = module {
    // PDF Extractor para iOS
    single<PdfExtractor> {
        IosPdfExtractor()
    }

    // File Picker para iOS
    single<FilePicker> {
        IosFilePicker()
    }
}
