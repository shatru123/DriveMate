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
import java.util.UUID

/**
 * Data class representing a verified driving session.
 */
data class DrivingSession(
    val sessionId: String,
    val connectionState: CarConnectionState.Connected
)

/**
 * Manages driving session lifecycles and guarantees greeting deduplication.
 */
interface DrivingSessionManager {
    val isSessionActive: StateFlow<Boolean>
    val currentSessionId: StateFlow<String?>
    val hasGreetingPlayed: StateFlow<Boolean>
    val sessionStartTime: StateFlow<Long?>
    val greetingTriggerEvents: SharedFlow<DrivingSession>

    fun startSessionMonitoring()
    /** Starts tracking only after an explicit/user or real-motion trigger. */
    suspend fun startDrivingSession(): Boolean = false
    /** Ends the current drive while retaining the verified car connection for a later restart. */
    suspend fun stopDrivingSession(reason: String = "Motion stopped"): Boolean = false
    fun markGreetingPlayed(sessionId: String): Boolean
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

    private val _currentSessionId = MutableStateFlow<String?>(null)
    override val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _hasGreetingPlayed = MutableStateFlow(false)
    override val hasGreetingPlayed: StateFlow<Boolean> = _hasGreetingPlayed.asStateFlow()

    private val _sessionStartTime = MutableStateFlow<Long?>(null)
    override val sessionStartTime: StateFlow<Long?> = _sessionStartTime.asStateFlow()

    private val _greetingTriggerEvents = MutableSharedFlow<DrivingSession>(replay = 0)
    override val greetingTriggerEvents: SharedFlow<DrivingSession> = _greetingTriggerEvents.asSharedFlow()

    private var previousConnectionState: CarConnectionState = CarConnectionState.Unknown
    private var latestVerifiedConnection: CarConnectionState.Connected? = null
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
                if (!state.isVerifiedCarSession) {
                    latestVerifiedConnection = null
                    AppLogger.d(
                        AppLogger.Tag.SESSION,
                        "Connection is not a verified car session (${state.connectionType.displayName}). Ignoring for driving session trigger."
                    )
                    if (_isSessionActive.value) {
                        endSessionInternal("Connection transitioned away from verified car session")
                    }
                } else {
                    latestVerifiedConnection = state
                    if (!_isSessionActive.value) {
                        AppLogger.i(
                            AppLogger.Tag.SESSION,
                            "Verified car connection is passive; waiting for a driving-session trigger."
                        )
                    }
                }
            }

            is CarConnectionState.Disconnected -> {
                if (_isSessionActive.value) {
                    endSessionInternal("Car disconnected")
                }
            }

            is CarConnectionState.Unknown -> {
                // Initial detection or unverified state; maintain current session until definite disconnect
            }
        }

        previousConnectionState = state
    }

    override suspend fun startDrivingSession(): Boolean = sessionMutex.withLock {
        val connection = latestVerifiedConnection ?: (previousConnectionState as? CarConnectionState.Connected)
        if (connection == null || !connection.isVerifiedCarSession || _isSessionActive.value) return@withLock false

        val newSessionId = UUID.randomUUID().toString()
        AppLogger.i(AppLogger.Tag.SESSION, "Starting driving session [$newSessionId] after a verified trigger")
        _currentSessionId.value = newSessionId
        _isSessionActive.value = true
        _sessionStartTime.value = System.currentTimeMillis()
        _hasGreetingPlayed.value = false
        _greetingTriggerEvents.emit(DrivingSession(newSessionId, connection))
        true
    }

    override suspend fun stopDrivingSession(reason: String): Boolean = sessionMutex.withLock {
        if (!_isSessionActive.value) return@withLock false
        endSessionInternal(reason, clearConnection = false)
        true
    }

    private fun endSessionInternal(reason: String, clearConnection: Boolean = true) {
        AppLogger.i(AppLogger.Tag.SESSION, "Ending driving session [${_currentSessionId.value}]: $reason")
        _isSessionActive.value = false
        _currentSessionId.value = null
        _hasGreetingPlayed.value = false
        _sessionStartTime.value = null
        if (clearConnection) latestVerifiedConnection = null
    }

    override fun markGreetingPlayed(sessionId: String): Boolean {
        if (_isSessionActive.value && _currentSessionId.value == sessionId) {
            AppLogger.i(AppLogger.Tag.SESSION, "Marked greeting as played for session: $sessionId")
            _hasGreetingPlayed.value = true
            return true
        } else {
            AppLogger.w(
                AppLogger.Tag.SESSION,
                "Cannot mark greeting played: sessionId mismatch or inactive session (current=${_currentSessionId.value}, provided=$sessionId)"
            )
            return false
        }
    }

    override fun resetSession() {
        AppLogger.i(AppLogger.Tag.SESSION, "Manual session reset requested.")
        _isSessionActive.value = false
        _currentSessionId.value = null
        _hasGreetingPlayed.value = false
        _sessionStartTime.value = null
        latestVerifiedConnection = null
    }
}
