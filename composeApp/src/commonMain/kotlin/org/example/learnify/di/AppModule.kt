package org.example.learnify.di

import org.example.learnify.BuildKonfig
import org.example.learnify.data.remote.GeminiApiClient
import org.example.learnify.domain.usecase.ExtractPdfContentUseCase
import org.example.learnify.domain.usecase.GenerateLearningPathUseCase
import org.example.learnify.domain.usecase.GenerateQuizUseCase
import org.example.learnify.presentation.learning_path.LearningPathViewModel
import org.example.learnify.presentation.upload.UploadViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    // Gemini API Client
    single {
        // API Key leída desde local.properties en tiempo de compilación
        GeminiApiClient(apiKey = BuildKonfig.GEMINI_API_KEY)
    }

    // Use Cases
    single { ExtractPdfContentUseCase(get()) }
    single { GenerateLearningPathUseCase(get()) }
    single { GenerateQuizUseCase(get()) }

    // ViewModels
    viewModelOf(::UploadViewModel)
    viewModelOf(::LearningPathViewModel)

    // TODO: Add Database instance
    // TODO: Add Repositories
}

// Platform-specific module (defined in each platform's source set)
expect val platformModule: org.koin.core.module.Module
