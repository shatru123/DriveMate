package com.shatrughna.drivemate.core.telemetry

import com.shatrughna.drivemate.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Obd2VehicleDataProvider : VehicleTelemetryProvider {
    override val name: String = "OBD2 Bluetooth / Wi-Fi Adapter"
    override val source: TelemetrySource = TelemetrySource.OBD2_BLE

    private val _telemetry = MutableStateFlow(
        VehicleTelemetry(
            isCarConnected = false,
            speedSource = TelemetrySource.OBD2_BLE,
            speedAvailability = TelemetryAvailability.NOT_CONNECTED,
            odometerSource = TelemetrySource.OBD2_BLE,
            odometerAvailability = TelemetryAvailability.NOT_CONNECTED,
            fuelSource = TelemetrySource.OBD2_BLE,
            fuelAvailability = TelemetryAvailability.NOT_CONNECTED
        )
    )
    override val telemetry: StateFlow<VehicleTelemetry> = _telemetry.asStateFlow()

    override val isAvailable: Boolean = false

    override fun start() {
        AppLogger.i(AppLogger.TAG_OBD, "OBD2 provider initialized in disconnected standby mode")
    }

    override fun stop() {
        AppLogger.i(AppLogger.TAG_OBD, "OBD2 provider stopped")
    }
}
