package org.example.learnify.presentation.strings

import java.util.Locale

actual fun currentLanguageTag(): String {
    return Locale.getDefault().toLanguageTag()
}
