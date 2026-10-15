package com.pronavicola.camion.domain.model

object DefinicionConstants {
    // Categorías
    const val CAT_ESTAALAR = "ESTAALAR."
    const val CAT_ESTAUSUA = "ESTAUSUA."
    const val CAT_TIPONUME = "TIPONUME."
    const val CAT_UNIDMEDI = "UNIDMEDI."

    // Estados de las Alarmas
    const val ESTADO_ALARMA_ACTIVA = "ESTAALAR.ACT"
    const val ESTADO_ALARMA_CONFIRMADA = "ESTAALAR.CON"
    const val ESTADO_ALARMA_FINALIZADA = "ESTAALAR.FIN"

    // Estados de los Usuarios
    const val ESTADO_USUARIO_ACTIVO = "ESTAUSUA.ACT"
    const val ESTADO_USUARIO_INACTIVO = "ESTAUSUA.INA"

    // Tipos de número
    const val TIPO_NUMERO_DECIMAL = "TIPONUME.DEC"
    const val TIPO_NUMERO_ENTERO = "TIPONUME.INT"

    // Unidades de Medida
    const val UNIDAD_AMPERIOS = "UNIDMEDI.AMP"
    const val UNIDAD_HERTZ = "UNIDMEDI.HTZ"
    const val UNIDAD_KILOGRAMOS = "UNIDMEDI.KGS"
    const val UNIDAD_KILOGRAMOS_POR_SEGUNDO = "UNIDMEDI.KXS"
    const val UNIDAD_MILISEGUNDOS = "UNIDMEDI.MSG"
    const val UNIDAD_PORCENTAJE = "UNIDMEDI.PCT"
    const val UNIDAD_SEGUNDOS = "UNIDMEDI.SEG"
    const val UNIDAD_TEMPERATURA = "UNIDMEDI.TEM"
    const val UNIDAD_UNIDAD = "UNIDMEDI.UNI"
}

enum class EstadoAlarma(val idDefinicion: String, val descripcion: String) {
    ACTIVA(DefinicionConstants.ESTADO_ALARMA_ACTIVA, "Activa"),
    CONFIRMADA(DefinicionConstants.ESTADO_ALARMA_CONFIRMADA, "Confirmada"),
    FINALIZADA(DefinicionConstants.ESTADO_ALARMA_FINALIZADA, "Finalizada");

    companion object {
        fun fromId(id: String?): EstadoAlarma = entries.firstOrNull { it.idDefinicion == id } ?: ACTIVA
    }
}

enum class EstadoUsuario(val idDefinicion: String, val descripcion: String) {
    ACTIVO(DefinicionConstants.ESTADO_USUARIO_ACTIVO, "Activo"),
    INACTIVO(DefinicionConstants.ESTADO_USUARIO_INACTIVO, "Inactivo");

    companion object {
        fun fromId(id: String?): EstadoUsuario = entries.firstOrNull { it.idDefinicion == id } ?: ACTIVO
    }
}

enum class TipoNumero(val idDefinicion: String, val descripcion: String) {
    DECIMAL(DefinicionConstants.TIPO_NUMERO_DECIMAL, "Decimal"),
    ENTERO(DefinicionConstants.TIPO_NUMERO_ENTERO, "Entero");

    companion object {
        fun fromId(id: String?): TipoNumero = entries.firstOrNull { it.idDefinicion == id } ?: DECIMAL
    }
}

enum class UnidadMedida(val idDefinicion: String, val simbolo: String) {
    AMPERIOS(DefinicionConstants.UNIDAD_AMPERIOS, "A"),
    HERTZ(DefinicionConstants.UNIDAD_HERTZ, "Hz"),
    KILOGRAMOS(DefinicionConstants.UNIDAD_KILOGRAMOS, "kg"),
    KILOGRAMOS_POR_SEGUNDO(DefinicionConstants.UNIDAD_KILOGRAMOS_POR_SEGUNDO, "Kg/s"),
    MILISEGUNDOS(DefinicionConstants.UNIDAD_MILISEGUNDOS, "ms"),
    PORCENTAJE(DefinicionConstants.UNIDAD_PORCENTAJE, "%"),
    SEGUNDOS(DefinicionConstants.UNIDAD_SEGUNDOS, "s"),
    TEMPERATURA(DefinicionConstants.UNIDAD_TEMPERATURA, "°C"),
    UNIDAD(DefinicionConstants.UNIDAD_UNIDAD, "u");

    companion object {
        fun fromId(id: String?): UnidadMedida = entries.firstOrNull { it.idDefinicion == id } ?: UNIDAD
    }
}
