package com.shatrughna.drivemate.driving

import com.shatrughna.drivemate.data.model.RoutePoint
import com.shatrughna.drivemate.data.model.TripReport
import com.shatrughna.drivemate.data.model.TripStats
import com.shatrughna.drivemate.data.preferences.DriveMatePreferencesRepository
import com.shatrughna.drivemate.greeting.GreetingTtsManager
import com.shatrughna.drivemate.location.DeviceLocationProvider
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Tracks active drive duration, real-time verified GPS breadcrumb route, and commits completed trips.
 * Strict data honesty: Never fabricates distance or movement when GPS is unavailable.
 */
interface TripTracker {
    val tripStats: StateFlow<TripStats>
    val activeRoutePoints: StateFlow<List<RoutePoint>>
    val latestCompletedTrip: StateFlow<TripReport?>
    fun start()
    fun stop()
    fun simulateDistanceTick(additionalKm: Float)
}

class TripTrackerImpl(
    private val sessionManager: DrivingSessionManager,
    private val preferencesRepository: DriveMatePreferencesRepository,
    private val locationProvider: DeviceLocationProvider? = null,
    private val tripHistoryRepository: TripHistoryRepository? = null,
    private val ttsManager: GreetingTtsManager? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : TripTracker {

    private val _activeTripDurationSeconds = MutableStateFlow(0L)
    private val _activeTripDistanceKm = MutableStateFlow(0.0f)
    private val _activeRoutePoints = MutableStateFlow<List<RoutePoint>>(emptyList())
    override val activeRoutePoints: StateFlow<List<RoutePoint>> = _activeRoutePoints.asStateFlow()

    private val _latestCompletedTrip = MutableStateFlow<TripReport?>(null)
    override val latestCompletedTrip: StateFlow<TripReport?> = _latestCompletedTrip.asStateFlow()

    private var activeTickerJob: Job? = null
    private var activeGpsJob: Job? = null
    private var isStarted = false
    private var sessionStartTimeMillis: Long = 0L
    private var startLocationResolvedName: String? = null
    private var fatigueAlertTriggered = false

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
        activeGpsJob?.cancel()
        activeGpsJob = null
        isStarted = false
    }

    override fun simulateDistanceTick(additionalKm: Float) {
        if (additionalKm > 0f) {
            _activeTripDistanceKm.value += additionalKm
        }
    }

    private fun onSessionStarted() {
        AppLogger.i(AppLogger.Tag.SESSION, "TripTracker: Active driving session started. Beginning verified trip tracking.")
        _activeTripDurationSeconds.value = 0L
        _activeTripDistanceKm.value = 0.0f
        _activeRoutePoints.value = emptyList()
        sessionStartTimeMillis = System.currentTimeMillis()
        startLocationResolvedName = null
        fatigueAlertTriggered = false

        // Resolve initial starting location honestly
        scope.launch {
            if (locationProvider != null && locationProvider.hasLocationPermission()) {
                val loc = locationProvider.getCurrentLocation()
                startLocationResolvedName = loc?.cityName
            }
        }

        activeTickerJob?.cancel()
        activeTickerJob = scope.launch {
            var seconds = 0L
            while (isActive) {
                delay(1000L)
                seconds += 1
                _activeTripDurationSeconds.value = seconds

                // ZERO FABRICATED DISTANCE: Distance strictly derives from verified GPS points or manual simulation.

                // Check 2-hour continuous driving fatigue alert (7200s), fired exactly once per session
                if (seconds >= 7200L && !fatigueAlertTriggered) {
                    fatigueAlertTriggered = true
                    val settings = preferencesRepository.settingsFlow.first()
                    if (settings.driverFatigueAlertEnabled && sessionManager.isSessionActive.value) {
                        AppLogger.i(AppLogger.Tag.SESSION, "Triggering 2-hour driver fatigue alert.")
                        ttsManager?.speak("Driver fatigue warning: You have been driving for two continuous hours. Please consider pulling over for a quick rest.")
                    }
                }
            }
        }

        // Periodic GPS Breadcrumb Sampling
        activeGpsJob?.cancel()
        activeGpsJob = scope.launch {
            while (isActive) {
                sampleGpsLocation()
                delay(5000L) // Sample every 5 seconds
            }
        }
    }

    private suspend fun sampleGpsLocation() {
        if (locationProvider == null || !locationProvider.hasLocationPermission()) return

        val location = locationProvider.getCurrentLocation() ?: return
        val currentPoints = _activeRoutePoints.value.toMutableList()
        val lastPoint = currentPoints.lastOrNull()

        var calculatedSpeed = 0f
        if (lastPoint != null) {
            val distMeters = calculateDistanceMeters(
                lastPoint.latitude, lastPoint.longitude,
                location.latitude, location.longitude
            )
            val timeDiffSec = ((System.currentTimeMillis() - lastPoint.timestampMillis) / 1000f).coerceAtLeast(1f)
            calculatedSpeed = ((distMeters / timeDiffSec) * 3.6).toFloat() // m/s to km/h

            // Reject impossible GPS jumps (speed > 160 km/h) caused by multipath jitter or cell tower jump
            if (calculatedSpeed > 160.0f) {
                AppLogger.w(
                    AppLogger.Tag.SESSION,
                    "TripTracker: Rejecting impossible GPS jump: ${distMeters}m in ${timeDiffSec}s (${calculatedSpeed} km/h)"
                )
                return
            }

            // Only add point if moved more than 5 meters (filters stationary noise at traffic signals)
            if (distMeters >= 5.0) {
                val newDistKm = _activeTripDistanceKm.value + (distMeters / 1000f).toFloat()
                _activeTripDistanceKm.value = newDistKm
                currentPoints.add(
                    RoutePoint(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        speedKmh = calculatedSpeed,
                        timestampMillis = System.currentTimeMillis()
                    )
                )
                _activeRoutePoints.value = currentPoints
            }
        } else {
            // First verified GPS point
            currentPoints.add(
                RoutePoint(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    speedKmh = 0f,
                    timestampMillis = System.currentTimeMillis()
                )
            )
            _activeRoutePoints.value = currentPoints
        }
    }

    private suspend fun onSessionEnded() {
        val durationSecs = _activeTripDurationSeconds.value
        val distanceKm = _activeTripDistanceKm.value
        val points = _activeRoutePoints.value
        val endTime = System.currentTimeMillis()

        activeTickerJob?.cancel()
        activeTickerJob = null
        activeGpsJob?.cancel()
        activeGpsJob = null

        if (durationSecs >= 5L) {
            val durationMinutes = (durationSecs / 60).coerceAtLeast(1L)
            val settings = preferencesRepository.settingsFlow.first()

            val endPoint = points.lastOrNull()

            // Resolve parked address honestly: only save if real location was acquired
            var parkedAddress: String? = null
            var parkedLat: Double? = null
            var parkedLon: Double? = null

            if (locationProvider != null && locationProvider.hasLocationPermission()) {
                val freshLoc = locationProvider.getCurrentLocation()
                if (freshLoc != null) {
                    parkedLat = freshLoc.latitude
                    parkedLon = freshLoc.longitude
                    parkedAddress = freshLoc.cityName ?: "${String.format("%.4f", freshLoc.latitude)}, ${String.format("%.4f", freshLoc.longitude)}"
                }
            }

            if (parkedLat == null && endPoint != null) {
                parkedLat = endPoint.latitude
                parkedLon = endPoint.longitude
                parkedAddress = "${String.format("%.4f", endPoint.latitude)}, ${String.format("%.4f", endPoint.longitude)}"
            }

            // Only persist parking location if REAL coordinates exist (zero fake Pune saving)
            if (parkedLat != null && parkedLon != null) {
                preferencesRepository.updateLastParkedLocation(
                    lat = parkedLat,
                    lon = parkedLon,
                    address = parkedAddress ?: "Parked Location"
                )
            }

            val hours = (durationSecs / 3600f).coerceAtLeast(0.01f)
            val avgSpeed = (distanceKm / hours).coerceAtMost(160f)
            val maxSpeed = points.maxOfOrNull { it.speedKmh } ?: avgSpeed
            val ecoScore = if (distanceKm > 0.1f) {
                (95 - (maxSpeed / 20f).toInt()).coerceIn(75, 98)
            } else 90
            val fuelUsed = distanceKm / settings.averageMileageKmpl

            val resolvedStart = startLocationResolvedName ?: "Location unavailable"
            val resolvedEnd = parkedAddress ?: "Location unavailable"

            val report = TripReport(
                startTimeMillis = sessionStartTimeMillis,
                endTimeMillis = endTime,
                distanceKm = distanceKm,
                durationMinutes = durationMinutes,
                avgSpeedKmh = avgSpeed,
                maxSpeedKmh = maxSpeed,
                ecoScore = ecoScore,
                startLocationName = resolvedStart,
                endLocationName = resolvedEnd,
                routePoints = points,
                fuelConsumedLiters = fuelUsed
            )

            _latestCompletedTrip.value = report
            tripHistoryRepository?.saveTrip(report)
            preferencesRepository.recordCompletedTrip(distanceKm, durationMinutes)

            AppLogger.i(
                AppLogger.Tag.SESSION,
                "TripTracker: Trip completed. Distance: ${report.formattedDistance}, Duration: ${report.formattedDuration}, Parked: ${parkedAddress ?: "Unavailable"}"
            )

            // Trigger Post-Drive Voice Audio Debrief via TTS (only for meaningful drives)
            if (ttsManager != null && durationSecs >= 15L && distanceKm > 0.05f) {
                val debriefSpeech = "Trip complete, ${settings.driverName}! You drove ${report.formattedDistance} in ${report.durationMinutes} minutes. Have a wonderful day!"
                ttsManager.speak(debriefSpeech)
            }
        } else {
            AppLogger.d(AppLogger.Tag.SESSION, "TripTracker: Trip too short (${durationSecs}s), discarding.")
        }

        _activeTripDurationSeconds.value = 0L
        _activeTripDistanceKm.value = 0.0f
        _activeRoutePoints.value = emptyList()
        fatigueAlertTriggered = false
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}
