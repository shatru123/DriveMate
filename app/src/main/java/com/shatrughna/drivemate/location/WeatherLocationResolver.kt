package com.shatrughna.drivemate.location

import com.shatrughna.drivemate.data.model.DriveMateSettings
import com.shatrughna.drivemate.util.AppLogger

/**
 * Origin of the resolved geographic location.
 */
enum class LocationSource {
    CURRENT_GPS,
    CACHED_DEVICE,
    USER_CONFIGURED,
    UNAVAILABLE
}

/**
 * Unified representation of a resolved geographic location.
 */
data class ResolvedLocation(
    val latitude: Double,
    val longitude: Double,
    val displayName: String?,
    val isAvailable: Boolean,
    val source: LocationSource
) {
    companion object {
        fun unavailable(configuredCity: String? = null): ResolvedLocation {
            val hasConfigured = !configuredCity.isNullOrBlank()
            return ResolvedLocation(
                latitude = 0.0,
                longitude = 0.0,
                displayName = if (hasConfigured) configuredCity else "Location unavailable",
                isAvailable = hasConfigured,
                source = if (hasConfigured) LocationSource.USER_CONFIGURED else LocationSource.UNAVAILABLE
            )
        }
    }
}

/**
 * Centralized location resolver for DriveMate.
 * Prioritizes:
 * 1. Current fresh GPS device location
 * 2. Fresh last-known device location
 * 3. User-configured location in settings (only if explicitly non-blank)
 * 4. Honest UNAVAILABLE (no implicit fallback to Pune or fake coordinates)
 */
interface WeatherLocationResolver {
    suspend fun resolveLocation(settings: DriveMateSettings): ResolvedLocation
}

class WeatherLocationResolverImpl(
    private val locationProvider: DeviceLocationProvider?
) : WeatherLocationResolver {

    override suspend fun resolveLocation(settings: DriveMateSettings): ResolvedLocation {
        // Step 1 & 2: Check device GPS/network location if autoDetect is enabled and permissions granted
        if (settings.autoDetectLocation && locationProvider != null && locationProvider.hasLocationPermission()) {
            try {
                val devLoc = locationProvider.getCurrentLocation()
                if (devLoc != null) {
                    AppLogger.d(
                        AppLogger.Tag.APP,
                        "WeatherLocationResolver: Resolved device location -> ${devLoc.cityName ?: "Coordinates"} (${devLoc.latitude}, ${devLoc.longitude})"
                    )
                    return ResolvedLocation(
                        latitude = devLoc.latitude,
                        longitude = devLoc.longitude,
                        displayName = devLoc.cityName ?: "${String.format("%.4f", devLoc.latitude)}, ${String.format("%.4f", devLoc.longitude)}",
                        isAvailable = true,
                        source = LocationSource.CURRENT_GPS
                    )
                }
            } catch (e: Exception) {
                AppLogger.w(AppLogger.Tag.APP, "WeatherLocationResolver: Error querying device location: ${e.message}")
            }
        }

        // Step 3: User-configured location (only if user explicitly specified a non-blank city name)
        val configuredCity = settings.weatherCityName.trim()
        if (configuredCity.isNotBlank()) {
            AppLogger.d(
                AppLogger.Tag.APP,
                "WeatherLocationResolver: Device location unavailable, using user-configured city: $configuredCity"
            )
            return ResolvedLocation(
                latitude = settings.weatherLatitude,
                longitude = settings.weatherLongitude,
                displayName = configuredCity,
                isAvailable = true,
                source = LocationSource.USER_CONFIGURED
            )
        }

        // Step 4: Honest Unavailable (zero fake Pune fallback)
        AppLogger.w(AppLogger.Tag.APP, "WeatherLocationResolver: No device or configured location available.")
        return ResolvedLocation.unavailable()
    }
}
