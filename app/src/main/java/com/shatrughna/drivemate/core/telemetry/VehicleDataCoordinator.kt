package com.shatrughna.drivemate.core.telemetry

import androidx.car.app.CarContext
import com.shatrughna.drivemate.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Merges providers without inventing values; projection and telemetry are separate states. */
class VehicleDataCoordinator(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private var carHardwareProvider: AndroidAutoVehicleTelemetryProvider? = null
    private var carHardwareJob: Job? = null
    private val obd2Provider = Obd2VehicleDataProvider()
    private val tpmsProvider: TpmsProvider = AndroidAutoTpmsProvider()

    private val _telemetry = MutableStateFlow(VehicleTelemetry(tpms = tpmsProvider.tpmsState.value))
    val telemetry: StateFlow<VehicleTelemetry> = _telemetry.asStateFlow()

    init {
        obd2Provider.start()
        scope.launch {
            obd2Provider.telemetry.collectLatest { mergeProviderData(it, TelemetrySource.OBD2_BLE) }
        }
        scope.launch {
            while (isActive) {
                delay(1_000L)
                expireStaleValues(System.currentTimeMillis())
            }
        }
    }

    @Synchronized
    fun attachCarContext(carContext: CarContext) {
        AppLogger.i(AppLogger.TAG_CAR_HARDWARE, "Attaching CarContext to VehicleDataCoordinator")
        detachCarContext()
        val provider = AndroidAutoVehicleTelemetryProvider(carContext)
        carHardwareProvider = provider
        provider.start()
        carHardwareJob = scope.launch {
            provider.telemetry.collectLatest { mergeProviderData(it, TelemetrySource.ANDROID_AUTO_CAR_HARDWARE) }
        }
        _telemetry.update { it.copy(androidAutoConnected = true) }
    }

    @Synchronized
    fun detachCarContext() {
        AppLogger.i(AppLogger.TAG_CAR_HARDWARE, "Detaching CarContext from VehicleDataCoordinator")
        carHardwareJob?.cancel()
        carHardwareJob = null
        carHardwareProvider?.stop()
        carHardwareProvider = null
        _telemetry.update {
            it.copy(
                androidAutoConnected = false,
                vehicleTelemetryConnected = false,
                speedKmh = null,
                speedSource = TelemetrySource.NONE,
                speedAvailability = TelemetryAvailability.NOT_CONNECTED,
                vehicleOdometerKm = null,
                odometerSource = TelemetrySource.NONE,
                odometerAvailability = TelemetryAvailability.NOT_CONNECTED,
                fuelLevelPercent = null,
                rangeRemainingKm = null,
                fuelSource = TelemetrySource.NONE,
                fuelAvailability = TelemetryAvailability.NOT_CONNECTED,
                rangeAvailability = TelemetryAvailability.NOT_CONNECTED,
                speedTimestampMillis = null,
                odometerTimestampMillis = null,
                fuelTimestampMillis = null,
                rangeTimestampMillis = null
            )
        }
    }

    fun updateManualOdometer(km: Double?) {
        _telemetry.update { it.copy(manualOdometerKm = km?.takeIf { value -> value >= 0.0 }) }
    }

    fun updateTripGpsDistance(gpsKm: Double) {
        _telemetry.update { it.copy(tripGpsDistanceKm = gpsKm.coerceAtLeast(0.0)) }
    }

    /** GPS is a fallback estimate only and never overwrites a live vehicle/OBD reading. */
    fun updateGpsSpeed(speedKmh: Float?) {
        _telemetry.update { current ->
            if (current.speedAvailability == TelemetryAvailability.LIVE && current.speedSource != TelemetrySource.PHONE_GPS) {
                current
            } else {
                val now = System.currentTimeMillis()
                current.copy(
                    speedKmh = speedKmh?.takeIf { it.isFinite() && it >= 0f },
                    speedSource = if (speedKmh != null) TelemetrySource.PHONE_GPS else TelemetrySource.NONE,
                    speedAvailability = if (speedKmh != null) TelemetryAvailability.LIVE else TelemetryAvailability.UNAVAILABLE,
                    speedTimestampMillis = speedKmh?.let { now },
                    lastUpdatedTimestamp = now
                )
            }
        }
    }

    fun getDiagnostics(): Map<String, String> {
        val t = _telemetry.value
        return mapOf(
            "Android Auto" to if (t.androidAutoConnected) "Connected" else "Disconnected",
            "Vehicle telemetry" to if (t.vehicleTelemetryConnected) "Connected" else "Unavailable",
            "Speed" to channelDescription(t.speedKmh?.let { String.format("%.0f km/h", it) }, t.speedSource, t.speedAvailability, t.speedTimestampMillis),
            "Odometer" to channelDescription(t.vehicleOdometerKm?.let { String.format("%.1f km", it) }, t.odometerSource, t.odometerAvailability, t.odometerTimestampMillis),
            "Manual odometer" to (t.manualOdometerKm?.let { String.format("%.1f km", it) } ?: "Not configured"),
            "GPS trip distance" to String.format("%.2f km", t.tripGpsDistanceKm),
            "Fuel" to channelDescription(t.fuelLevelPercent?.let { String.format("%.0f%%", it) }, t.fuelSource, t.fuelAvailability, t.fuelTimestampMillis),
            "Range" to channelDescription(t.rangeRemainingKm?.let { String.format("%.0f km", it) }, t.fuelSource, t.rangeAvailability, t.rangeTimestampMillis),
            "TPMS" to t.tpms.notice,
            "OBD2" to "Not connected"
        )
    }

    private fun mergeProviderData(data: VehicleTelemetry, source: TelemetrySource) {
        _telemetry.update { current ->
            val now = System.currentTimeMillis()
            val speedUnavailable = current.speedAvailability in setOf(
                TelemetryAvailability.UNAVAILABLE,
                TelemetryAvailability.NOT_CONNECTED,
                TelemetryAvailability.NOT_SUPPORTED,
                TelemetryAvailability.NOT_AUTHORIZED,
                TelemetryAvailability.STALE
            )
            val odometerUnavailable = current.odometerAvailability in setOf(
                TelemetryAvailability.UNAVAILABLE,
                TelemetryAvailability.NOT_CONNECTED,
                TelemetryAvailability.NOT_SUPPORTED,
                TelemetryAvailability.NOT_AUTHORIZED,
                TelemetryAvailability.STALE
            )
            val fuelUnavailable = current.fuelAvailability in setOf(
                TelemetryAvailability.UNAVAILABLE,
                TelemetryAvailability.NOT_CONNECTED,
                TelemetryAvailability.NOT_SUPPORTED,
                TelemetryAvailability.NOT_AUTHORIZED,
                TelemetryAvailability.STALE
            )
            val rangeUnavailable = current.rangeAvailability in setOf(
                TelemetryAvailability.UNAVAILABLE,
                TelemetryAvailability.NOT_CONNECTED,
                TelemetryAvailability.NOT_SUPPORTED,
                TelemetryAvailability.NOT_AUTHORIZED,
                TelemetryAvailability.STALE
            )
            val useSpeed = data.speedKmh != null && (current.speedKmh == null || speedUnavailable || sourceRank(source) <= sourceRank(current.speedSource))
            val useOdometer = data.vehicleOdometerKm != null && (current.vehicleOdometerKm == null || odometerUnavailable || sourceRank(source) <= sourceRank(current.odometerSource))
            val useFuel = data.fuelLevelPercent != null && (current.fuelLevelPercent == null || fuelUnavailable || sourceRank(source) <= sourceRank(current.fuelSource))
            val useRange = data.rangeRemainingKm != null && (current.rangeRemainingKm == null || rangeUnavailable || sourceRank(source) <= sourceRank(current.fuelSource))
            current.copy(
                androidAutoConnected = current.androidAutoConnected || data.androidAutoConnected,
                vehicleTelemetryConnected = current.vehicleTelemetryConnected || data.vehicleTelemetryConnected,
                speedKmh = if (useSpeed) data.speedKmh else current.speedKmh,
                speedSource = if (useSpeed) source else current.speedSource,
                speedAvailability = if (useSpeed) data.speedAvailability else current.speedAvailability,
                speedTimestampMillis = if (useSpeed) data.speedTimestampMillis ?: now else current.speedTimestampMillis,
                vehicleOdometerKm = if (useOdometer) data.vehicleOdometerKm else current.vehicleOdometerKm,
                odometerSource = if (useOdometer) source else current.odometerSource,
                odometerAvailability = if (useOdometer) data.odometerAvailability else current.odometerAvailability,
                odometerTimestampMillis = if (useOdometer) data.odometerTimestampMillis ?: now else current.odometerTimestampMillis,
                fuelLevelPercent = if (useFuel) data.fuelLevelPercent else current.fuelLevelPercent,
                rangeRemainingKm = if (useRange) data.rangeRemainingKm else current.rangeRemainingKm,
                fuelSource = if (useFuel) source else current.fuelSource,
                fuelAvailability = if (useFuel) data.fuelAvailability else current.fuelAvailability,
                rangeAvailability = if (useRange) data.rangeAvailability else current.rangeAvailability,
                fuelTimestampMillis = if (useFuel) data.fuelTimestampMillis ?: now else current.fuelTimestampMillis,
                rangeTimestampMillis = if (useRange) data.rangeTimestampMillis ?: now else current.rangeTimestampMillis,
                lastUpdatedTimestamp = now
            )
        }
    }

    private fun expireStaleValues(now: Long) {
        _telemetry.update { current ->
            fun state(timestamp: Long?, availability: TelemetryAvailability, timeout: Long): TelemetryAvailability {
                if (timestamp == null) return availability
                val age = now - timestamp
                return when {
                    age <= timeout -> availability
                    age <= timeout * 3 -> if (availability == TelemetryAvailability.LIVE) TelemetryAvailability.STALE else availability
                    else -> TelemetryAvailability.UNAVAILABLE
                }
            }
            val speed = state(current.speedTimestampMillis, current.speedAvailability, 2_000L)
            val odo = state(current.odometerTimestampMillis, current.odometerAvailability, 10_000L)
            val fuel = state(current.fuelTimestampMillis, current.fuelAvailability, 30_000L)
            val range = state(current.rangeTimestampMillis, current.rangeAvailability, 30_000L)
            current.copy(
                speedAvailability = speed,
                odometerAvailability = odo,
                fuelAvailability = fuel,
                rangeAvailability = range,
                vehicleTelemetryConnected = speed == TelemetryAvailability.LIVE || odo == TelemetryAvailability.LIVE || fuel == TelemetryAvailability.LIVE || range == TelemetryAvailability.LIVE
            )
        }
    }

    private fun channelDescription(value: String?, source: TelemetrySource, availability: TelemetryAvailability, timestamp: Long?): String {
        val age = timestamp?.let { "Updated ${((System.currentTimeMillis() - it) / 1_000L).coerceAtLeast(0)} sec ago" } ?: "No update"
        return "${value ?: "Unavailable"} (${source.displayName}, ${availability.label}; $age)"
    }

    private fun sourceRank(source: TelemetrySource): Int = when (source) {
        TelemetrySource.ANDROID_AUTO_CAR_HARDWARE -> 0
        TelemetrySource.OBD2_BLE, TelemetrySource.OBD2_WIFI -> 1
        TelemetrySource.PHONE_GPS -> 2
        TelemetrySource.MANUAL -> 3
        TelemetrySource.NONE -> 4
    }
}
