package com.shatrughna.drivemate

import com.shatrughna.drivemate.vehicle.model.VehicleProfile
import com.shatrughna.drivemate.vehicle.repository.VehicleRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var vehicleRepository: VehicleRepositoryImpl

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("drivemate_vehicle_test").toFile()
        vehicleRepository = VehicleRepositoryImpl(filesDir = tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testSaveAndRetrieveUserVehicle() = runTest {
        val vehicle = VehicleProfile(
            userId = "user_123",
            make = "Tata",
            model = "Nexon",
            variant = "Creative+ S",
            year = 2024,
            registrationNumber = "MH 28 BW 1624",
            fuelType = "Petrol",
            transmission = "Manual",
            currentOdometer = 12500.0
        )

        val saveResult = vehicleRepository.saveVehicle(vehicle)
        assertTrue(saveResult.isSuccess)
        assertEquals("Tata Nexon Creative+ S", vehicleRepository.currentVehicle.value?.fullDisplayName)

        // Fresh load for user
        val loadedVehicle = vehicleRepository.loadVehicleForUser("user_123")
        assertNotNull(loadedVehicle)
        assertEquals("Tata", loadedVehicle?.make)
        assertEquals("Nexon", loadedVehicle?.model)
        assertEquals("Creative+ S", loadedVehicle?.variant)
        assertEquals("MH 28 BW 1624", loadedVehicle?.registrationNumber)
        assertEquals(12500.0, loadedVehicle?.currentOdometer)
    }

    @Test
    fun testSingleVehicleEnforcement() = runTest {
        val firstVehicle = VehicleProfile(
            userId = "user_single_car",
            make = "Hyundai",
            model = "Creta",
            variant = "SX"
        )
        vehicleRepository.saveVehicle(firstVehicle)

        val updatedVehicle = VehicleProfile(
            id = firstVehicle.id,
            userId = "user_single_car",
            make = "Hyundai",
            model = "Creta Facelift",
            variant = "SX(O)"
        )
        val updateResult = vehicleRepository.updateVehicle(updatedVehicle)
        assertTrue(updateResult.isSuccess)

        val retrieved = vehicleRepository.loadVehicleForUser("user_single_car")
        assertNotNull(retrieved)
        assertEquals("Creta Facelift", retrieved?.model)
        assertEquals("SX(O)", retrieved?.variant)
    }

    @Test
    fun testVehicleIsolationBetweenUsers() = runTest {
        val vehicleUserA = VehicleProfile(
            userId = "user_A",
            make = "Tata",
            model = "Nexon",
            registrationNumber = "MH 12 AB 1234"
        )
        val vehicleUserB = VehicleProfile(
            userId = "user_B",
            make = "Maruti",
            model = "Swift",
            registrationNumber = "MH 14 CD 5678"
        )

        vehicleRepository.saveVehicle(vehicleUserA)
        vehicleRepository.saveVehicle(vehicleUserB)

        val loadedA = vehicleRepository.loadVehicleForUser("user_A")
        val loadedB = vehicleRepository.loadVehicleForUser("user_B")

        assertNotNull(loadedA)
        assertNotNull(loadedB)
        assertEquals("Tata", loadedA?.make)
        assertEquals("Maruti", loadedB?.make)
        assertEquals("MH 12 AB 1234", loadedA?.registrationNumber)
        assertEquals("MH 14 CD 5678", loadedB?.registrationNumber)
    }

    @Test
    fun testValidationRejectsEmptyMakeOrModel() = runTest {
        val blankMake = VehicleProfile(
            userId = "user_err",
            make = "   ",
            model = "Nexon"
        )
        val result1 = vehicleRepository.saveVehicle(blankMake)
        assertTrue(result1.isFailure)

        val blankModel = VehicleProfile(
            userId = "user_err",
            make = "Tata",
            model = ""
        )
        val result2 = vehicleRepository.saveVehicle(blankModel)
        assertTrue(result2.isFailure)
    }
}
