package com.shatrughna.drivemate.ui.auth

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shatrughna.drivemate.auth.model.AuthState
import com.shatrughna.drivemate.auth.model.UserProfile
import com.shatrughna.drivemate.auth.repository.AuthRepository
import com.shatrughna.drivemate.util.AppLogger
import com.shatrughna.drivemate.vehicle.model.VehicleProfile
import com.shatrughna.drivemate.vehicle.repository.VehicleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState
    val currentUser: StateFlow<UserProfile?> = authRepository.currentUser
    val currentVehicle: StateFlow<VehicleProfile?> = vehicleRepository.currentVehicle
    val profilePhotoLoading = MutableStateFlow(false)

    // Login Form State
    val loginEmail = MutableStateFlow("")
    val loginPassword = MutableStateFlow("")
    val loginLoading = MutableStateFlow(false)
    val loginError = MutableStateFlow<String?>(null)

    // Sign Up Form State
    val signUpName = MutableStateFlow("")
    val signUpEmail = MutableStateFlow("")
    val signUpPassword = MutableStateFlow("")
    val signUpConfirmPassword = MutableStateFlow("")
    val signUpProfilePhotoUri = MutableStateFlow<String?>(null)
    val signUpLoading = MutableStateFlow(false)
    val signUpError = MutableStateFlow<String?>(null)

    // Forgot Password Form State
    val forgotEmail = MutableStateFlow("")
    val forgotLoading = MutableStateFlow(false)
    val forgotSuccessMessage = MutableStateFlow<String?>(null)
    val forgotError = MutableStateFlow<String?>(null)

    // Vehicle Setup Form State
    val vehicleMake = MutableStateFlow("")
    val vehicleModel = MutableStateFlow("")
    val vehicleVariant = MutableStateFlow("")
    val vehicleYear = MutableStateFlow("")
    val vehicleReg = MutableStateFlow("")
    val vehicleFuel = MutableStateFlow("Petrol")
    val vehicleTransmission = MutableStateFlow("Manual")
    val vehicleOdometer = MutableStateFlow("")
    val vehicleNickname = MutableStateFlow("")
    val vehicleLoading = MutableStateFlow(false)
    val vehicleError = MutableStateFlow<String?>(null)

    fun login(onSuccess: () -> Unit = {}) {
        val email = loginEmail.value.trim()
        val password = loginPassword.value

        if (email.isBlank()) {
            loginError.value = "Please enter your email."
            return
        }
        if (password.isBlank()) {
            loginError.value = "Please enter your password."
            return
        }

        viewModelScope.launch {
            loginLoading.value = true
            loginError.value = null
            val result = authRepository.login(email, password)
            loginLoading.value = false
            result.onSuccess { user ->
                val vehicle = vehicleRepository.loadVehicleForUser(user.id)
                authRepository.refreshVehicleContext(vehicle)
                clearLoginForm()
                onSuccess()
            }.onFailure { ex ->
                loginError.value = ex.message ?: "Authentication failed."
            }
        }
    }

    fun setSignUpProfilePhoto(uri: String?) {
        signUpProfilePhotoUri.value = uri
    }

    fun signUp(context: Context? = null, onSuccess: () -> Unit = {}) {
        val name = signUpName.value.trim()
        val email = signUpEmail.value.trim()
        val password = signUpPassword.value
        val confirm = signUpConfirmPassword.value

        if (name.isBlank()) {
            signUpError.value = "Please enter your full name."
            return
        }
        if (email.isBlank()) {
            signUpError.value = "Please enter your email address."
            return
        }
        if (password.length < 6) {
            signUpError.value = "Password must be at least 6 characters long."
            return
        }
        if (password != confirm) {
            signUpError.value = "Passwords do not match."
            return
        }

        viewModelScope.launch {
            signUpLoading.value = true
            signUpError.value = null
            val result = authRepository.signUp(name, email, password)
            signUpLoading.value = false
            result.onSuccess { newUser ->
                val selectedUri = signUpProfilePhotoUri.value
                if (context != null && !selectedUri.isNullOrBlank()) {
                    try {
                        val userDir = File(context.filesDir, "users/${newUser.id}").apply { mkdirs() }
                        val destFile = File(userDir, "profile_avatar.jpg")
                        context.contentResolver.openInputStream(Uri.parse(selectedUri))?.use { input ->
                            destFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        authRepository.updateProfile(newUser.name, profileImageUrl = destFile.absolutePath)
                    } catch (e: Exception) {
                        AppLogger.e(AppLogger.Tag.APP, "Failed to save initial profile photo", e)
                    }
                }
                clearSignUpForm()
                onSuccess()
            }.onFailure { ex ->
                signUpError.value = ex.message ?: "Registration failed."
            }
        }
    }

    fun resetPassword() {
        val email = forgotEmail.value.trim()
        if (email.isBlank()) {
            forgotError.value = "Please enter your account email address."
            return
        }

        viewModelScope.launch {
            forgotLoading.value = true
            forgotError.value = null
            forgotSuccessMessage.value = null
            val result = authRepository.resetPassword(email)
            forgotLoading.value = false
            result.onSuccess {
                forgotSuccessMessage.value = "Password reset instructions sent to $email."
            }.onFailure { ex ->
                forgotError.value = ex.message ?: "Unable to reset password."
            }
        }
    }

    fun saveVehicle(onSuccess: () -> Unit = {}) {
        val user = currentUser.value
        if (user == null) {
            vehicleError.value = "You must be signed in to register a vehicle."
            return
        }

        val make = vehicleMake.value.trim()
        val model = vehicleModel.value.trim()
        val variant = vehicleVariant.value.trim()
        val reg = vehicleReg.value.trim()
        val fuel = vehicleFuel.value
        val trans = vehicleTransmission.value
        val nick = vehicleNickname.value.trim()
        val yearInt = vehicleYear.value.trim().toIntOrNull()
        val odoDouble = vehicleOdometer.value.trim().toDoubleOrNull()

        if (make.isBlank()) {
            vehicleError.value = "Please enter vehicle make / brand."
            return
        }
        if (model.isBlank()) {
            vehicleError.value = "Please enter vehicle model."
            return
        }

        val profile = VehicleProfile(
            userId = user.id,
            make = make,
            model = model,
            variant = variant,
            year = yearInt,
            registrationNumber = reg,
            fuelType = fuel,
            transmission = trans,
            nickname = nick,
            currentOdometer = odoDouble
        )

        viewModelScope.launch {
            vehicleLoading.value = true
            vehicleError.value = null
            val result = vehicleRepository.saveVehicle(profile)
            vehicleLoading.value = false
            result.onSuccess { savedVehicle ->
                authRepository.refreshVehicleContext(savedVehicle)
                clearVehicleForm()
                onSuccess()
            }.onFailure { ex ->
                vehicleError.value = ex.message ?: "Failed to save vehicle details."
            }
        }
    }

    fun uploadProfilePhoto(context: Context, sourceUri: Uri) {
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            profilePhotoLoading.value = true
            try {
                val userDir = File(context.filesDir, "users/${user.id}").apply { mkdirs() }
                val destinationFile = File(userDir, "profile_avatar.jpg")
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    destinationFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                authRepository.updateProfile(
                    name = user.name,
                    profileImageUrl = destinationFile.absolutePath
                )
                AppLogger.i(AppLogger.Tag.APP, "Profile photo updated to ${destinationFile.absolutePath}")
            } catch (e: Exception) {
                AppLogger.e(AppLogger.Tag.APP, "Failed to upload profile photo", e)
            } finally {
                profilePhotoLoading.value = false
            }
        }
    }

    fun removeProfilePhoto(context: Context) {
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            profilePhotoLoading.value = true
            try {
                val userDir = File(context.filesDir, "users/${user.id}")
                val file = File(userDir, "profile_avatar.jpg")
                if (file.exists()) {
                    file.delete()
                }
                authRepository.updateProfile(
                    name = user.name,
                    profileImageUrl = null,
                    clearProfileImage = true
                )
                AppLogger.i(AppLogger.Tag.APP, "Profile photo removed")
            } catch (e: Exception) {
                AppLogger.e(AppLogger.Tag.APP, "Failed to remove profile photo", e)
            } finally {
                profilePhotoLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            vehicleRepository.clearVehicleContext()
            clearAllForms()
        }
    }

    fun clearErrors() {
        loginError.value = null
        signUpError.value = null
        forgotError.value = null
        vehicleError.value = null
    }

    private fun clearLoginForm() {
        loginEmail.value = ""
        loginPassword.value = ""
        loginError.value = null
    }

    private fun clearSignUpForm() {
        signUpName.value = ""
        signUpEmail.value = ""
        signUpPassword.value = ""
        signUpConfirmPassword.value = ""
        signUpProfilePhotoUri.value = null
        signUpError.value = null
    }

    private fun clearVehicleForm() {
        vehicleMake.value = ""
        vehicleModel.value = ""
        vehicleVariant.value = ""
        vehicleYear.value = ""
        vehicleReg.value = ""
        vehicleFuel.value = "Petrol"
        vehicleTransmission.value = "Manual"
        vehicleOdometer.value = ""
        vehicleNickname.value = ""
        vehicleError.value = null
    }

    private fun clearAllForms() {
        clearLoginForm()
        clearSignUpForm()
        clearVehicleForm()
        forgotEmail.value = ""
        forgotSuccessMessage.value = null
        forgotError.value = null
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val vehicleRepository: VehicleRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authRepository, vehicleRepository) as T
        }
    }
}
