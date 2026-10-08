package com.shatrughna.drivemate

import com.shatrughna.drivemate.data.model.DriveMateSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleProfileTest {

    @Test
    fun testRegistrationNumberNormalization() {
        // Standard Maharashtra RTO format
        assertEquals("MH 28 BW 1624", DriveMateSettings.normalizeRegistration("MH 28 BW 1624"))
        // Lowercase without spaces
        assertEquals("MH 28 BW 1624", DriveMateSettings.normalizeRegistration("mh28bw1624"))
        // Irregular spacing and lowercase
        assertEquals("MH 28 BW 1624", DriveMateSettings.normalizeRegistration("  Mh  28  bw 1624  "))
        // Plate with special characters or hyphens
        assertEquals("MH 28 BW 1624", DriveMateSettings.normalizeRegistration("MH-28-BW-1624"))
        // Non-series plate (e.g. MH 12 1234)
        assertEquals("MH 12 1234", DriveMateSettings.normalizeRegistration("mh121234"))
    }

    @Test
    fun testDoubleOdometerPrecisionAccumulation() {
        val initialSettings = DriveMateSettings(
            odometerKm = 12500.0,
            nextServiceKm = 15000,
            serviceTargetConfigured = true
        )
        assertEquals("12,500.0 km", initialSettings.formattedOdometer)
        assertEquals(2500.0, initialSettings.remainingServiceKm!!, 0.001)

        // After a 0.8 km trip:
        val afterShortTrip = initialSettings.copy(odometerKm = initialSettings.odometerKm!! + 0.8)
        assertEquals(12500.8, afterShortTrip.odometerKm!!, 0.001)
        assertEquals("12,500.8 km", afterShortTrip.formattedOdometer)
        assertEquals(2499.2, afterShortTrip.remainingServiceKm!!, 0.001)

        // After another 1.4 km trip:
        val afterSecondTrip = afterShortTrip.copy(odometerKm = afterShortTrip.odometerKm!! + 1.4)
        assertEquals(12502.2, afterSecondTrip.odometerKm!!, 0.001)
        assertEquals("12,502.2 km", afterSecondTrip.formattedOdometer)
    }

    @Test
    fun freshProfileDoesNotInventVehicleOrOdometerData() {
        val settings = DriveMateSettings()
        assertNull(settings.odometerKm)
        assertEquals("Connected vehicle", settings.fullVehicleName)
        assertEquals("Odometer unavailable", settings.formattedOdometer)
        assertNull(settings.remainingServiceKm)
    }

    @Test
    fun testFullVehicleNameGeneration() {
        val settings = DriveMateSettings(
            vehicleBrand = "TATA",
            vehicleModel = "Nexon",
            vehicleVariant = "Creative+ S"
        )
        assertEquals("TATA Nexon Creative+ S", settings.fullVehicleName)
    }
}
