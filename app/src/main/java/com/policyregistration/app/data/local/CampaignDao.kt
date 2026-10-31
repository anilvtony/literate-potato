package com.policyregistration.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignDao {

    @Query("SELECT * FROM campaigns WHERE isActive = 1 LIMIT 1")
    fun getActiveCampaignFlow(): Flow<CampaignEntity?>

    @Query("SELECT * FROM campaigns WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveCampaign(): CampaignEntity?

    @Query("SELECT * FROM campaigns ORDER BY createdAt DESC")
    fun getAllCampaigns(): Flow<List<CampaignEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaign(campaign: CampaignEntity): Long

    @Update
    suspend fun updateCampaign(campaign: CampaignEntity)
}

