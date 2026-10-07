package com.shatrughna.drivemate.car

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/**
 * Entry point for Android Auto Car Screen application.
 * Discovered by Android Auto head units via the official AndroidX Car App Library.
 */
class DriveMateCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        // Allows official Android Auto head units, Desktop Head Unit (DHU), and verified OEM hosts
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return DriveMateCarSession()
    }
}
