package com.shatrughna.drivemate.care

import com.shatrughna.drivemate.data.model.DriveMateSettings
import com.shatrughna.drivemate.data.model.VehicleCareInfo

interface VehicleCareManager {
    fun getVehicleCareInfo(settings: DriveMateSettings): VehicleCareInfo
    fun generateCareReminderPhrase(settings: DriveMateSettings): String?
}

class VehicleCareManagerImpl : VehicleCareManager {

    override fun getVehicleCareInfo(settings: DriveMateSettings): VehicleCareInfo {
        return VehicleCareInfo(
            currentOdometerKm = settings.odometerKm ?: Double.NaN,
            nextServiceTargetKm = settings.nextServiceKm,
            isServiceScheduleAvailable = settings.serviceTargetConfigured,
            isFuelReminderEnabled = settings.fuelReminderEnabled,
            estimatedFuelLevelPercent = null // Real vehicle telemetry unavailable without direct CAN / OBD integration
        )
    }

    override fun generateCareReminderPhrase(settings: DriveMateSettings): String? {
        val careInfo = getVehicleCareInfo(settings)

        if (careInfo.isServiceDueSoon && careInfo.currentOdometerKm.isFinite()) {
            return "Please note: Your vehicle service is due in ${careInfo.distanceRemainingKm} km."
        }

        // Only trigger fuel reminder if REAL fuel telemetry is present
        val fuel = careInfo.estimatedFuelLevelPercent
        if (settings.fuelReminderEnabled && fuel != null && fuel < 25) {
            return "Reminder: Fuel level is low at $fuel%. Please consider refueling soon."
        }

        return null
    }
}
