package com.shatrughna.drivemate.data.model

import java.util.Locale

/**
 * Maintenance and vehicle care information tailored for Tata Nexon.
 * Honest data presentation: Fuel percentage is null/unavailable unless
 * read from real OBD-II / CAN-bus vehicle telemetry.
 * High-precision double odometer prevents fractional truncation.
 */
data class VehicleCareInfo(
    val currentOdometerKm: Double = 12500.0,
    val nextServiceTargetKm: Int = 15000,
    val isFuelReminderEnabled: Boolean = false,
    val estimatedFuelLevelPercent: Int? = null,
    val daysUntilServiceEstimate: Int = 45
) {
    val isFuelLevelAvailable: Boolean
        get() = estimatedFuelLevelPercent != null

    val fuelStatusDescription: String
        get() = estimatedFuelLevelPercent?.let { "$it%" } ?: "Not available"

    val distanceRemainingKm: Int
        get() = (nextServiceTargetKm - currentOdometerKm.toInt()).coerceAtLeast(0)

    val formattedOdometer: String
        get() = String.format(Locale.US, "%,.1f km", currentOdometerKm)

    val isServiceDueSoon: Boolean
        get() = distanceRemainingKm <= 1500

    val serviceStatusDescription: String
        get() = if (isServiceDueSoon) {
            "Service due soon in $distanceRemainingKm km"
        } else {
            "All systems normal • Next service in $distanceRemainingKm km"
        }
}
