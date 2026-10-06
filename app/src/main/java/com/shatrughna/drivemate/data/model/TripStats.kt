package com.shatrughna.drivemate.data.model

/**
 * Real-time driving statistics and cumulative daily summaries.
 */
data class TripStats(
    val activeTripDurationSeconds: Long = 0L,
    val activeTripDistanceKm: Float = 0.0f,
    val todayTripsCount: Int = 0,
    val todayTotalDistanceKm: Float = 0.0f,
    val todayTotalDurationMinutes: Long = 0L
) {
    val formattedActiveDuration: String
        get() {
            val mins = activeTripDurationSeconds / 60
            val secs = activeTripDurationSeconds % 60
            return String.format("%02d:%02d", mins, secs)
        }

    val formattedActiveDistance: String
        get() = String.format("%.1f km", activeTripDistanceKm)

    val formattedTodayDistance: String
        get() = String.format("%.1f km", todayTotalDistanceKm)
}
