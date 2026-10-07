package com.shatrughna.drivemate.data.model

import java.util.UUID
import java.util.concurrent.TimeUnit

enum class ServiceType(val displayName: String) {
    PERIODIC_SERVICE("Scheduled Periodic Service"),
    OIL_CHANGE("Engine Oil & Filter Change"),
    WHEEL_ALIGNMENT_BALANCING("Wheel Alignment & Balancing"),
    BRAKE_SERVICE("Brake Pad & Disc Inspection"),
    BATTERY_CHECK("Battery Health & Terminal Check"),
    COOLANT_FLUSH("Coolant & Radiator Flush"),
    GENERAL_REPAIR("General Mechanical Repair"),
    OTHER("Other Maintenance")
}

enum class ServiceUrgency {
    ON_SCHEDULE,
    DUE_SOON,
    DUE_NOW,
    OVERDUE
}

data class ServiceRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: ServiceType,
    val dateMillis: Long = System.currentTimeMillis(),
    val odometerKm: Double,
    val workshopName: String = "Tata Authorized Service Center",
    val cost: Double = 0.0,
    val invoiceNumber: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class ServiceSchedule(
    val intervalKm: Int = 15000,
    val intervalMonths: Int = 12,
    val lastServiceOdometerKm: Double = 15000.0,
    val lastServiceDateMillis: Long = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(180)
) {
    val nextServiceOdometerKm: Double get() = lastServiceOdometerKm + intervalKm

    val nextServiceDateMillis: Long
        get() = lastServiceDateMillis + TimeUnit.DAYS.toMillis(intervalMonths.toLong() * 30L)

    fun kmRemaining(currentOdometerKm: Double): Double {
        return (nextServiceOdometerKm - currentOdometerKm).coerceAtLeast(0.0)
    }

    fun daysRemaining(nowMillis: Long = System.currentTimeMillis()): Long {
        val diff = nextServiceDateMillis - nowMillis
        return TimeUnit.MILLISECONDS.toDays(diff).coerceAtLeast(0L)
    }

    fun kmProgress(currentOdometerKm: Double): Float {
        val drivenSinceLast = (currentOdometerKm - lastServiceOdometerKm).coerceAtLeast(0.0)
        return (drivenSinceLast / intervalKm).toFloat().coerceIn(0f, 1f)
    }

    fun urgency(currentOdometerKm: Double, nowMillis: Long = System.currentTimeMillis()): ServiceUrgency {
        val kmLeft = nextServiceOdometerKm - currentOdometerKm
        val daysLeft = TimeUnit.MILLISECONDS.toDays(nextServiceDateMillis - nowMillis)

        return when {
            kmLeft < 0 || daysLeft < 0 -> ServiceUrgency.OVERDUE
            kmLeft <= 1000 || daysLeft <= 15 -> ServiceUrgency.DUE_NOW
            kmLeft <= 2500 || daysLeft <= 45 -> ServiceUrgency.DUE_SOON
            else -> ServiceUrgency.ON_SCHEDULE
        }
    }
}
