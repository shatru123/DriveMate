package com.shatrughna.drivemate.core.capabilities

/**
 * Strict vehicle capability availability status.
 * Eliminates fake telemetry or simulated bus actuation.
 */
enum class CapabilityStatus(val displayName: String) {
    /** Feature is actively supported and currently functional via vehicle bus/OBD2. */
    SUPPORTED("Supported"),

    /** Feature is generally supported on this vehicle model, but temporarily offline or disconnected. */
    UNAVAILABLE("Unavailable"),

    /** Phone is not connected to vehicle CAN bus / head unit. */
    NOT_CONNECTED("Not Connected"),

    /** User has not granted permissions or OEM authorization required for this subsystem. */
    NOT_AUTHORIZED("Not Authorized"),

    /** The vehicle model or trim hardware does not expose this capability to DriveMate. */
    NOT_SUPPORTED_BY_VEHICLE("Not Supported by Vehicle"),

    /** Feature is in development or pending future OEM gateway release. */
    COMING_SOON("Coming Soon");

    val isUsable: Boolean get() = this == SUPPORTED
}

/**
 * Represents an individual vehicle subsystem capability state.
 */
data class FeatureCapability(
    val featureId: String,
    val title: String,
    val status: CapabilityStatus,
    val detailMessage: String,
    val iconName: String = "car"
)

/**
 * Result of attempting a vehicle action (e.g. climate set, door lock, camera view).
 */
data class CapabilityActionResult(
    val success: Boolean,
    val status: CapabilityStatus,
    val userMessage: String
)

/**
 * Complete snapshot of vehicle capabilities for the current vehicle (Tata Nexon Creative+ S).
 */
data class VehicleCapabilitiesState(
    val climateControl: FeatureCapability = FeatureCapability(
        featureId = "climate_control",
        title = "Automatic Climate Control",
        status = CapabilityStatus.NOT_SUPPORTED_BY_VEHICLE,
        detailMessage = "Vehicle climate controls are read-only in companion mode. Direct HVAC bus control is not exposed.",
        iconName = "climate"
    ),
    val camera360: FeatureCapability = FeatureCapability(
        featureId = "camera_360",
        title = "360° Surround View Camera",
        status = CapabilityStatus.NOT_SUPPORTED_BY_VEHICLE,
        detailMessage = "OEM 360° camera feed is displayed on infotainment display. Phone companion access is restricted by OEM.",
        iconName = "camera"
    ),
    val tyrePressureMonitoring: FeatureCapability = FeatureCapability(
        featureId = "tpms",
        title = "Tyre Pressure Monitoring (TPMS)",
        status = CapabilityStatus.COMING_SOON,
        detailMessage = "Real-time TPMS telemetry requires OBD2 Bluetooth gateway integration.",
        iconName = "tire"
    ),
    val fuelTelemetry: FeatureCapability = FeatureCapability(
        featureId = "fuel_telemetry",
        title = "Fuel & Range Telemetry",
        status = CapabilityStatus.COMING_SOON,
        detailMessage = "Direct fuel tank sensor reading requires vehicle CAN bus bridge.",
        iconName = "fuel"
    ),
    val doorLockControl: FeatureCapability = FeatureCapability(
        featureId = "door_lock",
        title = "Remote Door Lock/Unlock",
        status = CapabilityStatus.NOT_SUPPORTED_BY_VEHICLE,
        detailMessage = "Remote keyless locking is restricted to physical keyfob and OEM connected app.",
        iconName = "lock"
    ),
    val parkingSensors: FeatureCapability = FeatureCapability(
        featureId = "parking_sensors",
        title = "Reverse Parking Assist",
        status = CapabilityStatus.NOT_SUPPORTED_BY_VEHICLE,
        detailMessage = "Vehicle ultrasonic sensor bus is not exposed to third-party Android Auto companion apps. Factory sensors and camera view remain active on OEM infotainment screen.",
        iconName = "parking"
    )
) {
    val allCapabilities: List<FeatureCapability>
        get() = listOf(
            climateControl,
            camera360,
            tyrePressureMonitoring,
            fuelTelemetry,
            doorLockControl,
            parkingSensors
        )
}
