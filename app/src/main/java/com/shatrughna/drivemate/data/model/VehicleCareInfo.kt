package com.shatrughna.drivemate.data.model

import java.util.Locale

/**
 * Maintenance and vehicle care information for the configured vehicle.
 * Honest data presentation: Fuel percentage is null/unavailable unless
 * read from real OBD-II / CAN-bus vehicle telemetry.
 * High-precision double odometer prevents fractional truncation.
 */
data class VehicleCareInfo(
    val currentOdometerKm: Double = Double.NaN,
    val nextServiceTargetKm: Int = 15000,
    val isServiceScheduleAvailable: Boolean = false,
    val isFuelReminderEnabled: Boolean = false,
    val estimatedFuelLevelPercent: Int? = null,
    val daysUntilServiceEstimate: Int? = null
) {
    val isFuelLevelAvailable: Boolean
        get() = estimatedFuelLevelPercent != null

    val fuelStatusDescription: String
        get() = estimatedFuelLevelPercent?.let { "$it%" } ?: "Not available"

    val distanceRemainingKm: Int
        get() = if (currentOdometerKm.isFinite()) (nextServiceTargetKm - currentOdometerKm.toInt()).coerceAtLeast(0) else 0

    val formattedOdometer: String
        get() = if (currentOdometerKm.isFinite()) String.format(Locale.US, "%,.1f km", currentOdometerKm) else "Odometer unavailable"

    val isServiceDueSoon: Boolean
        get() = isServiceScheduleAvailable && currentOdometerKm.isFinite() && distanceRemainingKm <= 1500

    val serviceStatusDescription: String
        get() = if (!isServiceScheduleAvailable || !currentOdometerKm.isFinite()) {
            "Service interval unavailable"
        } else if (isServiceDueSoon) {
            "Service due soon in $distanceRemainingKm km"
        } else {
            "All systems normal • Next service in $distanceRemainingKm km"
        }
}
