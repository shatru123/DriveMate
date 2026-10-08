package com.shatrughna.drivemate

import com.shatrughna.drivemate.auth.model.AuthState
import com.shatrughna.drivemate.auth.repository.AuthRepositoryImpl
import com.shatrughna.drivemate.auth.security.PasswordHasher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var authRepository: AuthRepositoryImpl

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("drivemate_auth_test").toFile()
        authRepository = AuthRepositoryImpl(filesDir = tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testPasswordHasherProducesSecureHashAndVerifiesCorrectly() {
        val password = "SuperSecretPassword123!"
        val hash = PasswordHasher.hashPassword(password)

        assertTrue(hash.contains(":")) // salt:hash format
        assertFalse(hash.contains(password)) // Never stores plain text
        assertTrue(PasswordHasher.verifyPassword(password, hash))
        assertFalse(PasswordHasher.verifyPassword("WrongPassword", hash))
    }

    @Test
    fun testSignUpSuccessAndUserCreation() = runTest {
        val result = authRepository.signUp("Shatrughna", "shatru@drivemate.app", "password123")
        assertTrue(result.isSuccess)

        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("Shatrughna", user?.name)
        assertEquals("shatru@drivemate.app", user?.email)

        val state = authRepository.authState.value
        assertTrue(state is AuthState.LoggedIn)
        assertEquals(user?.id, (state as AuthState.LoggedIn).user.id)
    }

    @Test
    fun testSignUpDuplicateEmailFails() = runTest {
        val first = authRepository.signUp("Shatrughna", "shatru@drivemate.app", "password123")
        assertTrue(first.isSuccess)

        val second = authRepository.signUp("Other User", "SHATRU@DRIVEMATE.APP", "differentPass")
        assertTrue(second.isFailure)
        assertTrue(second.exceptionOrNull()?.message?.contains("already exists") == true)
    }

    @Test
    fun testLoginSuccessWithValidCredentials() = runTest {
        authRepository.signUp("Driver One", "driver@drivemate.app", "securePass123")
        authRepository.logout()

        assertEquals(AuthState.LoggedOut, authRepository.authState.value)

        val loginResult = authRepository.login("driver@drivemate.app", "securePass123")
        assertTrue(loginResult.isSuccess)
        assertEquals("Driver One", loginResult.getOrNull()?.name)

        val state = authRepository.authState.value
        assertTrue(state is AuthState.LoggedIn)
        assertEquals("driver@drivemate.app", (state as AuthState.LoggedIn).user.email)
    }

    @Test
    fun testLoginFailsWithInvalidPassword() = runTest {
        authRepository.signUp("Driver One", "driver@drivemate.app", "securePass123")
        authRepository.logout()

        val loginResult = authRepository.login("driver@drivemate.app", "incorrectPassword")
        assertTrue(loginResult.isFailure)
        assertEquals(AuthState.LoggedOut, authRepository.authState.value)
    }

    @Test
    fun testSessionPersistenceAcrossAppRestart() = runTest {
        val signUpResult = authRepository.signUp("Shatrughna", "shatru@drivemate.app", "securePass123")
        val userId = signUpResult.getOrNull()!!.id

        // Simulate app restart by instantiating new AuthRepository on same filesDir
        val restartedRepo = AuthRepositoryImpl(filesDir = tempDir)
        val restoredState = kotlinx.coroutines.withTimeoutOrNull(3000) {
            restartedRepo.authState.first { it !is AuthState.Loading }
        }

        assertTrue(restoredState is AuthState.LoggedIn)
        val loggedInUser = (restoredState as AuthState.LoggedIn).user
        assertEquals(userId, loggedInUser.id)
        assertEquals("shatru@drivemate.app", loggedInUser.email)
    }

    @Test
    fun testLogoutClearsActiveSession() = runTest {
        authRepository.signUp("Shatrughna", "shatru@drivemate.app", "securePass123")
        assertNotNull(authRepository.currentUser.value)

        authRepository.logout()
        assertNull(authRepository.currentUser.value)
        assertEquals(AuthState.LoggedOut, authRepository.authState.value)

        // Session file should no longer restore this user
        val restartedRepo = AuthRepositoryImpl(filesDir = tempDir)
        val restartedState = kotlinx.coroutines.withTimeoutOrNull(3000) {
            restartedRepo.authState.first { it !is AuthState.Loading }
        }
        assertEquals(AuthState.LoggedOut, restartedState)
        assertNull(restartedRepo.currentUser.value)
    }

    @Test
    fun testPasswordResetSuccess() = runTest {
        authRepository.signUp("Shatrughna", "shatru@drivemate.app", "securePass123")

        val validReset = authRepository.resetPassword("shatru@drivemate.app")
        assertTrue(validReset.isSuccess)

        val invalidReset = authRepository.resetPassword("nonexistent@drivemate.app")
        assertTrue(invalidReset.isFailure)
    }
}
