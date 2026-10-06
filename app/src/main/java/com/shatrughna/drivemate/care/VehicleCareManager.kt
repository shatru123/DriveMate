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
            currentOdometerKm = settings.odometerKm,
            nextServiceTargetKm = settings.nextServiceKm,
            isFuelReminderEnabled = settings.fuelReminderEnabled,
            estimatedFuelLevelPercent = 68
        )
    }

    override fun generateCareReminderPhrase(settings: DriveMateSettings): String? {
        val careInfo = getVehicleCareInfo(settings)

        if (careInfo.isServiceDueSoon) {
            return "Please note: Your Tata Nexon service is due in ${careInfo.distanceRemainingKm} km."
        }

        if (settings.fuelReminderEnabled && careInfo.estimatedFuelLevelPercent < 25) {
            return "Reminder: Fuel level is low. Please consider refueling soon."
        }

        return null
    }
}
