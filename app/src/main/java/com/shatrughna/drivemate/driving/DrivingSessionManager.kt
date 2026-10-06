package com.shatrughna.drivemate.driving

import com.shatrughna.drivemate.car.CarConnectionManager
import com.shatrughna.drivemate.car.CarConnectionState
import com.shatrughna.drivemate.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages driving session lifecycles and guarantees greeting deduplication.
 */
interface DrivingSessionManager {
    val isSessionActive: StateFlow<Boolean>
    val hasGreetingPlayed: StateFlow<Boolean>
    val sessionStartTime: StateFlow<Long?>
    val greetingTriggerEvents: SharedFlow<CarConnectionState.Connected>

    fun startSessionMonitoring()
    fun markGreetingPlayed()
    fun resetSession()
    suspend fun onConnectionStateChanged(state: CarConnectionState)
}

class DrivingSessionManagerImpl(
    private val carConnectionManager: CarConnectionManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : DrivingSessionManager {

    private val sessionMutex = Mutex()

    private val _isSessionActive = MutableStateFlow(false)
    override val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _hasGreetingPlayed = MutableStateFlow(false)
    override val hasGreetingPlayed: StateFlow<Boolean> = _hasGreetingPlayed.asStateFlow()

    private val _sessionStartTime = MutableStateFlow<Long?>(null)
    override val sessionStartTime: StateFlow<Long?> = _sessionStartTime.asStateFlow()

    private val _greetingTriggerEvents = MutableSharedFlow<CarConnectionState.Connected>(replay = 0)
    override val greetingTriggerEvents: SharedFlow<CarConnectionState.Connected> = _greetingTriggerEvents.asSharedFlow()

    private var previousConnectionState: CarConnectionState = CarConnectionState.Unknown
    private var isMonitoring = false

    override fun startSessionMonitoring() {
        if (isMonitoring) return
        isMonitoring = true

        scope.launch {
            carConnectionManager.connectionState.collectLatest { newState ->
                onConnectionStateChanged(newState)
            }
        }
    }

    override suspend fun onConnectionStateChanged(state: CarConnectionState) = sessionMutex.withLock {
        AppLogger.d(
            AppLogger.Tag.SESSION,
            "State transition: Previous=${previousConnectionState::class.simpleName}, New=${state::class.simpleName}"
        )

        when (state) {
            is CarConnectionState.Connected -> {
                if (!_isSessionActive.value) {
                    // Brand new connection session
                    AppLogger.i(AppLogger.Tag.SESSION, "Starting new driving session for: ${state.deviceOrVehicleName}")
                    _isSessionActive.value = true
                    _sessionStartTime.value = state.timestampMillis
                    _hasGreetingPlayed.value = false

                    // Emit greeting event
                    _greetingTriggerEvents.emit(state)
                } else {
                    // Already in active session: ignore duplicate connection callbacks or configuration changes
                    AppLogger.d(
                        AppLogger.Tag.SESSION,
                        "Already in active session. Greeting already played: ${_hasGreetingPlayed.value}. Suppressing duplicate announcement."
                    )
                }
            }

            is CarConnectionState.Disconnected -> {
                if (_isSessionActive.value) {
                    AppLogger.i(AppLogger.Tag.SESSION, "Ending driving session due to car disconnection.")
                    _isSessionActive.value = false
                    _hasGreetingPlayed.value = false
                    _sessionStartTime.value = null
                }
            }

            is CarConnectionState.Unknown -> {
                // Initial detection or unverified state; maintain current session until definite disconnect
            }
        }

        previousConnectionState = state
    }

    override fun markGreetingPlayed() {
        AppLogger.i(AppLogger.Tag.SESSION, "Marked greeting as played for current session.")
        _hasGreetingPlayed.value = true
    }

    override fun resetSession() {
        AppLogger.i(AppLogger.Tag.SESSION, "Manual session reset requested.")
        _isSessionActive.value = false
        _hasGreetingPlayed.value = false
        _sessionStartTime.value = null
    }
}
