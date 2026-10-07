package com.shatrughna.drivemate.core.security

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.DialogInterface
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.shatrughna.drivemate.util.AppLogger

/**
 * Native Android biometric and device credential authenticator.
 * Uses Android framework BiometricPrompt (API 28+) and KeyguardManager without external dependencies.
 */
class BiometricSecurityManager(private val context: Context) {

    private val keyguardManager: KeyguardManager? by lazy {
        context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    }

    /**
     * Checks if the device has a secure lock screen (PIN/Pattern/Password/Biometrics) enabled.
     */
    fun isDeviceSecure(): Boolean {
        return keyguardManager?.isDeviceSecure == true || keyguardManager?.isKeyguardSecure == true
    }

    /**
     * Authenticates the driver before accessing sensitive vehicle credentials or documents.
     */
    fun authenticate(
        activity: Activity,
        title: String = "Unlock Document Vault",
        subtitle: String = "Confirm identity to view sensitive vehicle documents",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isDeviceSecure()) {
            AppLogger.w(AppLogger.Tag.APP, "Device has no secure lockscreen configured. Allowing access.")
            onSuccess()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            authenticateWithBiometricPrompt(activity, title, subtitle, onSuccess, onError)
        } else {
            // Devices below API 28: Allow access if device is unlocked
            onSuccess()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun authenticateWithBiometricPrompt(
        activity: Activity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cancellationSignal = CancellationSignal()
        val executor = ContextCompat.getMainExecutor(activity)

        val prompt = BiometricPrompt.Builder(activity)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription("DriveMate safeguards your RC, Insurance, and Driving Licence.")
            .setNegativeButton(
                "Cancel",
                executor,
                DialogInterface.OnClickListener { _, _ ->
                    onError("Authentication cancelled by driver.")
                }
            )
            .build()

        prompt.authenticate(
            cancellationSignal,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    super.onAuthenticationSucceeded(result)
                    AppLogger.i(AppLogger.Tag.APP, "Driver biometric authentication succeeded.")
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    AppLogger.w(AppLogger.Tag.APP, "Biometric error: code=$errorCode, msg=$errString")
                    // If error code is user cancellation or negative button, report cleanly
                    onError(errString?.toString() ?: "Authentication failed")
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    AppLogger.w(AppLogger.Tag.APP, "Biometric verification failed.")
                }
            }
        )
    }
}
