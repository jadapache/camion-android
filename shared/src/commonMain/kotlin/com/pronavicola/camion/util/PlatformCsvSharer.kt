package com.pronavicola.camion.util

interface PlatformCsvSharer {
    /**
     * Guarda el contenido CSV en un archivo temporal y abre el selector / visor de
     * aplicaciones del sistema para que el usuario elija con qué app visualizarlo
     * o almacenarlo (por ejemplo Google Drive, Sheets, Excel o Gestor de Archivos).
     */
    fun shareCsv(fileName: String, csvContent: String): Result<String>
}

expect fun getPlatformCsvSharer(platformContext: Any? = null): PlatformCsvSharer
