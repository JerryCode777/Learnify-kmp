package org.example.learnify

import org.koin.core.context.startKoin
import org.example.learnify.di.appModule
import org.example.learnify.di.platformModule

fun initializeKoin() {
    startKoin {
        modules(appModule, platformModule)
    }
}
