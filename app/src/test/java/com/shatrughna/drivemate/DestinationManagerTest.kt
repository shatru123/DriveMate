package com.shatrughna.drivemate

import com.shatrughna.drivemate.data.model.Destination
import com.shatrughna.drivemate.data.model.DestinationCategory
import com.shatrughna.drivemate.data.model.DestinationSource
import com.shatrughna.drivemate.data.model.DriveMateSettings
import com.shatrughna.drivemate.destination.DestinationManagerImpl
import com.shatrughna.drivemate.destination.RecentDestinationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DestinationManagerTest {

    private class FakeRecentRepo : RecentDestinationRepository {
        private val _recents = MutableStateFlow<List<Destination>>(emptyList())
        override val recentDestinations: StateFlow<List<Destination>> = _recents

        override suspend fun addDestination(destination: Destination) {
            val list = _recents.value.toMutableList()
            list.removeAll { it.name.equals(destination.name, ignoreCase = true) }
            list.add(0, destination.copy(source = DestinationSource.RECENT))
            _recents.value = list.take(15)
        }

        override suspend fun clearHistory() {
            _recents.value = emptyList()
        }
    }

    private lateinit var fakeRepo: FakeRecentRepo
    private lateinit var destinationManager: DestinationManagerImpl

    @Before
    fun setUp() {
        fakeRepo = FakeRecentRepo()
        destinationManager = DestinationManagerImpl(
            recentDestinationRepository = fakeRepo,
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        )
    }

    @Test
    fun testServiceCenterSuggestedWhenServiceIntervalNear() {
        // Vehicle odometer at 14,200 km, next service at 15,000 km (<= 1000 km remaining)
        val settings = DriveMateSettings(
            odometerKm = 14200.0,
            nextServiceKm = 15000,
            serviceTargetConfigured = true,
            fuelReminderEnabled = false
        )

        val suggestions = destinationManager.getSuggestedDestinations(settings)
        assertTrue(suggestions.any { it.category == DestinationCategory.SERVICE_CENTER && it.name.contains("Vehicle Service") })
    }

    @Test
    fun testPetrolPumpSuggestedWhenFuelReminderEnabled() {
        val settings = DriveMateSettings(
            odometerKm = 10000.0,
            nextServiceKm = 15000,
            fuelReminderEnabled = true
        )

        val suggestions = destinationManager.getSuggestedDestinations(settings)
        assertTrue(suggestions.any { it.category == DestinationCategory.FUEL && it.name.contains("Petrol Pump") })
    }

    @Test
    fun testSavedHomeAndOfficeShortcutsIncluded() {
        val settings = DriveMateSettings(
            homeAddress = "Bavdhan, Pune",
            officeAddress = "Hinjawadi Phase 1, Pune"
        )

        val suggestions = destinationManager.getSuggestedDestinations(settings)
        assertTrue(suggestions.any { it.name == "Home" && it.address == "Bavdhan, Pune" })
        assertTrue(suggestions.any { it.name == "Office" && it.address == "Hinjawadi Phase 1, Pune" })
    }

    @Test
    fun testRecentDestinationAddingAndClearing() = runTest {
        val testDest = Destination(
            name = "Phoenix Marketcity",
            query = "Phoenix Marketcity Pune",
            source = DestinationSource.MAP_SEARCH,
            category = DestinationCategory.SHOPPING
        )

        fakeRepo.addDestination(testDest)
        assertEquals(1, fakeRepo.recentDestinations.value.size)
        assertEquals("Phoenix Marketcity", fakeRepo.recentDestinations.value.first().name)

        // Clear history
        destinationManager.clearRecentDestinations()
        fakeRepo.clearHistory()
        assertEquals(0, fakeRepo.recentDestinations.value.size)
    }

    @Test
    fun testRecentDestinationDeduplicationAndCapping() = runTest {
        for (i in 1..20) {
            fakeRepo.addDestination(
                Destination(
                    name = "Stop $i",
                    query = "Stop $i Query",
                    source = DestinationSource.MAP_SEARCH
                )
            )
        }

        // Must be capped at 15
        assertEquals(15, fakeRepo.recentDestinations.value.size)
        assertEquals("Stop 20", fakeRepo.recentDestinations.value.first().name)

        // Re-adding existing destination moves it to the top without increasing total count
        fakeRepo.addDestination(
            Destination(
                name = "Stop 10",
                query = "Stop 10 Query",
                source = DestinationSource.MAP_SEARCH
            )
        )
        assertEquals(15, fakeRepo.recentDestinations.value.size)
        assertEquals("Stop 10", fakeRepo.recentDestinations.value.first().name)
    }
}
