package com.shatrughna.drivemate

import android.app.Application
import com.shatrughna.drivemate.car.AndroidAutoCarConnectionManager
import com.shatrughna.drivemate.car.CarConnectionManager
import com.shatrughna.drivemate.data.preferences.DriveMatePreferencesRepository
import com.shatrughna.drivemate.data.preferences.DriveMatePreferencesRepositoryImpl
import com.shatrughna.drivemate.driving.DrivingSessionManager
import com.shatrughna.drivemate.driving.DrivingSessionManagerImpl
import com.shatrughna.drivemate.greeting.GreetingController
import com.shatrughna.drivemate.greeting.GreetingControllerImpl
import com.shatrughna.drivemate.greeting.GreetingGenerator
import com.shatrughna.drivemate.greeting.GreetingGeneratorImpl
import com.shatrughna.drivemate.greeting.GreetingTtsManager
import com.shatrughna.drivemate.greeting.GreetingTtsManagerImpl
import com.shatrughna.drivemate.util.AppLogger

class DriveMateApplication : Application() {

    lateinit var preferencesRepository: DriveMatePreferencesRepository
        private set

    lateinit var carConnectionManager: CarConnectionManager
        private set

    lateinit var sessionManager: DrivingSessionManager
        private set

    lateinit var greetingGenerator: GreetingGenerator
        private set

    lateinit var ttsManager: GreetingTtsManager
        private set

    lateinit var greetingController: GreetingController
        private set

    override fun onCreate() {
        super.onCreate()
        AppLogger.i(AppLogger.Tag.APP, "Initializing DriveMate Application...")

        preferencesRepository = DriveMatePreferencesRepositoryImpl(this)
        carConnectionManager = AndroidAutoCarConnectionManager(this)
        sessionManager = DrivingSessionManagerImpl(carConnectionManager)
        greetingGenerator = GreetingGeneratorImpl()
        ttsManager = GreetingTtsManagerImpl(this)

        greetingController = GreetingControllerImpl(
            preferencesRepository = preferencesRepository,
            sessionManager = sessionManager,
            greetingGenerator = greetingGenerator,
            ttsManager = ttsManager
        )

        // Start connection monitoring and greeting controller
        carConnectionManager.startMonitoring()
        greetingController.start()
    }
}
