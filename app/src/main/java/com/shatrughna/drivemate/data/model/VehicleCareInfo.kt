package com.shatrughna.drivemate.data.model

/**
 * Maintenance and vehicle care information tailored for Tata Nexon.
 */
data class VehicleCareInfo(
    val currentOdometerKm: Int = 12500,
    val nextServiceTargetKm: Int = 15000,
    val isFuelReminderEnabled: Boolean = true,
    val estimatedFuelLevelPercent: Int = 68,
    val daysUntilServiceEstimate: Int = 45
) {
    val distanceRemainingKm: Int
        get() = (nextServiceTargetKm - currentOdometerKm).coerceAtLeast(0)

    val isServiceDueSoon: Boolean
        get() = distanceRemainingKm <= 1500

    val serviceStatusDescription: String
        get() = if (isServiceDueSoon) {
            "Service due soon in $distanceRemainingKm km"
        } else {
            "All systems normal • Next service in $distanceRemainingKm km"
        }
}
