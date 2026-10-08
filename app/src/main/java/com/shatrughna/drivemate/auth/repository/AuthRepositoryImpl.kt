package com.shatrughna.drivemate.auth.repository

import android.content.Context
import com.shatrughna.drivemate.auth.model.AuthState
import com.shatrughna.drivemate.auth.model.UserProfile
import com.shatrughna.drivemate.auth.security.PasswordHasher
import com.shatrughna.drivemate.util.AppLogger
import com.shatrughna.drivemate.vehicle.model.VehicleProfile
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

class AuthRepositoryImpl(
    private val context: Context? = null,
    private val filesDir: File = context?.filesDir ?: File(System.getProperty("java.io.tmpdir"), "drivemate_auth"),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : AuthRepository {

    companion object {
        private const val AUTH_DIR = "auth"
        private const val ACCOUNTS_FILE = "accounts.json"
        private const val SESSION_FILE = "session.json"
    }

    private val authDir: File by lazy {
        File(filesDir, AUTH_DIR).apply { if (!exists()) mkdirs() }
    }

    private val accountsFile: File by lazy {
        File(authDir, ACCOUNTS_FILE)
    }

    private val sessionFile: File by lazy {
        File(authDir, SESSION_FILE)
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private var activeVehicle: VehicleProfile? = null

    init {
        scope.launch {
            restoreSession()
        }
    }

    private suspend fun restoreSession() = withContext(Dispatchers.IO) {
        try {
            if (!sessionFile.exists()) {
                _authState.value = AuthState.LoggedOut
                return@withContext
            }
            val jsonStr = sessionFile.readText()
            if (jsonStr.isBlank()) {
                _authState.value = AuthState.LoggedOut
                return@withContext
            }
            val obj = JSONObject(jsonStr)
            val userId = obj.optString("activeUserId", "")
            if (userId.isBlank()) {
                _authState.value = AuthState.LoggedOut
                return@withContext
            }

            val user = findUserById(userId)
            if (user != null) {
                _currentUser.value = user
                _authState.value = AuthState.LoggedIn(user, activeVehicle)
                AppLogger.i(AppLogger.Tag.APP, "Auth: Session restored for user ${user.id} (${user.email})")
            } else {
                sessionFile.delete()
                _authState.value = AuthState.LoggedOut
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.APP, "Auth: Failed to restore session", e)
            _authState.value = AuthState.LoggedOut
        }
    }

    override suspend fun signUp(name: String, email: String, password: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim().lowercase()

        if (trimmedName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your name."))
        }
        if (!isValidEmail(trimmedEmail)) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters long."))
        }

        val accounts = loadAllAccounts()
        if (accounts.any { it.email.equals(trimmedEmail, ignoreCase = true) }) {
            return@withContext Result.failure(IllegalArgumentException("An account with this email already exists."))
        }

        val passwordHash = PasswordHasher.hashPassword(password)
        val user = UserProfile(
            id = UUID.randomUUID().toString(),
            name = trimmedName,
            email = trimmedEmail,
            profileImageUrl = null,
            voiceLanguage = "auto",
            createdAtMillis = System.currentTimeMillis()
        )

        val updatedAccounts = accounts + StoredAccount(user, passwordHash)
        saveAllAccounts(updatedAccounts)
        saveActiveSession(user.id)

        _currentUser.value = user
        _authState.value = AuthState.LoggedIn(user, activeVehicle)
        AppLogger.i(AppLogger.Tag.APP, "Auth: Successfully created new user account for ${user.email}")
        Result.success(user)
    }

    override suspend fun login(email: String, password: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase()
        if (!isValidEmail(trimmedEmail)) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (password.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your password."))
        }

        val accounts = loadAllAccounts()
        val account = accounts.find { it.email.equals(trimmedEmail, ignoreCase = true) }
            ?: return@withContext Result.failure(IllegalArgumentException("No account found with this email."))

        val isPasswordValid = PasswordHasher.verifyPassword(password, account.passwordHash)
        if (!isPasswordValid) {
            return@withContext Result.failure(IllegalArgumentException("Email or password is incorrect."))
        }

        saveActiveSession(account.user.id)
        _currentUser.value = account.user
        _authState.value = AuthState.LoggedIn(account.user, activeVehicle)
        AppLogger.i(AppLogger.Tag.APP, "Auth: User ${account.user.email} successfully logged in.")
        Result.success(account.user)
    }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            if (sessionFile.exists()) {
                sessionFile.delete()
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.APP, "Auth: Error deleting session file", e)
        }
        activeVehicle = null
        _currentUser.value = null
        _authState.value = AuthState.LoggedOut
        AppLogger.i(AppLogger.Tag.APP, "Auth: User logged out.")
    }

    override suspend fun resetPassword(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase()
        if (!isValidEmail(trimmedEmail)) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        val accounts = loadAllAccounts()
        val exists = accounts.any { it.email.equals(trimmedEmail, ignoreCase = true) }
        if (!exists) {
            return@withContext Result.failure(IllegalArgumentException("No account found with this email address."))
        }
        AppLogger.i(AppLogger.Tag.APP, "Auth: Password reset token requested for $trimmedEmail.")
        Result.success(Unit)
    }

    override suspend fun updateProfile(
        name: String,
        profileImageUrl: String?,
        voiceLanguage: String?,
        clearProfileImage: Boolean
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(IllegalStateException("No active session."))
        val accounts = loadAllAccounts().toMutableList()
        val index = accounts.indexOfFirst { it.user.id == user.id }
        if (index < 0) return@withContext Result.failure(IllegalStateException("User not found."))

        val newProfileImageUrl = if (clearProfileImage) null else (profileImageUrl ?: user.profileImageUrl)
        val updatedUser = user.copy(
            name = name.trim().ifBlank { user.name },
            profileImageUrl = newProfileImageUrl,
            voiceLanguage = voiceLanguage ?: user.voiceLanguage
        )

        accounts[index] = accounts[index].copy(user = updatedUser)
        saveAllAccounts(accounts)
        _currentUser.value = updatedUser
        _authState.value = AuthState.LoggedIn(updatedUser, activeVehicle)
        Result.success(updatedUser)
    }

    override fun refreshVehicleContext(vehicle: VehicleProfile?) {
        activeVehicle = vehicle
        val user = _currentUser.value
        if (user != null) {
            _authState.value = AuthState.LoggedIn(user, vehicle)
        }
    }

    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    private fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && emailRegex.matches(email)
    }

    private fun findUserById(id: String): UserProfile? {
        return loadAllAccounts().find { it.user.id == id }?.user
    }

    private fun saveActiveSession(userId: String) {
        try {
            val obj = JSONObject().apply {
                put("activeUserId", userId)
                put("sessionToken", UUID.randomUUID().toString())
                put("createdAtMillis", System.currentTimeMillis())
            }
            sessionFile.writeText(obj.toString(2))
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.APP, "Auth: Failed to save session", e)
        }
    }

    private fun loadAllAccounts(): List<StoredAccount> {
        if (!accountsFile.exists()) return emptyList()
        return try {
            val jsonStr = accountsFile.readText()
            if (jsonStr.isBlank()) return emptyList()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<StoredAccount>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val user = UserProfile(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    email = obj.getString("email"),
                    profileImageUrl = obj.optString("profileImageUrl").takeIf { it.isNotBlank() && it != "null" },
                    voiceLanguage = obj.optString("voiceLanguage", "auto"),
                    createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis())
                )
                val hash = obj.getString("passwordHash")
                list.add(StoredAccount(user, hash))
            }
            list
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.APP, "Auth: Failed to read accounts file", e)
            emptyList()
        }
    }

    private fun saveAllAccounts(accounts: List<StoredAccount>) {
        try {
            val array = JSONArray()
            for (acc in accounts) {
                val obj = JSONObject().apply {
                    put("id", acc.user.id)
                    put("name", acc.user.name)
                    put("email", acc.user.email)
                    put("profileImageUrl", acc.user.profileImageUrl ?: JSONObject.NULL)
                    put("voiceLanguage", acc.user.voiceLanguage)
                    put("createdAtMillis", acc.user.createdAtMillis)
                    put("passwordHash", acc.passwordHash)
                }
                array.put(obj)
            }
            accountsFile.writeText(array.toString(2))
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.APP, "Auth: Failed to write accounts file", e)
        }
    }

    private data class StoredAccount(
        val user: UserProfile,
        val passwordHash: String
    ) {
        val email: String get() = user.email
    }
}
