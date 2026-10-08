package com.shatrughna.drivemate

import com.shatrughna.drivemate.core.telemetry.TelemetryAvailability
import com.shatrughna.drivemate.core.telemetry.TelemetrySource
import com.shatrughna.drivemate.voice.VoiceCommand
import com.shatrughna.drivemate.voice.VoiceCommandParser
import com.shatrughna.drivemate.voice.VoiceLanguage
import com.shatrughna.drivemate.voice.VoiceLanguageClassifier
import com.shatrughna.drivemate.voice.VoiceResponseProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MultilingualVoiceAssistantTest {

    private lateinit var parser: VoiceCommandParser
    private lateinit var responseProvider: VoiceResponseProvider

    @Before
    fun setUp() {
        parser = VoiceCommandParser()
        responseProvider = VoiceResponseProvider()
    }

    // ==========================================
    // 1. LANGUAGE CLASSIFICATION & NORMALIZATION
    // ==========================================

    @Test
    fun testLanguageDetectionEnglish() {
        assertEquals(VoiceLanguage.ENGLISH, VoiceLanguageClassifier.detectLanguage("What is my speed?"))
        assertEquals(VoiceLanguage.ENGLISH, VoiceLanguageClassifier.detectLanguage("Where did I park?"))
        assertEquals(VoiceLanguage.ENGLISH, VoiceLanguageClassifier.detectLanguage("How much fuel is left?"))
    }

    @Test
    fun testLanguageDetectionHindi() {
        assertEquals(VoiceLanguage.HINDI, VoiceLanguageClassifier.detectLanguage("मेरी स्पीड कितनी है?"))
        assertEquals(VoiceLanguage.HINDI, VoiceLanguageClassifier.detectLanguage("मौसम कैसा है?"))
        assertEquals(VoiceLanguage.HINDI, VoiceLanguageClassifier.detectLanguage("मैंने गाड़ी कहाँ पार्क की?"))
    }

    @Test
    fun testLanguageDetectionMarathi() {
        assertEquals(VoiceLanguage.MARATHI, VoiceLanguageClassifier.detectLanguage("माझा स्पीड किती आहे?"))
        assertEquals(VoiceLanguage.MARATHI, VoiceLanguageClassifier.detectLanguage("हवामान कसं आहे?"))
        assertEquals(VoiceLanguage.MARATHI, VoiceLanguageClassifier.detectLanguage("मी गाडी कुठे पार्क केली?"))
    }

    @Test
    fun testLanguageDetectionMixed() {
        assertEquals(VoiceLanguage.MIXED, VoiceLanguageClassifier.detectLanguage("माझी car speed किती आहे?"))
        assertEquals(VoiceLanguage.MIXED, VoiceLanguageClassifier.detectLanguage("मेरी car कहाँ park की?"))
        assertEquals(VoiceLanguage.MIXED, VoiceLanguageClassifier.detectLanguage("आज weather कसं आहे?"))
        assertEquals(VoiceLanguage.MIXED, VoiceLanguageClassifier.detectLanguage("Nearest petrol pump कुठे आहे?"))
    }

    @Test
    fun testNormalizationStripsPunctuationAndWhitespace() {
        assertEquals("what is my speed", VoiceLanguageClassifier.normalize("  What is my speed???  "))
        assertEquals("मेरी स्पीड कितनी है", VoiceLanguageClassifier.normalize("मेरी स्पीड कितनी है!?"))
        assertEquals("माझा स्पीड किती आहे", VoiceLanguageClassifier.normalize("माझा, स्पीड; किती आहे..."))
    }

    // ==========================================
    // 2. SPEED QUERIES (English, Hindi, Marathi, Mixed)
    // ==========================================

    @Test
    fun testSpeedQueriesMapping() {
        // English
        assertEquals(VoiceCommand.CheckSpeed, parser.parse("What is my speed?"))
        assertEquals(VoiceCommand.CheckSpeed, parser.parse("How fast am I going?"))
        assertEquals(VoiceCommand.CheckSpeed, parser.parse("How fast am I driving?"))
        assertEquals(VoiceCommand.CheckSpeed, parser.parse("Current speed?"))
        assertEquals(VoiceCommand.CheckSpeed, parser.parse("Tell me my speed."))

        // Hindi
        assertEquals(VoiceCommand.CheckSpeed, parser.parse("मेरी स्पीड कितनी है?"))
        assertEquals(VoiceCommand.CheckSpeed, parser.parse("मैं कितनी स्पीड से चल रहा हूँ?"))
        assertEquals(VoiceCommand.CheckSpeed, parser.parse("अभी मेरी स्पीड कितनी है?"))
        assertEquals(VoiceCommand.CheckSpeed, parser.parse("गाड़ी की स्पीड कितनी है?"))

        // Marathi
        assertEquals(VoiceCommand.CheckSpeed, parser.parse("माझा स्पीड किती आहे?"))
        assertEquals(VoiceCommand.CheckSpeed, parser.parse("मी किती स्पीडने चाललोय?"))
        assertEquals(VoiceCommand.CheckSpeed, parser.parse("आत्ता माझा स्पीड किती आहे?"))
        assertEquals(VoiceCommand.CheckSpeed, parser.parse("गाडीचा स्पीड किती आहे?"))

        // Mixed
        assertEquals(VoiceCommand.CheckSpeed, parser.parse("माझी car speed किती आहे?"))
        assertEquals(VoiceCommand.CheckSpeed, parser.parse("गाडीचा speed किती आहे?"))
        assertEquals(VoiceCommand.CheckSpeed, parser.parse("मेरी car की speed कितनी है?"))
        assertEquals(VoiceCommand.CheckSpeed, parser.parse("अभी car कितनी speed से चल रही है?"))
    }

    // ==========================================
    // 3. ODOMETER QUERIES (English, Hindi, Marathi)
    // ==========================================

    @Test
    fun testOdometerQueriesMapping() {
        assertEquals(VoiceCommand.CheckOdometer, parser.parse("What is my odometer?"))
        assertEquals(VoiceCommand.CheckOdometer, parser.parse("What's my odometer reading?"))
        assertEquals(VoiceCommand.CheckOdometer, parser.parse("How many kilometers has my car driven?"))
        assertEquals(VoiceCommand.CheckOdometer, parser.parse("How many kilometers has my car covered?"))

        assertEquals(VoiceCommand.CheckOdometer, parser.parse("माझ्या गाडीचे किती किलोमीटर झाले?"))
        assertEquals(VoiceCommand.CheckOdometer, parser.parse("गाडी किती किलोमीटर चालली आहे?"))

        assertEquals(VoiceCommand.CheckOdometer, parser.parse("मेरी गाड़ी कितने किलोमीटर चली है?"))
        assertEquals(VoiceCommand.CheckOdometer, parser.parse("ओडोमीटर कितना है?"))
    }

    // ==========================================
    // 4. FUEL QUERIES (English, Hindi, Marathi, Mixed)
    // ==========================================

    @Test
    fun testFuelQueriesMapping() {
        assertEquals(VoiceCommand.CheckFuel, parser.parse("How much fuel do I have?"))
        assertEquals(VoiceCommand.CheckFuel, parser.parse("What's my fuel level?"))
        assertEquals(VoiceCommand.CheckFuel, parser.parse("How much fuel is left?"))
        assertEquals(VoiceCommand.CheckFuel, parser.parse("How much petrol is left?"))

        assertEquals(VoiceCommand.CheckFuel, parser.parse("गाड़ी में कितना पेट्रोल है?"))
        assertEquals(VoiceCommand.CheckFuel, parser.parse("कितना fuel बचा है?"))

        assertEquals(VoiceCommand.CheckFuel, parser.parse("गाडीमध्ये किती पेट्रोल आहे?"))
        assertEquals(VoiceCommand.CheckFuel, parser.parse("किती fuel शिल्लक आहे?"))

        assertEquals(VoiceCommand.CheckFuel, parser.parse("car me kitna fuel hai?"))
        assertEquals(VoiceCommand.CheckFuel, parser.parse("fuel kiti shillak ahe?"))
    }

    // ==========================================
    // 5. RANGE QUERIES (English, Hindi, Marathi)
    // ==========================================

    @Test
    fun testRangeQueriesMapping() {
        assertEquals(VoiceCommand.CheckRange, parser.parse("What's my range?"))
        assertEquals(VoiceCommand.CheckRange, parser.parse("How far can I drive?"))
        assertEquals(VoiceCommand.CheckRange, parser.parse("How many kilometers can I drive?"))
        assertEquals(VoiceCommand.CheckRange, parser.parse("How much range is left?"))

        assertEquals(VoiceCommand.CheckRange, parser.parse("कितनी range बची है?"))
        assertEquals(VoiceCommand.CheckRange, parser.parse("मैं कितने किलोमीटर जा सकता हूँ?"))
        assertEquals(VoiceCommand.CheckRange, parser.parse("कितनी दूरी तक जा सकता हूँ?"))

        assertEquals(VoiceCommand.CheckRange, parser.parse("किती range बाकी आहे?"))
        assertEquals(VoiceCommand.CheckRange, parser.parse("मी अजून किती किलोमीटर जाऊ शकतो?"))
    }

    // ==========================================
    // 6. VEHICLE STATUS QUERIES (English, Hindi, Marathi)
    // ==========================================

    @Test
    fun testVehicleStatusQueriesMapping() {
        assertEquals(VoiceCommand.CheckVehicleStatus, parser.parse("What is my vehicle status?"))
        assertEquals(VoiceCommand.CheckVehicleStatus, parser.parse("Is my car okay?"))
        assertEquals(VoiceCommand.CheckVehicleStatus, parser.parse("Is my car connected?"))
        assertEquals(VoiceCommand.CheckVehicleStatus, parser.parse("Is Android Auto connected?"))
        assertEquals(VoiceCommand.CheckVehicleStatus, parser.parse("Are there any vehicle warnings?"))

        assertEquals(VoiceCommand.CheckVehicleStatus, parser.parse("गाड़ी की स्थिति क्या है?"))
        assertEquals(VoiceCommand.CheckVehicleStatus, parser.parse("मेरी गाड़ी ठीक है?"))
        assertEquals(VoiceCommand.CheckVehicleStatus, parser.parse("गाड़ी कनेक्टेड है?"))
        assertEquals(VoiceCommand.CheckVehicleStatus, parser.parse("Android Auto connected है?"))

        assertEquals(VoiceCommand.CheckVehicleStatus, parser.parse("गाडीची स्थिती काय आहे?"))
        assertEquals(VoiceCommand.CheckVehicleStatus, parser.parse("माझी गाडी ठीक आहे का?"))
        assertEquals(VoiceCommand.CheckVehicleStatus, parser.parse("गाडी connected आहे का?"))
        assertEquals(VoiceCommand.CheckVehicleStatus, parser.parse("Android Auto connected आहे का?"))
    }

    // ==========================================
    // 7. TRIP QUESTIONS, AVERAGE SPEED, TODAY'S DRIVING
    // ==========================================

    @Test
    fun testTripQueriesMapping() {
        assertEquals(VoiceCommand.CheckTripStats, parser.parse("How long have I been driving?"))
        assertEquals(VoiceCommand.CheckTripStats, parser.parse("How far have I driven?"))
        assertEquals(VoiceCommand.CheckTripStats, parser.parse("Tell me about my current trip."))
        assertEquals(VoiceCommand.CheckTripStats, parser.parse("मैं कितनी देर से गाड़ी चला रहा हूँ?"))
        assertEquals(VoiceCommand.CheckTripStats, parser.parse("मी किती वेळ गाडी चालवत आहे?"))

        assertEquals(VoiceCommand.CheckAverageSpeed, parser.parse("What is my average speed?"))
        assertEquals(VoiceCommand.CheckAverageSpeed, parser.parse("औसत स्पीड कितनी है?"))
        assertEquals(VoiceCommand.CheckAverageSpeed, parser.parse("सरासरी वेग किती आहे?"))

        assertEquals(VoiceCommand.CheckTodayDriving, parser.parse("How many kilometers did I drive today?"))
        assertEquals(VoiceCommand.CheckTodayDriving, parser.parse("How many trips did I make today?"))
        assertEquals(VoiceCommand.CheckTodayDriving, parser.parse("आज मैंने कितने किलोमीटर गाड़ी चलाई?"))
        assertEquals(VoiceCommand.CheckTodayDriving, parser.parse("आज मी किती kilometers drive केले?"))
        assertEquals(VoiceCommand.CheckTodayDriving, parser.parse("आज किती km drive केलं?"))
    }

    // ==========================================
    // 8. WEATHER QUERIES (English, Hindi, Marathi, Mixed)
    // ==========================================

    @Test
    fun testWeatherQueriesMapping() {
        assertEquals(VoiceCommand.CheckWeather, parser.parse("What's the weather?"))
        assertEquals(VoiceCommand.CheckWeather, parser.parse("What's the temperature outside?"))
        assertEquals(VoiceCommand.CheckWeather, parser.parse("Will it rain today?"))

        assertEquals(VoiceCommand.CheckWeather, parser.parse("मौसम कैसा है?"))
        assertEquals(VoiceCommand.CheckWeather, parser.parse("तापमान कितना है?"))
        assertEquals(VoiceCommand.CheckWeather, parser.parse("बारिश होगी क्या?"))

        assertEquals(VoiceCommand.CheckWeather, parser.parse("हवामान कसं आहे?"))
        assertEquals(VoiceCommand.CheckWeather, parser.parse("तापमान किती आहे?"))
        assertEquals(VoiceCommand.CheckWeather, parser.parse("पाऊस पडेल का?"))

        assertEquals(VoiceCommand.CheckWeather, parser.parse("आज weather कसं आहे?"))
        assertEquals(VoiceCommand.CheckWeather, parser.parse("आज rain पडणार आहे का?"))
        assertEquals(VoiceCommand.CheckWeather, parser.parse("Temperature किती आहे?"))
    }

    // ==========================================
    // 9. PARKING QUERIES (English, Hindi, Marathi, Mixed)
    // ==========================================

    @Test
    fun testParkingQueriesMapping() {
        assertEquals(VoiceCommand.FindCar, parser.parse("Where did I park?"))
        assertEquals(VoiceCommand.FindCar, parser.parse("Where is my car?"))
        assertEquals(VoiceCommand.FindCar, parser.parse("Where did I leave my car?"))
        assertEquals(VoiceCommand.FindCar, parser.parse("मैंने गाड़ी कहाँ पार्क की?"))
        assertEquals(VoiceCommand.FindCar, parser.parse("मी गाडी कुठे पार्क केली?"))
        assertEquals(VoiceCommand.FindCar, parser.parse("माझी car कुठे park केली?"))
        assertEquals(VoiceCommand.FindCar, parser.parse("मेरी car कहाँ park की?"))

        assertEquals(VoiceCommand.SaveParking, parser.parse("Save my parking."))
        assertEquals(VoiceCommand.SaveParking, parser.parse("Remember where I parked."))
        assertEquals(VoiceCommand.SaveParking, parser.parse("पार्किंग सेव करो"))
        assertEquals(VoiceCommand.SaveParking, parser.parse("पार्किंग सेव कर"))
    }

    // ==========================================
    // 10. MAINTENANCE & SERVICE QUERIES
    // ==========================================

    @Test
    fun testMaintenanceQueriesMapping() {
        assertEquals(VoiceCommand.CheckMaintenance, parser.parse("When is my car service due?"))
        assertEquals(VoiceCommand.CheckMaintenance, parser.parse("How many kilometers until service?"))
        assertEquals(VoiceCommand.CheckMaintenance, parser.parse("How much distance is left for service?"))
        assertEquals(VoiceCommand.CheckMaintenance, parser.parse("Is my car due for service?"))
        assertEquals(VoiceCommand.CheckMaintenance, parser.parse("Tell me my maintenance status."))

        assertEquals(VoiceCommand.CheckMaintenance, parser.parse("मेरी गाड़ी की सर्विस कब है?"))
        assertEquals(VoiceCommand.CheckMaintenance, parser.parse("अगली सर्विस कब है?"))
        assertEquals(VoiceCommand.CheckMaintenance, parser.parse("सर्विस के लिए कितने किलोमीटर बाकी हैं?"))

        assertEquals(VoiceCommand.CheckMaintenance, parser.parse("माझ्या गाडीची सर्विस कधी आहे?"))
        assertEquals(VoiceCommand.CheckMaintenance, parser.parse("पुढची सर्विस कधी आहे?"))
        assertEquals(VoiceCommand.CheckMaintenance, parser.parse("सर्विससाठी किती किलोमीटर बाकी आहेत?"))

        assertEquals(VoiceCommand.CheckMaintenance, parser.parse("Next service किती km नंतर आहे?"))
    }

    // ==========================================
    // 11. DOCUMENT VAULT QUERIES
    // ==========================================

    @Test
    fun testDocumentQueriesMapping() {
        assertTrue(parser.parse("Is my insurance valid?") is VoiceCommand.CheckDocument)
        assertTrue(parser.parse("When does my insurance expire?") is VoiceCommand.CheckDocument)
        assertTrue(parser.parse("When does my PUC expire?") is VoiceCommand.CheckDocument)
        assertTrue(parser.parse("Is my PUC valid?") is VoiceCommand.CheckDocument)

        assertTrue(parser.parse("मेरा इंश्योरेंस कब expire होगा?") is VoiceCommand.CheckDocument)
        assertTrue(parser.parse("मेरा PUC valid है?") is VoiceCommand.CheckDocument)

        assertTrue(parser.parse("माझं insurance कधी expire होईल?") is VoiceCommand.CheckDocument)
        assertTrue(parser.parse("माझा PUC valid आहे का?") is VoiceCommand.CheckDocument)
    }

    // ==========================================
    // 12. EXPENSE MANAGER QUERIES
    // ==========================================

    @Test
    fun testExpenseQueriesMapping() {
        assertTrue(parser.parse("How much did I spend on fuel?") is VoiceCommand.CheckExpenses)
        assertTrue(parser.parse("How much did I spend this month?") is VoiceCommand.CheckExpenses)
        assertTrue(parser.parse("What is my running cost?") is VoiceCommand.CheckExpenses)
        assertTrue(parser.parse("How much does my car cost per kilometer?") is VoiceCommand.CheckExpenses)

        assertTrue(parser.parse("मैंने fuel पर कितना खर्च किया?") is VoiceCommand.CheckExpenses)
        assertTrue(parser.parse("इस महीने मैंने कितना खर्च किया?") is VoiceCommand.CheckExpenses)

        assertTrue(parser.parse("मी fuel वर किती खर्च केला?") is VoiceCommand.CheckExpenses)
        assertTrue(parser.parse("या महिन्यात किती खर्च केला?") is VoiceCommand.CheckExpenses)
    }

    // ==========================================
    // 13. MUSIC & NAVIGATION
    // ==========================================

    @Test
    fun testMusicQueriesMapping() {
        val playArijit = parser.parse("Play Arijit Singh")
        assertTrue(playArijit is VoiceCommand.PlayMusic)
        assertEquals("arijit singh", (playArijit as VoiceCommand.PlayMusic).query)

        val playHindi = parser.parse("हिंदी गाना बजाओ")
        assertTrue(playHindi is VoiceCommand.PlayMusic)

        val playMarathi = parser.parse("मराठी गाणी लाव")
        assertTrue(playMarathi is VoiceCommand.PlayMusic)
    }

    @Test
    fun testNavigationQueriesMapping() {
        val navHome = parser.parse("Navigate home")
        assertTrue(navHome is VoiceCommand.Navigate)

        val navPune = parser.parse("Navigate to Pune")
        assertTrue(navPune is VoiceCommand.Navigate)
        assertEquals("pune", (navPune as VoiceCommand.Navigate).destination)

        val navHindi = parser.parse("मुझे घर ले चलो")
        assertTrue(navHindi is VoiceCommand.Navigate)

        val navMarathi = parser.parse("मला घरी घेऊन चल")
        assertTrue(navMarathi is VoiceCommand.Navigate)

        val navMixed = parser.parse("Nearest petrol pump कुठे आहे?")
        assertTrue(navMixed is VoiceCommand.Navigate)
    }

    // ==========================================
    // 14. HELP, CONVERSATION, STOP & CANCEL
    // ==========================================

    @Test
    fun testHelpQueriesMapping() {
        assertEquals(VoiceCommand.Help, parser.parse("What can you do?"))
        assertEquals(VoiceCommand.Help, parser.parse("How can you help me?"))
        assertEquals(VoiceCommand.Help, parser.parse("Help me."))
        assertEquals(VoiceCommand.Help, parser.parse("तुम क्या कर सकते हो?"))
        assertEquals(VoiceCommand.Help, parser.parse("तू काय करू शकतोस?"))
    }

    @Test
    fun testAboutAndStatusMapping() {
        assertEquals(VoiceCommand.AboutAssistant, parser.parse("Who are you?"))
        assertEquals(VoiceCommand.AboutAssistant, parser.parse("तुम कौन हो?"))
        assertEquals(VoiceCommand.AboutAssistant, parser.parse("तू कोण आहेस?"))

        assertEquals(VoiceCommand.AssistantStatus, parser.parse("Assistant status"))
        assertEquals(VoiceCommand.AssistantStatus, parser.parse("क्या तुम सुन रहे हो?"))
        assertEquals(VoiceCommand.AssistantStatus, parser.parse("तू ऐकतोयस का?"))
    }

    @Test
    fun testStopAndCancelMapping() {
        assertEquals(VoiceCommand.StopAssistant, parser.parse("Stop"))
        assertEquals(VoiceCommand.StopAssistant, parser.parse("Cancel"))
        assertEquals(VoiceCommand.StopAssistant, parser.parse("Never mind"))
        assertEquals(VoiceCommand.StopAssistant, parser.parse("रुको"))
        assertEquals(VoiceCommand.StopAssistant, parser.parse("रहने दो"))
        assertEquals(VoiceCommand.StopAssistant, parser.parse("थांब"))
        assertEquals(VoiceCommand.StopAssistant, parser.parse("राहू दे"))
    }

    @Test
    fun testGreetingAndGratitudeMapping() {
        assertEquals(VoiceCommand.Greeting, parser.parse("Hello DriveMate"))
        assertEquals(VoiceCommand.Greeting, parser.parse("नमस्ते DriveMate"))
        assertEquals(VoiceCommand.Greeting, parser.parse("नमस्कार DriveMate"))

        assertEquals(VoiceCommand.ThankYou, parser.parse("Thank you"))
        assertEquals(VoiceCommand.ThankYou, parser.parse("धन्यवाद"))
        assertEquals(VoiceCommand.ThankYou, parser.parse("आभार"))
    }

    @Test
    fun testUnknownAndEdgeCases() {
        assertTrue(parser.parse("") is VoiceCommand.Unknown)
        assertTrue(parser.parse("   ") is VoiceCommand.Unknown)
        assertTrue(parser.parse("Quantum physics theorem calculation") is VoiceCommand.Unknown)
    }

    // ==========================================
    // 15. VOICE RESPONSE PROVIDER HONESTY
    // ==========================================

    @Test
    fun testResponseHonestySpeed() {
        // Authoritative live
        val enLive = responseProvider.speed(VoiceLanguage.ENGLISH, 64f, TelemetrySource.ANDROID_AUTO_CAR_HARDWARE, TelemetryAvailability.LIVE)
        assertEquals("You are currently driving at 64 kilometers per hour.", enLive)

        val hiLive = responseProvider.speed(VoiceLanguage.HINDI, 64f, TelemetrySource.ANDROID_AUTO_CAR_HARDWARE, TelemetryAvailability.LIVE)
        assertEquals("अभी आपकी स्पीड 64 किलोमीटर प्रति घंटा है।", hiLive)

        val mrLive = responseProvider.speed(VoiceLanguage.MARATHI, 64f, TelemetrySource.ANDROID_AUTO_CAR_HARDWARE, TelemetryAvailability.LIVE)
        assertEquals("तुमचा सध्याचा वेग 64 किलोमीटर प्रति तास आहे.", mrLive)

        // GPS
        val enGps = responseProvider.speed(VoiceLanguage.ENGLISH, 64f, TelemetrySource.PHONE_GPS, TelemetryAvailability.LIVE)
        assertEquals("Your current GPS speed is 64 kilometers per hour.", enGps)

        // Stale
        val enStale = responseProvider.speed(VoiceLanguage.ENGLISH, 64f, TelemetrySource.PHONE_GPS, TelemetryAvailability.STALE)
        assertEquals("I have an older speed reading, but I can't confirm your current speed.", enStale)

        // Unavailable - NEVER 0 km/h
        val enUnavail = responseProvider.speed(VoiceLanguage.ENGLISH, null, TelemetrySource.NONE, TelemetryAvailability.UNAVAILABLE)
        assertEquals("I can't get your current speed right now.", enUnavail)
        assertFalse(enUnavail.contains("0"))
    }

    @Test
    fun testResponseHonestyFuel() {
        // Available
        val enFuel = responseProvider.fuel(VoiceLanguage.ENGLISH, 45f, TelemetryAvailability.LIVE)
        assertEquals("Your current fuel level is 45 percent.", enFuel)

        // Unavailable - NEVER estimated
        val enNoFuel = responseProvider.fuel(VoiceLanguage.ENGLISH, null, TelemetryAvailability.UNAVAILABLE)
        assertEquals("Fuel level isn't available from the connected vehicle data.", enNoFuel)
    }

    @Test
    fun testResponseHonestyOdometer() {
        val enOdo = responseProvider.odometer(VoiceLanguage.ENGLISH, 12845.6, true)
        assertEquals("Your vehicle has driven 12,845.6 kilometers.", enOdo)

        val enNoOdo = responseProvider.odometer(VoiceLanguage.ENGLISH, null, false)
        assertEquals("I don't have a reliable odometer reading right now.", enNoOdo)
    }

    @Test
    fun testResponseHonestyRange() {
        val enRange = responseProvider.range(VoiceLanguage.ENGLISH, 320f, TelemetryAvailability.LIVE)
        assertEquals("Your estimated driving range is 320 kilometers.", enRange)

        val enNoRange = responseProvider.range(VoiceLanguage.ENGLISH, null, TelemetryAvailability.UNAVAILABLE)
        assertEquals("Range information isn't available from your vehicle.", enNoRange)
    }

    @Test
    fun testResponseHonestyVehicleStatus() {
        val enConn = responseProvider.vehicleStatus(VoiceLanguage.ENGLISH, isCarConnected = true, hasTelemetry = true)
        assertEquals("DriveMate is connected with live vehicle telemetry active.", enConn)

        val enPartial = responseProvider.vehicleStatus(VoiceLanguage.ENGLISH, isCarConnected = true, hasTelemetry = false)
        assertEquals("DriveMate is connected, but I don't currently have complete diagnostic information from your vehicle.", enPartial)

        val enDisconn = responseProvider.vehicleStatus(VoiceLanguage.ENGLISH, isCarConnected = false, hasTelemetry = false)
        assertEquals("No vehicle is currently connected to DriveMate.", enDisconn)
    }
}
