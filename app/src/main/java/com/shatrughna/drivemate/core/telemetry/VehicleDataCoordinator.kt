package com.shatrughna.drivemate.core.telemetry

import androidx.car.app.CarContext
import com.shatrughna.drivemate.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VehicleDataCoordinator(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private var carHardwareProvider: AndroidAutoVehicleTelemetryProvider? = null
    private var carHardwareJob: Job? = null

    private val obd2Provider = Obd2VehicleDataProvider()
    private val tpmsProvider: TpmsProvider = AndroidAutoTpmsProvider()

    private val _telemetry = MutableStateFlow(
        VehicleTelemetry(
            manualOdometerKm = 12500.0,
            tpms = tpmsProvider.tpmsState.value
        )
    )
    val telemetry: StateFlow<VehicleTelemetry> = _telemetry.asStateFlow()

    init {
        obd2Provider.start()
    }

    @Synchronized
    fun attachCarContext(carContext: CarContext) {
        AppLogger.i(AppLogger.TAG_CAR_HARDWARE, "Attaching CarContext to VehicleDataCoordinator")
        detachCarContext()

        val provider = AndroidAutoVehicleTelemetryProvider(carContext)
        carHardwareProvider = provider
        provider.start()

        carHardwareJob = scope.launch {
            provider.telemetry.collectLatest { carData ->
                _telemetry.update { current ->
                    current.copy(
                        isCarConnected = carData.isCarConnected,
                        speedKmh = carData.speedKmh ?: current.speedKmh,
                        speedSource = if (carData.speedKmh != null) TelemetrySource.ANDROID_AUTO_CAR_HARDWARE else current.speedSource,
                        speedAvailability = carData.speedAvailability,
                        vehicleOdometerKm = carData.vehicleOdometerKm ?: current.vehicleOdometerKm,
                        odometerSource = if (carData.vehicleOdometerKm != null) TelemetrySource.ANDROID_AUTO_CAR_HARDWARE else current.odometerSource,
                        odometerAvailability = carData.odometerAvailability,
                        fuelLevelPercent = carData.fuelLevelPercent ?: current.fuelLevelPercent,
                        rangeRemainingKm = carData.rangeRemainingKm ?: current.rangeRemainingKm,
                        fuelSource = if (carData.fuelLevelPercent != null) TelemetrySource.ANDROID_AUTO_CAR_HARDWARE else current.fuelSource,
                        fuelAvailability = carData.fuelAvailability,
                        lastUpdatedTimestamp = System.currentTimeMillis()
                    )
                }
            }
        }
    }

    @Synchronized
    fun detachCarContext() {
        AppLogger.i(AppLogger.TAG_CAR_HARDWARE, "Detaching CarContext from VehicleDataCoordinator")
        carHardwareJob?.cancel()
        carHardwareJob = null
        carHardwareProvider?.stop()
        carHardwareProvider = null

        _telemetry.update { current ->
            current.copy(
                isCarConnected = false,
                speedAvailability = TelemetryAvailability.NOT_CONNECTED,
                odometerAvailability = if (current.vehicleOdometerKm != null) TelemetryAvailability.STALE else TelemetryAvailability.NOT_CONNECTED,
                fuelAvailability = if (current.fuelLevelPercent != null) TelemetryAvailability.STALE else TelemetryAvailability.NOT_CONNECTED
            )
        }
    }

    fun updateManualOdometer(km: Double) {
        _telemetry.update { it.copy(manualOdometerKm = km) }
    }

    fun updateTripGpsDistance(gpsKm: Double) {
        // Data honesty: GPS trip distance never overwrites or increments vehicleOdometerKm
        _telemetry.update { it.copy(tripGpsDistanceKm = gpsKm) }
    }

    fun updateGpsSpeed(speedKmh: Float?) {
        // If Android Auto car hardware speed is not live, use GPS speed with PHONE_GPS source
        _telemetry.update { current ->
            if (current.speedAvailability != TelemetryAvailability.LIVE) {
                current.copy(
                    speedKmh = speedKmh,
                    speedSource = TelemetrySource.PHONE_GPS,
                    speedAvailability = if (speedKmh != null) TelemetryAvailability.LIVE else TelemetryAvailability.UNAVAILABLE
                )
            } else {
                current
            }
        }
    }

    fun getDiagnostics(): Map<String, String> {
        val t = _telemetry.value
        return mapOf(
            "Car Connected" to if (t.isCarConnected) "Yes" else "No",
            "Hardware API" to if (carHardwareProvider != null) "Attached" else "Detached",
            "Speed" to "${t.speedKmh?.let { String.format("%.0f km/h", it) } ?: "Unavailable"} (${t.speedSource.displayName} - ${t.speedAvailability.label})",
            "Vehicle Odometer" to "${t.vehicleOdometerKm?.let { String.format("%.1f km", it) } ?: "Unavailable"} (Direct Vehicle: ${t.odometerAvailability.label})",
            "Manual Odometer" to String.format("%.1f km", t.manualOdometerKm),
            "GPS Trip Dist" to String.format("%.2f km", t.tripGpsDistanceKm),
            "Fuel Level" to "${t.fuelLevelPercent?.let { String.format("%.0f%%", it) } ?: "Unavailable"} (${t.fuelAvailability.label})",
            "Range Remaining" to "${t.rangeRemainingKm?.let { String.format("%.0f km", it) } ?: "Unavailable"}",
            "TPMS" to t.tpms.notice,
            "OBD2 Adapter" to "Not connected"
        )
    }
}
