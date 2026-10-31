package com.policyregistration.app.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.policyregistration.app.PolicyApplication
import com.policyregistration.app.data.local.CampaignEntity
import com.policyregistration.app.data.local.OcrConfidenceStatus
import com.policyregistration.app.data.local.SourceType
import com.policyregistration.app.data.repository.DecryptedRegistration
import com.policyregistration.app.domain.ExtractedRegistrationFields
import com.policyregistration.app.domain.FormValidators
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RegistrationFormState(
    val sNo: Long = 1L,
    val aadhaarNo: String = "",
    val name: String = "",
    val address: String = "",
    val age: String = "",
    val nominee: String = "",
    val relation: String = "Wife",
    val phoneNumber: String = "",
    val dob: String = "",
    val gender: String = "Male",
    val accidentPrevious: String = "No",
    val existingPolicy: String = "No",
    val isStickyAddressEnabled: Boolean = true,
    val stickyAddressValue: String = "",
    val duplicateAadhaarWarning: Boolean = false,
    val duplicatePhoneWarning: Boolean = false,
    val editingRecordId: Long? = null,
    val ocrReviewWarnings: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val saveSuccessMessage: String? = null,
    val errorMessage: String? = null,
    val scannedImagePath: String? = null,
    val sourceType: SourceType = SourceType.MANUAL
)

data class BatchItem(
    val id: String,
    val uri: Uri,
    var bitmap: Bitmap? = null,
    var extractedFields: ExtractedRegistrationFields? = null,
    var isDuplicateAadhaar: Boolean = false,
    var isSelectedForSave: Boolean = true,
    var isSaved: Boolean = false
)

data class BatchOcrState(
    val isProcessing: Boolean = false,
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val items: List<BatchItem> = emptyList(),
    val isSavingBatch: Boolean = false,
    val savedCount: Int = 0,
    val errorMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PolicyApplication
    private val repository = app.repository
    private val ocrEngine = app.ocrEngine
    private val excelExporter = app.excelExporter
    private val backupManager = app.backupManager

    val totalCount = repository.getTotalCount().stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val todayCount = repository.getTodayCount().stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val activeCampaign = repository.getActiveCampaignFlow().stateIn(viewModelScope, SharingStarted.Lazily, null)
    val allRegistrations = repository.getAllRegistrations().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _formState = MutableStateFlow(RegistrationFormState())
    val formState: StateFlow<RegistrationFormState> = _formState.asStateFlow()

    private val _ocrPendingFields = MutableStateFlow<ExtractedRegistrationFields?>(null)
    val ocrPendingFields: StateFlow<ExtractedRegistrationFields?> = _ocrPendingFields.asStateFlow()

    private val _currentScannedImage = MutableStateFlow<Bitmap?>(null)
    val currentScannedImage: StateFlow<Bitmap?> = _currentScannedImage.asStateFlow()

    private val _batchOcrState = MutableStateFlow(BatchOcrState())
    val batchOcrState: StateFlow<BatchOcrState> = _batchOcrState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultCampaignUpdated()
            loadNextSerialNumber()
        }
    }

    fun loadNextSerialNumber() {
        viewModelScope.launch {
            val nextSno = repository.getNextSerialNumber()
            val campaign = repository.getActiveCampaign()
            _formState.value = _formState.value.copy(
                sNo = nextSno,
                address = if (_formState.value.isStickyAddressEnabled && _formState.value.stickyAddressValue.isNotBlank()) {
                    _formState.value.stickyAddressValue
                } else {
                    campaign?.defaultVillageAddress ?: ""
                }
            )
        }
    }

    fun onAadhaarChanged(value: String) {
        val digits = value.filter { it.isDigit() }.take(12)
        _formState.value = _formState.value.copy(
            aadhaarNo = digits,
            duplicateAadhaarWarning = false
        )
        if (digits.length == 12) {
            checkDuplicateAadhaar(digits)
        }
    }

    fun onNameChanged(value: String) {
        _formState.value = _formState.value.copy(name = FormValidators.toTitleCase(value))
    }

    fun onAddressChanged(value: String) {
        _formState.value = _formState.value.copy(
            address = value,
            stickyAddressValue = if (_formState.value.isStickyAddressEnabled) value else _formState.value.stickyAddressValue
        )
    }

    fun toggleStickyAddress(enabled: Boolean) {
        _formState.value = _formState.value.copy(
            isStickyAddressEnabled = enabled,
            stickyAddressValue = if (enabled) _formState.value.address else ""
        )
    }

    fun onDobChanged(value: String) {
        _formState.value = _formState.value.copy(dob = value)
        val normalized = FormValidators.normalizeDate(value)
        if (normalized != null) {
            val calcAge = FormValidators.calculateAge(normalized)
            if (calcAge != null) {
                _formState.value = _formState.value.copy(
                    dob = normalized,
                    age = calcAge.toString()
                )
            }
        }
    }

    fun onAgeChanged(value: String) {
        val digits = value.filter { it.isDigit() }.take(3)
        _formState.value = _formState.value.copy(age = digits)
    }

    fun onNomineeChanged(value: String) {
        _formState.value = _formState.value.copy(nominee = FormValidators.toTitleCase(value))
    }

    fun onRelationSelected(relation: String) {
        _formState.value = _formState.value.copy(relation = relation)
    }

    fun onPhoneChanged(value: String) {
        val digits = value.filter { it.isDigit() }.take(10)
        _formState.value = _formState.value.copy(
            phoneNumber = digits,
            duplicatePhoneWarning = false
        )
        if (digits.length == 10) {
            checkDuplicatePhone(digits)
        }
    }

    private fun checkDuplicatePhone(phone: String) {
        viewModelScope.launch {
            val exists = repository.isPhoneRegistered(phone, _formState.value.editingRecordId)
            _formState.value = _formState.value.copy(duplicatePhoneWarning = exists)
        }
    }

    fun startEditingRecord(item: DecryptedRegistration) {
        val ent = item.entity
        _formState.value = RegistrationFormState(
            editingRecordId = ent.recordId,
            sNo = ent.sNo,
            aadhaarNo = item.aadhaarPlain,
            name = ent.nameOfTheMason,
            address = ent.address,
            age = ent.age.toString(),
            nominee = ent.nominee,
            relation = ent.relation,
            phoneNumber = ent.phoneNumber,
            dob = ent.dob,
            gender = ent.gender,
            accidentPrevious = ent.accidentPrevious,
            existingPolicy = ent.existingPolicy,
            sourceType = ent.sourceType,
            duplicateAadhaarWarning = false,
            duplicatePhoneWarning = false
        )
    }

    fun resetFormForNewPerson() {
        loadNextSerialNumber()
    }

    fun onGenderChanged(gender: String) {
        _formState.value = _formState.value.copy(gender = gender)
    }

    fun onAccidentPreviousChanged(value: String) {
        _formState.value = _formState.value.copy(accidentPrevious = value)
    }

    fun onExistingPolicyChanged(value: String) {
        _formState.value = _formState.value.copy(existingPolicy = value)
    }

    private fun checkDuplicateAadhaar(digits: String) {
        viewModelScope.launch {
            val exists = repository.isAadhaarRegistered(digits)
            _formState.value = _formState.value.copy(duplicateAadhaarWarning = exists)
        }
    }

    fun processScannedImage(bitmap: Bitmap, fromGallery: Boolean = false) {
        _currentScannedImage.value = bitmap
        viewModelScope.launch {
            try {
                val extracted = ocrEngine.processImage(bitmap)
                _ocrPendingFields.value = extracted
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(errorMessage = "OCR Processing failed: ${e.localizedMessage}")
            }
        }
    }

    // MULTI-IMAGE BATCH OCR PROCESSING
    fun processMultipleImages(context: Context, uris: List<Uri>, onCompleted: () -> Unit) {
        if (uris.isEmpty()) return
        _batchOcrState.value = BatchOcrState(
            isProcessing = true,
            progressCurrent = 0,
            progressTotal = uris.size,
            items = emptyList()
        )

        viewModelScope.launch {
            val processedItems = mutableListOf<BatchItem>()
            val campaign = repository.getActiveCampaign()
            val defaultAddr = campaign?.defaultVillageAddress ?: ""

            for ((index, uri) in uris.withIndex()) {
                _batchOcrState.value = _batchOcrState.value.copy(progressCurrent = index + 1)
                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it)
                        }
                    }

                    if (bitmap != null) {
                        var extracted = ocrEngine.processImage(bitmap)
                        if (extracted.address.isBlank() && defaultAddr.isNotBlank()) {
                            extracted = extracted.copy(address = defaultAddr)
                        }

                        val isDuplicate = if (extracted.aadhaarNo.length == 12) {
                            repository.isAadhaarRegistered(extracted.aadhaarNo)
                        } else false

                        processedItems.add(
                            BatchItem(
                                id = uri.toString(),
                                uri = uri,
                                bitmap = bitmap,
                                extractedFields = extracted,
                                isDuplicateAadhaar = isDuplicate,
                                isSelectedForSave = !isDuplicate
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Skip failed image or continue
                }
            }

            _batchOcrState.value = BatchOcrState(
                isProcessing = false,
                progressCurrent = uris.size,
                progressTotal = uris.size,
                items = processedItems
            )
            onCompleted()
        }
    }

    fun toggleBatchItemSelection(itemId: String) {
        val updated = _batchOcrState.value.items.map {
            if (it.id == itemId) it.copy(isSelectedForSave = !it.isSelectedForSave) else it
        }
        _batchOcrState.value = _batchOcrState.value.copy(items = updated)
    }

    fun updateBatchItemField(itemId: String, updatedFields: ExtractedRegistrationFields) {
        val updated = _batchOcrState.value.items.map {
            if (it.id == itemId) {
                it.copy(extractedFields = updatedFields)
            } else it
        }
        _batchOcrState.value = _batchOcrState.value.copy(items = updated)
    }

    fun saveAllSelectedBatch(onFinished: (Int) -> Unit) {
        _batchOcrState.value = _batchOcrState.value.copy(isSavingBatch = true)
        viewModelScope.launch {
            var savedCount = 0
            val itemsToSave = _batchOcrState.value.items.filter { it.isSelectedForSave && !it.isSaved }

            for (item in itemsToSave) {
                val f = item.extractedFields ?: continue
                if (f.aadhaarNo.length != 12) continue

                val isDup = repository.isAadhaarRegistered(f.aadhaarNo)
                if (isDup) continue

                val res = repository.saveRegistration(
                    plainAadhaar = f.aadhaarNo,
                    name = f.name.ifBlank { "Mason" },
                    address = f.address,
                    age = f.age.toIntOrNull() ?: 0,
                    nominee = f.nominee,
                    relation = f.relation.ifBlank { "Wife" },
                    phoneNumber = f.phoneNumber,
                    dob = f.dob,
                    gender = f.gender,
                    accidentPrevious = f.accidentPrevious,
                    existingPolicy = f.existingPolicy,
                    sourceType = SourceType.GALLERY_OCR,
                    ocrStatus = if (f.confidenceNotes.isEmpty()) OcrConfidenceStatus.CONFIDENT else OcrConfidenceStatus.NEEDS_REVIEW
                )

                if (res.isSuccess) {
                    savedCount++
                    item.isSaved = true
                }
            }

            loadNextSerialNumber()
            _batchOcrState.value = _batchOcrState.value.copy(
                isSavingBatch = false,
                savedCount = savedCount
            )
            onFinished(savedCount)
        }
    }

    fun applyOcrFieldsToForm(extracted: ExtractedRegistrationFields, fromGallery: Boolean = false) {
        val currentSticky = if (_formState.value.isStickyAddressEnabled && _formState.value.stickyAddressValue.isNotBlank()) {
            _formState.value.stickyAddressValue
        } else {
            extracted.address
        }

        _formState.value = _formState.value.copy(
            aadhaarNo = extracted.aadhaarNo,
            name = extracted.name,
            address = if (currentSticky.isNotBlank()) currentSticky else extracted.address,
            age = extracted.age,
            nominee = extracted.nominee,
            relation = if (extracted.relation.isNotBlank()) extracted.relation else _formState.value.relation,
            phoneNumber = extracted.phoneNumber,
            dob = extracted.dob,
            gender = extracted.gender,
            accidentPrevious = extracted.accidentPrevious,
            existingPolicy = extracted.existingPolicy,
            sourceType = if (fromGallery) SourceType.GALLERY_OCR else SourceType.CAMERA_OCR,
            ocrReviewWarnings = extracted.confidenceNotes
        )
        if (extracted.aadhaarNo.length == 12) {
            checkDuplicateAadhaar(extracted.aadhaarNo)
        }
    }

    fun saveCurrentRegistration(onSuccess: (Long) -> Unit) {
        val s = _formState.value
        if (s.aadhaarNo.length != 12) {
            _formState.value = _formState.value.copy(errorMessage = "Aadhaar number must be exactly 12 digits")
            return
        }
        if (s.name.isBlank()) {
            _formState.value = _formState.value.copy(errorMessage = "Mason Name cannot be empty")
            return
        }
        if (s.phoneNumber.length != 10) {
            _formState.value = _formState.value.copy(errorMessage = "Phone number must be 10 digits")
            return
        }

        _formState.value = _formState.value.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            val status = if (s.sourceType == SourceType.MANUAL) {
                OcrConfidenceStatus.NONE
            } else if (s.ocrReviewWarnings.isEmpty()) {
                OcrConfidenceStatus.CONFIDENT
            } else {
                OcrConfidenceStatus.NEEDS_REVIEW
            }

            if (s.editingRecordId != null) {
                // UPDATE EXISTING RECORD
                val activeCampaign = repository.getActiveCampaign()
                val updateRes = repository.updateRegistration(
                    recordId = s.editingRecordId,
                    sNo = s.sNo,
                    plainAadhaar = s.aadhaarNo,
                    name = s.name,
                    address = s.address,
                    age = s.age.toIntOrNull() ?: 0,
                    nominee = s.nominee,
                    relation = s.relation,
                    phoneNumber = s.phoneNumber,
                    dob = s.dob,
                    gender = s.gender,
                    accidentPrevious = s.accidentPrevious,
                    existingPolicy = s.existingPolicy,
                    sourceType = s.sourceType,
                    ocrStatus = status,
                    campaignId = activeCampaign?.id ?: 1L
                )

                updateRes.onSuccess {
                    // Check and take weekly auto backup if 7 days elapsed
                    viewModelScope.launch { backupManager.performWeeklyAutoBackup(app) }
                    _formState.value = _formState.value.copy(
                        isSaving = false,
                        saveSuccessMessage = "Record #${s.sNo} updated successfully!"
                    )
                    onSuccess(s.sNo)
                }.onFailure { err ->
                    _formState.value = _formState.value.copy(
                        isSaving = false,
                        errorMessage = err.message ?: "Failed to update record"
                    )
                }
            } else {
                // INSERT NEW REGISTRATION
                val result = repository.saveRegistration(
                    plainAadhaar = s.aadhaarNo,
                    name = s.name,
                    address = s.address,
                    age = s.age.toIntOrNull() ?: 0,
                    nominee = s.nominee,
                    relation = s.relation,
                    phoneNumber = s.phoneNumber,
                    dob = s.dob,
                    gender = s.gender,
                    accidentPrevious = s.accidentPrevious,
                    existingPolicy = s.existingPolicy,
                    sourceType = s.sourceType,
                    ocrStatus = status,
                    scannedImagePath = s.scannedImagePath
                )

                result.onSuccess { sNo ->
                    val nextSNo = sNo + 1
                    val preservedAddress = if (s.isStickyAddressEnabled) s.address else ""
                    // Check and take weekly auto backup if 7 days elapsed
                    viewModelScope.launch { backupManager.performWeeklyAutoBackup(app) }
                    _formState.value = RegistrationFormState(
                        sNo = nextSNo,
                        address = preservedAddress,
                        stickyAddressValue = preservedAddress,
                        isStickyAddressEnabled = s.isStickyAddressEnabled,
                        saveSuccessMessage = "Successfully registered record S.No $sNo"
                    )
                    onSuccess(sNo)
                }.onFailure { err ->
                    _formState.value = _formState.value.copy(
                        isSaving = false,
                        errorMessage = err.message ?: "Failed to save registration"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _formState.value = _formState.value.copy(saveSuccessMessage = null, errorMessage = null)
    }

    fun deleteRecord(recordId: Long) {
        viewModelScope.launch {
            repository.deleteById(recordId)
        }
    }

    fun updateCampaignSettings(campaign: CampaignEntity) {
        viewModelScope.launch {
            repository.updateCampaign(campaign)
        }
    }

    fun exportExcel(
        context: android.content.Context,
        uri: Uri,
        startTime: Long? = null,
        endTime: Long? = null,
        onResult: (Result<Int>) -> Unit
    ) {
        viewModelScope.launch {
            val active = repository.getActiveCampaign()
            val records = repository.getRegistrationsForExport(
                campaignId = active?.id,
                startTime = startTime,
                endTime = endTime
            )
            val res = excelExporter.exportToUri(context, uri, active, records)
            onResult(res)
        }
    }

    fun exportBackup(context: android.content.Context, uri: Uri, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val res = backupManager.exportBackup(context, uri)
            onResult(res)
        }
    }

    fun restoreBackup(context: android.content.Context, uri: Uri, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val res = backupManager.restoreBackup(context, uri)
            loadNextSerialNumber()
            onResult(res)
        }
    }

    fun getAutoBackups(context: android.content.Context, onResult: (List<java.io.File>) -> Unit) {
        viewModelScope.launch {
            val list = backupManager.getAutoBackupList(context)
            onResult(list)
        }
    }

    fun restoreAutoBackup(file: java.io.File, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val res = backupManager.restoreFromLocalFile(file)
            loadNextSerialNumber()
            onResult(res)
        }
    }

    fun triggerManualWeeklySnapshot(context: android.content.Context, onResult: (Result<java.io.File?>) -> Unit) {
        viewModelScope.launch {
            val res = backupManager.performWeeklyAutoBackup(context)
            onResult(res)
        }
    }
}
