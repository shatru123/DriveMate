package com.shatrughna.drivemate.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.shatrughna.drivemate.DriveMateApplication
import com.shatrughna.drivemate.car.screens.DriveMateHomeScreen
import com.shatrughna.drivemate.util.AppLogger

/**
 * Android Auto Car Session that manages screen backstack and life cycle.
 * Coordinates attaching and detaching CarContext from VehicleDataCoordinator.
 */
class DriveMateCarSession : Session() {

    override fun onCreateScreen(intent: Intent): Screen {
        AppLogger.i(AppLogger.TAG_ANDROID_AUTO, "DriveMateCarSession.onCreateScreen - initializing car session")
        val app = carContext.applicationContext as? DriveMateApplication
        app?.vehicleDataCoordinator?.attachCarContext(carContext)

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                AppLogger.i(AppLogger.TAG_ANDROID_AUTO, "DriveMateCarSession onDestroy - releasing car session resources")
                app?.vehicleDataCoordinator?.detachCarContext()
            }
        })

        return DriveMateHomeScreen(carContext)
    }
}
