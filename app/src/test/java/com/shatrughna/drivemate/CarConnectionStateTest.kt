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
        assertTrue(connectedAuto.isVerifiedCarSession)
        assertEquals(CarConnectionType.ANDROID_AUTO_PROJECTION, connectedAuto.connectionType)
        assertEquals("Connected via Android Auto Projection", connectedAuto.statusDescription)

        val connectedBt = CarConnectionState.Connected(
            connectionType = CarConnectionType.BLUETOOTH_ONLY,
            deviceOrVehicleName = "Tata Nexon BT"
        )
        assertTrue(connectedBt.isConnected)
        assertFalse(connectedBt.isVerifiedCarSession)
        assertEquals(CarConnectionType.BLUETOOTH_ONLY, connectedBt.connectionType)
        assertEquals("Connected via Bluetooth (Tata Nexon BT)", connectedBt.statusDescription)
    }

    @Test
    fun testDriveMateSettingsDefaultsAndVehicleName() {
        val defaultSettings = DriveMateSettings()
        assertEquals("", defaultSettings.driverName)
        assertEquals("", defaultSettings.vehicleBrand)
        assertEquals("", defaultSettings.vehicleModel)
        assertEquals("", defaultSettings.vehicleVariant)
        assertEquals("Connected vehicle", defaultSettings.fullVehicleName)
        assertTrue(defaultSettings.greetingEnabled)
        assertEquals(GreetingStyle.NORMAL, defaultSettings.greetingStyle)

        val configured = DriveMateSettings(
            driverName = "Shatrughna",
            vehicleBrand = "TATA",
            vehicleModel = "Nexon",
            vehicleVariant = "Creative+ S"
        )
        assertEquals("TATA Nexon Creative+ S", configured.fullVehicleName)
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
