package com.policyregistration.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistrationDao {

    @Query("SELECT * FROM registrations ORDER BY sNo ASC")
    fun getAllRegistrations(): Flow<List<RegistrationEntity>>

    @Query("SELECT * FROM registrations WHERE campaignId = :campaignId ORDER BY sNo ASC")
    fun getRegistrationsByCampaign(campaignId: Long): Flow<List<RegistrationEntity>>

    @Query("SELECT * FROM registrations WHERE campaignId = :campaignId ORDER BY sNo ASC")
    suspend fun getRegistrationsByCampaignDirect(campaignId: Long): List<RegistrationEntity>

    @Query("SELECT * FROM registrations ORDER BY sNo ASC")
    suspend fun getAllRegistrationsDirect(): List<RegistrationEntity>

    @Query("SELECT * FROM registrations WHERE createdAt >= :startTime AND createdAt <= :endTime ORDER BY sNo ASC")
    fun getRegistrationsByDateRange(startTime: Long, endTime: Long): Flow<List<RegistrationEntity>>

    @Query("SELECT * FROM registrations WHERE createdAt >= :startTime AND createdAt <= :endTime ORDER BY sNo ASC")
    suspend fun getRegistrationsByDateRangeDirect(startTime: Long, endTime: Long): List<RegistrationEntity>

    @Query("SELECT * FROM registrations WHERE recordId = :id LIMIT 1")
    suspend fun getRegistrationById(id: Long): RegistrationEntity?

    @Query("SELECT MAX(sNo) FROM registrations")
    suspend fun getMaxSerialNumber(): Long?

    @Query("SELECT COUNT(*) FROM registrations")
    fun getRegistrationCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM registrations WHERE createdAt >= :startOfDayTimestamp")
    fun getTodayRegistrationCount(startOfDayTimestamp: Long): Flow<Int>

    @Query("SELECT * FROM registrations WHERE aadhaarNoEncrypted = :encryptedAadhaar LIMIT 1")
    suspend fun findByAadhaarEncrypted(encryptedAadhaar: String): RegistrationEntity?

    @Query("SELECT * FROM registrations WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun findByPhoneNumber(phoneNumber: String): RegistrationEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRegistration(entity: RegistrationEntity): Long

    @Update
    suspend fun updateRegistration(entity: RegistrationEntity)

    @Delete
    suspend fun deleteRegistration(entity: RegistrationEntity)

    @Query("DELETE FROM registrations WHERE recordId = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM registrations")
    suspend fun clearAll()
}
