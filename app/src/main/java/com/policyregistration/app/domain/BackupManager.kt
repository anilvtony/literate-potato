package com.policyregistration.app.domain

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.policyregistration.app.data.local.AppDatabase
import com.policyregistration.app.data.local.CampaignEntity
import com.policyregistration.app.data.local.RegistrationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupContainer(
    val exportTimestamp: Long,
    val campaigns: List<CampaignEntity>,
    val registrations: List<RegistrationEntity>
)

class BackupManager(private val db: AppDatabase) {

    private val gson = Gson()

    /**
     * Automatically creates a weekly local snapshot file in app's internal backup directory.
     * Checks if a snapshot has been taken within the last 7 days. If not, it saves a new weekly snapshot.
     * Keeps the last 8 weekly snapshots (2 months of full history).
     */
    suspend fun performWeeklyAutoBackup(context: Context): Result<File?> = withContext(Dispatchers.IO) {
        try {
            val backupDir = File(context.filesDir, "weekly_backups").apply { if (!exists()) mkdirs() }
            val existingBackups = backupDir.listFiles { file -> file.name.startsWith("weekly_backup_") }
            val oneWeekMillis = 7L * 24 * 60 * 60 * 1000 // 7 days in milliseconds

            val latestBackup = existingBackups?.maxByOrNull { it.lastModified() }
            val now = System.currentTimeMillis()

            // If a backup was already created within the last 7 days, skip until the week is up
            if (latestBackup != null && (now - latestBackup.lastModified()) < oneWeekMillis) {
                return@withContext Result.success(null)
            }

            val registrations = db.registrationDao().getAllRegistrationsDirect()
            if (registrations.isEmpty()) {
                return@withContext Result.success(null)
            }

            val campaign = db.campaignDao().getActiveCampaign()
            val campaigns = if (campaign != null) listOf(campaign) else emptyList()

            val backup = BackupContainer(
                exportTimestamp = now,
                campaigns = campaigns,
                registrations = registrations
            )

            val timeTag = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(now))
            val targetFile = File(backupDir, "weekly_backup_$timeTag.json")

            FileWriter(targetFile).use { writer ->
                gson.toJson(backup, writer)
            }

            // Keep only the latest 8 weekly backups (approx 2 months of snapshots)
            val allBackups = backupDir.listFiles { file -> file.name.startsWith("weekly_backup_") }
            if (allBackups != null && allBackups.size > 8) {
                allBackups.sortedBy { it.lastModified() }
                    .take(allBackups.size - 8)
                    .forEach { it.delete() }
            }

            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAutoBackupList(context: Context): List<File> = withContext(Dispatchers.IO) {
        val backupDir = File(context.filesDir, "weekly_backups")
        if (!backupDir.exists()) return@withContext emptyList()
        backupDir.listFiles { file -> file.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    suspend fun restoreFromLocalFile(file: File): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val backup: BackupContainer = FileReader(file).use { reader ->
                gson.fromJson(reader, object : TypeToken<BackupContainer>() {}.type)
            }

            var importedCount = 0
            for (reg in backup.registrations) {
                val existing = db.registrationDao().findByAadhaarEncrypted(reg.aadhaarNoEncrypted)
                if (existing == null) {
                    db.registrationDao().insertRegistration(reg.copy(recordId = 0L))
                    importedCount++
                }
            }

            for (camp in backup.campaigns) {
                db.campaignDao().insertCampaign(camp.copy(id = 0L))
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportBackup(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val registrations = db.registrationDao().getAllRegistrationsDirect()
            val campaign = db.campaignDao().getActiveCampaign()
            val campaigns = if (campaign != null) listOf(campaign) else emptyList()

            val backup = BackupContainer(
                exportTimestamp = System.currentTimeMillis(),
                campaigns = campaigns,
                registrations = registrations
            )

            context.contentResolver.openOutputStream(uri)?.use { out ->
                OutputStreamWriter(out).use { writer ->
                    gson.toJson(backup, writer)
                }
            } ?: return@withContext Result.failure(Exception("Cannot open backup output stream"))

            Result.success(registrations.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open backup input stream"))

            val backup: BackupContainer = inputStream.use { inp ->
                InputStreamReader(inp).use { reader ->
                    gson.fromJson(reader, object : TypeToken<BackupContainer>() {}.type)
                }
            }

            var importedCount = 0
            for (reg in backup.registrations) {
                // Avoid duplicating exact encrypted Aadhaar records
                val existing = db.registrationDao().findByAadhaarEncrypted(reg.aadhaarNoEncrypted)
                if (existing == null) {
                    db.registrationDao().insertRegistration(reg.copy(recordId = 0L))
                    importedCount++
                }
            }

            for (camp in backup.campaigns) {
                db.campaignDao().insertCampaign(camp.copy(id = 0L))
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
