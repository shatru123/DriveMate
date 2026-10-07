package com.shatrughna.drivemate.data.timeline

import java.util.UUID

enum class TimelineCategory {
    DRIVE,
    FUEL,
    MAINTENANCE,
    EXPENSE,
    DOCUMENT_EXPIRY,
    REMINDER
}

data class VehicleTimelineItem(
    val id: String = UUID.randomUUID().toString(),
    val category: TimelineCategory,
    val title: String,
    val subtitle: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val metricText: String? = null,
    val isAlert: Boolean = false,
    val details: String = ""
)
