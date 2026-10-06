package com.shatrughna.drivemate

import com.shatrughna.drivemate.car.CarConnectionManager
import com.shatrughna.drivemate.car.CarConnectionState
import com.shatrughna.drivemate.car.CarConnectionType
import com.shatrughna.drivemate.driving.DrivingSession
import com.shatrughna.drivemate.driving.DrivingSessionManagerImpl
import kotlinx.coroutines.CoroutineScope
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun testBluetoothOnlyDoesNotStartSession() = runTest(testDispatcher) {
        val receivedGreetingEvents = mutableListOf<DrivingSession>()
        val collectorJob = launch {
            sessionManager.greetingTriggerEvents.toList(receivedGreetingEvents)
        }

        // Bluetooth-only pairing (no Android Auto projection)
        val btState = CarConnectionState.Connected(
            connectionType = CarConnectionType.BLUETOOTH_ONLY,
            deviceOrVehicleName = "Tata Nexon BT Audio"
        )
        sessionManager.onConnectionStateChanged(btState)
        advanceUntilIdle()

        assertFalse(sessionManager.isSessionActive.value)
        assertNull(sessionManager.currentSessionId.value)
        assertFalse(sessionManager.hasGreetingPlayed.value)
        assertEquals(0, receivedGreetingEvents.size)

        collectorJob.cancel()
    }

    @Test
    fun testConnectionLifecycleAndDeduplication() = runTest(testDispatcher) {
        val receivedGreetingEvents = mutableListOf<DrivingSession>()
        val collectorJob = launch {
            sessionManager.greetingTriggerEvents.toList(receivedGreetingEvents)
        }

        // 1. Initial State: Disconnected
        sessionManager.onConnectionStateChanged(CarConnectionState.Disconnected())
        advanceUntilIdle()
        assertFalse(sessionManager.isSessionActive.value)
        assertNull(sessionManager.currentSessionId.value)
        assertFalse(sessionManager.hasGreetingPlayed.value)
        assertEquals(0, receivedGreetingEvents.size)

        // 2. Event: CONNECT via Android Auto Projection
        val connectedState1 = CarConnectionState.Connected(
            connectionType = CarConnectionType.ANDROID_AUTO_PROJECTION,
            deviceOrVehicleName = "Tata Nexon Infotainment",
            timestampMillis = 1000L
        )
        sessionManager.onConnectionStateChanged(connectedState1)
        advanceUntilIdle()

        assertTrue(sessionManager.isSessionActive.value)
        val firstSessionId = sessionManager.currentSessionId.value
        assertNotNull(firstSessionId)
        assertEquals(1, receivedGreetingEvents.size)
        assertEquals("Tata Nexon Infotainment", receivedGreetingEvents.first().connectionState.deviceOrVehicleName)
        assertEquals(firstSessionId, receivedGreetingEvents.first().sessionId)

        // Simulate greeting playback completing successfully with valid session ID
        val marked = sessionManager.markGreetingPlayed(firstSessionId!!)
        assertTrue(marked)
        assertTrue(sessionManager.hasGreetingPlayed.value)

        // Stale session ID cannot mark greeting
        val staleMark = sessionManager.markGreetingPlayed("invalid-session-id")
        assertFalse(staleMark)

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
        assertEquals(firstSessionId, sessionManager.currentSessionId.value)

        // 4. Event: DISCONNECT (Real car unplug/stop)
        sessionManager.onConnectionStateChanged(CarConnectionState.Disconnected(timestampMillis = 2000L))
        advanceUntilIdle()

        assertFalse(sessionManager.isSessionActive.value)
        assertNull(sessionManager.currentSessionId.value)
        assertFalse(sessionManager.hasGreetingPlayed.value)

        // 5. Event: RECONNECT (New drive begins)
        val newConnectedState = CarConnectionState.Connected(
            connectionType = CarConnectionType.ANDROID_AUTO_PROJECTION,
            deviceOrVehicleName = "Tata Nexon Infotainment",
            timestampMillis = 3000L
        )
        sessionManager.onConnectionStateChanged(newConnectedState)
        advanceUntilIdle()

        // Greeting MUST trigger again for the new drive with new session ID
        assertTrue(sessionManager.isSessionActive.value)
        val secondSessionId = sessionManager.currentSessionId.value
        assertNotNull(secondSessionId)
        assertTrue(firstSessionId != secondSessionId)
        assertEquals(2, receivedGreetingEvents.size)
        assertEquals(secondSessionId, receivedGreetingEvents[1].sessionId)

        collectorJob.cancel()
    }
}
