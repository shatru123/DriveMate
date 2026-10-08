package com.shatrughna.drivemate.driving

import android.content.Context
import com.shatrughna.drivemate.data.model.RoutePoint
import com.shatrughna.drivemate.data.model.TripReport
import com.shatrughna.drivemate.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

interface TripHistoryRepository {
    val recentTrips: StateFlow<List<TripReport>>
    val latestTrip: StateFlow<TripReport?>
    suspend fun saveTrip(trip: TripReport)
    suspend fun clearHistory()
    suspend fun switchUser(userId: String?) {}
}

class TripHistoryRepositoryImpl(
    private val context: Context? = null,
    private val filesDir: File = context?.filesDir ?: File(System.getProperty("java.io.tmpdir"), "drivemate_trips"),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : TripHistoryRepository {

    private var currentUserId: String? = null

    private val tripsFile: File
        get() = if (currentUserId != null) {
            File(File(File(filesDir, "users"), currentUserId), "drivemate_trips.json")
        } else {
            File(filesDir, "drivemate_trips.json")
        }

    private val _recentTrips = MutableStateFlow<List<TripReport>>(emptyList())
    override val recentTrips: StateFlow<List<TripReport>> = _recentTrips.asStateFlow()

    private val _latestTrip = MutableStateFlow<TripReport?>(null)
    override val latestTrip: StateFlow<TripReport?> = _latestTrip.asStateFlow()

    init {
        scope.launch {
            loadTripsFromStorage()
        }
    }

    override suspend fun switchUser(userId: String?) = withContext(Dispatchers.IO) {
        currentUserId = userId
        if (userId == null) {
            _recentTrips.value = emptyList()
            _latestTrip.value = null
            return@withContext
        }
        val targetFile = tripsFile
        targetFile.parentFile?.mkdirs()
        val legacyFile = File(filesDir, "drivemate_trips.json")
        if (!targetFile.exists() && legacyFile.exists()) {
            try {
                legacyFile.copyTo(targetFile, overwrite = true)
                AppLogger.i(AppLogger.Tag.SESSION, "TripHistory: Migrated legacy trips to user $userId")
            } catch (e: Exception) {
                AppLogger.e(AppLogger.Tag.SESSION, "TripHistory: Failed migrating legacy trips", e)
            }
        }
        loadTripsFromStorage()
    }

    override suspend fun saveTrip(trip: TripReport) = withContext(Dispatchers.IO) {
        val currentList = _recentTrips.value.toMutableList()
        // Insert at beginning (most recent first) and cap at 30 trips
        currentList.add(0, trip)
        val cappedList = currentList.take(30)
        _recentTrips.value = cappedList
        _latestTrip.value = trip
        persistTripsToFile(cappedList)
        AppLogger.i(AppLogger.Tag.SESSION, "TripHistory: Successfully saved trip ${trip.id} (${trip.formattedDistance}, ${trip.formattedDuration})")
    }

    override suspend fun clearHistory() = withContext(Dispatchers.IO) {
        _recentTrips.value = emptyList()
        _latestTrip.value = null
        val file = tripsFile
        if (file.exists()) {
            file.delete()
        }
    }

    private fun loadTripsFromStorage() {
        try {
            val file = tripsFile
            if (!file.exists()) {
                _recentTrips.value = emptyList()
                _latestTrip.value = null
                return
            }
            val jsonString = file.readText()
            if (jsonString.isBlank()) {
                _recentTrips.value = emptyList()
                _latestTrip.value = null
                return
            }

            val jsonArray = JSONArray(jsonString)
            val loadedList = mutableListOf<TripReport>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val pointsArray = obj.optJSONArray("routePoints") ?: JSONArray()
                val routePoints = mutableListOf<RoutePoint>()

                for (j in 0 until pointsArray.length()) {
                    val pObj = pointsArray.getJSONObject(j)
                    routePoints.add(
                        RoutePoint(
                            latitude = pObj.getDouble("lat"),
                            longitude = pObj.getDouble("lon"),
                            speedKmh = if (pObj.has("speed") && !pObj.isNull("speed")) pObj.getDouble("speed").toFloat() else null,
                            timestampMillis = pObj.optLong("time", 0L)
                        )
                    )
                }

                loadedList.add(
                    TripReport(
                        id = obj.getString("id"),
                        startTimeMillis = obj.getLong("startTime"),
                        endTimeMillis = obj.getLong("endTime"),
                        distanceKm = obj.getDouble("distanceKm").toFloat(),
                        durationMinutes = obj.getLong("durationMinutes"),
                        avgSpeedKmh = if (obj.has("avgSpeedKmh") && !obj.isNull("avgSpeedKmh")) obj.getDouble("avgSpeedKmh").toFloat() else null,
                        maxSpeedKmh = if (obj.has("maxSpeedKmh") && !obj.isNull("maxSpeedKmh")) obj.getDouble("maxSpeedKmh").toFloat() else null,
                        ecoScore = if (obj.has("ecoScore") && !obj.isNull("ecoScore")) obj.getInt("ecoScore") else null,
                        startLocationName = obj.optString("startLocation", "Location unavailable"),
                        endLocationName = obj.optString("endLocation", "Location unavailable"),
                        routePoints = routePoints,
                        fuelConsumedLiters = if (obj.has("fuelConsumedLiters") && !obj.isNull("fuelConsumedLiters")) obj.getDouble("fuelConsumedLiters").toFloat() else null
                    )
                )
            }

            _recentTrips.value = loadedList
            _latestTrip.value = loadedList.firstOrNull()
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.SESSION, "Failed to load trip history: ${e.message}", e)
        }
    }

    private fun persistTripsToFile(trips: List<TripReport>) {
        try {
            val file = tripsFile
            file.parentFile?.mkdirs()
            val jsonArray = JSONArray()
            for (trip in trips) {
                val obj = JSONObject().apply {
                    put("id", trip.id)
                    put("startTime", trip.startTimeMillis)
                    put("endTime", trip.endTimeMillis)
                    put("distanceKm", trip.distanceKm.toDouble())
                    put("durationMinutes", trip.durationMinutes)
                    trip.avgSpeedKmh?.let { put("avgSpeedKmh", it.toDouble()) }
                    trip.maxSpeedKmh?.let { put("maxSpeedKmh", it.toDouble()) }
                    trip.ecoScore?.let { put("ecoScore", it) }
                    put("startLocation", trip.startLocationName)
                    put("endLocation", trip.endLocationName)
                    trip.fuelConsumedLiters?.let { put("fuelConsumedLiters", it.toDouble()) }

                    val pointsArray = JSONArray()
                    for (point in trip.routePoints) {
                        pointsArray.put(
                            JSONObject().apply {
                                put("lat", point.latitude)
                                put("lon", point.longitude)
                                point.speedKmh?.let { put("speed", it.toDouble()) }
                                put("time", point.timestampMillis)
                            }
                        )
                    }
                    put("routePoints", pointsArray)
                }
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.SESSION, "Failed to persist trip history: ${e.message}", e)
        }
    }
}
