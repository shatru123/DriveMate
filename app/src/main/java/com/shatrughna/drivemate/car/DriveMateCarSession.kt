package com.shatrughna.drivemate.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import com.shatrughna.drivemate.car.screens.DriveMateHomeScreen

/**
 * Android Auto Car Session that manages screen backstack and life cycle.
 */
class DriveMateCarSession : Session() {

    override fun onCreateScreen(intent: Intent): Screen {
        return DriveMateHomeScreen(carContext)
    }
}
