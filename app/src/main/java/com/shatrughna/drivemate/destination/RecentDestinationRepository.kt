package com.shatrughna.drivemate.destination

import android.content.Context
import com.shatrughna.drivemate.data.model.Destination
import com.shatrughna.drivemate.data.model.DestinationCategory
import com.shatrughna.drivemate.data.model.DestinationSource
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
import java.util.UUID

interface RecentDestinationRepository {
    val recentDestinations: StateFlow<List<Destination>>
    suspend fun addDestination(destination: Destination)
    suspend fun clearHistory()
    suspend fun switchUser(userId: String?) {}
}

class RecentDestinationRepositoryImpl(
    private val context: Context? = null,
    private val filesDir: File = context?.filesDir ?: File(System.getProperty("java.io.tmpdir"), "drivemate_destinations"),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : RecentDestinationRepository {

    companion object {
        private const val FILE_NAME = "drivemate_recent_destinations.json"
        const val MAX_RECENT_COUNT = 15
    }

    private var currentUserId: String? = null

    private val _recentDestinations = MutableStateFlow<List<Destination>>(emptyList())
    override val recentDestinations: StateFlow<List<Destination>> = _recentDestinations.asStateFlow()

    private val storageFile: File
        get() = if (currentUserId != null) {
            File(File(File(filesDir, "users"), currentUserId), FILE_NAME)
        } else {
            File(filesDir, FILE_NAME)
        }

    init {
        scope.launch {
            loadFromDisk()
        }
    }

    override suspend fun switchUser(userId: String?) = withContext(Dispatchers.IO) {
        currentUserId = userId
        if (userId == null) {
            _recentDestinations.value = emptyList()
            return@withContext
        }
        val targetFile = storageFile
        targetFile.parentFile?.mkdirs()
        val legacyFile = File(filesDir, FILE_NAME)
        if (!targetFile.exists() && legacyFile.exists()) {
            try {
                legacyFile.copyTo(targetFile, overwrite = true)
                AppLogger.i(AppLogger.Tag.APP, "RecentDestinationRepository: Migrated legacy destinations to user $userId")
            } catch (e: Exception) {
                AppLogger.e(AppLogger.Tag.APP, "RecentDestinationRepository: Failed migrating legacy destinations", e)
            }
        }
        loadFromDisk()
    }

    private suspend fun loadFromDisk() = withContext(Dispatchers.IO) {
        if (!storageFile.exists()) {
            _recentDestinations.value = emptyList()
            return@withContext
        }
        try {
            val jsonStr = storageFile.readText()
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<Destination>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    Destination(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.getString("name"),
                        address = if (obj.has("address")) obj.getString("address") else null,
                        latitude = if (obj.has("latitude")) obj.getDouble("latitude") else null,
                        longitude = if (obj.has("longitude")) obj.getDouble("longitude") else null,
                        source = DestinationSource.valueOf(obj.optString("source", DestinationSource.RECENT.name)),
                        query = if (obj.has("query")) obj.getString("query") else null,
                        category = DestinationCategory.valueOf(obj.optString("category", DestinationCategory.CUSTOM.name)),
                        timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis())
                    )
                )
            }
            _recentDestinations.value = list
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tag.APP, "Failed to load recent destinations: ${e.message}")
            _recentDestinations.value = emptyList()
        }
    }

    override suspend fun addDestination(destination: Destination) = withContext(Dispatchers.IO) {
        val current = _recentDestinations.value.toMutableList()
        // Deduplicate against same name or query
        current.removeAll {
            it.name.equals(destination.name, ignoreCase = true) ||
            (it.query != null && it.query.equals(destination.query, ignoreCase = true))
        }
        val recentEntry = destination.copy(source = DestinationSource.RECENT)
        current.add(0, recentEntry)
        val trimmed = current.take(MAX_RECENT_COUNT)
        _recentDestinations.value = trimmed
        saveToDisk(trimmed)
    }

    override suspend fun clearHistory() = withContext(Dispatchers.IO) {
        _recentDestinations.value = emptyList()
        if (storageFile.exists()) {
            storageFile.delete()
        }
        AppLogger.i(AppLogger.Tag.APP, "Recent destinations cleared.")
    }

    private fun saveToDisk(list: List<Destination>) {
        try {
            storageFile.parentFile?.mkdirs()
            val jsonArray = JSONArray()
            for (dest in list) {
                val obj = JSONObject().apply {
                    put("id", dest.id)
                    put("name", dest.name)
                    if (dest.address != null) put("address", dest.address)
                    if (dest.latitude != null) put("latitude", dest.latitude)
                    if (dest.longitude != null) put("longitude", dest.longitude)
                    put("source", dest.source.name)
                    if (dest.query != null) put("query", dest.query)
                    put("category", dest.category.name)
                    put("timestampMillis", dest.timestampMillis)
                }
                jsonArray.put(obj)
            }
            storageFile.writeText(jsonArray.toString(2))
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.APP, "Failed to save recent destinations", e)
        }
    }
}
