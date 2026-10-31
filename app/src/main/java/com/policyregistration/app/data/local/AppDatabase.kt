package com.policyregistration.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Converters {
    @TypeConverter
    fun fromSourceType(value: SourceType): String = value.name

    @TypeConverter
    fun toSourceType(value: String): SourceType = enumValueOf(value)

    @TypeConverter
    fun fromOcrConfidenceStatus(value: OcrConfidenceStatus): String = value.name

    @TypeConverter
    fun toOcrConfidenceStatus(value: String): OcrConfidenceStatus = enumValueOf(value)
}

@Database(
    entities = [RegistrationEntity::class, CampaignEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun registrationDao(): RegistrationDao
    abstract fun campaignDao(): CampaignDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "PolicyRegistrationDB"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Initialize default active campaign
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).campaignDao().insertCampaign(
                                    CampaignEntity(
                                        id = 1L,
                                        insuranceCompany = "UNITED INDIA INSURANCE COMPANY LIMITED",
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
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

