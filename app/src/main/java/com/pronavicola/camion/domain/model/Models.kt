package com.pronavicola.camion.domain.model

data class SessionUser(
    val idUsuario: String,
    val usuario: String,
    val idPerfil: Int,
    val idVehiculo: String,
)

data class ParametroUiModel(
    val idParametro: Int,
    val nombre: String,
    val valor: Double,
    val unidad: String,
    val minimo: Double,
    val maximo: Double,
    val esDecimal: Boolean,
    val editable: Boolean,
    val tag: String,
)

enum class ProcesoEstado(val label: String) {
    INACTIVO("DETENIDO"),
    ACTIVO("EN PROCESO"),
}

data class HmiState(
    val idVehiculo: String = "AAA000",
    val conectado: Boolean = false,
    val temperatura1: Double? = null,
    val temperatura2: Double? = null,
    val humedad: Double? = null,
    val tension: Double? = null,
    val procesoEstado: ProcesoEstado = ProcesoEstado.INACTIVO,
    val zonas: List<Boolean> = List(6) { false },
    val bombaAgua: Boolean = false,
    val history: List<HmiHistoryPoint> = emptyList(),
    val mensaje: String? = null,
)

data class HmiHistoryPoint(
    val t1: Double,
    val t2: Double,
    val hum: Double,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AppDestination(val route: String, val label: String, val accessLevel: Int) {
    HMI("hmi", "CAMION", 4),
    PARAMETROS("parametros", "PARAM", 4),
    CONFIG("config", "CONFIG", 4),
}
