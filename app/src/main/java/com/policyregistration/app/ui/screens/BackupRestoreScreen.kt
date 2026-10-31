package com.policyregistration.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.policyregistration.app.ui.viewmodel.MainViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var isOperating by remember { mutableStateOf(false) }
    var autoBackupList by remember { mutableStateOf<List<File>>(emptyList()) }
    var fileToRestore by remember { mutableStateOf<File?>(null) }

    fun refreshAutoBackups() {
        viewModel.getAutoBackups(context) { list ->
            autoBackupList = list
        }
    }

    LaunchedEffect(Unit) {
        refreshAutoBackups()
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { destUri ->
            isOperating = true
            viewModel.exportBackup(context, destUri) { result ->
                isOperating = false
                result.onSuccess { count ->
                    Toast.makeText(context, "Backup complete! Saved $count registrations.", Toast.LENGTH_LONG).show()
                }.onFailure { err ->
                    Toast.makeText(context, "Backup failed: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { srcUri ->
            isOperating = true
            viewModel.restoreBackup(context, srcUri) { result ->
                isOperating = false
                result.onSuccess { count ->
                    Toast.makeText(context, "Restore complete! Imported $count registrations.", Toast.LENGTH_LONG).show()
                    refreshAutoBackups()
                }.onFailure { err ->
                    Toast.makeText(context, "Restore failed: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    if (fileToRestore != null) {
        AlertDialog(
            onDismissRequest = { fileToRestore = null },
            title = { Text("Restore Local Backup?") },
            text = { Text("Do you want to restore records from snapshot '${fileToRestore?.name}'? Any missing records will be safely restored without creating duplicates.") },
            confirmButton = {
                TextButton(onClick = {
                    val file = fileToRestore
                    fileToRestore = null
                    if (file != null) {
                        isOperating = true
                        viewModel.restoreAutoBackup(file) { result ->
                            isOperating = false
                            result.onSuccess { count ->
                                Toast.makeText(context, "Restored $count records successfully!", Toast.LENGTH_LONG).show()
                            }.onFailure { err ->
                                Toast.makeText(context, "Restore failed: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }) {
                    Text("Restore", fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToRestore = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Automatic Local Backup", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Safety & Database Recovery", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E40AF),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF4F6F9)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Weekly Auto Backup Active Notice
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF059669)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Weekly Auto-Backup is Active", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF065F46))
                        Text(
                            "A secure local snapshot of the database is automatically saved once every week. Keeps up to 8 weekly snapshots (2 months of full history).",
                            fontSize = 12.sp,
                            color = Color(0xFF047857),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Recent Weekly Snapshots Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Weekly Stored Snapshots", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF111827))
                        Text("${autoBackupList.size} of 8 max", fontSize = 12.sp, color = Color(0xFF6B7280))
                    }

                    if (autoBackupList.isEmpty()) {
                        Text(
                            "No weekly snapshot created yet. It will automatically create on registration or you can trigger a backup below.",
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280)
                        )
                    } else {
                        val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        autoBackupList.forEach { file ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { fileToRestore = file },
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "Week of ${dateFormatter.format(Date(file.lastModified()))}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF1E293B)
                                        )
                                        Text(
                                            "${file.length() / 1024} KB • Tap to restore",
                                            fontSize = 11.sp,
                                            color = Color(0xFF2563EB)
                                        )
                                    }
                                    Icon(
                                        Icons.Default.Restore,
                                        contentDescription = "Restore",
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text("MANUAL ACTIONS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4B5563))

            // Manual Export File Button
            Button(
                onClick = {
                    backupLauncher.launch("policy_db_backup_${System.currentTimeMillis()}.json")
                },
                enabled = !isOperating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF))
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text("EXPORT BACKUP FILE TO DOWNLOADS", fontWeight = FontWeight.Bold)
            }

            // Manual Import File Button
            OutlinedButton(
                onClick = {
                    restoreLauncher.launch(arrayOf("application/json", "*/*"))
                },
                enabled = !isOperating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF1E40AF))
                Spacer(modifier = Modifier.width(10.dp))
                Text("IMPORT BACKUP FILE FROM STORAGE", fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
            }

            if (isOperating) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
