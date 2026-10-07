package com.shatrughna.drivemate.data.repository

import android.content.Context
import com.shatrughna.drivemate.data.model.DocumentType
import com.shatrughna.drivemate.data.model.VehicleDocument
import com.shatrughna.drivemate.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

interface DocumentVaultRepository {
    val documents: StateFlow<List<VehicleDocument>>
    suspend fun addOrUpdateDocument(document: VehicleDocument)
    suspend fun deleteDocument(id: String)
    fun getDocument(id: String): VehicleDocument?
    suspend fun seedDemoDocuments()
    suspend fun clearDemoDocuments()
}

class DocumentVaultRepositoryImpl(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : DocumentVaultRepository {

    companion object {
        private const val DIRECTORY_NAME = "documents"
        private const val FILE_NAME = "vehicle_vault.json"
    }

    private val _documents = MutableStateFlow<List<VehicleDocument>>(emptyList())
    override val documents: StateFlow<List<VehicleDocument>> = _documents.asStateFlow()

    private val storageDir: File by lazy {
        File(context.filesDir, DIRECTORY_NAME).apply { if (!exists()) mkdirs() }
    }

    private val storageFile: File by lazy {
        File(storageDir, FILE_NAME)
    }

    init {
        scope.launch {
            loadFromDisk()
        }
    }

    private suspend fun loadFromDisk() = withContext(Dispatchers.IO) {
        if (!storageFile.exists()) {
            // Production first launch: Start completely empty
            _documents.value = emptyList()
            return@withContext
        }
        try {
            val jsonStr = storageFile.readText()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<VehicleDocument>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    VehicleDocument(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        type = DocumentType.valueOf(obj.optString("type", DocumentType.OTHER.name)),
                        documentNumber = obj.getString("documentNumber"),
                        issuingAuthority = obj.optString("issuingAuthority", ""),
                        issueDateMillis = obj.optLong("issueDateMillis", System.currentTimeMillis()),
                        expiryDateMillis = if (obj.has("expiryDateMillis") && !obj.isNull("expiryDateMillis")) obj.getLong("expiryDateMillis") else null,
                        notes = obj.optString("notes", ""),
                        localImagePath = if (obj.has("localImagePath") && !obj.isNull("localImagePath")) obj.getString("localImagePath") else null,
                        isSensitive = obj.optBoolean("isSensitive", true),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
            _documents.value = list
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.APP, "Failed to load document vault: corrupted file", e)
            try {
                val backupFile = File(storageDir, "vehicle_vault.json.corrupt.${System.currentTimeMillis()}")
                storageFile.renameTo(backupFile)
            } catch (backupEx: Exception) {
                AppLogger.e(AppLogger.Tag.APP, "Failed to rename corrupt vault file", backupEx)
            }
            _documents.value = emptyList()
        }
    }

    override suspend fun addOrUpdateDocument(document: VehicleDocument) = withContext(Dispatchers.IO) {
        val current = _documents.value.toMutableList()
        val index = current.indexOfFirst { it.id == document.id }
        if (index >= 0) {
            current[index] = document.copy(updatedAt = System.currentTimeMillis())
        } else {
            current.add(0, document)
        }
        _documents.value = current
        saveToDisk(current)
    }

    override suspend fun deleteDocument(id: String) = withContext(Dispatchers.IO) {
        val current = _documents.value.toMutableList()
        current.removeAll { it.id == id }
        _documents.value = current
        saveToDisk(current)
    }

    override fun getDocument(id: String): VehicleDocument? {
        return _documents.value.find { it.id == id }
    }

    private fun saveToDisk(list: List<VehicleDocument>) {
        try {
            val array = JSONArray()
            for (doc in list) {
                val obj = JSONObject().apply {
                    put("id", doc.id)
                    put("title", doc.title)
                    put("type", doc.type.name)
                    put("documentNumber", doc.documentNumber)
                    put("issuingAuthority", doc.issuingAuthority)
                    put("issueDateMillis", doc.issueDateMillis)
                    if (doc.expiryDateMillis != null) put("expiryDateMillis", doc.expiryDateMillis)
                    put("notes", doc.notes)
                    if (doc.localImagePath != null) put("localImagePath", doc.localImagePath)
                    put("isSensitive", doc.isSensitive)
                    put("createdAt", doc.createdAt)
                    put("updatedAt", doc.updatedAt)
                }
                array.put(obj)
            }
            storageFile.writeText(array.toString(2))
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.APP, "Failed to write document vault to disk", e)
        }
    }

    override suspend fun seedDemoDocuments() = withContext(Dispatchers.IO) {
        val current = _documents.value.filterNot { it.id.startsWith("demo_") || it.title.startsWith("[DEMO]") }.toMutableList()
        current.addAll(0, createDemoDocuments())
        _documents.value = current
        saveToDisk(current)
        AppLogger.i(AppLogger.Tag.APP, "Seeded demo vehicle documents into vault.")
    }

    override suspend fun clearDemoDocuments() = withContext(Dispatchers.IO) {
        val filtered = _documents.value.filterNot { it.id.startsWith("demo_") || it.title.startsWith("[DEMO]") }
        _documents.value = filtered
        saveToDisk(filtered)
        AppLogger.i(AppLogger.Tag.APP, "Cleared all demo documents from vault.")
    }

    private fun createDemoDocuments(): List<VehicleDocument> {
        val now = System.currentTimeMillis()
        val oneDay = TimeUnit.DAYS.toMillis(1)
        return listOf(
            VehicleDocument(
                id = "demo_doc_rc",
                title = "[DEMO] Tata Nexon RC Smart Card",
                type = DocumentType.REGISTRATION_CERTIFICATE,
                documentNumber = "MH 28 BW 1624",
                issuingAuthority = "RTO Buldhana, Maharashtra",
                issueDateMillis = now - (365 * oneDay),
                expiryDateMillis = now + (3650 * oneDay),
                notes = "Tata Nexon Creative+ S (Creative Plus Sunroof)",
                isSensitive = true
            ),
            VehicleDocument(
                id = "demo_doc_insurance",
                title = "[DEMO] Comprehensive Motor Insurance",
                type = DocumentType.INSURANCE,
                documentNumber = "POL-TATA-2026-98124",
                issuingAuthority = "Tata AIG General Insurance",
                issueDateMillis = now - (60 * oneDay),
                expiryDateMillis = now + (305 * oneDay),
                notes = "Zero Dep + Engine Protect + Roadside Assistance",
                isSensitive = true
            ),
            VehicleDocument(
                id = "demo_doc_puc",
                title = "[DEMO] Pollution Under Control (PUC)",
                type = DocumentType.PUC,
                documentNumber = "MH28-PUC-2026-443",
                issuingAuthority = "Govt. of Maharashtra Transport",
                issueDateMillis = now - (160 * oneDay),
                expiryDateMillis = now + (20 * oneDay),
                notes = "Emission test compliant (BS6 Phase 2)",
                isSensitive = false
            ),
            VehicleDocument(
                id = "demo_doc_dl",
                title = "[DEMO] Driving Licence",
                type = DocumentType.DRIVING_LICENCE,
                documentNumber = "MH28 20190004521",
                issuingAuthority = "Govt. of Maharashtra",
                issueDateMillis = now - (730 * oneDay),
                expiryDateMillis = now + (5000 * oneDay),
                notes = "LMV-NT + MCWG (Shatrughna Ambhore)",
                isSensitive = true
            )
        )
    }
}
