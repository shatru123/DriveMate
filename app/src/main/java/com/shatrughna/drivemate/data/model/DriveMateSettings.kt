package com.shatrughna.drivemate.data.model

/**
 * Persisted application settings for DriveMate (V2).
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
    // V2 Features
    val includeWeatherInGreeting: Boolean = true,
    val weatherCityName: String = "Pune",
    val weatherLatitude: Double = 18.5204,
    val weatherLongitude: Double = 73.8567,
    val odometerKm: Int = 12500,
    val nextServiceKm: Int = 15000,
    val fuelReminderEnabled: Boolean = true,
    val homeAddress: String = "Home",
    val officeAddress: String = "Office"
) {
    val fullVehicleName: String
        get() = "$vehicleBrand $vehicleModel $vehicleVariant".trim()
}
