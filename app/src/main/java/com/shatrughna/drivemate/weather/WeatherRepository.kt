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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

/**
 * Normalized geo-bucket key for location-aware weather caching.
 * Rounded to 2 decimal places (~1.1 km resolution).
 */
data class WeatherCacheKey(
    val latBucket: Long,
    val lonBucket: Long
) {
    companion object {
        fun fromCoordinates(latitude: Double, longitude: Double): WeatherCacheKey {
            return WeatherCacheKey(
                latBucket = (latitude * 100.0).roundToLong(),
                lonBucket = (longitude * 100.0).roundToLong()
            )
        }
    }
}

private data class CachedWeatherEntry(
    val weather: WeatherInfo,
    val timestampMillis: Long
)

interface WeatherRepository {
    suspend fun getCurrentWeather(
        cityName: String,
        latitude: Double,
        longitude: Double,
        forceRefresh: Boolean = false
    ): WeatherInfo

    fun mapWmoCodeToCondition(code: Int): String
}

class OpenMeteoWeatherRepository : WeatherRepository {

    private val cache = ConcurrentHashMap<WeatherCacheKey, CachedWeatherEntry>()
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
        val cacheKey = WeatherCacheKey.fromCoordinates(latitude, longitude)
        val cachedEntry = cache[cacheKey]

        // 1. Check location-specific cache (fresh within 30 min)
        if (!forceRefresh && cachedEntry != null && (now - cachedEntry.timestampMillis) < cacheDurationMillis) {
            AppLogger.d(
                AppLogger.Tag.APP,
                "Returning location-cached weather for ${cachedEntry.weather.cityName.ifBlank { "Coordinates" }}: ${cachedEntry.weather.displayTemperature}"
            )
            return@withContext cachedEntry.weather
        }

        // If coordinates are invalid/zero and city is blank, fail honestly
        if (latitude == 0.0 && longitude == 0.0 && cityName.isBlank()) {
            AppLogger.d(AppLogger.Tag.APP, "Location is empty/unavailable, returning unavailable weather.")
            return@withContext WeatherInfo.unavailable()
        }

        // 2. Fetch live weather from Open-Meteo with bounded 2s timeout
        try {
            val endpoint = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,weather_code"
            AppLogger.i(AppLogger.Tag.APP, "Fetching live weather from Open-Meteo for ($latitude, $longitude): $endpoint")

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
                    isAvailable = true,
                    timestampMillis = now
                )

                cache[cacheKey] = CachedWeatherEntry(result, now)
                AppLogger.i(AppLogger.Tag.APP, "Weather successfully updated: ${result.speechFormattedDescription}")
                return@withContext result
            } else {
                AppLogger.w(AppLogger.Tag.APP, "Open-Meteo returned HTTP ${connection.responseCode}")
            }
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tag.APP, "Could not fetch weather from Open-Meteo: ${e.message}")
        }

        // 3. Fallback: If network failed, only return cache if it matches the SAME location
        if (cachedEntry != null) {
            AppLogger.i(AppLogger.Tag.APP, "Using stale cached weather for same location ($cacheKey) during network failure.")
            return@withContext cachedEntry.weather.copy(isFetchedFromNetwork = false)
        }

        // 4. Honest unavailable response: NEVER fabricate fake weather from a different city!
        return@withContext WeatherInfo.unavailable(cityName = cityName)
    }
}
