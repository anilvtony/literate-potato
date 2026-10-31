package com.policyregistration.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.policyregistration.app.data.local.CampaignEntity
import com.policyregistration.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignSettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val campaignState by viewModel.activeCampaign.collectAsState()

    var company by remember(campaignState) {
        mutableStateOf(campaignState?.insuranceCompany?.ifBlank { null } ?: "UNITED INDIA INSURANCE COMPANY LIMITED, Tirupathi")
    }
    var district by remember(campaignState) {
        mutableStateOf(campaignState?.district?.ifBlank { null } ?: "CHITTOOR(Dist)")
    }
    var meetingPlace by remember(campaignState) {
        mutableStateOf(campaignState?.meetingPlace?.ifBlank { null } ?: "Duddeda")
    }
    var campaignDate by remember(campaignState) {
        mutableStateOf(campaignState?.campaignDate?.ifBlank { null } ?: "14-10-2026")
    }
    var certificateNo by remember(campaignState) {
        mutableStateOf(campaignState?.certificateNo?.ifBlank { null } ?: "051200/47")
    }
    var startingSerialNo by remember(campaignState) {
        mutableStateOf((campaignState?.startingSerialNo ?: 10119L).toString())
    }
    var serialRange by remember(campaignState) {
        mutableStateOf(campaignState?.serialNumberRange?.ifBlank { null } ?: "10119 to 10151")
    }
    var validFrom by remember(campaignState) {
        mutableStateOf(campaignState?.policyValidFrom?.ifBlank { null } ?: "15-11-2026")
    }
    var validTo by remember(campaignState) {
        mutableStateOf(campaignState?.policyValidTo?.ifBlank { null } ?: "14-11-2027")
    }
    var defaultAddress by remember(campaignState) {
        mutableStateOf(campaignState?.defaultVillageAddress ?: "")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Campaign Configuration") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = company,
                onValueChange = { company = it },
                label = { Text("Insurance Company Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = district,
                    onValueChange = { district = it },
                    label = { Text("District") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = meetingPlace,
                    onValueChange = { meetingPlace = it },
                    label = { Text("Meeting Place") },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = campaignDate,
                    onValueChange = { campaignDate = it },
                    label = { Text("Campaign Date") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = certificateNo,
                    onValueChange = { certificateNo = it },
                    label = { Text("Certificate Number") },
                    modifier = Modifier.weight(1.3f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = startingSerialNo,
                    onValueChange = { startingSerialNo = it },
                    label = { Text("Starting S.No") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = serialRange,
                    onValueChange = { serialRange = it },
                    label = { Text("Serial Range") },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = validFrom,
                    onValueChange = { validFrom = it },
                    label = { Text("Valid From") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = validTo,
                    onValueChange = { validTo = it },
                    label = { Text("Valid To") },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = defaultAddress,
                onValueChange = { defaultAddress = it },
                label = { Text("Default Village / Sticky Address") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    val updated = CampaignEntity(
                        id = campaignState?.id ?: 1L,
                        insuranceCompany = company.trim(),
                        district = district.trim(),
                        meetingPlace = meetingPlace.trim(),
                        campaignDate = campaignDate.trim(),
                        certificateNo = certificateNo.trim(),
                        startingSerialNo = startingSerialNo.toLongOrNull() ?: 1L,
                        serialNumberRange = serialRange.trim(),
                        policyValidFrom = validFrom.trim(),
                        policyValidTo = validTo.trim(),
                        defaultVillageAddress = defaultAddress.trim(),
                        isActive = true
                    )
                    viewModel.updateCampaignSettings(updated)
                    viewModel.loadNextSerialNumber()
                    Toast.makeText(context, "Campaign settings saved!", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SAVE CAMPAIGN CONFIGURATION", fontWeight = FontWeight.Bold)
            }
        }
    }
}

