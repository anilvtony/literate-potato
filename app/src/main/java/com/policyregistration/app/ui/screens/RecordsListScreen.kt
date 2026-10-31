package com.policyregistration.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.policyregistration.app.data.local.SourceType
import com.policyregistration.app.data.repository.DecryptedRegistration
import com.policyregistration.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DateFilterMode {
    TODAY,
    CUSTOM_RANGE,
    ALL_TIME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsListScreen(
    viewModel: MainViewModel,
    onNavigateToEdit: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val registrations by viewModel.allRegistrations.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterMode by remember { mutableStateOf(DateFilterMode.TODAY) }
    var itemToDelete by remember { mutableStateOf<DecryptedRegistration?>(null) }

    // Start of Today 12:00:00 AM in millis
    val todayStartMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // Default custom range: 7 days ago to today end
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
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    // Filter registrations based on Selected Date Mode and Search Query
    val filteredList = remember(
        registrations,
        searchQuery,
        selectedFilterMode,
        todayStartMillis,
        customFromMillis,
        customToMillis
    ) {
        registrations.filter { item ->
            val ent = item.entity
            val recordTime = ent.createdAt

            // 1. Date Filter
            val matchesDate = when (selectedFilterMode) {
                DateFilterMode.TODAY -> recordTime >= todayStartMillis
                DateFilterMode.CUSTOM_RANGE -> recordTime in customFromMillis..customToMillis
                DateFilterMode.ALL_TIME -> true
            }

            // 2. Search Filter
            val query = searchQuery.trim()
            val matchesQuery = query.isBlank() ||
                    ent.sNo.toString().contains(query) ||
                    ent.nameOfTheMason.contains(query, ignoreCase = true) ||
                    ent.phoneNumber.contains(query) ||
                    item.aadhaarPlain.contains(query) ||
                    ent.aadhaarLast4.contains(query) ||
                    ent.nominee.contains(query, ignoreCase = true)

            matchesDate && matchesQuery
        }
    }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Person Record") },
            text = { Text("Are you sure you want to delete S.No #${itemToDelete?.entity?.sNo} (${itemToDelete?.entity?.nameOfTheMason})? Serial numbers will never be reused.") },
            confirmButton = {
                TextButton(onClick = {
                    itemToDelete?.let { viewModel.deleteRecord(it.entity.recordId) }
                    itemToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
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
                        Text("Registration Records", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        val subtitle = when (selectedFilterMode) {
                            DateFilterMode.TODAY -> "Showing Today's active records (${filteredList.size})"
                            DateFilterMode.CUSTOM_RANGE -> "Showing custom date range (${filteredList.size})"
                            DateFilterMode.ALL_TIME -> "Showing all recorded persons (${filteredList.size})"
                        }
                        Text(subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // SEARCH BOX
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, phone, Aadhaar, S.No...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF2563EB)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // FILTER MODE CHIPS: Today (Auto-resets at 12 AM) vs Date Range vs All
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // TODAY CHIP
                FilterChip(
                    selected = selectedFilterMode == DateFilterMode.TODAY,
                    onClick = { selectedFilterMode = DateFilterMode.TODAY },
                    label = { Text("Today (Active)") },
                    leadingIcon = {
                        Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFDBEAFE),
                        selectedLabelColor = Color(0xFF1E40AF)
                    )
                )

                // CUSTOM DATE RANGE CHIP
                FilterChip(
                    selected = selectedFilterMode == DateFilterMode.CUSTOM_RANGE,
                    onClick = { selectedFilterMode = DateFilterMode.CUSTOM_RANGE },
                    label = { Text("Date Range") },
                    leadingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFD1FAE5),
                        selectedLabelColor = Color(0xFF065F46)
                    )
                )

                // ALL TIME CHIP
                FilterChip(
                    selected = selectedFilterMode == DateFilterMode.ALL_TIME,
                    onClick = { selectedFilterMode = DateFilterMode.ALL_TIME },
                    label = { Text("All Time (${registrations.size})") },
                    leadingIcon = {
                        Icon(Icons.Default.AllInclusive, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFEDE9FE),
                        selectedLabelColor = Color(0xFF5B21B6)
                    )
                )
            }

            // DATE PICKER SELECTORS (Only visible when CUSTOM_RANGE is selected)
            if (selectedFilterMode == DateFilterMode.CUSTOM_RANGE) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Select Date Range to Search Records:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4B5563)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // FROM DATE BUTTON
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
                                    Text("From Date", fontSize = 10.sp, color = Color(0xFF6B7280))
                                    Text(
                                        dateFormatter.format(Date(customFromMillis)),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E40AF)
                                    )
                                }
                            }

                            // TO DATE BUTTON
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
                                    Text("To Date", fontSize = 10.sp, color = Color(0xFF6B7280))
                                    Text(
                                        dateFormatter.format(Date(customToMillis)),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E40AF)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // LIST OF RECORDS
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            when (selectedFilterMode) {
                                DateFilterMode.TODAY -> "No registrations done today yet.\n(Daily count resets at 12:00 AM)"
                                DateFilterMode.CUSTOM_RANGE -> "No records found between selected dates."
                                DateFilterMode.ALL_TIME -> "No registrations saved yet."
                            },
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280),
                            lineHeight = 20.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (selectedFilterMode == DateFilterMode.TODAY && registrations.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(onClick = { selectedFilterMode = DateFilterMode.ALL_TIME }) {
                                Text("View previous days (${registrations.size} total)")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.entity.recordId }) { item ->
                        val ent = item.entity
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Top row: Token badge + Name + Delete
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            color = Color(0xFF1E40AF),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                "#${ent.sNo}",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                ent.nameOfTheMason,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = Color(0xFF111827)
                                            )
                                            Text(
                                                "${dateFormatter.format(Date(ent.createdAt))} • ${timeFormatter.format(Date(ent.createdAt))}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF6B7280)
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                viewModel.startEditingRecord(item)
                                                onNavigateToEdit()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit Record",
                                                tint = Color(0xFF2563EB)
                                            )
                                        }

                                        IconButton(
                                            onClick = { itemToDelete = item },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Delete",
                                                tint = Color(0xFFDC2626)
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFF1F5F9))

                                // Details Grid
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            "Aadhaar: ",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF4B5563)
                                        )
                                        Text(
                                            item.aadhaarPlain,
                                            fontSize = 13.sp,
                                            color = Color(0xFF111827),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row {
                                            Text(
                                                "Mobile: ",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF4B5563)
                                            )
                                            Text(
                                                ent.phoneNumber,
                                                fontSize = 13.sp,
                                                color = Color(0xFF111827)
                                            )
                                        }

                                        Row {
                                            Text(
                                                "Age: ",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF4B5563)
                                            )
                                            Text(
                                                "${ent.age} yrs • ${ent.gender}",
                                                fontSize = 13.sp,
                                                color = Color(0xFF111827)
                                            )
                                        }
                                    }

                                    if (ent.nominee.isNotBlank()) {
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                "Nominee: ",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF4B5563)
                                            )
                                            Text(
                                                "${ent.nominee} (${ent.relation})",
                                                fontSize = 13.sp,
                                                color = Color(0xFF065F46),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    if (ent.address.isNotBlank()) {
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                "Address: ",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF4B5563)
                                            )
                                            Text(
                                                ent.address,
                                                fontSize = 13.sp,
                                                color = Color(0xFF4B5563),
                                                maxLines = 1
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
    }
}
