package org.example.learnify.di

import org.example.learnify.BuildKonfig
import org.example.learnify.data.remote.GeminiApiClient
import org.example.learnify.domain.usecase.ExtractPdfContentUseCase
import org.example.learnify.domain.usecase.ExtractPdfToJsonUseCase
import org.example.learnify.domain.usecase.GenerateLearningPathUseCase
import org.example.learnify.domain.usecase.GenerateQuizUseCase
import org.example.learnify.domain.usecase.ProcessDocumentInChunksUseCase
import org.example.learnify.presentation.learning_path.LearningPathViewModel
import org.example.learnify.presentation.quiz.QuizViewModel
import org.example.learnify.presentation.upload.UploadViewModel
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

    // New Use Cases for chunked processing
    single { ExtractPdfToJsonUseCase(get()) }
    single { ProcessDocumentInChunksUseCase(get()) }

    // ViewModels - Using factory instead of viewModelOf to avoid iOS crash with SavedStateHandle
    factory { UploadViewModel(get(), get(), get()) }
    factory { LearningPathViewModel() }
    factory { QuizViewModel(get()) }

    // TODO: Add Database instance
    // TODO: Add Repositories
}

// Platform-specific module (defined in each platform's source set)
expect val platformModule: org.koin.core.module.Module
