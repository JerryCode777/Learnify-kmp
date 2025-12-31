package org.example.learnify.di

import org.example.learnify.util.AndroidPdfExtractor
import org.example.learnify.util.PdfExtractor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    // PDF Extractor para Android
    single<PdfExtractor> {
        AndroidPdfExtractor(androidContext())
    }
}
