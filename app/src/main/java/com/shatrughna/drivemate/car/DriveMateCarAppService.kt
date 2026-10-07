package com.shatrughna.drivemate.car

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.shatrughna.drivemate.BuildConfig
import com.shatrughna.drivemate.util.AppLogger

/**
 * Entry point for Android Auto Car Screen application.
 * Discovered by Android Auto head units via the official AndroidX Car App Library.
 */
class DriveMateCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        return if (BuildConfig.DEBUG) {
            AppLogger.d(AppLogger.Tag.APP, "CarAppService: Using ALLOW_ALL_HOSTS_VALIDATOR for debug / DHU testing.")
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            AppLogger.i(AppLogger.Tag.APP, "CarAppService: Using official Android for Cars production host allowlist.")
            HostValidator.Builder(applicationContext)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build()
        }
    }

    override fun onCreateSession(): Session {
        return DriveMateCarSession()
    }
}
