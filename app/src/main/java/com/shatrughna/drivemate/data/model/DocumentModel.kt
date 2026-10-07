package com.shatrughna.drivemate.data.model

import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Types of vehicle and driver documents supported by DriveMate Document Vault.
 */
enum class DocumentType(val displayName: String, val isVehicleSpecific: Boolean) {
    REGISTRATION_CERTIFICATE("RC (Registration Certificate)", true),
    INSURANCE("Motor Insurance Policy", true),
    PUC("PUC (Pollution Under Control)", true),
    DRIVING_LICENCE("Driving Licence (DL)", false),
    FITNESS_CERTIFICATE("Fitness Certificate", true),
    ROAD_TAX("Road Tax Receipt", true),
    OTHER("Other Document", true)
}

/**
 * Document validity status.
 */
enum class DocumentExpiryStatus {
    VALID,
    EXPIRING_SOON,
    EXPIRED
}

/**
 * Secure document record model stored in app-private storage.
 */
data class VehicleDocument(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: DocumentType,
    val documentNumber: String,
    val issuingAuthority: String = "",
    val issueDateMillis: Long = System.currentTimeMillis(),
    val expiryDateMillis: Long? = null,
    val notes: String = "",
    val localImagePath: String? = null,
    val isSensitive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun daysUntilExpiry(nowMillis: Long = System.currentTimeMillis()): Long? {
        val expiry = expiryDateMillis ?: return null
        val diffMillis = expiry - nowMillis
        return TimeUnit.MILLISECONDS.toDays(diffMillis)
    }

    fun status(nowMillis: Long = System.currentTimeMillis()): DocumentExpiryStatus {
        val days = daysUntilExpiry(nowMillis) ?: return DocumentExpiryStatus.VALID
        return when {
            days < 0 -> DocumentExpiryStatus.EXPIRED
            days <= 30 -> DocumentExpiryStatus.EXPIRING_SOON
            else -> DocumentExpiryStatus.VALID
        }
    }

    fun expiryBadgeText(nowMillis: Long = System.currentTimeMillis()): String {
        val days = daysUntilExpiry(nowMillis) ?: return "No Expiry"
        return when {
            days < 0 -> "Expired ${-days}d ago"
            days == 0L -> "Expires today"
            days <= 30 -> "Expires in ${days}d"
            else -> "Valid (${days}d left)"
        }
    }

    fun maskedDocumentNumber(): String {
        if (documentNumber.length <= 4) return documentNumber
        val visiblePart = documentNumber.takeLast(4)
        val maskedPart = "•".repeat(documentNumber.length - 4)
        return "$maskedPart $visiblePart"
    }
}
