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
}

/**
 * Función expect para obtener el FilePicker específico de plataforma
 */
expect fun getFilePicker(): FilePicker
