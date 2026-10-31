package com.policyregistration.app.data.repository

import android.content.Context
import com.policyregistration.app.data.local.AppDatabase
import com.policyregistration.app.data.local.CampaignEntity
import com.policyregistration.app.data.local.RegistrationEntity
import com.policyregistration.app.data.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar

data class DecryptedRegistration(
    val entity: RegistrationEntity,
    val aadhaarPlain: String
)

class RegistrationRepository(
    private val db: AppDatabase,
    private val cryptoManager: CryptoManager
) {
    private val registrationDao = db.registrationDao()
    private val campaignDao = db.campaignDao()

    fun getAllRegistrations(): Flow<List<DecryptedRegistration>> {
        return registrationDao.getAllRegistrations().map { list ->
            list.map { entity ->
                DecryptedRegistration(
                    entity = entity,
                    aadhaarPlain = cryptoManager.decryptAadhaar(entity.aadhaarNoEncrypted)
                )
            }
        }
    }

    fun getRegistrationsByDateRange(startTime: Long, endTime: Long): Flow<List<DecryptedRegistration>> {
        return registrationDao.getRegistrationsByDateRange(startTime, endTime).map { list ->
            list.map { entity ->
                DecryptedRegistration(
                    entity = entity,
                    aadhaarPlain = cryptoManager.decryptAadhaar(entity.aadhaarNoEncrypted)
                )
            }
        }
    }

    fun getRegistrationsByCampaign(campaignId: Long): Flow<List<DecryptedRegistration>> {
        return registrationDao.getRegistrationsByCampaign(campaignId).map { list ->
            list.map { entity ->
                DecryptedRegistration(
                    entity = entity,
                    aadhaarPlain = cryptoManager.decryptAadhaar(entity.aadhaarNoEncrypted)
                )
            }
        }
    }

    suspend fun getRegistrationsForExport(
        campaignId: Long?,
        startTime: Long? = null,
        endTime: Long? = null
    ): List<DecryptedRegistration> = withContext(Dispatchers.IO) {
        val list = when {
            startTime != null && endTime != null -> {
                registrationDao.getRegistrationsByDateRangeDirect(startTime, endTime)
            }
            campaignId != null -> {
                registrationDao.getRegistrationsByCampaignDirect(campaignId)
            }
            else -> {
                registrationDao.getAllRegistrationsDirect()
            }
        }
        list.map {
            DecryptedRegistration(
                entity = it,
                aadhaarPlain = cryptoManager.decryptAadhaar(it.aadhaarNoEncrypted)
            )
        }
    }

    fun getTotalCount(): Flow<Int> = registrationDao.getRegistrationCount()

    fun getTodayCount(): Flow<Int> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return registrationDao.getTodayRegistrationCount(cal.timeInMillis)
    }

    fun getActiveCampaignFlow(): Flow<CampaignEntity?> = campaignDao.getActiveCampaignFlow()

    suspend fun getActiveCampaign(): CampaignEntity? = withContext(Dispatchers.IO) {
        val existing = campaignDao.getActiveCampaign()
        if (existing != null) {
            existing
        } else {
            val defaultCampaign = CampaignEntity(
                id = 1L,
                insuranceCompany = "UNITED INDIA INSURANCE COMPANY LIMITED, Tirupathi",
                district = "CHITTOOR(Dist)",
                meetingPlace = "Duddeda",
                campaignDate = "14-10-2026",
                certificateNo = "051200/47",
                startingSerialNo = 10119L,
                serialNumberRange = "10119 to 10151",
                policyValidFrom = "15-11-2026",
                policyValidTo = "14-11-2027",
                defaultVillageAddress = "",
                isActive = true
            )
            campaignDao.insertCampaign(defaultCampaign)
            defaultCampaign
        }
    }

    suspend fun ensureDefaultCampaignUpdated() = withContext(Dispatchers.IO) {
        val existing = campaignDao.getActiveCampaign()
        if (existing == null || existing.insuranceCompany.isBlank() || existing.meetingPlace.isBlank()) {
            val defaultCampaign = CampaignEntity(
                id = existing?.id ?: 1L,
                insuranceCompany = "UNITED INDIA INSURANCE COMPANY LIMITED, Tirupathi",
                district = "CHITTOOR(Dist)",
                meetingPlace = "Duddeda",
                campaignDate = "14-10-2026",
                certificateNo = "051200/47",
                startingSerialNo = 10119L,
                serialNumberRange = "10119 to 10151",
                policyValidFrom = "15-11-2026",
                policyValidTo = "14-11-2027",
                defaultVillageAddress = existing?.defaultVillageAddress ?: "",
                isActive = true
            )
            campaignDao.insertCampaign(defaultCampaign)
        }
    }

    suspend fun updateCampaign(campaign: CampaignEntity) = withContext(Dispatchers.IO) {
        campaignDao.updateCampaign(campaign)
    }

    /**
     * Calculates the strictly monotonically increasing serial number.
     * Guaranteed never to reuse deleted numbers (MAX(sNo) + 1).
     */
    suspend fun getNextSerialNumber(): Long = withContext(Dispatchers.IO) {
        val maxExisting = registrationDao.getMaxSerialNumber() ?: 0L
        val activeCampaign = campaignDao.getActiveCampaign()
        val startNumber = activeCampaign?.startingSerialNo ?: 1L
        if (maxExisting >= startNumber) {
            maxExisting + 1
        } else {
            startNumber
        }
    }

    suspend fun isAadhaarRegistered(plainAadhaar: String): Boolean = withContext(Dispatchers.IO) {
        val encrypted = cryptoManager.encryptAadhaar(plainAadhaar.trim())
        registrationDao.findByAadhaarEncrypted(encrypted) != null
    }

    suspend fun isPhoneRegistered(phone: String, excludeRecordId: Long? = null): Boolean = withContext(Dispatchers.IO) {
        val clean = phone.filter { it.isDigit() }
        if (clean.length != 10) return@withContext false
        val found = registrationDao.findByPhoneNumber(clean)
        found != null && (excludeRecordId == null || found.recordId != excludeRecordId)
    }

    suspend fun saveRegistration(
        plainAadhaar: String,
        name: String,
        address: String,
        age: Int,
        nominee: String,
        relation: String,
        phoneNumber: String,
        dob: String,
        gender: String,
        accidentPrevious: String,
        existingPolicy: String,
        sourceType: com.policyregistration.app.data.local.SourceType,
        ocrStatus: com.policyregistration.app.data.local.OcrConfidenceStatus,
        scannedImagePath: String? = null
    ): Result<Long> = withContext(Dispatchers.IO) {
        val cleanAadhaar = plainAadhaar.filter { it.isDigit() }
        if (cleanAadhaar.length != 12) {
            return@withContext Result.failure(IllegalArgumentException("Aadhaar must be exactly 12 digits"))
        }

        val encryptedAadhaar = cryptoManager.encryptAadhaar(cleanAadhaar)
        val existing = registrationDao.findByAadhaarEncrypted(encryptedAadhaar)
        if (existing != null) {
            return@withContext Result.failure(IllegalStateException("Aadhaar already registered with S.No ${existing.sNo}"))
        }

        val sNo = getNextSerialNumber()
        val activeCampaign = campaignDao.getActiveCampaign()
        val campaignId = activeCampaign?.id ?: 1L
        val last4 = if (cleanAadhaar.length >= 4) cleanAadhaar.takeLast(4) else "0000"

        val entity = RegistrationEntity(
            sNo = sNo,
            aadhaarNoEncrypted = encryptedAadhaar,
            aadhaarLast4 = "XXXX-XXXX-$last4",
            nameOfTheMason = name.trim(),
            address = address.trim(),
            age = age,
            nominee = nominee.trim(),
            relation = relation.trim(),
            phoneNumber = phoneNumber.filter { it.isDigit() },
            dob = dob.trim(),
            gender = gender.trim(),
            accidentPrevious = accidentPrevious.trim(),
            existingPolicy = existingPolicy.trim(),
            campaignId = campaignId,
            sourceType = sourceType,
            ocrStatus = ocrStatus,
            scannedImagePath = scannedImagePath
        )

        val rowId = registrationDao.insertRegistration(entity)
        Result.success(sNo)
    }

    suspend fun updateRegistration(
        recordId: Long,
        sNo: Long,
        plainAadhaar: String,
        name: String,
        address: String,
        age: Int,
        nominee: String,
        relation: String,
        phoneNumber: String,
        dob: String,
        gender: String,
        accidentPrevious: String,
        existingPolicy: String,
        sourceType: com.policyregistration.app.data.local.SourceType,
        ocrStatus: com.policyregistration.app.data.local.OcrConfidenceStatus,
        campaignId: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanAadhaar = plainAadhaar.filter { it.isDigit() }
        val encryptedAadhaar = cryptoManager.encryptAadhaar(cleanAadhaar)
        val last4 = if (cleanAadhaar.length >= 4) cleanAadhaar.takeLast(4) else "0000"

        val existing = registrationDao.findByAadhaarEncrypted(encryptedAadhaar)
        if (existing != null && existing.recordId != recordId) {
            return@withContext Result.failure(IllegalStateException("Another record already has Aadhaar $cleanAadhaar"))
        }

        val entity = RegistrationEntity(
            recordId = recordId,
            sNo = sNo,
            aadhaarNoEncrypted = encryptedAadhaar,
            aadhaarLast4 = "XXXX-XXXX-$last4",
            nameOfTheMason = name.trim(),
            address = address.trim(),
            age = age,
            nominee = nominee.trim(),
            relation = relation.trim(),
            phoneNumber = phoneNumber.filter { it.isDigit() },
            dob = dob.trim(),
            gender = gender.trim(),
            accidentPrevious = accidentPrevious.trim(),
            existingPolicy = existingPolicy.trim(),
            campaignId = campaignId,
            updatedAt = System.currentTimeMillis(),
            sourceType = sourceType,
            ocrStatus = ocrStatus
        )
        registrationDao.updateRegistration(entity)
        Result.success(Unit)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        registrationDao.deleteById(id)
    }
}
