package com.shatrughna.drivemate.data.model

/**
 * Single GPS waypoint captured during an active driving session.
 */
data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Float? = null,
    val timestampMillis: Long = System.currentTimeMillis()
)
