package com.shatrughna.drivemate

import com.shatrughna.drivemate.data.model.DriveMateSettings
import com.shatrughna.drivemate.location.DeviceLocation
import com.shatrughna.drivemate.location.DeviceLocationProvider
import com.shatrughna.drivemate.location.LocationSource
import com.shatrughna.drivemate.location.WeatherLocationResolverImpl
import com.shatrughna.drivemate.weather.WeatherCacheKey
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherLocationResolverTest {

    private class FakeLocationProvider(
        var hasPermission: Boolean = true,
        var mockLocation: DeviceLocation? = null
    ) : DeviceLocationProvider {
        override fun hasLocationPermission(): Boolean = hasPermission
        override suspend fun getCurrentLocation(): DeviceLocation? = mockLocation
    }

    @Test
    fun testResolveLocation_withDeviceGps_returnsCurrentGps() = runTest {
        val fakeProvider = FakeLocationProvider(
            hasPermission = true,
            mockLocation = DeviceLocation(19.0760, 72.8777, "Mumbai")
        )
        val resolver = WeatherLocationResolverImpl(fakeProvider)
        val settings = DriveMateSettings(
            autoDetectLocation = true,
            weatherCityName = "Pune" // Even if user configured Pune, GPS takes priority
        )

        val resolved = resolver.resolveLocation(settings)
        assertEquals(LocationSource.CURRENT_GPS, resolved.source)
        assertTrue(resolved.isAvailable)
        assertEquals(19.0760, resolved.latitude, 0.0001)
        assertEquals(72.8777, resolved.longitude, 0.0001)
        assertEquals("Mumbai", resolved.displayName)
    }

    @Test
    fun testResolveLocation_permissionDenied_withUserConfiguredCity_returnsUserConfigured() = runTest {
        val fakeProvider = FakeLocationProvider(
            hasPermission = false,
            mockLocation = null
        )
        val resolver = WeatherLocationResolverImpl(fakeProvider)
        val settings = DriveMateSettings(
            autoDetectLocation = true,
            weatherCityName = "Mumbai",
            weatherLatitude = 19.0760,
            weatherLongitude = 72.8777
        )

        val resolved = resolver.resolveLocation(settings)
        assertEquals(LocationSource.USER_CONFIGURED, resolved.source)
        assertTrue(resolved.isAvailable)
        assertEquals("Mumbai", resolved.displayName)
        assertEquals(19.0760, resolved.latitude, 0.0001)
    }

    @Test
    fun testResolveLocation_autoDetectDisabled_usesUserConfigured() = runTest {
        val fakeProvider = FakeLocationProvider(
            hasPermission = true,
            mockLocation = DeviceLocation(12.9716, 77.5946, "Bengaluru")
        )
        val resolver = WeatherLocationResolverImpl(fakeProvider)
        val settings = DriveMateSettings(
            autoDetectLocation = false,
            weatherCityName = "Delhi",
            weatherLatitude = 28.6139,
            weatherLongitude = 77.2090
        )

        val resolved = resolver.resolveLocation(settings)
        assertEquals(LocationSource.USER_CONFIGURED, resolved.source)
        assertEquals("Delhi", resolved.displayName)
    }

    @Test
    fun testResolveLocation_noPermission_noConfiguredCity_returnsHonestUnavailable() = runTest {
        val fakeProvider = FakeLocationProvider(
            hasPermission = false,
            mockLocation = null
        )
        val resolver = WeatherLocationResolverImpl(fakeProvider)
        val settings = DriveMateSettings(
            autoDetectLocation = true,
            weatherCityName = "" // User hasn't configured a city
        )

        val resolved = resolver.resolveLocation(settings)
        assertEquals(LocationSource.UNAVAILABLE, resolved.source)
        assertFalse(resolved.isAvailable)
        assertEquals("Location unavailable", resolved.displayName)
        // Ensure no fabricated fallback coordinates
        assertEquals(0.0, resolved.latitude, 0.0001)
        assertEquals(0.0, resolved.longitude, 0.0001)
    }

    @Test
    fun testWeatherCacheKey_segregatesPuneAndMumbai() {
        val puneKey = WeatherCacheKey.fromCoordinates(18.5204, 73.8567)
        val mumbaiKey = WeatherCacheKey.fromCoordinates(19.0760, 72.8777)

        assertNotEquals(puneKey, mumbaiKey)
        assertEquals(1852L, puneKey.latBucket)
        assertEquals(7386L, puneKey.lonBucket)
        assertEquals(1908L, mumbaiKey.latBucket)
        assertEquals(7288L, mumbaiKey.lonBucket)
    }

    @Test
    fun testWeatherCacheKey_groupsCloseLocations() {
        // Points ~200 meters apart map to the same bucket
        val point1 = WeatherCacheKey.fromCoordinates(18.52041, 73.85671)
        val point2 = WeatherCacheKey.fromCoordinates(18.52044, 73.85674)
        assertEquals(point1, point2)
    }
}
