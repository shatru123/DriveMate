package com.shatrughna.drivemate.voice

/**
 * Optional AI / external question fallback interface for queries not handled
 * by DriveMate's local deterministic companion rules.
 */
interface VoiceQuestionFallback {
    /**
     * Answers an open-ended question in the specified language, or returns null
     * if no fallback provider is configured or able to respond.
     */
    suspend fun answer(
        text: String,
        language: VoiceLanguage
    ): String?
}
