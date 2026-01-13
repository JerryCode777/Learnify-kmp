package org.example.learnify.presentation.strings

import platform.Foundation.NSUserDefaults

actual fun currentLanguageTag(): String {
    @Suppress("UNCHECKED_CAST")
    val languages = NSUserDefaults.standardUserDefaults
        .stringArrayForKey("AppleLanguages") as? List<String>
    return languages?.firstOrNull() ?: "es"
}
