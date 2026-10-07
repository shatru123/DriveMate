package com.shatrughna.drivemate.data.analytics

import com.shatrughna.drivemate.data.model.TripReport
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DrivingAnalyticsEngine {

    fun computeMonthlySummary(trips: List<TripReport>): MonthlyDrivingSummary {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val monthName = monthFormat.format(Date())

        val thisMonthTrips = trips.filter { trip ->
            calendar.timeInMillis = trip.startTimeMillis
            calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
        }

        // If no trips exist yet, return clean empty summary (No fabricated metrics)
        if (thisMonthTrips.isEmpty()) {
            return MonthlyDrivingSummary(
                monthName = monthName,
                totalDistanceKm = 0f,
                totalTrips = 0,
                totalDurationMinutes = 0L,
                avgSpeedKmh = 0f,
                estimatedFuelConsumedLiters = 0f,
                avgEcoScore = 0,
                weeklyMetrics = emptyList()
            )
        }

        val totalDist = thisMonthTrips.sumOf { it.distanceKm.toDouble() }.toFloat()
        val totalMinutes = thisMonthTrips.sumOf { it.durationMinutes }
        // Distance-weighted average speed
        val avgSpeed = if (totalDist > 0f) {
            (thisMonthTrips.sumOf { it.distanceKm.toDouble() * it.avgSpeedKmh.toDouble() } / totalDist).toFloat()
        } else {
            (thisMonthTrips.sumOf { it.avgSpeedKmh.toDouble() } / thisMonthTrips.size).toFloat()
        }
        val fuelLiters = thisMonthTrips.sumOf { it.fuelConsumedLiters.toDouble() }.toFloat()
        val avgEco = if (thisMonthTrips.isNotEmpty()) {
            (thisMonthTrips.sumOf { it.ecoScore } / thisMonthTrips.size)
        } else 90

        // Group into 4 weeks
        val weekBuckets = Array(4) { 0f }
        val weekCounts = Array(4) { 0 }
        for (trip in thisMonthTrips) {
            calendar.timeInMillis = trip.startTimeMillis
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            val weekIndex = ((dayOfMonth - 1) / 7).coerceIn(0, 3)
            weekBuckets[weekIndex] += trip.distanceKm
            weekCounts[weekIndex] += 1
        }

        val weeklyMetrics = listOf(
            WeeklyDrivingMetric("W1", weekBuckets[0], weekCounts[0]),
            WeeklyDrivingMetric("W2", weekBuckets[1], weekCounts[1]),
            WeeklyDrivingMetric("W3", weekBuckets[2], weekCounts[2]),
            WeeklyDrivingMetric("W4", weekBuckets[3], weekCounts[3])
        )

        return MonthlyDrivingSummary(
            monthName = monthName,
            totalDistanceKm = totalDist,
            totalTrips = thisMonthTrips.size,
            totalDurationMinutes = totalMinutes,
            avgSpeedKmh = avgSpeed,
            estimatedFuelConsumedLiters = fuelLiters,
            avgEcoScore = avgEco,
            weeklyMetrics = weeklyMetrics
        )
    }
}
