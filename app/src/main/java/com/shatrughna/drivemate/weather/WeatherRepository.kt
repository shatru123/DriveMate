package com.shatrughna.drivemate.weather

import com.shatrughna.drivemate.data.model.WeatherInfo
import com.shatrughna.drivemate.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

interface WeatherRepository {
    suspend fun getCurrentWeather(
        cityName: String = "Pune",
        latitude: Double = 18.5204,
        longitude: Double = 73.8567,
        forceRefresh: Boolean = false
    ): WeatherInfo

    fun mapWmoCodeToCondition(code: Int): String
}

class OpenMeteoWeatherRepository : WeatherRepository {

    private var cachedWeather: WeatherInfo? = null
    private var lastFetchTimestamp: Long = 0L
    private val cacheDurationMillis = TimeUnit.MINUTES.toMillis(30)

    override fun mapWmoCodeToCondition(code: Int): String {
        return when (code) {
            0 -> "Clear"
            1 -> "Mainly clear"
            2 -> "Partly cloudy"
            3 -> "Overcast"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63 -> "Rainy"
            65 -> "Heavy rain"
            71, 73, 75 -> "Snowy"
            80, 81, 82 -> "Rain showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Pleasant"
        }
    }

    override suspend fun getCurrentWeather(
        cityName: String,
        latitude: Double,
        longitude: Double,
        forceRefresh: Boolean
    ): WeatherInfo = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cached = cachedWeather

        if (!forceRefresh && cached != null && (now - lastFetchTimestamp) < cacheDurationMillis) {
            AppLogger.d(AppLogger.Tag.APP, "Returning cached weather for ${cached.cityName}: ${cached.displayTemperature}")
            return@withContext cached
        }

        if (cityName.isBlank()) {
            AppLogger.d(AppLogger.Tag.APP, "City name is blank, skipping network weather fetch")
            return@withContext cached ?: WeatherInfo(
                temperatureCelsius = 24.0f,
                weatherCode = 0,
                conditionText = "Clear",
                cityName = "Local",
                isFetchedFromNetwork = false,
                timestampMillis = now
            )
        }

        try {
            val endpoint = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,weather_code"
            AppLogger.i(AppLogger.Tag.APP, "Fetching live weather from Open-Meteo: $endpoint")

            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 2000
                readTimeout = 2000
                setRequestProperty("User-Agent", "DriveMate-Android/2.0")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val responseText = reader.use { it.readText() }
                connection.disconnect()

                val json = JSONObject(responseText)
                val current = json.getJSONObject("current")
                val temp = current.getDouble("temperature_2m").toFloat()
                val code = current.getInt("weather_code")
                val condition = mapWmoCodeToCondition(code)

                val result = WeatherInfo(
                    temperatureCelsius = temp,
                    weatherCode = code,
                    conditionText = condition,
                    cityName = cityName,
                    isFetchedFromNetwork = true,
                    timestampMillis = now
                )

                cachedWeather = result
                lastFetchTimestamp = now
                AppLogger.i(AppLogger.Tag.APP, "Weather successfully updated: ${result.speechFormattedDescription}")
                return@withContext result
            } else {
                AppLogger.w(AppLogger.Tag.APP, "Open-Meteo returned HTTP ${connection.responseCode}")
            }
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tag.APP, "Could not fetch weather from Open-Meteo (network unavailable or timeout): ${e.message}")
        }

        // Return cached or fallback if offline
        return@withContext cached ?: WeatherInfo(
            temperatureCelsius = 24.0f,
            weatherCode = 0,
            conditionText = "Clear",
            cityName = cityName,
            isFetchedFromNetwork = false,
            timestampMillis = now
        )
    }
}
