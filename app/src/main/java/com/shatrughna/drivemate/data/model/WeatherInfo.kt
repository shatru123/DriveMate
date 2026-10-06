package com.shatrughna.drivemate.data.model

/**
 * Weather details for automotive greetings and dashboard presentation.
 */
data class WeatherInfo(
    val temperatureCelsius: Float = 24.0f,
    val weatherCode: Int = 0,
    val conditionText: String = "Clear",
    val cityName: String = "Pune",
    val isFetchedFromNetwork: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    val speechFormattedDescription: String
        get() = "${temperatureCelsius.toInt()}°C and ${conditionText.lowercase()} in $cityName"

    val displayTemperature: String
        get() = "${temperatureCelsius.toInt()}°C"
}
