package com.shatrughna.drivemate.vehicle.repository

import com.shatrughna.drivemate.vehicle.model.VehicleProfile
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for managing the user's single registered vehicle.
 */
interface VehicleRepository {
    val currentVehicle: StateFlow<VehicleProfile?>

    suspend fun loadVehicleForUser(userId: String): VehicleProfile?
    suspend fun saveVehicle(vehicle: VehicleProfile): Result<VehicleProfile>
    suspend fun updateVehicle(vehicle: VehicleProfile): Result<VehicleProfile>
    suspend fun clearVehicleContext()
}
