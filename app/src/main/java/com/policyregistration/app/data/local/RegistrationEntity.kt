package com.policyregistration.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SourceType {
    MANUAL,
    CAMERA_OCR,
    GALLERY_OCR
}

enum class OcrConfidenceStatus {
    NONE,       // for manual entries
    CONFIDENT,  // all fields extracted cleanly
    NEEDS_REVIEW // some fields were ambiguous/missing and operator corrected
}

@Entity(
    tableName = "registrations",
    indices = [
        Index(value = ["aadhaarNoEncrypted"]),
        Index(value = ["sNo"]),
        Index(value = ["campaignId"])
    ]
)
data class RegistrationEntity(
    @PrimaryKey(autoGenerate = true)
    val recordId: Long = 0L,
    val sNo: Long,                      // Column A: S_No (strictly non-reusable)
    val aadhaarNoEncrypted: String,     // Column B: Aadhaar_No (Encrypted at rest)
    val aadhaarLast4: String,           // Masked helper (e.g. "XXXX-XXXX-1234") for duplicate check & display
    val nameOfTheMason: String,         // Column C: Name_of_the_Mason (Title Case)
    val address: String,                // Column D: Address
    val age: Int,                       // Column E: Age
    val nominee: String,                // Column F: Nominee
    val relation: String,               // Column G: Relation
    val phoneNumber: String,            // Column H: Phone_Number
    val dob: String,                    // Column I: DOB (DD.MM.YYYY)
    val gender: String,                 // Column J: Gender (Male / Female / Other)
    val accidentPrevious: String,       // Column K: Accident_Previous (Yes / No)
    val existingPolicy: String,         // Column L: Existing_Policy (Yes / No)

    // Internal Metadata
    val campaignId: Long = 1L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sourceType: SourceType = SourceType.MANUAL,
    val ocrStatus: OcrConfidenceStatus = OcrConfidenceStatus.NONE,
    val scannedImagePath: String? = null
)

