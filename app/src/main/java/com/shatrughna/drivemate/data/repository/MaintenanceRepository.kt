package com.shatrughna.drivemate.data.repository

import android.content.Context
import com.shatrughna.drivemate.data.model.ServiceRecord
import com.shatrughna.drivemate.data.model.ServiceSchedule
import com.shatrughna.drivemate.data.model.ServiceType
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
import java.util.concurrent.TimeUnit

interface MaintenanceRepository {
    val serviceRecords: StateFlow<List<ServiceRecord>>
    val serviceSchedule: StateFlow<ServiceSchedule>
    suspend fun addServiceRecord(record: ServiceRecord)
    suspend fun deleteServiceRecord(id: String)
    suspend fun updateSchedule(schedule: ServiceSchedule)
    suspend fun seedDemoMaintenance()
    suspend fun clearDemoMaintenance()
}

class MaintenanceRepositoryImpl(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : MaintenanceRepository {

    companion object {
        private const val DIRECTORY_NAME = "maintenance"
        private const val FILE_NAME = "maintenance.json"
    }

    private val _serviceRecords = MutableStateFlow<List<ServiceRecord>>(emptyList())
    override val serviceRecords: StateFlow<List<ServiceRecord>> = _serviceRecords.asStateFlow()

    private val _serviceSchedule = MutableStateFlow(ServiceSchedule())
    override val serviceSchedule: StateFlow<ServiceSchedule> = _serviceSchedule.asStateFlow()

    private val storageDir: File by lazy {
        File(context.filesDir, DIRECTORY_NAME).apply { if (!exists()) mkdirs() }
    }

    private val storageFile: File by lazy {
        File(storageDir, FILE_NAME)
    }

    init {
        scope.launch {
            loadFromDisk()
        }
    }

    private suspend fun loadFromDisk() = withContext(Dispatchers.IO) {
        if (!storageFile.exists()) {
            // Production first launch: Start completely empty
            _serviceRecords.value = emptyList()
            _serviceSchedule.value = ServiceSchedule()
            return@withContext
        }

        try {
            val jsonStr = storageFile.readText()
            val root = JSONObject(jsonStr)

            // Parse schedule
            if (root.has("schedule")) {
                val schedObj = root.getJSONObject("schedule")
                _serviceSchedule.value = ServiceSchedule(
                    intervalKm = schedObj.optInt("intervalKm", 15000),
                    intervalMonths = schedObj.optInt("intervalMonths", 12),
                    lastServiceOdometerKm = schedObj.optDouble("lastServiceOdometerKm", 0.0),
                    lastServiceDateMillis = schedObj.optLong("lastServiceDateMillis", 0L)
                )
            }

            // Parse records
            val recordsArray = root.optJSONArray("records") ?: JSONArray()
            val list = mutableListOf<ServiceRecord>()
            for (i in 0 until recordsArray.length()) {
                val obj = recordsArray.getJSONObject(i)
                list.add(
                    ServiceRecord(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        type = ServiceType.valueOf(obj.optString("type", ServiceType.PERIODIC_SERVICE.name)),
                        dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                        odometerKm = obj.getDouble("odometerKm"),
                        workshopName = obj.optString("workshopName", "Tata Authorized Service"),
                        cost = obj.optDouble("cost", 0.0),
                        invoiceNumber = obj.optString("invoiceNumber", ""),
                        notes = obj.optString("notes", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            _serviceRecords.value = list
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.APP, "Failed to load maintenance records: corrupted file", e)
            try {
                val backupFile = File(storageDir, "maintenance.json.corrupt.${System.currentTimeMillis()}")
                storageFile.renameTo(backupFile)
            } catch (backupEx: Exception) {
                AppLogger.e(AppLogger.Tag.APP, "Failed to rename corrupt maintenance file", backupEx)
            }
            _serviceRecords.value = emptyList()
            _serviceSchedule.value = ServiceSchedule()
        }
    }

    override suspend fun addServiceRecord(record: ServiceRecord) = withContext(Dispatchers.IO) {
        val current = _serviceRecords.value.toMutableList()
        current.add(0, record)
        _serviceRecords.value = current

        // If this record has a higher odometer reading, update last service odometer
        val currentSched = _serviceSchedule.value
        if (record.odometerKm > currentSched.lastServiceOdometerKm) {
            val updatedSchedule = currentSched.copy(
                lastServiceOdometerKm = record.odometerKm,
                lastServiceDateMillis = record.dateMillis
            )
            _serviceSchedule.value = updatedSchedule
            saveToDisk(current, updatedSchedule)
        } else {
            saveToDisk(current, currentSched)
        }
    }

    override suspend fun deleteServiceRecord(id: String) = withContext(Dispatchers.IO) {
        val current = _serviceRecords.value.toMutableList()
        current.removeAll { it.id == id }
        _serviceRecords.value = current
        saveToDisk(current, _serviceSchedule.value)
    }

    override suspend fun updateSchedule(schedule: ServiceSchedule) = withContext(Dispatchers.IO) {
        _serviceSchedule.value = schedule
        saveToDisk(_serviceRecords.value, schedule)
    }

    private fun saveToDisk(records: List<ServiceRecord>, schedule: ServiceSchedule) {
        try {
            val root = JSONObject()

            val schedObj = JSONObject().apply {
                put("intervalKm", schedule.intervalKm)
                put("intervalMonths", schedule.intervalMonths)
                put("lastServiceOdometerKm", schedule.lastServiceOdometerKm)
                put("lastServiceDateMillis", schedule.lastServiceDateMillis)
            }
            root.put("schedule", schedObj)

            val recordsArray = JSONArray()
            for (rec in records) {
                val obj = JSONObject().apply {
                    put("id", rec.id)
                    put("title", rec.title)
                    put("type", rec.type.name)
                    put("dateMillis", rec.dateMillis)
                    put("odometerKm", rec.odometerKm)
                    put("workshopName", rec.workshopName)
                    put("cost", rec.cost)
                    put("invoiceNumber", rec.invoiceNumber)
                    put("notes", rec.notes)
                    put("createdAt", rec.createdAt)
                }
                recordsArray.put(obj)
            }
            root.put("records", recordsArray)

            storageFile.writeText(root.toString(2))
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.APP, "Failed to save maintenance to disk", e)
        }
    }

    override suspend fun seedDemoMaintenance() = withContext(Dispatchers.IO) {
        val current = _serviceRecords.value.filterNot { it.id.startsWith("demo_") || it.title.startsWith("[DEMO]") }.toMutableList()
        val demoRecords = createDemoRecords()
        current.addAll(0, demoRecords)
        val demoSchedule = ServiceSchedule(
            intervalKm = 15000,
            intervalMonths = 12,
            lastServiceOdometerKm = 15000.0,
            lastServiceDateMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(150)
        )
        _serviceRecords.value = current
        _serviceSchedule.value = demoSchedule
        saveToDisk(current, demoSchedule)
        AppLogger.i(AppLogger.Tag.APP, "Seeded demo maintenance records.")
    }

    override suspend fun clearDemoMaintenance() = withContext(Dispatchers.IO) {
        val filtered = _serviceRecords.value.filterNot { it.id.startsWith("demo_") || it.title.startsWith("[DEMO]") }
        val latestOdo = filtered.maxOfOrNull { it.odometerKm } ?: 0.0
        val latestDate = filtered.maxOfOrNull { it.dateMillis } ?: 0L
        val resetSchedule = ServiceSchedule(
            intervalKm = 15000,
            intervalMonths = 12,
            lastServiceOdometerKm = latestOdo,
            lastServiceDateMillis = latestDate
        )
        _serviceRecords.value = filtered
        _serviceSchedule.value = resetSchedule
        saveToDisk(filtered, resetSchedule)
        AppLogger.i(AppLogger.Tag.APP, "Cleared demo maintenance records.")
    }

    private fun createDemoRecords(): List<ServiceRecord> {
        val now = System.currentTimeMillis()
        val oneDay = TimeUnit.DAYS.toMillis(1)
        return listOf(
            ServiceRecord(
                id = "demo_rec_service_3",
                title = "[DEMO] 3rd Scheduled Periodic Service (15,000 km)",
                type = ServiceType.PERIODIC_SERVICE,
                dateMillis = now - (150 * oneDay),
                odometerKm = 15000.0,
                workshopName = "Tata Motors Cars Workshop, Pune",
                cost = 4850.0,
                invoiceNumber = "INV-TATA-7749",
                notes = "Engine oil change (Synthetic 0W-20), oil filter, wheel alignment, brake inspection"
            ),
            ServiceRecord(
                id = "demo_rec_service_2",
                title = "[DEMO] 2nd Periodic Inspection (7,500 km)",
                type = ServiceType.PERIODIC_SERVICE,
                dateMillis = now - (320 * oneDay),
                odometerKm = 7500.0,
                workshopName = "Tata Motors Authorized Service Center",
                cost = 0.0,
                invoiceNumber = "INV-TATA-3102",
                notes = "Free service check, top-up fluids, general inspection"
            ),
            ServiceRecord(
                id = "demo_rec_service_1",
                title = "[DEMO] 1st Periodic Service (1,500 km / 1 Month)",
                type = ServiceType.PERIODIC_SERVICE,
                dateMillis = now - (480 * oneDay),
                odometerKm = 1450.0,
                workshopName = "Tata Motors Authorized Service Center",
                cost = 0.0,
                invoiceNumber = "INV-TATA-1004",
                notes = "First running-in service, complete vehicle check"
            )
        )
    }
}
