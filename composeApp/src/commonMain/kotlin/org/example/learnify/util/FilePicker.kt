package org.example.learnify.util

/**
 * Interface para seleccionar archivos desde el sistema
 * Implementaciones específicas por plataforma
 */
interface FilePicker {
    /**
     * Abre el selector de archivos para PDFs
     * @return URI del archivo seleccionado o null si se canceló
     */
    suspend fun pickPdfFile(): String?

    /**
     * Indica si el picker puede abrirse directamente desde la capa compartida.
     * Si es false, la UI debe delegar la apertura al host de plataforma.
     */
    val supportsDirectPicker: Boolean
}

/**
 * Función expect para obtener el FilePicker específico de plataforma
 */
expect fun getFilePicker(): FilePicker
