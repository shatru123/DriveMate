package com.shatrughna.drivemate.driving

import com.shatrughna.drivemate.data.model.TripStats
import com.shatrughna.drivemate.data.preferences.DriveMatePreferencesRepository
import com.shatrughna.drivemate.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Tracks active drive duration, distance, and commits completed trips to daily statistics.
 */
interface TripTracker {
    val tripStats: StateFlow<TripStats>
    fun start()
    fun stop()
    fun simulateDistanceTick(additionalKm: Float)
}

class TripTrackerImpl(
    private val sessionManager: DrivingSessionManager,
    private val preferencesRepository: DriveMatePreferencesRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : TripTracker {

    private val _activeTripDurationSeconds = MutableStateFlow(0L)
    private val _activeTripDistanceKm = MutableStateFlow(0.0f)

    private var activeTickerJob: Job? = null
    private var isStarted = false

    override val tripStats: StateFlow<TripStats> = combine(
        preferencesRepository.tripStatsFlow,
        _activeTripDurationSeconds,
        _activeTripDistanceKm
    ) { dailySummary, activeDuration, activeDistance ->
        dailySummary.copy(
            activeTripDurationSeconds = activeDuration,
            activeTripDistanceKm = activeDistance
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = TripStats()
    )

    override fun start() {
        if (isStarted) return
        isStarted = true

        scope.launch {
            sessionManager.isSessionActive.collectLatest { isActive ->
                if (isActive) {
                    onSessionStarted()
                } else {
                    onSessionEnded()
                }
            }
        }
    }

    override fun stop() {
        activeTickerJob?.cancel()
        activeTickerJob = null
        isStarted = false
    }

    override fun simulateDistanceTick(additionalKm: Float) {
        _activeTripDistanceKm.value += additionalKm
    }

    private fun onSessionStarted() {
        AppLogger.i(AppLogger.Tag.SESSION, "TripTracker: Active driving session started. Beginning trip timer.")
        _activeTripDurationSeconds.value = 0L
        _activeTripDistanceKm.value = 0.0f

        activeTickerJob?.cancel()
        activeTickerJob = scope.launch {
            var seconds = 0L
            while (isActive) {
                delay(1000L)
                seconds += 1
                _activeTripDurationSeconds.value = seconds

                // In driving simulation/companion mode, accumulate a realistic distance
                // Average urban speed ~ 36 km/h (0.01 km/sec)
                _activeTripDistanceKm.value = (seconds * 0.01f)
            }
        }
    }

    private suspend fun onSessionEnded() {
        val durationSecs = _activeTripDurationSeconds.value
        val distanceKm = _activeTripDistanceKm.value

        activeTickerJob?.cancel()
        activeTickerJob = null

        if (durationSecs >= 5L) {
            val durationMinutes = (durationSecs / 60).coerceAtLeast(1L)
            AppLogger.i(
                AppLogger.Tag.SESSION,
                "TripTracker: Trip ended. Duration: ${durationSecs}s (${durationMinutes} min), Distance: ${String.format("%.1f", distanceKm)} km. Committing to today's stats."
            )
            preferencesRepository.recordCompletedTrip(distanceKm, durationMinutes)
        } else {
            AppLogger.d(AppLogger.Tag.SESSION, "TripTracker: Trip too short (${durationSecs}s), discarding without committing.")
        }

        _activeTripDurationSeconds.value = 0L
        _activeTripDistanceKm.value = 0.0f
    }
}
