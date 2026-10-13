package com.pronavicola.camion.domain.usecase.alarmas

class ExportAlarmasCsvUseCase {
    operator fun invoke(alarmas: List<AlarmaRegistroUiModel>): String {
        val sb = StringBuilder()
        sb.appendLine("IdAlarma,Alarma,IdVehiculo,IdViaje,HoraInicio,HoraFin,Estado")
        for (item in alarmas) {
            val safeDesc = item.alarmaDescripcion.replace("\"", "\"\"")
            val fin = if (item.horaFin.isBlank()) "-" else item.horaFin
            sb.appendLine("${item.idAlarma},\"$safeDesc\",${item.idVehiculo},${item.idViaje},\"${item.horaInicio}\",\"$fin\",\"${item.estado ?: ""}\"")
        }
        return sb.toString()
    }
}
