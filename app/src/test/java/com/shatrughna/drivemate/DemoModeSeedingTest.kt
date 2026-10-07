package com.shatrughna.drivemate

import com.shatrughna.drivemate.data.model.DocumentType
import com.shatrughna.drivemate.data.model.DriveMateSettings
import com.shatrughna.drivemate.data.model.ExpenseCategory
import com.shatrughna.drivemate.data.model.ServiceRecord
import com.shatrughna.drivemate.data.model.ServiceType
import com.shatrughna.drivemate.data.model.VehicleDocument
import com.shatrughna.drivemate.data.model.VehicleExpense
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoModeSeedingTest {

    @Test
    fun testDefaultSettingsDemoModeIsDisabled() {
        val settings = DriveMateSettings()
        assertFalse(settings.isDemoModeEnabled)
    }

    @Test
    fun testDemoDocumentIdentification() {
        val demoDoc = VehicleDocument(
            id = "demo_doc_rc",
            title = "[DEMO] Registration Certificate (RC)",
            type = DocumentType.REGISTRATION_CERTIFICATE,
            documentNumber = "MH 28 BW 1624"
        )
        val realDoc = VehicleDocument(
            id = "user_doc_1",
            title = "Registration Certificate (RC)",
            type = DocumentType.REGISTRATION_CERTIFICATE,
            documentNumber = "MH 28 BW 1624"
        )

        assertTrue(demoDoc.id.startsWith("demo_") || demoDoc.title.startsWith("[DEMO]"))
        assertFalse(realDoc.id.startsWith("demo_") || realDoc.title.startsWith("[DEMO]"))
    }

    @Test
    fun testDemoServiceRecordIdentification() {
        val demoRec = ServiceRecord(
            id = "demo_rec_1",
            title = "[DEMO] First Free Periodic Service",
            type = ServiceType.PERIODIC_SERVICE,
            odometerKm = 1500.0
        )
        val realRec = ServiceRecord(
            id = "real_rec_1",
            title = "Oil Change & Filter",
            type = ServiceType.OIL_CHANGE,
            odometerKm = 7500.0
        )

        assertTrue(demoRec.id.startsWith("demo_") || demoRec.title.startsWith("[DEMO]"))
        assertFalse(realRec.id.startsWith("demo_") || realRec.title.startsWith("[DEMO]"))
    }

    @Test
    fun testDemoExpenseIdentification() {
        val demoExp = VehicleExpense(
            id = "demo_exp_1",
            category = ExpenseCategory.FUEL,
            amount = 3500.0,
            notes = "[DEMO] Full tank petrol - Shell Highway"
        )
        val realExp = VehicleExpense(
            id = "real_exp_1",
            category = ExpenseCategory.FUEL,
            amount = 2500.0,
            notes = "BPCL Petrol"
        )

        assertTrue(demoExp.id.startsWith("demo_") || demoExp.notes.contains("[DEMO]"))
        assertFalse(realExp.id.startsWith("demo_") || realExp.notes.contains("[DEMO]"))
    }
}
