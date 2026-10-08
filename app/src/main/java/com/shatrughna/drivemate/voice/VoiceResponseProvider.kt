package com.shatrughna.drivemate.voice

import com.shatrughna.drivemate.core.telemetry.TelemetryAvailability
import com.shatrughna.drivemate.core.telemetry.TelemetrySource
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Centralized, driver-friendly localized response provider.
 * Strict data honesty: Never fabricates telemetry, speeds, fuel, or ranges.
 */
class VoiceResponseProvider {

    fun speed(
        language: VoiceLanguage,
        speedKmh: Float?,
        source: TelemetrySource,
        availability: TelemetryAvailability
    ): String {
        val validSpeed = speedKmh?.takeIf { it.isFinite() && it >= 0f }
        if (validSpeed == null || availability == TelemetryAvailability.UNAVAILABLE || availability == TelemetryAvailability.NOT_CONNECTED) {
            return when (language) {
                VoiceLanguage.HINDI -> "अभी आपकी स्पीड उपलब्ध नहीं है।"
                VoiceLanguage.MARATHI -> "सध्या तुमचा वेग उपलब्ध नाही."
                else -> "I can't get your current speed right now."
            }
        }

        if (availability == TelemetryAvailability.STALE) {
            return when (language) {
                VoiceLanguage.HINDI -> "पुराना स्पीड डेटा है, लेकिन वर्तमान स्पीड कन्फर्म नहीं है।"
                VoiceLanguage.MARATHI -> "जुनी स्पीड नोंद आहे, पण सध्याचा वेग निश्चित सांगता येत नाही."
                else -> "I have an older speed reading, but I can't confirm your current speed."
            }
        }

        val roundedSpeed = validSpeed.roundToInt()
        return if (source == TelemetrySource.PHONE_GPS) {
            when (language) {
                VoiceLanguage.HINDI -> "आपकी वर्तमान जीपीएस स्पीड $roundedSpeed किलोमीटर प्रति घंटा है।"
                VoiceLanguage.MARATHI -> "तुमचा सध्याचा जीपीएस वेग $roundedSpeed किलोमीटर प्रति तास आहे."
                else -> "Your current GPS speed is $roundedSpeed kilometers per hour."
            }
        } else {
            when (language) {
                VoiceLanguage.HINDI -> "अभी आपकी स्पीड $roundedSpeed किलोमीटर प्रति घंटा है।"
                VoiceLanguage.MARATHI -> "तुमचा सध्याचा वेग $roundedSpeed किलोमीटर प्रति तास आहे."
                else -> "You are currently driving at $roundedSpeed kilometers per hour."
            }
        }
    }

    fun odometer(
        language: VoiceLanguage,
        odometerKm: Double?,
        isAuthoritative: Boolean
    ): String {
        if (odometerKm == null || odometerKm < 0.0) {
            return when (language) {
                VoiceLanguage.HINDI -> "अभी भरोसेमंद ओडोमीटर रीडिंग उपलब्ध नहीं है।"
                VoiceLanguage.MARATHI -> "सध्या खात्रीशीर ओडोमीटर वाचन उपलब्ध नाही."
                else -> "I don't have a reliable odometer reading right now."
            }
        }
        val formattedKm = String.format(Locale.US, "%,.1f", odometerKm)
        return when (language) {
            VoiceLanguage.HINDI -> "आपकी गाड़ी $formattedKm किलोमीटर चली है।"
            VoiceLanguage.MARATHI -> "तुमच्या गाडीचे $formattedKm किलोमीटर झाले आहेत."
            else -> "Your vehicle has driven $formattedKm kilometers."
        }
    }

    fun fuel(
        language: VoiceLanguage,
        fuelPercent: Float?,
        availability: TelemetryAvailability
    ): String {
        val validFuel = fuelPercent?.takeIf { it.isFinite() && it in 0f..100f }
        if (validFuel == null || availability !in setOf(TelemetryAvailability.LIVE, TelemetryAvailability.STALE)) {
            return when (language) {
                VoiceLanguage.HINDI -> "कनेक्टेड वाहन से ईंधन स्तर की जानकारी उपलब्ध नहीं है।"
                VoiceLanguage.MARATHI -> "कनेक्ट केलेल्या वाहनाकडून इंधनाची माहिती उपलब्ध नाही."
                else -> "Fuel level isn't available from the connected vehicle data."
            }
        }
        val percent = validFuel.roundToInt()
        return when (language) {
            VoiceLanguage.HINDI -> "गाड़ी में अभी $percent प्रतिशत ईंधन बचा है।"
            VoiceLanguage.MARATHI -> "गाडीत सध्या $percent टक्के इंधन शिल्लक आहे."
            else -> "Your current fuel level is $percent percent."
        }
    }

    fun range(
        language: VoiceLanguage,
        rangeKm: Float?,
        availability: TelemetryAvailability
    ): String {
        val validRange = rangeKm?.takeIf { it.isFinite() && it >= 0f }
        if (validRange == null || availability !in setOf(TelemetryAvailability.LIVE, TelemetryAvailability.STALE)) {
            return when (language) {
                VoiceLanguage.HINDI -> "आपके वाहन से ड्राइविंग रेंज की जानकारी उपलब्ध नहीं है।"
                VoiceLanguage.MARATHI -> "तुमच्या वाहनाकडून ड्रायव्हिंग रेंजची माहिती उपलब्ध नाही."
                else -> "Range information isn't available from your vehicle."
            }
        }
        val range = validRange.roundToInt()
        return when (language) {
            VoiceLanguage.HINDI -> "आपकी बची हुई ड्राइविंग रेंज लगभग $range किलोमीटर है।"
            VoiceLanguage.MARATHI -> "तुमची अंदाजे ड्रायव्हिंग रेंज $range किलोमीटर आहे."
            else -> "Your estimated driving range is $range kilometers."
        }
    }

    fun vehicleStatus(
        language: VoiceLanguage,
        isCarConnected: Boolean,
        hasTelemetry: Boolean
    ): String {
        return if (!isCarConnected) {
            when (language) {
                VoiceLanguage.HINDI -> "अभी ड्राइवमेट से कोई वाहन कनेक्टेड नहीं है।"
                VoiceLanguage.MARATHI -> "सध्या ड्राईव्हमेटशी कोणतेही वाहन जोडलेले नाही."
                else -> "No vehicle is currently connected to DriveMate."
            }
        } else if (hasTelemetry) {
            when (language) {
                VoiceLanguage.HINDI -> "ड्राइवमेट कनेक्टेड है और लाइव व्हीकल टेलीमेट्री सक्रिय है।"
                VoiceLanguage.MARATHI -> "ड्राईव्हमेट कनेक्टेड असून थेट व्हेईकल टेलीमेट्री सुरू आहे."
                else -> "DriveMate is connected with live vehicle telemetry active."
            }
        } else {
            when (language) {
                VoiceLanguage.HINDI -> "ड्राइवमेट कनेक्टेड है, लेकिन अभी आपके वाहन की पूरी डायग्नोस्टिक जानकारी उपलब्ध नहीं है।"
                VoiceLanguage.MARATHI -> "ड्राईव्हमेट कनेक्टेड आहे, पण सध्या तुमच्या वाहनाची पूर्ण डायग्नोस्टिक माहिती उपलब्ध नाही."
                else -> "DriveMate is connected, but I don't currently have complete diagnostic information from your vehicle."
            }
        }
    }

    fun averageSpeed(
        language: VoiceLanguage,
        avgSpeedKmh: Float?
    ): String {
        val validSpeed = avgSpeedKmh?.takeIf { it.isFinite() && it > 0f }
        if (validSpeed == null) {
            return when (language) {
                VoiceLanguage.HINDI -> "वर्तमान ट्रिप के लिए औसत स्पीड अभी उपलब्ध नहीं है।"
                VoiceLanguage.MARATHI -> "सध्याच्या ट्रिपसाठी सरासरी वेग अजून उपलब्ध नाही."
                else -> "Average speed is not available yet for the current trip."
            }
        }
        val rounded = validSpeed.roundToInt()
        return when (language) {
            VoiceLanguage.HINDI -> "इस ट्रिप की औसत स्पीड $rounded किलोमीटर प्रति घंटा है।"
            VoiceLanguage.MARATHI -> "या ट्रिपचा सरासरी वेग $rounded किलोमीटर प्रति तास आहे."
            else -> "Your average speed for this trip is $rounded kilometers per hour."
        }
    }

    fun todayDriving(
        language: VoiceLanguage,
        todayDistanceKm: Float,
        todayTripsCount: Int,
        todayDurationMinutes: Long = 0L
    ): String {
        if (todayDistanceKm <= 0.05f && todayTripsCount == 0) {
            return when (language) {
                VoiceLanguage.HINDI -> "आज अभी तक कोई ड्राइव रिकॉर्ड नहीं हुई है।"
                VoiceLanguage.MARATHI -> "आज अजून कोणतीही ड्राईव्ह नोंदवलेली नाही."
                else -> "No completed drives recorded yet today."
            }
        }
        val distText = String.format(Locale.US, "%.1f", todayDistanceKm)
        return when (language) {
            VoiceLanguage.HINDI -> "आज आपने $todayTripsCount ट्रिप में $distText किलोमीटर गाड़ी चलाई है।"
            VoiceLanguage.MARATHI -> "आज तुम्ही $todayTripsCount ट्रिपमध्ये $distText किलोमीटर गाडी चालवली आहे."
            else -> "Today you drove $distText kilometers across $todayTripsCount trips."
        }
    }

    fun tripStats(
        language: VoiceLanguage,
        activeDurationSeconds: Long,
        activeDistanceKm: Float,
        todayDistanceKm: Float,
        todayTripsCount: Int
    ): String {
        val activeMins = activeDurationSeconds / 60
        if (activeMins > 0 || activeDistanceKm > 0.05f) {
            val distText = String.format(Locale.US, "%.1f", activeDistanceKm)
            return when (language) {
                VoiceLanguage.HINDI -> "आप $activeMins मिनट से गाड़ी चला रहे हैं और $distText किलोमीटर तय किए हैं।"
                VoiceLanguage.MARATHI -> "तुम्ही $activeMins मिनिटांपासून गाडी चालवत आहात आणि $distText किलोमीटर अंतर पार केले आहे."
                else -> "You have been driving for $activeMins minutes, covering $distText kilometers."
            }
        }
        val distText = String.format(Locale.US, "%.1f", todayDistanceKm)
        return when (language) {
            VoiceLanguage.HINDI -> "आज आपने $todayTripsCount ट्रिप में कुल $distText किलोमीटर गाड़ी चलाई है।"
            VoiceLanguage.MARATHI -> "आजचे एकूण ड्रायव्हिंग $todayTripsCount ट्रिपमध्ये $distText किलोमीटर आहे."
            else -> "Today's total driving distance is $distText kilometers across $todayTripsCount trips."
        }
    }

    fun weather(
        language: VoiceLanguage,
        tempText: String?,
        conditionText: String?,
        cityName: String?
    ): String {
        if (tempText == null || conditionText == null || cityName.isNullOrBlank()) {
            return when (language) {
                VoiceLanguage.HINDI -> "मौसम की जानकारी अभी उपलब्ध नहीं है।"
                VoiceLanguage.MARATHI -> "हवामानाची माहिती सध्या उपलब्ध नाही."
                else -> "Weather is currently unavailable."
            }
        }
        return when (language) {
            VoiceLanguage.HINDI -> "$cityName में अभी तापमान $tempText है और मौसम $conditionText है।"
            VoiceLanguage.MARATHI -> "$cityName मध्ये सध्या तापमान $tempText असून हवामान $conditionText आहे."
            else -> "It is currently $tempText and $conditionText in $cityName."
        }
    }

    fun findCar(
        language: VoiceLanguage,
        address: String?
    ): String {
        if (address.isNullOrBlank()) {
            return when (language) {
                VoiceLanguage.HINDI -> "कोई सेव की गई पार्किंग लोकेशन नहीं मिली। पार्क करने पर ड्राइवमेट खुद सेव कर लेगा।"
                VoiceLanguage.MARATHI -> "कोणतीही सेव्ह केलेली पार्किंग जागा सापडली नाही. पार्क केल्यावर ड्राईव्हमेट आपोआप नोंद करेल."
                else -> "No saved parking location found. DriveMate will automatically save your spot when you park."
            }
        }
        return when (language) {
            VoiceLanguage.HINDI -> "आपकी गाड़ी $address पर पार्क है।"
            VoiceLanguage.MARATHI -> "तुमची गाडी $address येथे पार्क केलेली आहे."
            else -> "Your vehicle is parked at $address."
        }
    }

    fun saveParking(
        language: VoiceLanguage,
        locationDetails: String?
    ): String {
        return when (language) {
            VoiceLanguage.HINDI -> "पार्किंग लोकेशन सेव कर ली गई है।"
            VoiceLanguage.MARATHI -> "पार्किंग लोकेशन सेव्ह केली आहे."
            else -> if (locationDetails != null) "Your parking spot is saved at $locationDetails." else "Parking spot saved at your current vehicle location."
        }
    }

    fun maintenance(
        language: VoiceLanguage,
        remainingKm: Double?,
        nextServiceKm: Int?,
        isConfigured: Boolean
    ): String {
        if (!isConfigured || remainingKm == null || nextServiceKm == null) {
            return when (language) {
                VoiceLanguage.HINDI -> "ओडोमीटर और सर्विस टारगेट सेट होने तक सर्विस की जानकारी उपलब्ध नहीं है।"
                VoiceLanguage.MARATHI -> "ओडोमीटर आणि सर्विस टार्गेट सेट होईपर्यंत सर्विस अंतर सांगता येणार नाही."
                else -> "Service interval is unavailable until an odometer and service target are configured."
            }
        }
        val remKm = remainingKm.toInt()
        return when (language) {
            VoiceLanguage.HINDI -> "आपकी अगली सर्विस $remKm किलोमीटर के बाद, $nextServiceKm किलोमीटर पर है।"
            VoiceLanguage.MARATHI -> "तुमची पुढची सर्विस $remKm किलोमीटरनंतर, $nextServiceKm किलोमीटरवर आहे."
            else -> "Next periodic service is due in $remKm kilometers at $nextServiceKm kilometers."
        }
    }

    fun vehicleCare(
        language: VoiceLanguage,
        effectiveOdoKm: Double?,
        remainingKm: Double?,
        nextServiceKm: Int?,
        isConfigured: Boolean
    ): String {
        if (effectiveOdoKm != null && isConfigured && nextServiceKm != null) {
            val remKm = remainingKm?.toInt() ?: 0
            val odoText = String.format(Locale.US, "%,.1f", effectiveOdoKm)
            return when (language) {
                VoiceLanguage.HINDI -> "आपकी गाड़ी $odoText किलोमीटर चली है। अगली सर्विस $nextServiceKm किलोमीटर पर है, जो $remKm किलोमीटर में होगी।"
                VoiceLanguage.MARATHI -> "तुमच्या गाडीचे $odoText किलोमीटर झाले आहेत. पुढची सर्विस $nextServiceKm किलोमीटरवर आहे, म्हणजेच $remKm किलोमीटर शिल्लक आहेत."
                else -> "Your vehicle has covered $odoText kilometers. Next service is due at $nextServiceKm kilometers, which is in $remKm kilometers."
            }
        }
        return when (language) {
            VoiceLanguage.HINDI -> "ओडोमीटर डेटा उपलब्ध नहीं है, इसलिए सर्विस दूरी की गणना नहीं की जा सकती।"
            VoiceLanguage.MARATHI -> "ओडोमीटर डेटा उपलब्ध नसल्याने सर्विस अंतराची गणना करता येत नाही."
            else -> "Odometer data is unavailable, so service distance cannot be calculated."
        }
    }

    fun documents(
        language: VoiceLanguage,
        isAndroidAuto: Boolean,
        expiringTitle: String?,
        daysLeft: Long?,
        totalDocsCount: Int
    ): String {
        if (isAndroidAuto) {
            return when (language) {
                VoiceLanguage.HINDI -> "सुरक्षा के लिए वाहन दस्तावेज केवल आपके फोन पर उपलब्ध हैं।"
                VoiceLanguage.MARATHI -> "गोपनीयतेसाठी वाहनाची कागदपत्रे केवळ फोनवर उपलब्ध आहेत."
                else -> "Vehicle documents are available only on your phone for privacy."
            }
        }
        if (expiringTitle != null && daysLeft != null) {
            return when (language) {
                VoiceLanguage.HINDI -> "ध्यान दें: आपका $expiringTitle $daysLeft दिनों में एक्सपायर हो रहा है। कृपया नवीनीकरण कराएं।"
                VoiceLanguage.MARATHI -> "लक्ष द्या: तुमचे $expiringTitle $daysLeft दिवसांत संपत आहे. कृपया लवकर नूतनीकरण करा."
                else -> "Attention: Your $expiringTitle expires in $daysLeft days. Please renew soon."
            }
        }
        if (totalDocsCount > 0) {
            return when (language) {
                VoiceLanguage.HINDI -> "आपके सभी $totalDocsCount वाहन दस्तावेज अभी वैध हैं।"
                VoiceLanguage.MARATHI -> "तुमचे सर्व $totalDocsCount वाहन कागदपत्रे सध्या वैध आहेत."
                else -> "All your $totalDocsCount stored vehicle documents are currently valid."
            }
        }
        return when (language) {
            VoiceLanguage.HINDI -> "आपके वाहन दस्तावेज फोन डॉक्यूमेंट वॉल्ट में सुरक्षित हैं।"
            VoiceLanguage.MARATHI -> "तुमची वाहन कागदपत्रे फोन डॉक्युमेंट व्हॉल्टमध्ये सुरक्षित आहेत."
            else -> "Your vehicle documents are saved in the phone-only document vault."
        }
    }

    fun expenses(
        language: VoiceLanguage,
        currentMonthSpent: Double?,
        costPerKm: Double?
    ): String {
        if (currentMonthSpent == null || costPerKm == null) {
            return when (language) {
                VoiceLanguage.HINDI -> "गाड़ी के खर्चे और फ्यूल लॉग आप एक्सपेंस मैनेजर में देख सकते हैं।"
                VoiceLanguage.MARATHI -> "वाहनाचा खर्च आणि फ्युएल नोंदी तुम्ही एक्सपेंस मॅनेजरमध्ये पाहू शकता."
                else -> "Vehicle running costs and fuel logs can be viewed in your Expense Manager."
            }
        }
        val spent = currentMonthSpent.toInt()
        val cpk = String.format(Locale.US, "%.1f", costPerKm)
        return when (language) {
            VoiceLanguage.HINDI -> "इस महीने आपने $spent रुपये खर्च किए हैं। प्रति किलोमीटर औसत खर्च $cpk रुपये है।"
            VoiceLanguage.MARATHI -> "या महिन्यात तुम्ही $spent रुपये खर्च केले आहेत. प्रति किलोमीटर सरासरी खर्च $cpk रुपये आहे."
            else -> "You have spent $spent rupees this month. Average running cost is $cpk rupees per kilometer."
        }
    }

    fun help(language: VoiceLanguage): String {
        return when (language) {
            VoiceLanguage.HINDI -> "मैं नेविगेशन, संगीत, मौसम, वाहन जानकारी, ट्रिप, पार्किंग, सर्विस, दस्तावेज और खर्चों में मदद कर सकता हूँ।"
            VoiceLanguage.MARATHI -> "मी नेव्हिगेशन, संगीत, हवामान, वाहनाची माहिती, ट्रिप, पार्किंग, मेंटेनन्स, कागदपत्रे आणि खर्चासाठी मदत करू शकतो."
            else -> "I can help with navigation, music, weather, vehicle information, trips, parking, maintenance, documents and expenses."
        }
    }

    fun about(language: VoiceLanguage): String {
        return when (language) {
            VoiceLanguage.HINDI -> "मैं ड्राइवमेट हूँ, आपका स्मार्ट कार साथी।"
            VoiceLanguage.MARATHI -> "मी ड्राईव्हमेट आहे, तुमचा स्मार्ट कार सोबती."
            else -> "I am DriveMate, your smart vehicle companion."
        }
    }

    fun assistantStatus(language: VoiceLanguage): String {
        return when (language) {
            VoiceLanguage.HINDI -> "ड्राइवमेट वॉयस असिस्टेंट चालू है और सुन रहा है।"
            VoiceLanguage.MARATHI -> "ड्राईव्हमेट व्हॉइस असिस्टंट सक्रिय असून ऐकत आहे."
            else -> "DriveMate Voice Assistant is active and listening."
        }
    }

    fun stop(language: VoiceLanguage): String {
        return when (language) {
            VoiceLanguage.HINDI -> "ठीक है, अलविदा।"
            VoiceLanguage.MARATHI -> "ठीक आहे, थांबतो."
            else -> "Goodbye."
        }
    }

    fun greeting(language: VoiceLanguage): String {
        return when (language) {
            VoiceLanguage.HINDI -> "नमस्ते! आज आपकी यात्रा में मैं कैसे मदद कर सकता हूँ?"
            VoiceLanguage.MARATHI -> "नमस्कार! प्रवासात मी तुमची कशी मदत करू शकतो?"
            else -> "Hello! How can I help you on your drive?"
        }
    }

    fun thankYou(language: VoiceLanguage): String {
        return when (language) {
            VoiceLanguage.HINDI -> "आपका स्वागत है! सुरक्षित ड्राइव करें।"
            VoiceLanguage.MARATHI -> "काही हरकत नाही! सुरक्षित गाडी चालवा."
            else -> "You're welcome! Drive safe."
        }
    }

    fun unknown(language: VoiceLanguage): String {
        return when (language) {
            VoiceLanguage.HINDI -> "मैं अभी इसका जवाब देने में असमर्थ हूँ। आप मुझसे कार, ट्रिप, मौसम, नेविगेशन, पार्किंग, सर्विस या खर्चों के बारे में पूछ सकते हैं।"
            VoiceLanguage.MARATHI -> "मला अजून याचे उत्तर देता येत नाही. तुम्ही मला कार, ट्रिप, हवामान, नेव्हिगेशन, पार्किंग, मेंटेनन्स किंवा खर्चाबद्दल विचारू शकता."
            else -> "I'm not able to answer that yet. You can ask me about your car, trips, weather, navigation, parking, maintenance or expenses."
        }
    }

    fun unsupportedTtsFallback(language: VoiceLanguage): String {
        return "Voice output is not supported for ${language.displayName} on this device. Responding in English."
    }
}
