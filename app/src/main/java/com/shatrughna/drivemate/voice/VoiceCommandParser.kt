package com.shatrughna.drivemate.voice

import java.util.Locale

/**
 * Fast, zero-network natural language rule parser that maps multilingual driver utterances
 * into VoiceCommand intents.
 * Supports English, Hindi, Marathi, and mixed-language queries with zero LLM overhead.
 */
class VoiceCommandParser {

    fun parse(rawInput: String): VoiceCommand {
        val normalized = VoiceLanguageClassifier.normalize(rawInput)
        if (normalized.isBlank()) {
            return VoiceCommand.Unknown(rawInput)
        }
        val input = rawInput.trim().lowercase(Locale.getDefault())

        // 1. Basic Conversational Control & Interruption (Stop / Cancel)
        val stopKeywords = listOf(
            "stop assistant", "stop listening", "stop", "cancel", "never mind", "that's all", "bye", "exit", "close assistant",
            "रुको", "रहने दो", "बस इतना ही", "कैंसिल", "बंद करो", "अलविदा",
            "थांब", "थांबा", "राहू दे", "एवढंच", "कॅन्सल", "बंद कर",
            "ruko", "rehne do", "thamb", "rahu de", "bas itna hi", "evdhach"
        )
        if (stopKeywords.any { normalized == it || normalized.startsWith("$it ") }) {
            return VoiceCommand.StopAssistant
        }

        // 2. Gratitude
        val thankKeywords = listOf(
            "thank you", "thanks a lot", "thanks",
            "धन्यवाद", "शुक्रिया", "बहुत शुक्रिया",
            "आभार", "खूप खूप धन्यवाद", "dhanyawad", "shukriya"
        )
        if (thankKeywords.any { normalized == it || normalized.startsWith("$it ") }) {
            return VoiceCommand.ThankYou
        }

        // 3. Greetings
        val greetingKeywords = listOf(
            "hello drivemate", "hi drivemate", "hello", "hi", "good morning", "good afternoon", "good evening", "how are you", "are you there",
            "नमस्ते drivemate", "नमस्ते", "नमस्कार drivemate", "नमस्कार", "हेलो", "कैसे हो", "सुप्रभात",
            "कसा आहेस", "कशी आहेस", "शुभ सकाळ", "तू तिथे आहेस का", "क्या तुम वहाँ हो"
        )
        if (greetingKeywords.any { normalized == it || normalized.startsWith("$it ") }) {
            return VoiceCommand.Greeting
        }

        // 4. Help & Assistance
        val helpKeywords = listOf(
            "what can you do", "what can i ask you", "how can you help me", "help me", "what commands do you support", "help",
            "तुम क्या कर सकते हो", "मैं तुमसे क्या पूछ सकता हूँ", "तुम मेरी कैसे मदद कर सकते हो", "मदद करो", "हेल्प",
            "तू काय करू शकतोस", "मी तुला काय विचारू शकतो", "तू माझी कशी मदत करू शकतोस", "मदत कर",
            "tum kya kar sakte ho", "tu kay karu shaktos"
        )
        if (helpKeywords.any { normalized.contains(it) }) {
            return VoiceCommand.Help
        }

        // 5. About Assistant & Identity
        val aboutKeywords = listOf(
            "who are you", "what is your name", "tell me about yourself", "about drivemate",
            "तुम कौन हो", "तुम्हारा नाम क्या है", "अपने बारे में बताओ",
            "तू कोण आहेस", "तुझं नाव काय आहे", "तुझ्याबद्दल सांग",
            "tum kaun ho", "tu kon aahes"
        )
        if (aboutKeywords.any { normalized.contains(it) }) {
            return VoiceCommand.AboutAssistant
        }

        // 6. Assistant Status
        val statusKeywords = listOf(
            "assistant status", "are you listening", "voice status",
            "असिस्टेंट स्टेटस", "क्या तुम सुन रहे हो",
            "असिस्टंट स्टेटस", "तू ऐकतोयस का"
        )
        if (statusKeywords.any { normalized.contains(it) }) {
            return VoiceCommand.AssistantStatus
        }

        // 7. Parking / Save & Find Car
        val saveParkingKeywords = listOf(
            "save parking spot", "save my parking", "save parking", "remember where i parked", "parked here",
            "मेरी पार्किंग सेव करो", "पार्किंग सेव करो", "गाड़ी यहाँ पार्क की है सेव करो", "पार्किंग याद रखो",
            "माझी पार्किंग सेव कर", "पार्किंग सेव कर", "इथे पार्क केली आहे", "पार्किंग लक्षात ठेव",
            "parking save karo", "parking save kar", "save kar parking"
        )
        if (saveParkingKeywords.any { normalized.contains(it) }) {
            return VoiceCommand.SaveParking
        }

        val findParkingKeywords = listOf(
            "where did i park", "where did i leave my car", "where is my car", "find my car", "where is my parking",
            "where is my nexon", "parking location", "parked location",
            "मैंने गाड़ी कहाँ पार्क की", "मेरी गाड़ी कहाँ है", "मैंने कार कहाँ खड़ी की", "मेरी पार्किंग कहाँ है", "मेरी पार्किंग लोकेशन बताओ", "गाड़ी कहाँ खड़ी है",
            "मी गाडी कुठे पार्क केली", "माझी गाडी कुठे आहे", "मी कार कुठे पार्क केली", "माझी पार्किंग कुठे आहे", "माझ्या गाडीची पार्किंग लोकेशन सांग", "गाडी कुठे पार्क केली",
            "माझी car कुठे park केली", "मेरी car कहाँ park की", "parking location सांग", "parking location batao",
            "car kahan park ki", "car kuthe park keli"
        )
        if (findParkingKeywords.any { normalized.contains(it) }) {
            return VoiceCommand.FindCar
        }

        // 8. Vehicle Expenses & Running Costs (checked before fuel to avoid 'spend on fuel' misclassification)
        val expenseKeywords = listOf(
            "spend on fuel", "fuel expenses", "fuel spend", "total expenses", "expense report", "running cost", "cost per km", "how much did i spend", "car expenses", "how much did i spend on fuel", "how much did i spend this month", "how much does my car cost per kilometer", "show total expenses",
            "मैंने fuel पर कितना खर्च किया", "इस महीने मैंने कितना खर्च किया", "गाड़ी का running cost कितना है", "प्रति किलोमीटर कितना खर्च आता है", "खर्चे बताओ", "पेट्रोल का खर्च",
            "मी fuel वर किती खर्च केला", "या महिन्यात किती खर्च केला", "गाडीचा running cost किती आहे", "प्रति किलोमीटर किती खर्च येतो", "खर्च किती झाला", "पेट्रोलवर किती खर्च झाला",
            "fuel pe kitna kharch", "fuel var kiti kharch"
        )
        if (expenseKeywords.any { normalized.contains(it) } ||
            normalized.contains("expense") || normalized.contains("expenses") ||
            normalized.contains("spend") || normalized.contains("खर्च") ||
            (normalized.contains("cost") && !normalized.contains("ac"))
        ) {
            return VoiceCommand.CheckExpenses(rawInput)
        }

        // 9. Average Speed (checked before live speed to prevent 'average speed' matching 'my speed')
        val avgSpeedKeywords = listOf("average speed", "औसत स्पीड", "सरासरी वेग", "एवरेज स्पीड", "ausat speed", "sarasari veg")
        if (avgSpeedKeywords.any { normalized.contains(it) } ||
            ((normalized.contains("average") || normalized.contains("औसत") || normalized.contains("सरासरी")) &&
                    (normalized.contains("speed") || normalized.contains("स्पीड") || normalized.contains("वेग")))
        ) {
            return VoiceCommand.CheckAverageSpeed
        }

        // 10. Live Vehicle Speed
        val speedExactPhrases = listOf(
            "what is my speed", "what is the speed", "what is the current speed", "current speed", "tell me my speed", "how fast am i going", "how fast am i driving", "how fast are we going",
            "मेरी स्पीड कितनी है", "मैं कितनी स्पीड से चल रहा हूँ", "अभी मेरी स्पीड कितनी है", "गाड़ी की स्पीड कितनी है", "स्पीड कितनी है", "चाल कितनी है",
            "माझा स्पीड किती आहे", "मी किती स्पीडने चाललोय", "आत्ता माझा स्पीड किती आहे", "गाडीचा स्पीड किती आहे", "वेग किती आहे", "सध्याचा वेग किती आहे",
            "माझी car speed किती आहे", "गाडीचा speed किती आहे", "मेरी car की speed कितनी है", "अभी car कितनी speed से चल रही है",
            "car speed kitni hai", "car speed kiti ahe", "my speed"
        )
        if (!normalized.contains("average") && !normalized.contains("औसत") && !normalized.contains("सरासरी") &&
            (speedExactPhrases.any { normalized.contains(it) } ||
            ((normalized.contains("speed") || normalized.contains("स्पीड") || normalized.contains("वेग")) &&
                    (normalized.contains("my") || normalized.contains("current") || normalized.contains("कितनी") ||
                            normalized.contains("किती") || normalized.contains("सध्याचा") || normalized.contains("माझा") ||
                            normalized.contains("माझी") || normalized.contains("मेरी") || normalized.contains("गाडीचा") ||
                            normalized.contains("गाड़ी की") || normalized.contains("fast"))))
        ) {
            return VoiceCommand.CheckSpeed
        }

        // 11. Camera Feeds & Surround View
        val cameraKeywords = listOf("360 camera", "surround camera", "reverse camera", "rear camera", "front camera", "show camera", "open camera", "कॅमेरा", "कैमरा")
        if (cameraKeywords.any { normalized.contains(it) }) {
            val type = if (normalized.contains("reverse") || normalized.contains("rear") || normalized.contains("रिव्हर्स") || normalized.contains("रिवर्स")) "reverse"
            else if (normalized.contains("front")) "front" else "360"
            return VoiceCommand.ViewCamera(type)
        }

        // 12. Climate Control & AC Actions
        val climateActionKeywords = listOf(
            "turn on ac", "turn ac on", "start ac", "turn off ac", "turn ac off", "stop ac", "cool the car", "set temperature", "change temperature",
            "ac चालू", "ac बंद", "ac ऑन", "ac ऑफ", "ac 22 वर set", "ac 22 par set"
        )
        if (climateActionKeywords.any { normalized.contains(it) } || (normalized.contains("ac") && (normalized.contains("on") || normalized.contains("off") || normalized.contains("set")))) {
            val tempRegex = Regex("""(?:temperature\s+to\s+|to\s+|var\s+set\s+|set\s+to\s+|par\s+set\s+)(\d{2}(?:\.\d)?)""")
            val match = tempRegex.find(normalized)
            val temp = match?.groupValues?.get(1)?.toFloatOrNull()
            return VoiceCommand.ControlClimate(action = normalized, temperature = temp)
        }

        // 13. Vehicle & Driver Documents
        val docKeywords = listOf(
            "insurance", "puc", "rc smart card", "registration certificate", "driving licence", "driving license", "pollution certificate", "documents",
            "इंश्योरेंस", "कागजात", "दस्तावेज", "डॉक्यूमेंट्स", "कागदपत्रे", "विमा", "डॉक्युमेंट्स"
        )
        if (docKeywords.any { normalized.contains(it) } &&
            (normalized.contains("valid") || normalized.contains("expire") || normalized.contains("check") ||
                    normalized.contains("status") || normalized.contains("show") || normalized.contains("when") ||
                    normalized.contains("वैध") || normalized.contains("कधी") || normalized.contains("कब") || normalized.contains("संपणार"))
        ) {
            return VoiceCommand.CheckDocument(rawInput)
        }

        // 14. Fuel & Petrol Inquiry (excluding expense queries)
        val fuelKeywords = listOf(
            "how much fuel", "fuel level", "how much petrol", "how much diesel", "petrol level", "fuel remaining", "fuel left",
            "गाड़ी में कितना पेट्रोल", "कितना fuel बचा", "कितना पेट्रोल बचा", "फ्यूल कितना", "पेट्रोल कितना",
            "गाडीमध्ये किती पेट्रोल", "किती fuel शिल्लक", "किती पेट्रोल शिल्लक", "गाडीत किती पेट्रोल",
            "car me kitna fuel", "car madhe kiti fuel", "fuel kitna", "fuel kiti"
        )
        if (!normalized.contains("spend") && !normalized.contains("खर्च") && !normalized.contains("cost") && !normalized.contains("expense") &&
            (fuelKeywords.any { normalized.contains(it) } ||
            ((normalized.contains("fuel") || normalized.contains("petrol") || normalized.contains("पेट्रोल") || normalized.contains("ईंधन") || normalized.contains("फ्युएल")) &&
                    (normalized.contains("कितना") || normalized.contains("किती") || normalized.contains("how much") || normalized.contains("level") || normalized.contains("left") || normalized.contains("shillak") || normalized.contains("bacha") || normalized.contains("बाकी") || normalized.contains("बचा") || normalized.contains("शिल्लक"))))
        ) {
            return VoiceCommand.CheckFuel
        }

        // 15. Range & Distance to Empty
        val rangeKeywords = listOf(
            "what is my range", "what's my range", "how far can i drive", "how many kilometers can i drive", "how much range is left", "how far can i go", "driving range", "distance to empty",
            "कितनी range बची", "मैं कितने किलोमीटर जा सकता हूँ", "कितनी दूरी तक जा सकता हूँ", "रेंज कितनी है", "कितनी रेंज बाकी है",
            "किती range बाकी आहे", "मी अजून किती किलोमीटर जाऊ शकतो", "रेंज किती आहे", "किती रेंज शिल्लक आहे",
            "car ki range kitni", "car chi range kiti", "kitne km ja sakte", "kiti km jau shakto"
        )
        if (rangeKeywords.any { normalized.contains(it) } ||
            ((normalized.contains("range") || normalized.contains("रेंज")) &&
                    (normalized.contains("how far") || normalized.contains("what") || normalized.contains("कितनी") || normalized.contains("किती") || normalized.contains("left") || normalized.contains("बाकी") || normalized.contains("शिल्लक")))
        ) {
            return VoiceCommand.CheckRange
        }

        // 16. Today's Cumulative Driving
        val todayDrivingKeywords = listOf(
            "how many kilometers did i drive today", "how many trips did i make today", "how much did i drive today", "today driving", "today's driving", "drives today", "trips today",
            "आज मैंने कितने किलोमीटर गाड़ी चलाई", "आज कितने ट्रिप किए", "आज कितना ड्राइव किया", "आज कितने किलोमीटर चला",
            "आज मी किती किलोमीटर गाडी चालवली", "आज किती ट्रिप केले", "आज किती km drive", "आज मी किती kilometers drive",
            "aaj kitne km drive", "aaj kiti km drive", "aaj kitne trips", "km drive केलं", "km drive केले", "kilometers drive केले", "kilometers drive केलं"
        )
        if (todayDrivingKeywords.any { normalized.contains(it) } ||
            ((normalized.contains("today") || normalized.contains("आज") || normalized.contains("aaj")) &&
                    (normalized.contains("drive") || normalized.contains("चालवली") || normalized.contains("चलाई") || normalized.contains("trip") || normalized.contains("ट्रिप")) &&
                    (normalized.contains("how many") || normalized.contains("how much") || normalized.contains("कितने") || normalized.contains("कितना") || normalized.contains("किती") || normalized.contains("km") || normalized.contains("kilometer") || normalized.contains("किलोमीटर")))
        ) {
            return VoiceCommand.CheckTodayDriving
        }

        // 17. Vehicle Status & Health
        val vehicleStatusKeywords = listOf(
            "what is my vehicle status", "vehicle status", "car status", "is my car okay", "is my car connected", "is my car connected to drivemate", "is android auto connected", "are there any vehicle warnings", "is everything okay with my car",
            "गाड़ी की स्थिति क्या है", "मेरी गाड़ी ठीक है", "गाड़ी कनेक्टेड है", "android auto connected है", "गाड़ी में कोई warning", "मेरी कार ठीक है",
            "गाडीची स्थिती काय आहे", "माझी गाडी ठीक आहे का", "गाडी connected आहे का", "android auto connected आहे का", "गाडीमध्ये काही warning", "कार ठीक आहे का",
            "car status kya hai", "car status kay ahe", "car connected hai kya", "car connected ahe ka"
        )
        if (vehicleStatusKeywords.any { normalized.contains(it) }) {
            return VoiceCommand.CheckVehicleStatus
        }

        // 18. Maintenance & Service Countdown
        val maintenanceKeywords = listOf(
            "when is my car service due", "how many kilometers until service", "how much distance is left for service", "is my car due for service", "tell me my maintenance status", "next service due", "check maintenance",
            "मेरी गाड़ी की सर्विस कब है", "अगली सर्विस कब है", "सर्विस के लिए कितने किलोमीटर बाकी हैं", "गाड़ी की सर्विस कब करनी है", "सर्विस कब करानी है",
            "माझ्या गाडीची सर्विस कधी आहे", "पुढची सर्विस कधी आहे", "सर्विससाठी किती किलोमीटर बाकी आहेत", "गाडीची सर्विस कधी करायची", "सर्विस कधी आहे",
            "माझ्या car ची service कधी आहे", "मेरी car की service कब है", "next service किती km नंतर आहे", "next service kab hai", "next service kadhi ahe"
        )
        if (maintenanceKeywords.any { normalized.contains(it) }) {
            return VoiceCommand.CheckMaintenance
        }

        // 19. Dedicated Odometer Inquiries
        // Preserving "check odometer" for CheckVehicleCare backwards compatibility with VoiceCommandParserTest
        if (normalized == "check odometer") {
            return VoiceCommand.CheckVehicleCare
        }
        val odoKeywords = listOf(
            "what is my odometer", "what's my odometer", "what is my odometer reading", "what's my odometer reading", "odometer reading", "how many kilometers has my car driven", "how many kilometers has my car covered", "how many km has my car driven",
            "माझ्या गाडीचे किती किलोमीटर झाले", "गाडी किती किलोमीटर चालली आहे", "गाडीचे किती किलोमीटर झाले",
            "मेरी गाड़ी कितने किलोमीटर चली है", "ओडोमीटर कितना है", "गाड़ी कितने किलोमीटर चली है",
            "car kitne kilometer chali hai", "car kiti kilometer chalali ahe", "odometer kitna hai", "odometer kiti ahe", "odometer किती आहे", "ओडोमीटर किती आहे"
        )
        if (odoKeywords.any { normalized.contains(it) }) {
            return VoiceCommand.CheckOdometer
        }

        // 20. Legacy Vehicle Care & Service Queries (Preserving VoiceCommandParserTest)
        val serviceCareKeywords = listOf("service status", "next service", "service reminder", "car maintenance", "when is service", "when is my next service", "odometer")
        if (serviceCareKeywords.any { normalized.contains(it) }) {
            return VoiceCommand.CheckVehicleCare
        }

        // 21. YouTube Video Requests
        val isVideoRequest = normalized.startsWith("watch ") ||
                (normalized.contains(" on youtube") && !normalized.contains(" on youtube music")) ||
                normalized.startsWith("open youtube") ||
                normalized.startsWith("play video")
        if (isVideoRequest) {
            val query = normalized
                .removePrefix("watch ")
                .removePrefix("play video ")
                .removePrefix("open youtube ")
                .replace("on youtube", "")
                .trim()
            return VoiceCommand.WatchVideo(query.ifBlank { "YouTube" })
        }

        // 22. Music Streaming
        val isMusicRequest = normalized.startsWith("play ") || normalized.startsWith("listen to ") ||
                normalized.contains(" on spotify") || normalized.contains(" on youtube music") ||
                normalized.contains("गाना बजाओ") || normalized.contains("गाना लगाओ") || normalized.contains("संगीत बजाओ") ||
                normalized.contains("गाणी लाव") || normalized.contains("गाणं लाव") || normalized.contains("संगीत लाव")
        if (isMusicRequest) {
            var targetApp: String? = null
            var cleanQuery = normalized

            if (cleanQuery.contains(" on spotify")) {
                targetApp = "Spotify"
                cleanQuery = cleanQuery.replace(" on spotify", "")
            } else if (cleanQuery.contains(" on youtube music")) {
                targetApp = "YouTube Music"
                cleanQuery = cleanQuery.replace(" on youtube music", "")
            }

            cleanQuery = cleanQuery
                .removePrefix("play song ")
                .removePrefix("play track ")
                .removePrefix("play music ")
                .removePrefix("play ")
                .removePrefix("listen to song ")
                .removePrefix("listen to ")
                .removeSuffix(" बजाओ")
                .removeSuffix(" लगाओ")
                .removeSuffix(" लाव")
                .trim()

            if (cleanQuery.isNotBlank()) {
                return VoiceCommand.PlayMusic(query = cleanQuery, appName = targetApp)
            }
        }

        // 23. Navigation & Route Finding
        val navPrefixes = listOf(
            "navigate to ", "navigate ", "directions to ", "direction to ", "take me to ", "route to ", "drive to ", "go to ",
            "search for ", "find nearest ", "locate nearest ", "find the nearest ", "find a ", "find the ", "find ",
            "मुझे ", "मला "
        )
        for (prefix in navPrefixes) {
            if (normalized.startsWith(prefix)) {
                var destination = normalized.removePrefix(prefix).trim()
                destination = destination
                    .removePrefix("to ")
                    .removeSuffix(" ले चलो")
                    .removeSuffix(" लेकर चलो")
                    .removeSuffix(" घेऊन चल")
                    .removeSuffix(" चा रस्ता दाखव")
                    .removeSuffix(" का रास्ता दिखाओ")
                    .trim()
                if (destination.isNotBlank()) {
                    return VoiceCommand.Navigate(destination)
                }
            }
        }
        val navKeywords = listOf(
            "nearest petrol pump", "nearest gas station", "nearest ev charger", "nearest cafe", "nearest hospital", "nearest tata service center",
            "नजदीकी पेट्रोल पंप", "नजदीकी अस्पताल", "घर का रास्ता", "घर ले चलो",
            "जवळचा पेट्रोल पंप", "जवळचं हॉस्पिटल", "घराचा रस्ता", "घरी घेऊन चल"
        )
        if (navKeywords.any { normalized.contains(it) }) {
            return VoiceCommand.Navigate(normalized)
        }

        // 24. Weather Queries
        val weatherKeywords = listOf("weather", "temperature", "forecast", "will it rain", "is it raining", "rain", "raining", "climate", "मौसम", "तापमान", "बारिश", "हवामान", "पाऊस")
        if (weatherKeywords.any { normalized.contains(it) }) {
            return VoiceCommand.CheckWeather
        }

        // 25. Trip Stats
        val tripKeywords = listOf(
            "trip status", "trip info", "how long have i been driving", "how long have we been driving", "how far have i driven", "trip distance", "trip time", "how long is my trip", "what is my trip distance", "tell me about my current trip",
            "मैं कितनी देर से गाड़ी चला रहा हूँ", "मैंने कितने किलोमीटर ड्राइव किया", "मेरा ट्रिप कितना लंबा है",
            "मी किती वेळ गाडी चालवत आहे", "मी किती किलोमीटर गाडी चालवली", "माझा ट्रिप किती लांब आहे",
            "माझा trip किती लांब आहे"
        )
        if (tripKeywords.any { normalized.contains(it) }) {
            return VoiceCommand.CheckTripStats
        }

        return VoiceCommand.Unknown(rawInput)
    }
}
