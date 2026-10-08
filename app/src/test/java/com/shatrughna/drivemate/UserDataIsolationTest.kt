package com.shatrughna.drivemate

import com.shatrughna.drivemate.data.model.Destination
import com.shatrughna.drivemate.data.model.DocumentType
import com.shatrughna.drivemate.data.model.ExpenseCategory
import com.shatrughna.drivemate.data.model.ServiceRecord
import com.shatrughna.drivemate.data.model.ServiceType
import com.shatrughna.drivemate.data.model.TripReport
import com.shatrughna.drivemate.data.model.VehicleDocument
import com.shatrughna.drivemate.data.model.VehicleExpense
import com.shatrughna.drivemate.data.repository.DocumentVaultRepositoryImpl
import com.shatrughna.drivemate.data.repository.ExpenseRepositoryImpl
import com.shatrughna.drivemate.data.repository.MaintenanceRepositoryImpl
import com.shatrughna.drivemate.destination.RecentDestinationRepositoryImpl
import com.shatrughna.drivemate.driving.TripHistoryRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class UserDataIsolationTest {

    private lateinit var tempDir: File
    private lateinit var tripRepo: TripHistoryRepositoryImpl
    private lateinit var expenseRepo: ExpenseRepositoryImpl
    private lateinit var docRepo: DocumentVaultRepositoryImpl
    private lateinit var maintRepo: MaintenanceRepositoryImpl
    private lateinit var destRepo: RecentDestinationRepositoryImpl

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("drivemate_isolation_test").toFile()
        tripRepo = TripHistoryRepositoryImpl(filesDir = tempDir)
        expenseRepo = ExpenseRepositoryImpl(filesDir = tempDir)
        docRepo = DocumentVaultRepositoryImpl(filesDir = tempDir)
        maintRepo = MaintenanceRepositoryImpl(filesDir = tempDir)
        destRepo = RecentDestinationRepositoryImpl(filesDir = tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testTripIsolationBetweenUsers() = runTest {
        // User 1 logs in and saves a trip
        tripRepo.switchUser("user_1")
        val trip1 = TripReport(
            id = "trip_user1_001",
            startTimeMillis = 1000L,
            endTimeMillis = 2000L,
            distanceKm = 15.5f,
            durationMinutes = 25,
            startLocationName = "Home",
            endLocationName = "Office"
        )
        tripRepo.saveTrip(trip1)
        assertEquals(1, tripRepo.recentTrips.value.size)
        assertEquals("trip_user1_001", tripRepo.recentTrips.value[0].id)

        // Switch to User 2: trips must be completely empty
        tripRepo.switchUser("user_2")
        assertTrue(tripRepo.recentTrips.value.isEmpty())

        // User 2 saves a trip
        val trip2 = TripReport(
            id = "trip_user2_001",
            startTimeMillis = 3000L,
            endTimeMillis = 4000L,
            distanceKm = 8.0f,
            durationMinutes = 15,
            startLocationName = "Airport",
            endLocationName = "Hotel"
        )
        tripRepo.saveTrip(trip2)
        assertEquals(1, tripRepo.recentTrips.value.size)
        assertEquals("trip_user2_001", tripRepo.recentTrips.value[0].id)

        // Switch back to User 1: User 1's trips are restored
        tripRepo.switchUser("user_1")
        assertEquals(1, tripRepo.recentTrips.value.size)
        assertEquals("trip_user1_001", tripRepo.recentTrips.value[0].id)

        // Logging out clears in-memory state
        tripRepo.switchUser(null)
        assertTrue(tripRepo.recentTrips.value.isEmpty())
    }

    @Test
    fun testExpenseIsolationBetweenUsers() = runTest {
        expenseRepo.switchUser("user_1")
        val exp1 = VehicleExpense(
            id = "exp_user1_001",
            category = ExpenseCategory.FUEL,
            amount = 3500.0,
            notes = "User 1 Petrol"
        )
        expenseRepo.addExpense(exp1)
        assertEquals(1, expenseRepo.expenses.value.size)

        // Switch to User 2
        expenseRepo.switchUser("user_2")
        assertTrue(expenseRepo.expenses.value.isEmpty())

        val exp2 = VehicleExpense(
            id = "exp_user2_001",
            category = ExpenseCategory.SERVICE,
            amount = 12000.0,
            notes = "User 2 Annual Service"
        )
        expenseRepo.addExpense(exp2)
        assertEquals(1, expenseRepo.expenses.value.size)
        assertEquals("exp_user2_001", expenseRepo.expenses.value[0].id)

        // Switch back to User 1
        expenseRepo.switchUser("user_1")
        assertEquals(1, expenseRepo.expenses.value.size)
        assertEquals("exp_user1_001", expenseRepo.expenses.value[0].id)

        // Log out
        expenseRepo.switchUser(null)
        assertTrue(expenseRepo.expenses.value.isEmpty())
    }

    @Test
    fun testDocumentVaultIsolationBetweenUsers() = runTest {
        docRepo.switchUser("user_1")
        val doc1 = VehicleDocument(
            id = "doc_user1_rc",
            title = "Registration Certificate",
            type = DocumentType.REGISTRATION_CERTIFICATE,
            documentNumber = "MH 12 AB 1234"
        )
        docRepo.addOrUpdateDocument(doc1)
        assertEquals(1, docRepo.documents.value.size)

        // Switch to User 2
        docRepo.switchUser("user_2")
        assertTrue(docRepo.documents.value.isEmpty())

        // Switch back to User 1
        docRepo.switchUser("user_1")
        assertEquals(1, docRepo.documents.value.size)
        assertEquals("MH 12 AB 1234", docRepo.documents.value[0].documentNumber)
    }

    @Test
    fun testMaintenanceScheduleIsolationBetweenUsers() = runTest {
        maintRepo.switchUser("user_1")
        val service1 = ServiceRecord(
            id = "serv_user1_001",
            title = "10,000 Km Service",
            type = ServiceType.PERIODIC_SERVICE,
            odometerKm = 10000.0
        )
        maintRepo.addServiceRecord(service1)
        assertEquals(1, maintRepo.serviceRecords.value.size)

        // Switch to User 2
        maintRepo.switchUser("user_2")
        assertTrue(maintRepo.serviceRecords.value.isEmpty())

        // Switch back to User 1
        maintRepo.switchUser("user_1")
        assertEquals(1, maintRepo.serviceRecords.value.size)
        assertEquals("serv_user1_001", maintRepo.serviceRecords.value[0].id)
    }

    @Test
    fun testRecentDestinationIsolationBetweenUsers() = runTest {
        destRepo.switchUser("user_1")
        val dest1 = Destination(
            name = "Pune International Airport",
            latitude = 18.5822,
            longitude = 73.9197
        )
        destRepo.addDestination(dest1)
        assertEquals(1, destRepo.recentDestinations.value.size)

        // Switch to User 2
        destRepo.switchUser("user_2")
        assertTrue(destRepo.recentDestinations.value.isEmpty())

        // Switch back to User 1
        destRepo.switchUser("user_1")
        assertEquals(1, destRepo.recentDestinations.value.size)
        assertEquals("Pune International Airport", destRepo.recentDestinations.value[0].name)
    }
}
