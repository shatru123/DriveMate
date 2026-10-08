package com.shatrughna.drivemate.car

import android.content.pm.PackageManager
import androidx.car.app.CarContext
import androidx.car.app.OnRequestPermissionsListener
import androidx.core.content.ContextCompat
import com.shatrughna.drivemate.util.AppLogger

/** Centralizes Android Auto host permission requests and their safe fallback. */
object DriveMateCarPermissions {
    const val CAR_SPEED = "com.google.android.gms.permission.CAR_SPEED"
    const val CAR_MILEAGE = "com.google.android.gms.permission.CAR_MILEAGE"
    const val CAR_FUEL = "com.google.android.gms.permission.CAR_FUEL"

    val vehicleDataPermissions = listOf(CAR_SPEED, CAR_MILEAGE, CAR_FUEL)

    fun request(
        carContext: CarContext,
        permissions: List<String>,
        onResult: (approved: List<String>, rejected: List<String>) -> Unit
    ) {
        val missing = permissions.filter { permission ->
            carContext.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            onResult(permissions, emptyList())
            return
        }

        try {
            carContext.requestPermissions(
                missing,
                ContextCompat.getMainExecutor(carContext),
                object : OnRequestPermissionsListener {
                    override fun onRequestPermissionsResult(
                        approvedPermissions: List<String>,
                        rejectedPermissions: List<String>
                    ) {
                        onResult(approvedPermissions, rejectedPermissions)
                    }
                }
            )
        } catch (securityException: SecurityException) {
            AppLogger.w(
                AppLogger.TAG_ANDROID_AUTO,
                "Android Auto permission request was rejected by the host",
                securityException
            )
            onResult(emptyList(), missing)
        } catch (exception: Exception) {
            AppLogger.w(
                AppLogger.TAG_ANDROID_AUTO,
                "Android Auto permission request failed",
                exception
            )
            onResult(emptyList(), missing)
        }
    }
}
