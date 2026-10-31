package com.policyregistration.app.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.policyregistration.app.ui.viewmodel.MainViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ExcelExportScope {
    TODAY_ONLY,
    DATE_RANGE,
    ALL_RECORDS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportExcelScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val campaign by viewModel.activeCampaign.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val todayCount by viewModel.todayCount.collectAsState()

    var isExporting by remember { mutableStateOf(false) }
    var exportScope by remember { mutableStateOf(ExcelExportScope.TODAY_ONLY) }

    val todayStartMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val todayEndMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    var customFromMillis by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -7)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    }

    var customToMillis by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        )
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        uri?.let { destUri ->
            isExporting = true
            val (startTime, endTime) = when (exportScope) {
                ExcelExportScope.TODAY_ONLY -> Pair(todayStartMillis, todayEndMillis)
                ExcelExportScope.DATE_RANGE -> Pair(customFromMillis, customToMillis)
                ExcelExportScope.ALL_RECORDS -> Pair(null, null)
            }

            viewModel.exportExcel(context, destUri, startTime, endTime) { result ->
                isExporting = false
                result.onSuccess { count ->
                    Toast.makeText(context, "Successfully exported $count records to Excel!", Toast.LENGTH_LONG).show()
                }.onFailure { err ->
                    Toast.makeText(context, "Export failed: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Download Excel File", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Official Policy Format (.xlsx)", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
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
            // STEP 1: SELECT WHAT TO EXPORT
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Which records do you want to download?",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )

                    // Option 1: Today Only
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { exportScope = ExcelExportScope.TODAY_ONLY },
                        color = if (exportScope == ExcelExportScope.TODAY_ONLY) Color(0xFFEFF6FF) else Color(0xFFF9FAFB),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = exportScope == ExcelExportScope.TODAY_ONLY,
                                onClick = { exportScope = ExcelExportScope.TODAY_ONLY }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Today's Records Only", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("$todayCount new registrations done today", fontSize = 12.sp, color = Color(0xFF6B7280))
                            }
                        }
                    }

                    // Option 2: Date Range
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { exportScope = ExcelExportScope.DATE_RANGE },
                        color = if (exportScope == ExcelExportScope.DATE_RANGE) Color(0xFFEFF6FF) else Color(0xFFF9FAFB),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = exportScope == ExcelExportScope.DATE_RANGE,
                                onClick = { exportScope = ExcelExportScope.DATE_RANGE }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Custom Date Range", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Choose From Date and To Date", fontSize = 12.sp, color = Color(0xFF6B7280))
                            }
                        }
                    }

                    // If Date Range is selected, show Date Pickers
                    if (exportScope == ExcelExportScope.DATE_RANGE) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance().apply { timeInMillis = customFromMillis }
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val newCal = Calendar.getInstance().apply {
                                                set(y, m, d, 0, 0, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }
                                            customFromMillis = newCal.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text("From", fontSize = 10.sp, color = Color(0xFF6B7280))
                                    Text(
                                        dateFormatter.format(Date(customFromMillis)),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E40AF)
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance().apply { timeInMillis = customToMillis }
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val newCal = Calendar.getInstance().apply {
                                                set(y, m, d, 23, 59, 59)
                                                set(Calendar.MILLISECOND, 999)
                                            }
                                            customToMillis = newCal.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text("To", fontSize = 10.sp, color = Color(0xFF6B7280))
                                    Text(
                                        dateFormatter.format(Date(customToMillis)),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E40AF)
                                    )
                                }
                            }
                        }
                    }

                    // Option 3: All Time
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { exportScope = ExcelExportScope.ALL_RECORDS },
                        color = if (exportScope == ExcelExportScope.ALL_RECORDS) Color(0xFFEFF6FF) else Color(0xFFF9FAFB),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = exportScope == ExcelExportScope.ALL_RECORDS,
                                onClick = { exportScope = ExcelExportScope.ALL_RECORDS }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("All Records Ever Saved", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Total $totalCount records in database", fontSize = 12.sp, color = Color(0xFF6B7280))
                            }
                        }
                    }
                }
            }

            // EXPORT BUTTONS ROW: DIRECT SHARE VIA WHATSAPP & SAVE TO STORAGE
            Button(
                onClick = {
                    val (startTime, endTime) = when (exportScope) {
                        ExcelExportScope.TODAY_ONLY -> Pair(todayStartMillis, todayEndMillis)
                        ExcelExportScope.DATE_RANGE -> Pair(customFromMillis, customToMillis)
                        ExcelExportScope.ALL_RECORDS -> Pair(null, null)
                    }

                    isExporting = true
                    val timeTag = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                    val shareFile = File(context.cacheDir, "Policy_Report_$timeTag.xlsx")
                    val shareUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        shareFile
                    )

                    viewModel.exportExcel(context, shareUri, startTime, endTime) { result ->
                        isExporting = false
                        result.onSuccess { count ->
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                putExtra(Intent.EXTRA_STREAM, shareUri)
                                putExtra(Intent.EXTRA_SUBJECT, "Policy Registration Report ($count records)")
                                putExtra(Intent.EXTRA_TEXT, "Attached is the official registration report ($count records).")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Report via..."))
                        }.onFailure { err ->
                            Toast.makeText(context, "Export error: ${err.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = !isExporting && totalCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)) // WhatsApp brand green
            ) {
                if (isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("SHARE VIA WHATSAPP / APPS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            OutlinedButton(
                onClick = {
                    val timeTag = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val defaultFileName = when (exportScope) {
                        ExcelExportScope.TODAY_ONLY -> "Today_Registrations_$timeTag.xlsx"
                        ExcelExportScope.DATE_RANGE -> "Registrations_Range_$timeTag.xlsx"
                        ExcelExportScope.ALL_RECORDS -> "All_Registrations_$timeTag.xlsx"
                    }
                    createDocLauncher.launch(defaultFileName)
                },
                enabled = !isExporting && totalCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF1E40AF))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Save to Phone Storage", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E40AF))
            }
        }
    }
}
