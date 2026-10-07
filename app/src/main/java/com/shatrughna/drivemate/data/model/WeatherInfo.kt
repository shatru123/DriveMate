package com.shatrughna.drivemate.data.model

/**
 * Weather details for automotive greetings, dashboard, and Android Auto presentation.
 * Explicitly tracks availability to eliminate false weather data.
 */
data class WeatherInfo(
    val temperatureCelsius: Float = 0.0f,
    val weatherCode: Int = 0,
    val conditionText: String = "Clear",
    val cityName: String = "",
    val isFetchedFromNetwork: Boolean = false,
    val isAvailable: Boolean = true,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    val speechFormattedDescription: String
        get() = if (isAvailable) {
            val citySuffix = if (cityName.isNotBlank()) " in $cityName" else ""
            "${temperatureCelsius.toInt()}°C and ${conditionText.lowercase()}$citySuffix"
        } else {
            "Weather is currently unavailable"
        }

    val displayTemperature: String
        get() = if (isAvailable) "${temperatureCelsius.toInt()}°C" else "N/A"

    companion object {
        fun unavailable(cityName: String = ""): WeatherInfo {
            return WeatherInfo(
                temperatureCelsius = 0.0f,
                weatherCode = -1,
                conditionText = "Unavailable",
                cityName = cityName,
                isFetchedFromNetwork = false,
                isAvailable = false
            )
        }
    }
}
