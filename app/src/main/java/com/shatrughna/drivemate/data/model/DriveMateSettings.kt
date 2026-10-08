package com.shatrughna.drivemate.data.model

import java.util.Locale

/**
 * Persisted application settings for DriveMate (Production Ready).
 */
data class DriveMateSettings(
    val driverName: String = "",
    val vehicleBrand: String = "",
    val vehicleModel: String = "",
    val vehicleVariant: String = "",
    val vehicleRegistrationNumber: String = "",
    val vehiclePhotoUri: String? = null,
    val greetingEnabled: Boolean = true,
    val autoGreetingOnAndroidAuto: Boolean = false,
    val greetingStyle: GreetingStyle = GreetingStyle.NORMAL,
    val customGreetingTemplate: String = "Good {timeOfDay}, {name}. Welcome to your vehicle.",
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val languageTag: String = "en-IN",
    val voiceName: String? = null,
    val autoMonitorBluetooth: Boolean = true,
    val targetBluetoothName: String = "",
    // Dynamic Location & Weather
    val includeWeatherInGreeting: Boolean = true,
    val autoDetectLocation: Boolean = true,
    val weatherCityName: String = "",
    val weatherLatitude: Double = 0.0,
    val weatherLongitude: Double = 0.0,
    // High-Precision Odometer & Service Tracking (Strictly decoupled)
    val manualOdometerKm: Double? = null,
    val vehicleOdometerKm: Double? = null,
    val gpsTripDistanceKm: Double = 0.0,
    /** Legacy/manual field retained for persisted callers; null means not configured. */
    val odometerKm: Double? = null,
    val nextServiceKm: Int = 15000,
    val serviceTargetConfigured: Boolean = false,
    val fuelReminderEnabled: Boolean = false,
    val homeAddress: String = "",
    val officeAddress: String = "",
    // Smart Companion & Safety Features
    val lastParkedLatitude: Double? = null,
    val lastParkedLongitude: Double? = null,
    val lastParkedAddress: String? = null,
    val lastParkedTimestampMillis: Long? = null,
    val driverFatigueAlertEnabled: Boolean = true,
    val averageMileageKmpl: Float? = null,
    val preferredMusicApp: String = "Spotify",
    // Hands-Free Voice Assistant & Wake Word Settings
    val voiceAssistantEnabled: Boolean = true,
    val heyDriveMateEnabled: Boolean = false,
    val wakeWordSensitivity: Float = 0.5f,
    val voiceAssistantLanguage: String = "auto",
    // V5 Explicit Demo Data Mode
    val isDemoModeEnabled: Boolean = false
) {
    val fullVehicleName: String
        get() = listOf(vehicleBrand, vehicleModel, vehicleVariant)
            .filter(String::isNotBlank)
            .joinToString(" ")
            .ifBlank { "Connected vehicle" }

    val effectiveOdometerKm: Double?
        get() = vehicleOdometerKm ?: manualOdometerKm ?: odometerKm

    val formattedOdometer: String
        get() = effectiveOdometerKm?.let { String.format(Locale.US, "%,.1f km", it) } ?: "Odometer unavailable"

    val normalizedRegistrationNumber: String
        get() = normalizeRegistration(vehicleRegistrationNumber)

    val remainingServiceKm: Double?
        get() = if (serviceTargetConfigured) effectiveOdometerKm?.let { (nextServiceKm - it).coerceAtLeast(0.0) } else null

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
