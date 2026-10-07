package com.shatrughna.drivemate.data.model

import java.util.UUID

/**
 * Detailed driving trip summary including complete route trace, performance, and efficiency metrics.
 */
data class TripReport(
    val id: String = UUID.randomUUID().toString(),
    val startTimeMillis: Long = System.currentTimeMillis(),
    val endTimeMillis: Long = System.currentTimeMillis(),
    val distanceKm: Float = 0.0f,
    val durationMinutes: Long = 0L,
    val avgSpeedKmh: Float = 0.0f,
    val maxSpeedKmh: Float = 0.0f,
    val ecoScore: Int = 90, // 0 - 100 based on driving smoothness
    val startLocationName: String = "Origin",
    val endLocationName: String = "Destination",
    val routePoints: List<RoutePoint> = emptyList(),
    val fuelConsumedLiters: Float = 0.0f
) {
    val formattedDuration: String
        get() = when {
            durationMinutes < 60 -> "${durationMinutes}m"
            else -> "${durationMinutes / 60}h ${durationMinutes % 60}m"
        }

    val formattedDistance: String
        get() = String.format("%.1f km", distanceKm)

    val formattedAvgSpeed: String
        get() = String.format("%.0f km/h", avgSpeedKmh)
}
