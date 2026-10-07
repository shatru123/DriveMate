package com.shatrughna.drivemate.data.model

import java.util.Locale

/**
 * Persisted application settings for DriveMate (Production Ready).
 */
data class DriveMateSettings(
    val driverName: String = "Shatrughna",
    val vehicleBrand: String = "TATA",
    val vehicleModel: String = "Nexon",
    val vehicleVariant: String = "Creative+ S",
    val vehicleRegistrationNumber: String = "MH 28 BW 1624",
    val vehiclePhotoUri: String? = null,
    val greetingEnabled: Boolean = true,
    val autoGreetingOnAndroidAuto: Boolean = false,
    val greetingStyle: GreetingStyle = GreetingStyle.NORMAL,
    val customGreetingTemplate: String = "Good {timeOfDay}, {name}. Welcome to your {brand} {model}.",
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val languageTag: String = "en-IN",
    val voiceName: String? = null,
    val autoMonitorBluetooth: Boolean = true,
    val targetBluetoothName: String = "Tata Nexon",
    // Dynamic Location & Weather
    val includeWeatherInGreeting: Boolean = true,
    val autoDetectLocation: Boolean = true,
    val weatherCityName: String = "",
    val weatherLatitude: Double = 0.0,
    val weatherLongitude: Double = 0.0,
    // High-Precision Odometer & Service Tracking (Strictly decoupled)
    val manualOdometerKm: Double = 12500.0,
    val vehicleOdometerKm: Double? = null,
    val gpsTripDistanceKm: Double = 0.0,
    val odometerKm: Double = vehicleOdometerKm ?: manualOdometerKm,
    val nextServiceKm: Int = 15000,
    val fuelReminderEnabled: Boolean = false,
    val homeAddress: String = "Home",
    val officeAddress: String = "Office",
    // Smart Companion & Safety Features
    val lastParkedLatitude: Double? = null,
    val lastParkedLongitude: Double? = null,
    val lastParkedAddress: String? = null,
    val lastParkedTimestampMillis: Long? = null,
    val driverFatigueAlertEnabled: Boolean = true,
    val averageMileageKmpl: Float = 16.5f,
    val preferredMusicApp: String = "Spotify",
    // Hands-Free Voice Assistant & Wake Word Settings
    val voiceAssistantEnabled: Boolean = true,
    val heyDriveMateEnabled: Boolean = true,
    val wakeWordSensitivity: Float = 0.5f,
    // V5 Explicit Demo Data Mode
    val isDemoModeEnabled: Boolean = false
) {
    val fullVehicleName: String
        get() = "$vehicleBrand $vehicleModel $vehicleVariant".trim()

    val effectiveOdometerKm: Double
        get() = vehicleOdometerKm ?: if (manualOdometerKm != 12500.0 && odometerKm == 12500.0) manualOdometerKm else odometerKm

    val formattedOdometer: String
        get() = String.format(Locale.US, "%,.1f km", effectiveOdometerKm)

    val normalizedRegistrationNumber: String
        get() = normalizeRegistration(vehicleRegistrationNumber)

    val remainingServiceKm: Double
        get() = (nextServiceKm.toDouble() - effectiveOdometerKm).coerceAtLeast(0.0)

    val hasParkedLocation: Boolean
        get() = lastParkedLatitude != null && lastParkedLongitude != null

    companion object {
        /**
         * Normalizes Indian vehicle registration plates into standard space-delimited format:
         * e.g., "mh28bw1624" or "MH 28 BW 1624" -> "MH 28 BW 1624".
         */
        fun normalizeRegistration(raw: String): String {
            val clean = raw.uppercase().replace(Regex("[^A-Z0-9]"), "")
            val regex = Regex("^([A-Z]{2})(\\d{2})([A-Z]{1,3})?(\\d{1,4})$")
            val match = regex.find(clean)
            return if (match != null) {
                val state = match.groupValues[1]
                val rto = match.groupValues[2]
                val series = match.groupValues[3]
                val num = match.groupValues[4]
                if (series.isNotBlank()) "$state $rto $series $num" else "$state $rto $num"
            } else {
                raw.trim().uppercase()
            }
        }
    }
}
