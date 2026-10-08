package com.shatrughna.drivemate.core.telemetry

enum class TelemetrySource(val displayName: String) {
    ANDROID_AUTO_CAR_HARDWARE("Android Auto Car Hardware"),
    OBD2_BLE("OBD2 Bluetooth"),
    OBD2_WIFI("OBD2 Wi-Fi"),
    PHONE_GPS("Phone GPS"),
    MANUAL("Manual Calibration"),
    NONE("None")
}

enum class TelemetryAvailability(val label: String) {
    LIVE("Live"),
    STALE("Stale"),
    UNAVAILABLE("Unavailable"),
    NOT_CONNECTED("Not Connected"),
    NOT_AUTHORIZED("Permission Denied"),
    NOT_SUPPORTED("Not Supported by Vehicle"),
    COMING_SOON("Coming Soon")
}

enum class TpmsStatus(val label: String) {
    NORMAL("Normal"),
    LOW("Low"),
    CRITICAL("Critical"),
    UNKNOWN("Unknown"),
    UNAVAILABLE("Unavailable")
}

data class TpmsWheelPressure(
    val pressurePsi: Float? = null,
    val status: TpmsStatus = TpmsStatus.UNAVAILABLE,
    val temperatureCelsius: Float? = null
)

data class TpmsState(
    val frontLeft: TpmsWheelPressure = TpmsWheelPressure(),
    val frontRight: TpmsWheelPressure = TpmsWheelPressure(),
    val rearLeft: TpmsWheelPressure = TpmsWheelPressure(),
    val rearRight: TpmsWheelPressure = TpmsWheelPressure(),
    val availability: TelemetryAvailability = TelemetryAvailability.NOT_SUPPORTED,
    val notice: String = "Unavailable through Android Auto. Check instrument cluster or direct TPMS sensor."
)

data class VehicleTelemetry(
    // Authoritative or GPS Speed
    val speedKmh: Float? = null,
    val speedSource: TelemetrySource = TelemetrySource.NONE,
    val speedAvailability: TelemetryAvailability = TelemetryAvailability.UNAVAILABLE,

    // Authoritative Vehicle Odometer
    val vehicleOdometerKm: Double? = null,
    val odometerSource: TelemetrySource = TelemetrySource.NONE,
    val odometerAvailability: TelemetryAvailability = TelemetryAvailability.UNAVAILABLE,

    // Manual calibrated odometer fallback
    val manualOdometerKm: Double? = null,

    // Current Trip GPS Distance
    val tripGpsDistanceKm: Double = 0.0,

    // Fuel / Energy
    val fuelLevelPercent: Float? = null,
    val rangeRemainingKm: Float? = null,
    val fuelSource: TelemetrySource = TelemetrySource.NONE,
    val fuelAvailability: TelemetryAvailability = TelemetryAvailability.UNAVAILABLE,
    val rangeAvailability: TelemetryAvailability = TelemetryAvailability.UNAVAILABLE,

    // TPMS
    val tpms: TpmsState = TpmsState(),

    // Engine / Battery Status
    val batteryVoltage: Float? = null,
    val coolantTempCelsius: Int? = null,
    val engineRpm: Int? = null,

    // Metadata
    val androidAutoConnected: Boolean = false,
    val vehicleTelemetryConnected: Boolean = false,
    val vehicleName: String? = null,
    val registrationNumber: String? = null,
    val speedTimestampMillis: Long? = null,
    val odometerTimestampMillis: Long? = null,
    val fuelTimestampMillis: Long? = null,
    val rangeTimestampMillis: Long? = null,
    val lastUpdatedTimestamp: Long? = null
) {
    /**
     * Resolves the most accurate authoritative odometer value available:
     * 1. Direct vehicle telemetry from Car Hardware / OBD2 if live/stale
     * 2. Otherwise manual calibration from settings
     */
    val isCarConnected: Boolean
        get() = androidAutoConnected

    val effectiveOdometerKm: Double?
        get() = vehicleOdometerKm?.takeIf { isAuthoritativeOdometer } ?: manualOdometerKm

    val isAuthoritativeOdometer: Boolean
        get() = vehicleOdometerKm != null && (odometerAvailability == TelemetryAvailability.LIVE || odometerAvailability == TelemetryAvailability.STALE)
}
