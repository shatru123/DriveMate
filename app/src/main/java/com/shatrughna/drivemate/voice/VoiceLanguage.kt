package com.shatrughna.drivemate.voice

import java.util.Locale

/**
 * Supported spoken languages for DriveMate voice interactions.
 */
enum class VoiceLanguage(val code: String, val displayName: String, val tag: String) {
    ENGLISH("en", "English", "en-IN"),
    HINDI("hi", "Hindi", "hi-IN"),
    MARATHI("mr", "Marathi", "mr-IN"),
    MIXED("mixed", "Mixed", "en-IN"),
    UNKNOWN("unknown", "Unknown", "en-IN");

    val locale: Locale
        get() = when (this) {
            ENGLISH -> Locale.forLanguageTag("en-IN")
            HINDI -> Locale("hi", "IN")
            MARATHI -> Locale("mr", "IN")
            MIXED, UNKNOWN -> Locale.forLanguageTag("en-IN")
        }

    companion object {
        fun fromCode(code: String?): VoiceLanguage {
            return when (code?.lowercase()?.trim()) {
                "en", "english" -> ENGLISH
                "hi", "hindi" -> HINDI
                "mr", "marathi" -> MARATHI
                "mixed" -> MIXED
                else -> ENGLISH
            }
        }
    }
}

/**
 * Lightweight, zero-latency language classifier tailored for driver commands.
 * Identifies script boundaries, Marathi/Hindi morphological markers, and mixed-language intent.
 */
object VoiceLanguageClassifier {

    private val MARATHI_KEYWORDS = setOf(
        "कुठे", "कुठं", "कुठेही",
        "माझी", "माझा", "माझे", "माझ्या", "माझं",
        "किती", "कधी",
        "गाडी", "गाडीचा", "गाडीची", "गाडीचे", "गाडीत", "गाडीला", "गाडीमध्ये",
        "सांग", "सांगा",
        "कसं", "कसा", "कशी", "कसे",
        "चाललोय", "चालवली", "चालवत", "चालली", "चाललं",
        "पडेल", "पडणार", "पडतोय",
        "आहे", "आहेत", "आहेस", "नाही",
        "मला", "आम्हाला",
        "शोध", "बघ", "दाखव",
        "थांब", "थांबा", "राहू", "दे", "एवढंच", "बस",
        "नमस्कार", "आभार",
        "वेग", "सरासरी",
        "शिल्लक", "बाकी",
        "केला", "केली", "केले", "केलं", "झाला", "झाली", "झाले", "झालं",
        "घरी", "घर",
        // Transliterated romanized Marathi tokens
        "kuthe", "mazi", "maza", "maze", "mazya", "kiti", "kadhi",
        "gadi", "sang", "kasam", "kasa", "kashi", "chalaloy", "chalavli",
        "chalali", "padel", "padnar", "ahe", "ahes", "mala", "shodh",
        "dakhva", "thamb", "rahu", "evdhach", "namaskar", "sarasari",
        "shillak", "kela", "keli", "kele", "ghari"
    )

    private val HINDI_KEYWORDS = setOf(
        "कहाँ", "किधर", "कहा",
        "मेरी", "मेरा", "मेरे",
        "कितना", "कितनी", "कितने", "कब",
        "गाड़ी", "गाडी", "गाड़ी की", "गाड़ी का", "गाड़ी के", "गाड़ी में",
        "बताओ", "बताइए",
        "कैसा", "कैसी", "कैसे",
        "चला", "चलाई", "चलाया", "रहा", "रही", "रहे", "हूँ", "हो", "है", "हैं",
        "होगी", "होगा", "होंगे",
        "मुझे", "हमको", "हमें",
        "ढूंढो", "खोजो", "दिखाओ", "दिखाइए",
        "रुको", "रहने", "अलविदा", "शुक्रिया",
        "नमस्ते", "धन्यवाद",
        "औसत", "चाल",
        "बचा", "बची", "बचे",
        "किया", "की", "किए", "हुआ", "हुई", "हुए",
        // Transliterated romanized Hindi tokens
        "kahan", "kaha", "kidhar", "meri", "mera", "mere",
        "kitna", "kitni", "kitne", "kab", "batao", "bataiye",
        "kaisa", "kaisi", "kaise", "chala", "raha", "rahi", "hoon",
        "hai", "hain", "hogi", "hoga", "mujhe", "dhoondo", "khojo",
        "dikhao", "ruko", "rehne", "shukriya", "namaste", "dhanyawad",
        "ausat", "bacha", "bachi", "kiya"
    )

    /**
     * Cleans and normalizes input text for resilient pattern matching.
     */
    fun normalize(text: String): String {
        return text
            .lowercase(Locale.getDefault())
            .replace(Regex("""[?,.!;:\"'()\[\]{}—–\-_/\\|`~^#*+=]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    /**
     * Determines whether the given text contains any Devanagari script characters.
     */
    fun containsDevanagari(text: String): Boolean {
        return text.any { it in '\u0900'..'\u097F' }
    }

    /**
     * Determines whether the given text contains Latin alphabet characters.
     */
    fun containsLatin(text: String): Boolean {
        return text.any { it in 'a'..'z' || it in 'A'..'Z' }
    }

    /**
     * Detects the linguistic category of the raw user utterance.
     */
    fun detectLanguage(rawText: String): VoiceLanguage {
        val normalized = normalize(rawText)
        if (normalized.isBlank()) return VoiceLanguage.UNKNOWN

        val hasDevanagari = containsDevanagari(normalized)
        val hasLatin = containsLatin(normalized)

        val tokens = normalized.split(" ")
        var marathiScore = 0
        var hindiScore = 0

        for (token in tokens) {
            if (token in MARATHI_KEYWORDS) marathiScore++
            if (token in HINDI_KEYWORDS) hindiScore++
        }

        // Mixed script detection (Devanagari + Latin) or mixed vocabulary
        if (hasDevanagari && hasLatin) {
            return VoiceLanguage.MIXED
        }

        // Pure Devanagari script evaluation
        if (hasDevanagari && !hasLatin) {
            return when {
                marathiScore > hindiScore -> VoiceLanguage.MARATHI
                hindiScore > marathiScore -> VoiceLanguage.HINDI
                marathiScore > 0 -> VoiceLanguage.MARATHI
                hindiScore > 0 -> VoiceLanguage.HINDI
                else -> VoiceLanguage.HINDI // Default Devanagari when ambiguous
            }
        }

        // Latin script evaluation (could be English, Hinglish, or Maranglish)
        if (hasLatin) {
            if (marathiScore > 0 && hindiScore == 0) return VoiceLanguage.MIXED
            if (hindiScore > 0 && marathiScore == 0) return VoiceLanguage.MIXED
            if (marathiScore > 0 || hindiScore > 0) return VoiceLanguage.MIXED
            return VoiceLanguage.ENGLISH
        }

        return VoiceLanguage.UNKNOWN
    }

    /**
     * Resolves the primary response language for conversational output.
     * In mixed speech (e.g., "माझी car speed किती आहे?"), resolves to the dominant dialect.
     */
    fun resolveResponseLanguage(rawText: String, preferredLanguage: String = "auto"): VoiceLanguage {
        when (preferredLanguage.lowercase()) {
            "en", "english" -> return VoiceLanguage.ENGLISH
            "hi", "hindi" -> return VoiceLanguage.HINDI
            "mr", "marathi" -> return VoiceLanguage.MARATHI
        }

        val normalized = normalize(rawText)
        if (normalized.isBlank()) return VoiceLanguage.ENGLISH

        val tokens = normalized.split(" ")
        var marathiScore = 0
        var hindiScore = 0

        for (token in tokens) {
            if (token in MARATHI_KEYWORDS) marathiScore++
            if (token in HINDI_KEYWORDS) hindiScore++
        }

        return when {
            marathiScore > hindiScore -> VoiceLanguage.MARATHI
            hindiScore > marathiScore -> VoiceLanguage.HINDI
            containsDevanagari(rawText) -> VoiceLanguage.HINDI
            else -> VoiceLanguage.ENGLISH
        }
    }
}
