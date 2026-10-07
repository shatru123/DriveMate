package com.shatrughna.drivemate.data.model

import java.util.UUID

enum class ExpenseCategory(val displayName: String) {
    FUEL("Fuel (Petrol / Diesel)"),
    SERVICE("Scheduled Service"),
    REPAIRS("Mechanical Repairs"),
    INSURANCE("Insurance Policy"),
    TOLL("FASTag & Toll Charges"),
    PARKING("Parking Fees"),
    ACCESSORIES("Car Accessories & Mods"),
    CHALLAN("Traffic Challans / Fines"),
    OTHER("Miscellaneous")
}

data class VehicleExpense(
    val id: String = UUID.randomUUID().toString(),
    val category: ExpenseCategory,
    val amount: Double,
    val dateMillis: Long = System.currentTimeMillis(),
    val odometerKm: Double? = null,
    val fuelLiters: Double? = null,
    val fuelPricePerLiter: Double? = null,
    val location: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class ExpenseSummary(
    val totalSpent: Double,
    val currentMonthSpent: Double,
    val categoryTotals: Map<ExpenseCategory, Double>,
    val costPerKm: Double
)
