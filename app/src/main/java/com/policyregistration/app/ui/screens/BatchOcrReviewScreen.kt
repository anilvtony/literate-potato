package com.policyregistration.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.policyregistration.app.domain.ExtractedRegistrationFields
import com.policyregistration.app.ui.viewmodel.BatchItem
import com.policyregistration.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchOcrReviewScreen(
    viewModel: MainViewModel,
    onNavigateToRecords: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val batchState by viewModel.batchOcrState.collectAsState()

    var editingItem by remember { mutableStateOf<BatchItem?>(null) }

    // Dialog for quickly editing a batch item before saving
    if (editingItem != null) {
        val item = editingItem!!
        val fields = item.extractedFields ?: ExtractedRegistrationFields("")
        var aadhaar by remember(item) { mutableStateOf(fields.aadhaarNo) }
        var name by remember(item) { mutableStateOf(fields.name) }
        var phone by remember(item) { mutableStateOf(fields.phoneNumber) }
        var address by remember(item) { mutableStateOf(fields.address) }
        var dob by remember(item) { mutableStateOf(fields.dob) }
        var age by remember(item) { mutableStateOf(fields.age) }
        var nominee by remember(item) { mutableStateOf(fields.nominee) }
        var relation by remember(item) { mutableStateOf(fields.relation) }

        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("Edit Record Data", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = aadhaar,
                        onValueChange = { aadhaar = it.filter { c -> c.isDigit() }.take(12) },
                        label = { Text("Aadhaar (12 digits)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Mason Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.filter { c -> c.isDigit() }.take(10) },
                        label = { Text("Phone (10 digits)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dob,
                            onValueChange = { dob = it },
                            label = { Text("DOB (DD.MM.YYYY)") },
                            modifier = Modifier.weight(1.5f)
                        )
                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Age") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = nominee,
                            onValueChange = { nominee = it },
                            label = { Text("Nominee") },
                            modifier = Modifier.weight(1.5f)
                        )
                        OutlinedTextField(
                            value = relation,
                            onValueChange = { relation = it },
                            label = { Text("Relation") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val updated = fields.copy(
                        aadhaarNo = aadhaar,
                        name = name,
                        phoneNumber = phone,
                        address = address,
                        dob = dob,
                        age = age,
                        nominee = nominee,
                        relation = relation
                    )
                    viewModel.updateBatchItemField(item.id, updated)
                    editingItem = null
                }) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Batch Scan Review (${batchState.items.size} Forms)") },
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
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val selectedCount = batchState.items.count { it.isSelectedForSave && !it.isSaved }
                    Text(
                        "$selectedCount selected to save",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Button(
                        onClick = {
                            viewModel.saveAllSelectedBatch { saved ->
                                Toast.makeText(context, "Successfully created $saved registration records!", Toast.LENGTH_LONG).show()
                                onNavigateToRecords()
                            }
                        },
                        enabled = !batchState.isSavingBatch && selectedCount > 0,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (batchState.isSavingBatch) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SAVE ALL RECORDS")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            if (batchState.isProcessing) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            "Recognizing Images: ${batchState.progressCurrent} / ${batchState.progressTotal}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (batchState.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No documents processed. Please select images.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(batchState.items, key = { it.id }) { item ->
                        val f = item.extractedFields
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.isSaved) {
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                } else if (item.isDuplicateAadhaar) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = item.isSelectedForSave && !item.isSaved,
                                        onCheckedChange = { viewModel.toggleBatchItemSelection(item.id) },
                                        enabled = !item.isSaved && !item.isDuplicateAadhaar
                                    )

                                    item.bitmap?.let { bmp ->
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            f?.name?.ifBlank { "Unidentified Name" } ?: "Processing...",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            "Aadhaar: ${f?.aadhaarNo?.ifBlank { "Missing" }}",
                                            color = if (f?.aadhaarNo?.length == 12) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                        Text("Phone: ${f?.phoneNumber?.ifBlank { "Missing" }}  •  DOB: ${f?.dob ?: "-"}")
                                    }

                                    IconButton(onClick = { editingItem = item }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Fields")
                                    }
                                }

                                if (item.isDuplicateAadhaar) {
                                    Text(
                                        "Warning: This Aadhaar is already registered in database",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (item.isSaved) {
                                    Text(
                                        "✓ Saved into database",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

