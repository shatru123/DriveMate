package com.shatrughna.drivemate.car.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template
import com.shatrughna.drivemate.DriveMateApplication
import com.shatrughna.drivemate.voice.VoiceAssistantState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Dedicated Android Auto Voice Assistant Screen.
 * Prompts the driver and coordinates with VoiceAssistantManager.
 */
class CarVoiceAssistantScreen(carContext: CarContext) : Screen(carContext) {

    private val app = carContext.applicationContext as DriveMateApplication
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentState: VoiceAssistantState = VoiceAssistantState.Idle

    init {
        scope.launch {
            app.voiceAssistantManager.state.collectLatest { state ->
                currentState = state
                invalidate()
            }
        }
        // Auto-start listening on screen entry
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
