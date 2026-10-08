package com.shatrughna.drivemate.data.analytics

data class WeeklyDrivingMetric(
    val weekLabel: String,
    val distanceKm: Float,
    val tripCount: Int
)

data class MonthlyDrivingSummary(
    val monthName: String,
    val totalDistanceKm: Float,
    val totalTrips: Int,
    val totalDurationMinutes: Long,
    val avgSpeedKmh: Float?,
    val estimatedFuelConsumedLiters: Float?,
    val avgEcoScore: Int?,
    val weeklyMetrics: List<WeeklyDrivingMetric>
)
