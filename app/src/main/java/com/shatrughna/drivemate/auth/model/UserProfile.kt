package com.shatrughna.drivemate.auth.model

import com.shatrughna.drivemate.vehicle.model.VehicleProfile

/**
 * Domain representation of an authenticated DriveMate user account.
 */
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val profileImageUrl: String? = null,
    val voiceLanguage: String = "auto",
    val createdAtMillis: Long = System.currentTimeMillis()
)

/**
 * Authentication and session state for DriveMate.
 */
sealed interface AuthState {
    data object Loading : AuthState
    data object LoggedOut : AuthState
    data class LoggedIn(
        val user: UserProfile,
        val vehicle: VehicleProfile? = null
    ) : AuthState {
        val hasVehicle: Boolean get() = vehicle != null
    }
}
