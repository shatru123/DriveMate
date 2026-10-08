package com.shatrughna.drivemate.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.shatrughna.drivemate.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.coroutines.resume

data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val cityName: String?,
    val accuracyMeters: Float? = null,
    val speedKmh: Float? = null,
    val timestampMillis: Long = System.currentTimeMillis()
)

interface DeviceLocationProvider {
    fun hasLocationPermission(): Boolean
    suspend fun getCurrentLocation(): DeviceLocation?
}

class DeviceLocationProviderImpl(
    private val context: Context
) : DeviceLocationProvider {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    override fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): DeviceLocation? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            AppLogger.d(AppLogger.Tag.APP, "Location permission not granted. Skipping device location detection.")
            return@withContext null
        }

        val lm = locationManager ?: return@withContext null

        try {
            // Step 1: Check cached last-known locations first (fastest, no battery drain)
            val providers = lm.getProviders(true)
            var bestLocation: Location? = null

            for (provider in providers) {
                val loc = lm.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || loc.time > bestLocation.time) {
                    bestLocation = loc
                }
            }

            // If last known location is fresh (within 15 minutes), use it
            val now = System.currentTimeMillis()
            if (bestLocation != null && (now - bestLocation.time) < 15 * 60 * 1000) {
                AppLogger.d(AppLogger.Tag.APP, "Using fresh last-known location: ${bestLocation.latitude}, ${bestLocation.longitude}")
                val cityName = resolveCityName(bestLocation.latitude, bestLocation.longitude)
                return@withContext bestLocation.toDeviceLocation(cityName)
            }

            // Step 2: Request fresh one-shot location on API 30+ with 2.5s timeout
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val freshLoc = withTimeoutOrNull(2500L) {
                    suspendCancellableCoroutine<Location?> { continuation ->
                        val executor = Executors.newSingleThreadExecutor()
                        val cancellationSignal = android.os.CancellationSignal()

                        continuation.invokeOnCancellation {
                            cancellationSignal.cancel()
                            executor.shutdown()
                        }

                        val provider = if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            LocationManager.GPS_PROVIDER
                        } else {
                            LocationManager.NETWORK_PROVIDER
                        }

                        lm.getCurrentLocation(
                            provider,
                            cancellationSignal,
                            executor
                        ) { location ->
                            executor.shutdown()
                            if (continuation.isActive) {
                                continuation.resume(location)
                            }
                        }
                    }
                }

                if (freshLoc != null) {
                    AppLogger.d(AppLogger.Tag.APP, "Acquired fresh device location: ${freshLoc.latitude}, ${freshLoc.longitude}")
                    val cityName = resolveCityName(freshLoc.latitude, freshLoc.longitude)
                    return@withContext freshLoc.toDeviceLocation(cityName)
                }
            }

            // Fallback to any best last-known location if fresh query timed out
            if (bestLocation != null) {
                AppLogger.d(AppLogger.Tag.APP, "Using available last-known location: ${bestLocation.latitude}, ${bestLocation.longitude}")
                val cityName = resolveCityName(bestLocation.latitude, bestLocation.longitude)
                return@withContext bestLocation.toDeviceLocation(cityName)
            }
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tag.APP, "Error retrieving device location: ${e.message}")
        }

        return@withContext null
    }

    private fun Location.toDeviceLocation(cityName: String?): DeviceLocation = DeviceLocation(
        latitude = latitude,
        longitude = longitude,
        cityName = cityName,
        accuracyMeters = if (hasAccuracy()) accuracy else null,
        speedKmh = if (hasSpeed() && speed >= 0f) speed * 3.6f else null,
        timestampMillis = time
    )

    private suspend fun resolveCityName(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) {
            return@withContext null
        }

        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                withTimeoutOrNull(1500L) {
                    suspendCancellableCoroutine<String?> { continuation ->
                        geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                            val resolvedCity = addresses.firstOrNull()?.let { addr ->
                                addr.locality ?: addr.subAdminArea ?: addr.adminArea
                            }
                            if (continuation.isActive) {
                                continuation.resume(resolvedCity)
                            }
                        }
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.firstOrNull()?.let { addr ->
                    addr.locality ?: addr.subAdminArea ?: addr.adminArea
                }
            }
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tag.APP, "Geocoder reverse-lookup failed: ${e.message}")
            null
        }
    }
}
