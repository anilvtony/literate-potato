package com.policyregistration.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.policyregistration.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrReviewScreen(
    viewModel: MainViewModel,
    onAcceptAndContinue: () -> Unit,
    onRetake: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val pendingFields by viewModel.ocrPendingFields.collectAsState()
    val scannedImage by viewModel.currentScannedImage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OCR Extraction Review") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onRetake) {
                        Text("Retake", color = MaterialTheme.colorScheme.onPrimary)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Document Image Thumbnail Preview
            scannedImage?.let { bmp ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Scanned Form",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Review Notice
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Review extracted values below. Tapping 'Apply & Edit' will populate the registration form for operator confirmation.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Summary of Identified Fields
            pendingFields?.let { fields ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Extracted Candidate Fields", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        HorizontalDivider()

                        OcrFieldRow("Aadhaar Number", fields.aadhaarNo.ifBlank { "Not detected" }, fields.aadhaarNo.length == 12)
                        OcrFieldRow("Mason Name", fields.name.ifBlank { "Not detected" }, fields.name.isNotBlank())
                        OcrFieldRow("Address", fields.address.ifBlank { "Not detected" }, fields.address.isNotBlank())
                        OcrFieldRow("Age / DOB", "${fields.age.ifBlank { "-" }} / ${fields.dob.ifBlank { "-" }}", fields.dob.isNotBlank())
                        OcrFieldRow("Nominee", fields.nominee.ifBlank { "Not detected" }, fields.nominee.isNotBlank())
                        OcrFieldRow("Relation", fields.relation.ifBlank { "Not detected" }, fields.relation.isNotBlank())
                        OcrFieldRow("Phone Number", fields.phoneNumber.ifBlank { "Not detected" }, fields.phoneNumber.length == 10)
                        OcrFieldRow("Gender", fields.gender, true)
                        OcrFieldRow("Previous Accident", fields.accidentPrevious, true)
                        OcrFieldRow("Existing Policy", fields.existingPolicy, true)
                    }
                }

                // Confidence Warnings
                if (fields.confidenceNotes.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Attention Needed:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            fields.confidenceNotes.forEach { note ->
                                Text("• $note", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }

                // Action Buttons
                Button(
                    onClick = {
                        viewModel.applyOcrFieldsToForm(fields, fromGallery = false)
                        onAcceptAndContinue()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("APPLY TO FORM & CONFIRM", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                OutlinedButton(
                    onClick = onRetake,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retake Photo")
                }
            } ?: run {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun OcrFieldRow(label: String, value: String, isOk: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}

