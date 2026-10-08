package com.shatrughna.drivemate

import com.shatrughna.drivemate.car.CarConnectionManager
import com.shatrughna.drivemate.car.CarConnectionState
import com.shatrughna.drivemate.car.CarConnectionType
import com.shatrughna.drivemate.driving.DrivingSessionManagerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression coverage for the passive Android Auto connection contract. */
class AndroidAutoReliabilityTest {
    private class FakeConnectionManager : CarConnectionManager {
        override val connectionState = MutableStateFlow<CarConnectionState>(CarConnectionState.Disconnected())
        override fun startMonitoring() = Unit
        override fun stopMonitoring() = Unit
        override fun onBluetoothConnected(deviceName: String) = Unit
        override fun onBluetoothDisconnected(deviceName: String) = Unit
        override fun setSimulatedConnection(connected: Boolean) = Unit
    }

    @Test
    fun androidAutoConnectDoesNotStartDrivingOrGreeting() = runTest {
        val connection = FakeConnectionManager()
        val manager = DrivingSessionManagerImpl(connection, CoroutineScope(Dispatchers.Unconfined))
        manager.onConnectionStateChanged(
            CarConnectionState.Connected(
                connectionType = CarConnectionType.ANDROID_AUTO_PROJECTION,
                deviceOrVehicleName = "Connected vehicle"
            )
        )

        assertFalse(manager.isSessionActive.value)
        assertFalse(manager.hasGreetingPlayed.value)
        assertTrue(manager.startDrivingSession())
        assertTrue(manager.isSessionActive.value)
    }

    @Test
    fun disconnectEndsExplicitSessionAndAllowsReconnect() = runTest {
        val connection = FakeConnectionManager()
        val manager = DrivingSessionManagerImpl(connection, CoroutineScope(Dispatchers.Unconfined))
        val connected = CarConnectionState.Connected(
            connectionType = CarConnectionType.ANDROID_AUTO_PROJECTION,
            deviceOrVehicleName = "Connected vehicle"
        )
        manager.onConnectionStateChanged(connected)
        assertTrue(manager.startDrivingSession())
        manager.onConnectionStateChanged(CarConnectionState.Disconnected())
        assertFalse(manager.isSessionActive.value)
        manager.onConnectionStateChanged(connected)
        assertFalse(manager.isSessionActive.value)
    }
}
