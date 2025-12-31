package org.example.learnify.presentation.components

import androidx.compose.runtime.*

/**
 * Estado compartido para manejar la selección de archivos
 * Entre el file picker de plataforma y la UI
 */
class FilePickerState {
    var onFileSelected: ((String) -> Unit)? by mutableStateOf(null)
    var isPickerOpen by mutableStateOf(false)

    fun openPicker(onSelected: (String) -> Unit) {
        onFileSelected = onSelected
        isPickerOpen = true
    }

    fun handleFileSelected(uri: String) {
        onFileSelected?.invoke(uri)
        reset()
    }

    fun reset() {
        onFileSelected = null
        isPickerOpen = false
    }
}

@Composable
fun rememberFilePickerState(): FilePickerState {
    return remember { FilePickerState() }
}
