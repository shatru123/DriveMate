package com.shatrughna.drivemate

import com.shatrughna.drivemate.data.model.ServiceSchedule
import com.shatrughna.drivemate.data.model.ServiceUrgency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class MaintenanceScheduleTest {

    @Test
    fun testOnScheduleStatus() {
        val now = System.currentTimeMillis()
        val schedule = ServiceSchedule(
            intervalKm = 15000,
            intervalMonths = 12,
            lastServiceOdometerKm = 15000.0,
            lastServiceDateMillis = now - TimeUnit.DAYS.toMillis(60)
        )

        val currentOdo = 20000.0 // 10,000 km left
        val urgency = schedule.urgency(currentOdo, now)
        assertEquals(ServiceUrgency.ON_SCHEDULE, urgency)
        assertEquals(10000.0, schedule.kmRemaining(currentOdo), 0.1)
        assertEquals(30000.0, schedule.nextServiceOdometerKm, 0.1)
        assertEquals(0.333f, schedule.kmProgress(currentOdo), 0.01f)
    }

    @Test
    fun testDueSoonByDistance() {
        val now = System.currentTimeMillis()
        val schedule = ServiceSchedule(
            intervalKm = 15000,
            intervalMonths = 12,
            lastServiceOdometerKm = 15000.0,
            lastServiceDateMillis = now - TimeUnit.DAYS.toMillis(90)
        )

        val currentOdo = 28000.0 // 2,000 km left
        val urgency = schedule.urgency(currentOdo, now)
        assertEquals(ServiceUrgency.DUE_SOON, urgency)
        assertEquals(2000.0, schedule.kmRemaining(currentOdo), 0.1)
    }

    @Test
    fun testDueNowByDistance() {
        val now = System.currentTimeMillis()
        val schedule = ServiceSchedule(
            intervalKm = 15000,
            intervalMonths = 12,
            lastServiceOdometerKm = 15000.0,
            lastServiceDateMillis = now - TimeUnit.DAYS.toMillis(90)
        )

        val currentOdo = 29500.0 // 500 km left
        val urgency = schedule.urgency(currentOdo, now)
        assertEquals(ServiceUrgency.DUE_NOW, urgency)
    }

    @Test
    fun testOverdueByDistance() {
        val now = System.currentTimeMillis()
        val schedule = ServiceSchedule(
            intervalKm = 15000,
            intervalMonths = 12,
            lastServiceOdometerKm = 15000.0,
            lastServiceDateMillis = now - TimeUnit.DAYS.toMillis(90)
        )

        val currentOdo = 30500.0 // 500 km past due
        val urgency = schedule.urgency(currentOdo, now)
        assertEquals(ServiceUrgency.OVERDUE, urgency)
    }
}
