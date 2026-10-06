package com.shatrughna.drivemate

import com.shatrughna.drivemate.car.CarConnectionManager
import com.shatrughna.drivemate.car.CarConnectionState
import com.shatrughna.drivemate.car.CarConnectionType
import com.shatrughna.drivemate.driving.DrivingSessionManagerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DrivingSessionManagerTest {

    private class FakeCarConnectionManager : CarConnectionManager {
        val stateFlow = MutableStateFlow<CarConnectionState>(CarConnectionState.Disconnected())
        override val connectionState: StateFlow<CarConnectionState> = stateFlow

        override fun startMonitoring() {}
        override fun stopMonitoring() {}
        override fun onBluetoothConnected(deviceName: String) {}
        override fun onBluetoothDisconnected(deviceName: String) {}
        override fun setSimulatedConnection(connected: Boolean) {}
    }

    private lateinit var fakeCarConnectionManager: FakeCarConnectionManager
    private lateinit var sessionManager: DrivingSessionManagerImpl
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        fakeCarConnectionManager = FakeCarConnectionManager()
        sessionManager = DrivingSessionManagerImpl(
            carConnectionManager = fakeCarConnectionManager,
            scope = CoroutineScope(testDispatcher)
        )
    }

    @Test
    fun testConnectionLifecycleAndDeduplication() = runTest(testDispatcher) {
        val receivedGreetingEvents = mutableListOf<CarConnectionState.Connected>()
        val collectorJob = launch {
            sessionManager.greetingTriggerEvents.toList(receivedGreetingEvents)
        }

        // 1. Initial State: Disconnected
        sessionManager.onConnectionStateChanged(CarConnectionState.Disconnected())
        advanceUntilIdle()
        assertFalse(sessionManager.isSessionActive.value)
        assertFalse(sessionManager.hasGreetingPlayed.value)
        assertEquals(0, receivedGreetingEvents.size)

        // 2. Event: CONNECT
        val connectedState1 = CarConnectionState.Connected(
            connectionType = CarConnectionType.ANDROID_AUTO_PROJECTION,
            deviceOrVehicleName = "Tata Nexon Infotainment",
            timestampMillis = 1000L
        )
        sessionManager.onConnectionStateChanged(connectedState1)
        advanceUntilIdle()

        assertTrue(sessionManager.isSessionActive.value)
        assertEquals(1, receivedGreetingEvents.size)
        assertEquals("Tata Nexon Infotainment", receivedGreetingEvents.first().deviceOrVehicleName)

        // Simulate greeting playback completing
        sessionManager.markGreetingPlayed()
        assertTrue(sessionManager.hasGreetingPlayed.value)

        // 3. Event: Duplicate CONNECT (e.g. repeated callback, activity recreated)
        val duplicateConnectedState = CarConnectionState.Connected(
            connectionType = CarConnectionType.ANDROID_AUTO_PROJECTION,
            deviceOrVehicleName = "Tata Nexon Infotainment",
            timestampMillis = 1500L
        )
        sessionManager.onConnectionStateChanged(duplicateConnectedState)
        advanceUntilIdle()

        // Verify NO duplicate greeting was triggered
        assertEquals(1, receivedGreetingEvents.size)
        assertTrue(sessionManager.isSessionActive.value)
        assertTrue(sessionManager.hasGreetingPlayed.value)

        // 4. Event: Another duplicate CONNECT
        sessionManager.onConnectionStateChanged(duplicateConnectedState)
        advanceUntilIdle()
        assertEquals(1, receivedGreetingEvents.size)

        // 5. Event: DISCONNECT (Real car unplug/stop)
        sessionManager.onConnectionStateChanged(CarConnectionState.Disconnected(timestampMillis = 2000L))
        advanceUntilIdle()

        assertFalse(sessionManager.isSessionActive.value)
        assertFalse(sessionManager.hasGreetingPlayed.value)

        // 6. Event: RECONNECT (New drive begins)
        val newConnectedState = CarConnectionState.Connected(
            connectionType = CarConnectionType.ANDROID_AUTO_PROJECTION,
            deviceOrVehicleName = "Tata Nexon Infotainment",
            timestampMillis = 3000L
        )
        sessionManager.onConnectionStateChanged(newConnectedState)
        advanceUntilIdle()

        // Greeting MUST trigger again for the new drive
        assertTrue(sessionManager.isSessionActive.value)
        assertEquals(2, receivedGreetingEvents.size)

        collectorJob.cancel()
    }
}
