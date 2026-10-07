package com.shatrughna.drivemate

import com.shatrughna.drivemate.data.model.ExpenseCategory
import com.shatrughna.drivemate.data.model.VehicleExpense
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class ExpenseSummaryTest {

    @Test
    fun testExpenseAggregationMath() {
        val now = System.currentTimeMillis()
        val expenses = listOf(
            VehicleExpense(category = ExpenseCategory.FUEL, amount = 3000.0, dateMillis = now),
            VehicleExpense(category = ExpenseCategory.FUEL, amount = 2500.0, dateMillis = now),
            VehicleExpense(category = ExpenseCategory.TOLL, amount = 265.0, dateMillis = now),
            VehicleExpense(category = ExpenseCategory.SERVICE, amount = 4850.0, dateMillis = now)
        )

        val totalSpent = expenses.sumOf { it.amount }
        assertEquals(10615.0, totalSpent, 0.01)

        val fuelTotal = expenses.filter { it.category == ExpenseCategory.FUEL }.sumOf { it.amount }
        assertEquals(5500.0, fuelTotal, 0.01)

        val currentOdo = 25000.0
        val costPerKm = totalSpent / currentOdo
        assertEquals(0.4246, costPerKm, 0.01)
    }
}
