package org.example.learnify

import androidx.compose.ui.window.ComposeUIViewController
import org.example.learnify.di.appModule
import org.example.learnify.di.platformModule
import org.koin.core.context.startKoin

private var koinInitialized = false

fun MainViewController() = ComposeUIViewController {
    if (!koinInitialized) {
        startKoin {
            modules(appModule, platformModule)
        }
        koinInitialized = true
    }

    App()
}