package com.pronavicola.camion.service

import com.pronavicola.camion.domain.model.GpsLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DesktopGpsLocationTracker : GpsLocationTracker {
    private val _currentLocation = MutableStateFlow<GpsLocation?>(null)
    override val currentLocation: StateFlow<GpsLocation?> = _currentLocation.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    override val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    override suspend fun startTracking() {
        _isTracking.value = true
    }

    override suspend fun stopTracking() {
        _isTracking.value = false
    }
}
