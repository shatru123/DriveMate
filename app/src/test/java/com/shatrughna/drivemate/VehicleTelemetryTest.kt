package com.shatrughna.drivemate

import com.shatrughna.drivemate.core.telemetry.AndroidAutoTpmsProvider
import com.shatrughna.drivemate.core.telemetry.TelemetryAvailability
import com.shatrughna.drivemate.core.telemetry.TelemetrySource
import com.shatrughna.drivemate.core.telemetry.TpmsStatus
import com.shatrughna.drivemate.core.telemetry.VehicleDataCoordinator
import com.shatrughna.drivemate.core.telemetry.VehicleTelemetry
import com.shatrughna.drivemate.telemetry.FakeVehicleTelemetryProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleTelemetryTest {

    @Test
    fun testSpeedConversionMpsToKmh() {
        val speedMps = 20.0f // 20 m/s = 72 km/h
        val speedKmh = speedMps * 3.6f
        assertEquals(72.0f, speedKmh, 0.01f)
    }

    @Test
    fun testOdometerConversionMetersToKilometers() {
        val odometerMeters = 12845600.0f
        val odometerKm = odometerMeters / 1000.0
        assertEquals(12845.6, odometerKm, 0.01)
    }

    @Test
    fun testOdometerDecouplingGpsNeverMutatesOdometer() = runTest {
        val coordinator = VehicleDataCoordinator()
        assertNull(coordinator.telemetry.value.manualOdometerKm)
        coordinator.updateManualOdometer(12500.0)

        // GPS trip updates
        coordinator.updateTripGpsDistance(14.5)
        coordinator.updateTripGpsDistance(35.2)

        val telemetry = coordinator.telemetry.value

        // GPS distance updated accurately
        assertEquals(35.2, telemetry.tripGpsDistanceKm, 0.01)

        // Manual calibrated odometer NEVER altered by GPS trip
        assertEquals(12500.0, telemetry.manualOdometerKm!!, 0.01)
        assertNull(telemetry.vehicleOdometerKm)
        assertEquals(12500.0, telemetry.effectiveOdometerKm!!, 0.01)
        assertFalse(telemetry.isAuthoritativeOdometer)
    }

    @Test
    fun freshCoordinatorHasNoFabricatedVehicleValues() = runTest {
        val telemetry = VehicleDataCoordinator().telemetry.value
        assertNull(telemetry.manualOdometerKm)
        assertNull(telemetry.vehicleOdometerKm)
        assertNull(telemetry.speedKmh)
        assertNull(telemetry.fuelLevelPercent)
        assertNull(telemetry.rangeRemainingKm)
        assertFalse(telemetry.androidAutoConnected)
        assertFalse(telemetry.vehicleTelemetryConnected)
    }

    @Test
    fun gpsSpeedIsExplicitlyMarkedAsEstimateAndClearsWithoutZeroFallback() = runTest {
        val coordinator = VehicleDataCoordinator()
        coordinator.updateGpsSpeed(68f)
        assertEquals(TelemetrySource.PHONE_GPS, coordinator.telemetry.value.speedSource)
        assertEquals(TelemetryAvailability.LIVE, coordinator.telemetry.value.speedAvailability)
        assertEquals(68f, coordinator.telemetry.value.speedKmh!!, 0.01f)

        coordinator.updateGpsSpeed(null)
        assertNull(coordinator.telemetry.value.speedKmh)
        assertEquals(TelemetryAvailability.UNAVAILABLE, coordinator.telemetry.value.speedAvailability)
    }

    @Test
    fun testAuthoritativeOdometerOverridesManualCalibration() {
        val telemetryWithDirectOdo = VehicleTelemetry(
            vehicleOdometerKm = 12845.6,
            odometerSource = TelemetrySource.ANDROID_AUTO_CAR_HARDWARE,
            odometerAvailability = TelemetryAvailability.LIVE,
            manualOdometerKm = 12500.0
        )

        assertEquals(12845.6, telemetryWithDirectOdo.effectiveOdometerKm!!, 0.01)
        assertTrue(telemetryWithDirectOdo.isAuthoritativeOdometer)
    }

    @Test
    fun testTpmsDataHonesty() {
        val tpmsProvider = AndroidAutoTpmsProvider()
        val state = tpmsProvider.tpmsState.value

        // Direct TPMS must be marked NOT_SUPPORTED over Android Auto on Tata Nexon
        assertEquals(TelemetryAvailability.NOT_SUPPORTED, state.availability)
        assertEquals(TpmsStatus.UNAVAILABLE, state.frontLeft.status)
        assertNull(state.frontLeft.pressurePsi)
        assertFalse(tpmsProvider.isSupported)
        assertTrue(state.notice.contains("Android Auto"))
    }

    @Test
    fun testFakeTelemetryProviderEmissions() = runTest {
        val fakeProvider = FakeVehicleTelemetryProvider()
        fakeProvider.emitSpeed(65.0f)
        fakeProvider.emitOdometer(13200.0)
        fakeProvider.emitFuel(75.0f, 380.0f)

        val data = fakeProvider.telemetry.value
        assertEquals(65.0f, data.speedKmh)
        assertEquals(13200.0, data.vehicleOdometerKm)
        assertEquals(75.0f, data.fuelLevelPercent)
        assertEquals(380.0f, data.rangeRemainingKm)
        assertEquals(TelemetryAvailability.LIVE, data.speedAvailability)
    }
}
