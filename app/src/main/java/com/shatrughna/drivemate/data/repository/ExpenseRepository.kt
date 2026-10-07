package com.shatrughna.drivemate.data.repository

import android.content.Context
import com.shatrughna.drivemate.data.model.ExpenseCategory
import com.shatrughna.drivemate.data.model.ExpenseSummary
import com.shatrughna.drivemate.data.model.VehicleExpense
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
import java.util.Calendar
import java.util.concurrent.TimeUnit

interface ExpenseRepository {
    val expenses: StateFlow<List<VehicleExpense>>
    suspend fun addExpense(expense: VehicleExpense)
    suspend fun deleteExpense(id: String)
    fun getSummary(currentOdometerKm: Double): ExpenseSummary
    suspend fun seedDemoExpenses()
    suspend fun clearDemoExpenses()
}

class ExpenseRepositoryImpl(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : ExpenseRepository {

    companion object {
        private const val DIRECTORY_NAME = "expenses"
        private const val FILE_NAME = "expenses.json"
    }

    private val _expenses = MutableStateFlow<List<VehicleExpense>>(emptyList())
    override val expenses: StateFlow<List<VehicleExpense>> = _expenses.asStateFlow()

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
            _expenses.value = emptyList()
            return@withContext
        }

        try {
            val jsonStr = storageFile.readText()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<VehicleExpense>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    VehicleExpense(
                        id = obj.getString("id"),
                        category = ExpenseCategory.valueOf(obj.optString("category", ExpenseCategory.OTHER.name)),
                        amount = obj.getDouble("amount"),
                        dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                        odometerKm = if (obj.has("odometerKm") && !obj.isNull("odometerKm")) obj.getDouble("odometerKm") else null,
                        fuelLiters = if (obj.has("fuelLiters") && !obj.isNull("fuelLiters")) obj.getDouble("fuelLiters") else null,
                        fuelPricePerLiter = if (obj.has("fuelPricePerLiter") && !obj.isNull("fuelPricePerLiter")) obj.getDouble("fuelPricePerLiter") else null,
                        location = obj.optString("location", ""),
                        notes = obj.optString("notes", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            _expenses.value = list
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.APP, "Failed to load expenses from disk: corrupted file", e)
            try {
                val backupFile = File(storageDir, "expenses.json.corrupt.${System.currentTimeMillis()}")
                storageFile.renameTo(backupFile)
            } catch (backupEx: Exception) {
                AppLogger.e(AppLogger.Tag.APP, "Failed to rename corrupt expenses file", backupEx)
            }
            _expenses.value = emptyList()
        }
    }

    override suspend fun addExpense(expense: VehicleExpense) = withContext(Dispatchers.IO) {
        val current = _expenses.value.toMutableList()
        current.add(0, expense)
        _expenses.value = current
        saveToDisk(current)
    }

    override suspend fun deleteExpense(id: String) = withContext(Dispatchers.IO) {
        val current = _expenses.value.toMutableList()
        current.removeAll { it.id == id }
        _expenses.value = current
        saveToDisk(current)
    }

    override fun getSummary(currentOdometerKm: Double): ExpenseSummary {
        val list = _expenses.value
        val totalSpent = list.sumOf { it.amount }

        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        var monthSpent = 0.0
        val catMap = mutableMapOf<ExpenseCategory, Double>()

        for (item in list) {
            calendar.timeInMillis = item.dateMillis
            if (calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear) {
                monthSpent += item.amount
            }
            val existing = catMap[item.category] ?: 0.0
            catMap[item.category] = existing + item.amount
        }

        // Cost per km calculation
        val costPerKm = if (currentOdometerKm > 0) {
            totalSpent / currentOdometerKm
        } else 0.0

        return ExpenseSummary(
            totalSpent = totalSpent,
            currentMonthSpent = monthSpent,
            categoryTotals = catMap,
            costPerKm = costPerKm
        )
    }

    private fun saveToDisk(list: List<VehicleExpense>) {
        try {
            val array = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("category", item.category.name)
                    put("amount", item.amount)
                    put("dateMillis", item.dateMillis)
                    if (item.odometerKm != null) put("odometerKm", item.odometerKm)
                    if (item.fuelLiters != null) put("fuelLiters", item.fuelLiters)
                    if (item.fuelPricePerLiter != null) put("fuelPricePerLiter", item.fuelPricePerLiter)
                    put("location", item.location)
                    put("notes", item.notes)
                    put("createdAt", item.createdAt)
                }
                array.put(obj)
            }
            storageFile.writeText(array.toString(2))
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.APP, "Failed to save expenses to disk", e)
        }
    }

    override suspend fun seedDemoExpenses() = withContext(Dispatchers.IO) {
        val current = _expenses.value.filterNot { it.id.startsWith("demo_") || it.notes.startsWith("[DEMO]") }.toMutableList()
        current.addAll(0, createDemoExpenses())
        _expenses.value = current
        saveToDisk(current)
        AppLogger.i(AppLogger.Tag.APP, "Seeded demo vehicle expenses.")
    }

    override suspend fun clearDemoExpenses() = withContext(Dispatchers.IO) {
        val filtered = _expenses.value.filterNot { it.id.startsWith("demo_") || it.notes.startsWith("[DEMO]") }
        _expenses.value = filtered
        saveToDisk(filtered)
        AppLogger.i(AppLogger.Tag.APP, "Cleared demo vehicle expenses.")
    }

    private fun createDemoExpenses(): List<VehicleExpense> {
        val now = System.currentTimeMillis()
        val oneDay = TimeUnit.DAYS.toMillis(1)
        return listOf(
            VehicleExpense(
                id = "demo_exp_fuel_1",
                category = ExpenseCategory.FUEL,
                amount = 3500.0,
                dateMillis = now - (2 * oneDay),
                odometerKm = 24800.0,
                fuelLiters = 33.5,
                fuelPricePerLiter = 104.5,
                location = "Indian Oil Petrol Pump, Highway 48",
                notes = "[DEMO] Full tank petrol"
            ),
            VehicleExpense(
                id = "demo_exp_toll_1",
                category = ExpenseCategory.TOLL,
                amount = 265.0,
                dateMillis = now - (3 * oneDay),
                odometerKm = 24710.0,
                location = "Khed Shivapur Toll Plaza",
                notes = "[DEMO] FASTag auto-debit"
            ),
            VehicleExpense(
                id = "demo_exp_service_1",
                category = ExpenseCategory.SERVICE,
                amount = 4850.0,
                dateMillis = now - (150 * oneDay),
                odometerKm = 15000.0,
                location = "Tata Motors Cars Workshop",
                notes = "[DEMO] 3rd Periodic Service"
            ),
            VehicleExpense(
                id = "demo_exp_parking_1",
                category = ExpenseCategory.PARKING,
                amount = 120.0,
                dateMillis = now - (8 * oneDay),
                location = "Phoenix Marketcity Basement",
                notes = "[DEMO] Weekend mall parking"
            )
        )
    }
}
