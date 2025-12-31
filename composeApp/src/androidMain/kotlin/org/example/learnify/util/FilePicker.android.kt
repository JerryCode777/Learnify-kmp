package org.example.learnify.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidFilePicker(private val activity: ComponentActivity) : FilePicker {

    private var continuation: ((String?) -> Unit)? = null

    private val launcher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            continuation?.invoke(uri?.toString())
            continuation = null
        }

    override suspend fun pickPdfFile(): String? = suspendCancellableCoroutine { cont ->
        continuation = { uri ->
            cont.resume(uri)
        }

        // Abrir selector solo para PDFs
        launcher.launch(arrayOf("application/pdf"))

        cont.invokeOnCancellation {
            continuation = null
        }
    }
}

actual fun getFilePicker(): FilePicker {
    throw IllegalStateException("Use AndroidFilePicker(activity) directamente o inyéctelo con Koin")
}
