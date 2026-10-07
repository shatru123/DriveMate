package com.shatrughna.drivemate

import com.shatrughna.drivemate.core.capabilities.CapabilityStatus
import com.shatrughna.drivemate.core.capabilities.VehicleCameraProviderImpl
import com.shatrughna.drivemate.core.capabilities.VehicleCapabilityManagerImpl
import com.shatrughna.drivemate.core.capabilities.VehicleClimateControlProviderImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VehicleCapabilitiesTest {

    private lateinit var manager: VehicleCapabilityManagerImpl
    private lateinit var climateProvider: VehicleClimateControlProviderImpl
    private lateinit var cameraProvider: VehicleCameraProviderImpl

    @Before
    fun setUp() {
        manager = VehicleCapabilityManagerImpl()
        climateProvider = VehicleClimateControlProviderImpl(manager)
        cameraProvider = VehicleCameraProviderImpl(manager)
    }

    @Test
    fun testClimateControlCapabilityStatusHonesty() {
        val climateCap = manager.getCapability("climate_control")
        assertNotNull(climateCap)
        assertEquals(CapabilityStatus.NOT_SUPPORTED_BY_VEHICLE, climateCap?.status)

        val actionResult = manager.evaluateAction("climate_control", "climate temperature change")
        assertFalse(actionResult.success)
        assertEquals(CapabilityStatus.NOT_SUPPORTED_BY_VEHICLE, actionResult.status)
        assertTrue(actionResult.userMessage.contains("Your vehicle doesn't currently provide"))
    }

    @Test
    fun testCamera360CapabilityStatusHonesty() {
        val cameraCap = manager.getCapability("camera_360")
        assertNotNull(cameraCap)
        assertEquals(CapabilityStatus.NOT_SUPPORTED_BY_VEHICLE, cameraCap?.status)

        val result360 = cameraProvider.request360SurroundView()
        assertFalse(result360.success)
        assertTrue(result360.userMessage.contains("OEM 360° camera feed is displayed on your Nexon infotainment"))

        val resultReverse = cameraProvider.requestReverseCamera()
        assertFalse(resultReverse.success)
        assertTrue(resultReverse.userMessage.contains("reverse gear"))
    }

    @Test
    fun testClimateProviderReadOnlyProtection() {
        val resTemp = climateProvider.setTemperature(24.5f)
        assertFalse(resTemp.success) // Direct CAN actuation is false
        // State remains protected at default 22.0f because direct control is unsupported
        assertEquals(22.0f, climateProvider.climateState.value.targetTemperatureCelsius, 0.01f)

        val resFan = climateProvider.setFanSpeed(5)
        assertFalse(resFan.success)
        assertEquals(3, climateProvider.climateState.value.fanSpeed) // Default fan speed

        val resAc = climateProvider.setAcEnabled(false)
        assertFalse(resAc.success)
        assertTrue(climateProvider.climateState.value.isAcOn) // Default AC remains true
    }

    @Test
    fun testParkingSensorsCapabilityHonesty() {
        val parkingCap = manager.getCapability("parking_sensors")
        assertNotNull(parkingCap)
        assertEquals(CapabilityStatus.NOT_SUPPORTED_BY_VEHICLE, parkingCap?.status)

        val result = manager.evaluateAction("parking_sensors", "parking sensors")
        assertFalse(result.success)
        assertEquals(CapabilityStatus.NOT_SUPPORTED_BY_VEHICLE, result.status)
    }
}
