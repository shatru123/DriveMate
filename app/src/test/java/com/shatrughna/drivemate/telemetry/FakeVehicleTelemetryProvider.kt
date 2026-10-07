package com.shatrughna.drivemate.telemetry

import com.shatrughna.drivemate.core.telemetry.TelemetryAvailability
import com.shatrughna.drivemate.core.telemetry.TelemetrySource
import com.shatrughna.drivemate.core.telemetry.VehicleTelemetry
import com.shatrughna.drivemate.core.telemetry.VehicleTelemetryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeVehicleTelemetryProvider : VehicleTelemetryProvider {
    override val name: String = "Fake Vehicle Telemetry Provider"
    override val source: TelemetrySource = TelemetrySource.ANDROID_AUTO_CAR_HARDWARE
    override var isAvailable: Boolean = true

    private val _telemetry = MutableStateFlow(
        VehicleTelemetry(
            isCarConnected = true,
            speedSource = TelemetrySource.ANDROID_AUTO_CAR_HARDWARE,
            speedAvailability = TelemetryAvailability.LIVE,
            odometerSource = TelemetrySource.ANDROID_AUTO_CAR_HARDWARE,
            odometerAvailability = TelemetryAvailability.LIVE
        )
    )
    override val telemetry: StateFlow<VehicleTelemetry> = _telemetry.asStateFlow()

    fun emitSpeed(speedKmh: Float, availability: TelemetryAvailability = TelemetryAvailability.LIVE) {
        _telemetry.update {
            it.copy(
                speedKmh = speedKmh,
                speedSource = TelemetrySource.ANDROID_AUTO_CAR_HARDWARE,
                speedAvailability = availability,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
        }
    }

    fun emitOdometer(odometerKm: Double, availability: TelemetryAvailability = TelemetryAvailability.LIVE) {
        _telemetry.update {
            it.copy(
                vehicleOdometerKm = odometerKm,
                odometerSource = TelemetrySource.ANDROID_AUTO_CAR_HARDWARE,
                odometerAvailability = availability,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
        }
    }

    fun emitFuel(percent: Float, rangeKm: Float, availability: TelemetryAvailability = TelemetryAvailability.LIVE) {
        _telemetry.update {
            it.copy(
                fuelLevelPercent = percent,
                rangeRemainingKm = rangeKm,
                fuelSource = TelemetrySource.ANDROID_AUTO_CAR_HARDWARE,
                fuelAvailability = availability,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
        }
    }

    override fun start() {
        isAvailable = true
    }

    override fun stop() {
        isAvailable = false
        _telemetry.update {
            it.copy(
                isCarConnected = false,
                speedAvailability = TelemetryAvailability.NOT_CONNECTED,
                odometerAvailability = TelemetryAvailability.STALE
            )
        }
    }
}
