package com.shatrughna.drivemate.data.model

/**
 * Persisted application settings for DriveMate (Production Ready).
 */
data class DriveMateSettings(
    val driverName: String = "Shatrughna",
    val vehicleBrand: String = "TATA",
    val vehicleModel: String = "Nexon",
    val vehicleVariant: String = "Creative+ S",
    val greetingEnabled: Boolean = true,
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
    val odometerKm: Int = 12500,
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
    val wakeWordSensitivity: Float = 0.5f
) {
    val fullVehicleName: String
        get() = "$vehicleBrand $vehicleModel $vehicleVariant".trim()

    val hasParkedLocation: Boolean
        get() = lastParkedLatitude != null && lastParkedLongitude != null
}
