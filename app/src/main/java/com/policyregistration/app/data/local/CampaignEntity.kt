package com.policyregistration.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "campaigns")
data class CampaignEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val insuranceCompany: String = "UNITED INDIA INSURANCE COMPANY LIMITED",
    val district: String = "CHITTOOR(Dist)",
    val meetingPlace: String = "Duddeda",
    val campaignDate: String = "14-10-2026",
    val certificateNo: String = "051200/47",
    val startingSerialNo: Long = 10119L,
    val serialNumberRange: String = "10119 to 10151",
    val policyValidFrom: String = "15-11-2026",
    val policyValidTo: String = "14-11-2027",
    val defaultVillageAddress: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

