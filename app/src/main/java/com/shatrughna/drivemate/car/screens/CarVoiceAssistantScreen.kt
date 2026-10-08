package com.shatrughna.drivemate.car.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.shatrughna.drivemate.DriveMateApplication
import com.shatrughna.drivemate.car.DriveMateCarPermissions
import com.shatrughna.drivemate.util.AppLogger
import com.shatrughna.drivemate.voice.VoiceAssistantState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Dedicated Android Auto Voice Assistant Screen.
 * Push-to-talk only, with strict lifecycle cleanup ensuring audio focus
 * and microphone resources are released when exiting or popping the screen.
 */
class CarVoiceAssistantScreen(carContext: CarContext) : Screen(carContext) {

    private val app = carContext.applicationContext as DriveMateApplication
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentState: VoiceAssistantState = VoiceAssistantState.Idle

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                AppLogger.d(AppLogger.TAG_AUDIO_FOCUS, "CarVoiceAssistantScreen onStop - stopping listening")
                app.voiceAssistantManager.stopListening()
                app.audioInputCoordinator.abandonAudioFocus()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                AppLogger.d(AppLogger.TAG_AUDIO_FOCUS, "CarVoiceAssistantScreen onDestroy - cancelling scope and cleaning up")
                scope.cancel()
                app.voiceAssistantManager.stopListening()
                app.audioInputCoordinator.abandonAudioFocus()
            }
        })

        scope.launch {
            app.voiceAssistantManager.state.collectLatest { state ->
                currentState = state
                invalidate()
            }
        }

        // Opening the screen is passive. Microphone access starts only after the driver taps Speak.
    }

    override fun onGetTemplate(): Template {
        val message = when (val s = currentState) {
            is VoiceAssistantState.Listening -> "Listening... Speak your command now.\n\nExamples:\n• \"Play music on Spotify\"\n• \"Navigate to office\"\n• \"What's the weather?\""
            is VoiceAssistantState.Processing -> "Processing: \"${s.recognizedText}\"..."
            is VoiceAssistantState.Responding -> "Responding: \"${s.speechText}\""
            is VoiceAssistantState.Error -> s.message
            is VoiceAssistantState.Idle -> "DriveMate Voice Assistant ready.\nTap Speak to begin."
        }

        val header = Header.Builder()
            .setTitle("DriveMate Voice Assistant")
            .setStartHeaderAction(Action.BACK)
            .build()

        return MessageTemplate.Builder(message)
            .setHeader(header)
            .addAction(
                Action.Builder()
                    .setTitle("Speak")
                    .setOnClickListener {
                        requestMicrophoneAndStart()
                    }
                    .build()
            )
            .build()
    }

    private fun requestMicrophoneAndStart() {
        if (carContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            app.voiceAssistantManager.startListening()
            return
        }

        DriveMateCarPermissions.request(
            carContext = carContext,
            permissions = listOf(Manifest.permission.RECORD_AUDIO)
        ) { approvedPermissions, rejectedPermissions ->
            if (Manifest.permission.RECORD_AUDIO in approvedPermissions && rejectedPermissions.isEmpty()) {
                app.voiceAssistantManager.startListening()
            } else {
                app.audioInputCoordinator.setError("Microphone permission denied")
                currentState = VoiceAssistantState.Error(
                    "Microphone permission is required. Use your phone to grant access, then try again."
                )
                invalidate()
            }
        }
    }
}
