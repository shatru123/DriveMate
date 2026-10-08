package com.shatrughna.drivemate.vehicle.model

import java.util.UUID

/**
 * Domain representation of a user's single registered vehicle.
 * Every DriveMate user account is linked to exactly one vehicle in V10.4.
 */
data class VehicleProfile(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val make: String, // Brand e.g. TATA, Hyundai, Maruti
    val model: String, // Model e.g. Nexon, Creta, Swift
    val variant: String = "", // e.g. Creative+ S, SX(O), ZXi
    val year: Int? = null,
    val registrationNumber: String = "",
    val fuelType: String = "Petrol",
    val transmission: String = "Manual",
    val nickname: String = "",
    val currentOdometer: Double? = null,
    val photoUri: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val fullDisplayName: String
        get() = listOf(make, model, variant)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "My Car" }
}
