package com.shatrughna.drivemate.core.telemetry

import com.shatrughna.drivemate.data.preferences.DriveMatePreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VehicleTelemetryRepository(
    private val coordinator: VehicleDataCoordinator,
    private val preferencesRepository: DriveMatePreferencesRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    val telemetry: StateFlow<VehicleTelemetry> = coordinator.telemetry

    init {
        scope.launch {
            preferencesRepository.settingsFlow.collectLatest { settings ->
                coordinator.updateManualOdometer(settings.manualOdometerKm)
            }
        }
    }

    fun updateGpsTripDistance(distKm: Double) {
        coordinator.updateTripGpsDistance(distKm)
    }

    fun updateGpsSpeed(speedKmh: Float?) {
        coordinator.updateGpsSpeed(speedKmh)
    }

    fun getDiagnostics(): Map<String, String> {
        return coordinator.getDiagnostics()
    }
}
