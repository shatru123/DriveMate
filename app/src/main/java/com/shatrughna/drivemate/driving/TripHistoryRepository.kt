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
}

class TripHistoryRepositoryImpl(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : TripHistoryRepository {

    private val tripsFile: File
        get() = File(context.filesDir, "drivemate_trips.json")

    private val _recentTrips = MutableStateFlow<List<TripReport>>(emptyList())
    override val recentTrips: StateFlow<List<TripReport>> = _recentTrips.asStateFlow()

    private val _latestTrip = MutableStateFlow<TripReport?>(null)
    override val latestTrip: StateFlow<TripReport?> = _latestTrip.asStateFlow()

    init {
        scope.launch {
            loadTripsFromStorage()
        }
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
        if (tripsFile.exists()) {
            tripsFile.delete()
        }
    }

    private fun loadTripsFromStorage() {
        try {
            if (!tripsFile.exists()) return
            val jsonString = tripsFile.readText()
            if (jsonString.isBlank()) return

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
                            speedKmh = pObj.optDouble("speed", 0.0).toFloat(),
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
                        avgSpeedKmh = obj.getDouble("avgSpeedKmh").toFloat(),
                        maxSpeedKmh = obj.optDouble("maxSpeedKmh", 0.0).toFloat(),
                        ecoScore = obj.optInt("ecoScore", 90),
                        startLocationName = obj.optString("startLocation", "Origin"),
                        endLocationName = obj.optString("endLocation", "Destination"),
                        routePoints = routePoints,
                        fuelConsumedLiters = obj.optDouble("fuelConsumedLiters", 0.0).toFloat()
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
            val jsonArray = JSONArray()
            for (trip in trips) {
                val obj = JSONObject().apply {
                    put("id", trip.id)
                    put("startTime", trip.startTimeMillis)
                    put("endTime", trip.endTimeMillis)
                    put("distanceKm", trip.distanceKm.toDouble())
                    put("durationMinutes", trip.durationMinutes)
                    put("avgSpeedKmh", trip.avgSpeedKmh.toDouble())
                    put("maxSpeedKmh", trip.maxSpeedKmh.toDouble())
                    put("ecoScore", trip.ecoScore)
                    put("startLocation", trip.startLocationName)
                    put("endLocation", trip.endLocationName)
                    put("fuelConsumedLiters", trip.fuelConsumedLiters.toDouble())

                    val pointsArray = JSONArray()
                    for (point in trip.routePoints) {
                        pointsArray.put(
                            JSONObject().apply {
                                put("lat", point.latitude)
                                put("lon", point.longitude)
                                put("speed", point.speedKmh.toDouble())
                                put("time", point.timestampMillis)
                            }
                        )
                    }
                    put("routePoints", pointsArray)
                }
                jsonArray.put(obj)
            }
            tripsFile.writeText(jsonArray.toString())
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.SESSION, "Failed to persist trip history: ${e.message}", e)
        }
    }
}
