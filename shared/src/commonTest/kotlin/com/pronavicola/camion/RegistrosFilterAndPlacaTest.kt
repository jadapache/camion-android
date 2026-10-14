package com.pronavicola.camion

import com.pronavicola.camion.data.local.entity.VariableRegistroEntity
import com.pronavicola.camion.domain.usecase.registros.ExportRegistrosCsvUseCase
import com.pronavicola.camion.presentation.registros.RegistrosUiState
import com.pronavicola.camion.presentation.registros.SortColumn
import com.pronavicola.camion.presentation.registros.SortDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RegistrosFilterAndPlacaTest {

    private val sampleRegistros = listOf(
        VariableRegistroEntity(
            idRegistro = 1,
            idVehiculo = 1,
            placa = "ABC123",
            fecha = "2026-10-10 08:00:00",
            temperatura1 = 20.0,
            temperatura2 = 21.0,
            humedad = 60.0,
            tension = 24.0,
        ),
        VariableRegistroEntity(
            idRegistro = 2,
            idVehiculo = 2,
            placa = "FURG-002",
            fecha = "2026-10-10 09:00:00",
            temperatura1 = 22.0,
            temperatura2 = 23.0,
            humedad = 65.0,
            tension = 24.2,
        ),
        VariableRegistroEntity(
            idRegistro = 3,
            idVehiculo = 3,
            placa = "XYZ999",
            fecha = "2026-10-10 10:00:00",
            temperatura1 = 19.5,
            temperatura2 = 20.5,
            humedad = 58.0,
            tension = 23.8,
        ),
    )

    @Test
    fun testFilterByPlacaExactMatch() {
        val state = RegistrosUiState(
            registros = sampleRegistros,
            filterPlaca = "ABC123",
        )
        val filtered = state.filteredRegistros
        assertEquals(1, filtered.size)
        assertEquals("ABC123", filtered[0].placa)
    }

    @Test
    fun testFilterByPlacaCaseInsensitiveAndPartial() {
        val state = RegistrosUiState(
            registros = sampleRegistros,
            filterPlaca = "furg",
        )
        val filtered = state.filteredRegistros
        assertEquals(1, filtered.size)
        assertEquals("FURG-002", filtered[0].placa)
    }

    @Test
    fun testFilterByPlacaOrIdFallback() {
        val state = RegistrosUiState(
            registros = sampleRegistros,
            filterPlaca = "2",
        )
        val filtered = state.filteredRegistros
        // Matches "FURG-002" (contains '2') and idVehiculo = 2
        assertTrue(filtered.any { it.placa == "FURG-002" })
    }

    @Test
    fun testSortByPlacaAscendingAndDescending() {
        val stateAsc = RegistrosUiState(
            registros = sampleRegistros,
            sortColumn = SortColumn.PLACA,
            sortDirection = SortDirection.ASC,
        )
        val filteredAsc = stateAsc.filteredRegistros
        assertEquals(3, filteredAsc.size)
        assertEquals("ABC123", filteredAsc[0].placa)
        assertEquals("FURG-002", filteredAsc[1].placa)
        assertEquals("XYZ999", filteredAsc[2].placa)

        val stateDesc = RegistrosUiState(
            registros = sampleRegistros,
            sortColumn = SortColumn.PLACA,
            sortDirection = SortDirection.DESC,
        )
        val filteredDesc = stateDesc.filteredRegistros
        assertEquals(3, filteredDesc.size)
        assertEquals("XYZ999", filteredDesc[0].placa)
        assertEquals("FURG-002", filteredDesc[1].placa)
        assertEquals("ABC123", filteredDesc[2].placa)
    }

    @Test
    fun testExportCsvIncludesPlacaColumn() {
        val useCase = ExportRegistrosCsvUseCase()
        val csv = useCase(sampleRegistros)

        // Verify CSV header includes Placa
        assertTrue(csv.startsWith("IdRegistro,IdVehiculo,Placa,Fecha,Temperatura1,Temperatura2,Humedad,Tension"))

        // Verify rows contain the plates
        assertTrue(csv.contains("\"ABC123\""))
        assertTrue(csv.contains("\"FURG-002\""))
        assertTrue(csv.contains("\"XYZ999\""))
    }
}
