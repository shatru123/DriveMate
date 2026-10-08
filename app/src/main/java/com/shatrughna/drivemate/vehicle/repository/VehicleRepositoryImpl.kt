package com.shatrughna.drivemate.vehicle.repository

import android.content.Context
import com.shatrughna.drivemate.util.AppLogger
import com.shatrughna.drivemate.vehicle.model.VehicleProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class VehicleRepositoryImpl(
    private val context: Context? = null,
    private val filesDir: File = context?.filesDir ?: File(System.getProperty("java.io.tmpdir"), "drivemate_vehicle")
) : VehicleRepository {

    companion object {
        private const val USERS_DIR = "users"
        private const val VEHICLE_FILE = "vehicle.json"
    }

    private val _currentVehicle = MutableStateFlow<VehicleProfile?>(null)
    override val currentVehicle: StateFlow<VehicleProfile?> = _currentVehicle.asStateFlow()

    private fun getUserDir(userId: String): File {
        return File(File(filesDir, USERS_DIR), userId).apply {
            if (!exists()) mkdirs()
        }
    }

    private fun getVehicleFile(userId: String): File {
        return File(getUserDir(userId), VEHICLE_FILE)
    }

    override suspend fun loadVehicleForUser(userId: String): VehicleProfile? = withContext(Dispatchers.IO) {
        val file = getVehicleFile(userId)
        if (!file.exists()) {
            _currentVehicle.value = null
            return@withContext null
        }
        try {
            val jsonStr = file.readText()
            if (jsonStr.isBlank()) {
                _currentVehicle.value = null
                return@withContext null
            }
            val obj = JSONObject(jsonStr)
            val vehicle = VehicleProfile(
                id = obj.getString("id"),
                userId = obj.getString("userId"),
                make = obj.getString("make"),
                model = obj.getString("model"),
                variant = obj.optString("variant", ""),
                year = if (obj.has("year") && !obj.isNull("year")) obj.getInt("year") else null,
                registrationNumber = obj.optString("registrationNumber", ""),
                fuelType = obj.optString("fuelType", "Petrol"),
                transmission = obj.optString("transmission", "Manual"),
                nickname = obj.optString("nickname", ""),
                currentOdometer = if (obj.has("currentOdometer") && !obj.isNull("currentOdometer")) obj.getDouble("currentOdometer") else null,
                photoUri = obj.optString("photoUri").takeIf { it.isNotBlank() && it != "null" },
                createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis()),
                updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
            )
            _currentVehicle.value = vehicle
            AppLogger.i(AppLogger.Tag.SETTINGS, "VehicleRepository: Loaded vehicle ${vehicle.fullDisplayName} for user $userId")
            vehicle
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.SETTINGS, "VehicleRepository: Failed to load vehicle for user $userId", e)
            _currentVehicle.value = null
            null
        }
    }

    override suspend fun saveVehicle(vehicle: VehicleProfile): Result<VehicleProfile> = withContext(Dispatchers.IO) {
        val trimmedMake = vehicle.make.trim()
        val trimmedModel = vehicle.model.trim()

        if (trimmedMake.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your vehicle brand (make)."))
        }
        if (trimmedModel.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your vehicle model."))
        }

        val updatedVehicle = vehicle.copy(
            make = trimmedMake,
            model = trimmedModel,
            variant = vehicle.variant.trim(),
            registrationNumber = vehicle.registrationNumber.trim().uppercase(),
            updatedAtMillis = System.currentTimeMillis()
        )

        try {
            val file = getVehicleFile(updatedVehicle.userId)
            val obj = JSONObject().apply {
                put("id", updatedVehicle.id)
                put("userId", updatedVehicle.userId)
                put("make", updatedVehicle.make)
                put("model", updatedVehicle.model)
                put("variant", updatedVehicle.variant)
                put("year", updatedVehicle.year ?: JSONObject.NULL)
                put("registrationNumber", updatedVehicle.registrationNumber)
                put("fuelType", updatedVehicle.fuelType)
                put("transmission", updatedVehicle.transmission)
                put("nickname", updatedVehicle.nickname)
                put("currentOdometer", updatedVehicle.currentOdometer ?: JSONObject.NULL)
                put("photoUri", updatedVehicle.photoUri ?: JSONObject.NULL)
                put("createdAtMillis", updatedVehicle.createdAtMillis)
                put("updatedAtMillis", updatedVehicle.updatedAtMillis)
            }
            file.writeText(obj.toString(2))
            _currentVehicle.value = updatedVehicle
            AppLogger.i(AppLogger.Tag.SETTINGS, "VehicleRepository: Saved vehicle ${updatedVehicle.fullDisplayName} for user ${updatedVehicle.userId}")
            Result.success(updatedVehicle)
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.SETTINGS, "VehicleRepository: Failed to save vehicle", e)
            Result.failure(e)
        }
    }

    override suspend fun updateVehicle(vehicle: VehicleProfile): Result<VehicleProfile> {
        return saveVehicle(vehicle)
    }

    override suspend fun clearVehicleContext() {
        _currentVehicle.value = null
    }
}
