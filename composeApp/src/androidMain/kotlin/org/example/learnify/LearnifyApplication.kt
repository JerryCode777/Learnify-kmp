package org.example.learnify

import android.app.Application
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.example.learnify.di.appModule
import org.example.learnify.di.platformModule

class LearnifyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Configurar Napier para logging
        Napier.base(DebugAntilog())

        // Inicializar Koin
        startKoin {
            androidLogger()
            androidContext(this@LearnifyApplication)
            modules(appModule, platformModule)
        }

        Napier.d("LearnifyApplication initialized with Koin")
    }
}
