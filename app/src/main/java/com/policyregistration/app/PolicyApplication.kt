package com.policyregistration.app

import android.app.Application
import com.policyregistration.app.data.local.AppDatabase
import com.policyregistration.app.data.repository.RegistrationRepository
import com.policyregistration.app.data.security.CryptoManager
import com.policyregistration.app.domain.BackupManager
import com.policyregistration.app.domain.ExcelExporter
import com.policyregistration.app.domain.OcrExtractionEngine

class PolicyApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var cryptoManager: CryptoManager
        private set

    lateinit var repository: RegistrationRepository
        private set

    lateinit var ocrEngine: OcrExtractionEngine
        private set

    lateinit var excelExporter: ExcelExporter
        private set

    lateinit var backupManager: BackupManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        cryptoManager = CryptoManager(this)
        repository = RegistrationRepository(database, cryptoManager)
        ocrEngine = OcrExtractionEngine()
        excelExporter = ExcelExporter()
        backupManager = BackupManager(database)
    }
}

