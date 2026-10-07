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
    fun testClimateProviderCompanionStateAdjustment() {
        val resTemp = climateProvider.setTemperature(24.5f)
        assertFalse(resTemp.success) // Direct CAN actuation is false
        assertEquals(24.5f, climateProvider.climateState.value.targetTemperatureCelsius, 0.01f)

        val resFan = climateProvider.setFanSpeed(5)
        assertFalse(resFan.success)
        assertEquals(5, climateProvider.climateState.value.fanSpeed)

        climateProvider.setAcEnabled(false)
        assertFalse(climateProvider.climateState.value.isAcOn)
    }

    @Test
    fun testSupportedParkingSensors() {
        val parkingCap = manager.getCapability("parking_sensors")
        assertNotNull(parkingCap)
        assertEquals(CapabilityStatus.SUPPORTED, parkingCap?.status)

        val result = manager.evaluateAction("parking_sensors", "parking sensors")
        assertTrue(result.success)
        assertEquals(CapabilityStatus.SUPPORTED, result.status)
    }
}
