package com.shatrughna.drivemate.auth.repository

import com.shatrughna.drivemate.auth.model.AuthState
import com.shatrughna.drivemate.auth.model.UserProfile
import com.shatrughna.drivemate.vehicle.model.VehicleProfile
import kotlinx.coroutines.flow.StateFlow

/**
 * Clean repository abstraction for user authentication, session persistence, and profile lifecycle.
 */
interface AuthRepository {
    val authState: StateFlow<AuthState>
    val currentUser: StateFlow<UserProfile?>

    suspend fun signUp(name: String, email: String, password: String): Result<UserProfile>
    suspend fun login(email: String, password: String): Result<UserProfile>
    suspend fun logout()
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun updateProfile(
        name: String,
        profileImageUrl: String? = null,
        voiceLanguage: String? = null,
        clearProfileImage: Boolean = false
    ): Result<UserProfile>
    fun refreshVehicleContext(vehicle: VehicleProfile?)
}
