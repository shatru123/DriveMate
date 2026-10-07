package com.shatrughna.drivemate.car.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.shatrughna.drivemate.DriveMateApplication
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

        // Push-to-talk: Trigger single voice request on entry
        app.voiceAssistantManager.startListening()
    }

    override fun onGetTemplate(): Template {
        val message = when (val s = currentState) {
            is VoiceAssistantState.Listening -> "Listening... Speak your command now.\n\nExamples:\n• \"Play music on Spotify\"\n• \"Navigate to office\"\n• \"What's the weather?\""
            is VoiceAssistantState.Processing -> "Processing: \"${s.recognizedText}\"..."
            is VoiceAssistantState.Responding -> "Responding: \"${s.speechText}\""
            is VoiceAssistantState.Error -> "Error: ${s.message}\nTap Speak to retry."
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
                        app.voiceAssistantManager.startListening()
                    }
                    .build()
            )
            .build()
    }
}
