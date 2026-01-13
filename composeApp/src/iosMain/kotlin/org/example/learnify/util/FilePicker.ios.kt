package org.example.learnify.util

import io.github.aakira.napier.Napier
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURL
import platform.UIKit.*
import platform.UniformTypeIdentifiers.UTTypePDF
import platform.darwin.NSObject
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosFilePicker : FilePicker {

    override suspend fun pickPdfFile(): String? = suspendCancellableCoroutine { cont ->
        try {
            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController

            if (rootViewController == null) {
                Napier.e("No se pudo obtener el rootViewController")
                cont.resume(null)
                return@suspendCancellableCoroutine
            }

            // Flag para prevenir múltiples resumptions
            var isResumed = false

            // Crear el delegate para manejar la selección
            val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(
                    controller: UIDocumentPickerViewController,
                    didPickDocumentsAtURLs: List<*>
                ) {
                    // Prevenir múltiples llamadas al delegate
                    if (isResumed) {
                        Napier.w("Delegate ya fue llamado, ignorando llamada duplicada")
                        return
                    }
                    isResumed = true

                    @Suppress("UNCHECKED_CAST")
                    val urls = didPickDocumentsAtURLs as? List<NSURL>
                    val selectedUrl = urls?.firstOrNull()

                    if (selectedUrl != null) {
                        // Acceder al recurso con scope de seguridad
                        val hasAccess = selectedUrl.startAccessingSecurityScopedResource()

                        Napier.d("Archivo seleccionado - Path: ${selectedUrl.path}")
                        Napier.d("Archivo seleccionado - Absolute String: ${selectedUrl.absoluteString}")
                        Napier.d("Security scoped access: $hasAccess")

                        // Retornar la ruta absoluta (sin el esquema file://)
                        val filePath = selectedUrl.path ?: selectedUrl.absoluteString
                        cont.resume(filePath)

                        // IMPORTANTE: NO llamar stopAccessingSecurityScopedResource aquí
                        // porque necesitamos acceso durante la lectura del PDF
                    } else {
                        Napier.w("No se seleccionó ningún archivo")
                        cont.resume(null)
                    }
                }

                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                    // Prevenir múltiples llamadas al delegate
                    if (isResumed) {
                        Napier.w("Delegate ya fue llamado, ignorando cancelación duplicada")
                        return
                    }
                    isResumed = true

                    Napier.d("File picker cancelado por el usuario")
                    cont.resume(null)
                }
            }

            // Crear el document picker para PDFs
            val documentPicker = UIDocumentPickerViewController(
                forOpeningContentTypes = listOf(UTTypePDF),
                asCopy = true
            )

            documentPicker.delegate = delegate
            documentPicker.allowsMultipleSelection = false

            // Manejar cancelación de la coroutine
            cont.invokeOnCancellation {
                if (!isResumed) {
                    isResumed = true
                    Napier.d("Coroutine cancelada, cerrando file picker")
                }
            }

            // Presentar el picker
            rootViewController.presentViewController(
                documentPicker,
                animated = true,
                completion = null
            )

            Napier.d("File picker presentado correctamente")

        } catch (e: Exception) {
            Napier.e("Error al mostrar file picker", e)
            if (cont.isActive) {
                cont.resume(null)
            }
        }
    }

    override val supportsDirectPicker: Boolean = true
}

actual fun getFilePicker(): FilePicker {
    return IosFilePicker()
}
