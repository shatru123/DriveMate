package com.shatrughna.drivemate

import com.shatrughna.drivemate.car.CarConnectionState
import com.shatrughna.drivemate.car.CarConnectionType
import com.shatrughna.drivemate.data.model.DriveMateSettings
import com.shatrughna.drivemate.data.model.GreetingStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarConnectionStateTest {

    @Test
    fun testCarConnectionStateProperties() {
        val unknown = CarConnectionState.Unknown
        assertFalse(unknown.isConnected)
        assertEquals(CarConnectionType.NONE, unknown.connectionType)

        val disconnected = CarConnectionState.Disconnected()
        assertFalse(disconnected.isConnected)
        assertEquals(CarConnectionType.NONE, disconnected.connectionType)
        assertEquals("Not connected", disconnected.statusDescription)

        val connectedAuto = CarConnectionState.Connected(
            connectionType = CarConnectionType.ANDROID_AUTO_PROJECTION,
            deviceOrVehicleName = "Tata Nexon"
        )
        assertTrue(connectedAuto.isConnected)
        assertEquals(CarConnectionType.ANDROID_AUTO_PROJECTION, connectedAuto.connectionType)
        assertEquals("Connected via Android Auto", connectedAuto.statusDescription)

        val connectedBt = CarConnectionState.Connected(
            connectionType = CarConnectionType.BLUETOOTH_CAR_UNIT,
            deviceOrVehicleName = "Tata Nexon BT"
        )
        assertTrue(connectedBt.isConnected)
        assertEquals(CarConnectionType.BLUETOOTH_CAR_UNIT, connectedBt.connectionType)
        assertEquals("Connected via Car Bluetooth", connectedBt.statusDescription)
    }

    @Test
    fun testDriveMateSettingsDefaultsAndVehicleName() {
        val defaultSettings = DriveMateSettings()
        assertEquals("Shatrughna", defaultSettings.driverName)
        assertEquals("TATA", defaultSettings.vehicleBrand)
        assertEquals("Nexon", defaultSettings.vehicleModel)
        assertEquals("Creative+ S", defaultSettings.vehicleVariant)
        assertEquals("TATA Nexon Creative+ S", defaultSettings.fullVehicleName)
        assertTrue(defaultSettings.greetingEnabled)
        assertEquals(GreetingStyle.NORMAL, defaultSettings.greetingStyle)
    }

    @Test
    fun testGreetingStyleEnumLookup() {
        assertEquals(GreetingStyle.SHORT, GreetingStyle.fromName("SHORT"))
        assertEquals(GreetingStyle.NORMAL, GreetingStyle.fromName("normal"))
        assertEquals(GreetingStyle.DETAILED, GreetingStyle.fromName("Detailed"))
        assertEquals(GreetingStyle.CUSTOM, GreetingStyle.fromName("custom"))
        assertEquals(GreetingStyle.NORMAL, GreetingStyle.fromName("invalid_style"))
        assertEquals(GreetingStyle.NORMAL, GreetingStyle.fromName(null))
    }
}
