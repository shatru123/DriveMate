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
    val avgSpeedKmh: Float? = null,
    val maxSpeedKmh: Float? = null,
    val ecoScore: Int? = null,
    val startLocationName: String = "Location unavailable",
    val endLocationName: String = "Location unavailable",
    val routePoints: List<RoutePoint> = emptyList(),
    val fuelConsumedLiters: Float? = null
) {
    val formattedDuration: String
        get() = when {
            durationMinutes < 60 -> "${durationMinutes}m"
            else -> "${durationMinutes / 60}h ${durationMinutes % 60}m"
        }

    val formattedDistance: String
        get() = String.format("%.1f km", distanceKm)

    val formattedAvgSpeed: String
        get() = avgSpeedKmh?.let { String.format("%.0f km/h", it) } ?: "Unavailable"
}
