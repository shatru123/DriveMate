package com.shatrughna.drivemate

import com.shatrughna.drivemate.data.model.DocumentExpiryStatus
import com.shatrughna.drivemate.data.model.DocumentType
import com.shatrughna.drivemate.data.model.VehicleDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class DocumentVaultModelTest {

    @Test
    fun testValidDocumentStatus() {
        val now = System.currentTimeMillis()
        val doc = VehicleDocument(
            title = "Tata Nexon RC",
            type = DocumentType.REGISTRATION_CERTIFICATE,
            documentNumber = "MH 28 BW 1624",
            expiryDateMillis = now + TimeUnit.DAYS.toMillis(180)
        )

        assertEquals(DocumentExpiryStatus.VALID, doc.status(now))
        assertTrue(doc.daysUntilExpiry(now)!! in 179..181)
        assertTrue(doc.expiryBadgeText(now).contains("Valid"))
    }

    @Test
    fun testExpiringSoonDocumentStatus() {
        val now = System.currentTimeMillis()
        val doc = VehicleDocument(
            title = "Pollution Under Control (PUC)",
            type = DocumentType.PUC,
            documentNumber = "MH28-PUC-1234",
            expiryDateMillis = now + TimeUnit.DAYS.toMillis(12)
        )

        assertEquals(DocumentExpiryStatus.EXPIRING_SOON, doc.status(now))
        assertEquals(12L, doc.daysUntilExpiry(now))
        assertTrue(doc.expiryBadgeText(now).contains("Expires in 12d"))
    }

    @Test
    fun testExpiredDocumentStatus() {
        val now = System.currentTimeMillis()
        val doc = VehicleDocument(
            title = "Old Insurance",
            type = DocumentType.INSURANCE,
            documentNumber = "POL-9999",
            expiryDateMillis = now - TimeUnit.DAYS.toMillis(5)
        )

        assertEquals(DocumentExpiryStatus.EXPIRED, doc.status(now))
        assertTrue(doc.daysUntilExpiry(now)!! < 0)
        assertTrue(doc.expiryBadgeText(now).contains("Expired"))
    }

    @Test
    fun testMaskedDocumentNumber() {
        val doc = VehicleDocument(
            title = "Smart Card",
            type = DocumentType.REGISTRATION_CERTIFICATE,
            documentNumber = "MH28BW1624"
        )
        val masked = doc.maskedDocumentNumber()
        assertTrue(masked.endsWith("1624"))
        assertTrue(masked.contains("•"))
    }
}
