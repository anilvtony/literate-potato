package com.policyregistration.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.policyregistration.app.ui.viewmodel.MainViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationFormScreen(
    viewModel: MainViewModel,
    onNavigateToScanner: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val formState by viewModel.formState.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(formState.saveSuccessMessage) {
        formState.saveSuccessMessage?.let { msg ->
            // Audio Chime + Haptic Vibration confirmation
            try {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
            } catch (_: Exception) {}

            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(formState.errorMessage) {
        formState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val isEditing = formState.editingRecordId != null
                    Column {
                        Text(if (isEditing) "Edit Record" else "New Registration", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            if (isEditing) "Editing S.No #${formState.sNo}" else "Token #${formState.sNo} (Auto-Assigned)",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
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
        containerColor = Color(0xFFF4F6F9),
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            viewModel.saveCurrentRegistration { _ -> }
                        },
                        enabled = !formState.isSaving && !formState.duplicateAadhaarWarning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF))
                    ) {
                        if (formState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            val isEditing = formState.editingRecordId != null
                            Icon(if (isEditing) Icons.Default.Check else Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                if (isEditing) "UPDATE RECORD (#${formState.sNo})" else "SAVE PERSON (#${formState.sNo})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // CARD 1: PRIMARY PERSON INFO
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FormSectionHeader(
                        number = "1",
                        title = "Person's Information",
                        subtitle = "Name, Aadhaar, and Mobile"
                    )

                    // Mason Name
                    OutlinedTextField(
                        value = formState.name,
                        onValueChange = { viewModel.onNameChanged(it) },
                        label = { Text("Full Name *") },
                        placeholder = { Text("Enter person's name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF2563EB)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Aadhaar Number
                    val isAadhaarError = formState.duplicateAadhaarWarning || (formState.aadhaarNo.isNotEmpty() && formState.aadhaarNo.length != 12)
                    OutlinedTextField(
                        value = formState.aadhaarNo,
                        onValueChange = { viewModel.onAadhaarChanged(it) },
                        label = { Text("Aadhaar Number (12 Digits) *") },
                        placeholder = { Text("e.g. 123456789012") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFF2563EB)) },
                        isError = isAadhaarError,
                        supportingText = {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                if (formState.duplicateAadhaarWarning) {
                                    Text("⚠️ Already registered earlier!", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                } else if (formState.aadhaarNo.isNotEmpty() && formState.aadhaarNo.length < 12) {
                                    Text("Enter 12 digits (${12 - formState.aadhaarNo.length} more)", color = Color(0xFFD97706))
                                } else if (formState.aadhaarNo.length == 12) {
                                    Text("✓ 12 Digits Completed", color = Color(0xFF059669), fontWeight = FontWeight.SemiBold)
                                } else {
                                    Text("Required for policy")
                                }
                                Text("${formState.aadhaarNo.length}/12", fontWeight = FontWeight.Bold)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Phone Number
                    OutlinedTextField(
                        value = formState.phoneNumber,
                        onValueChange = { viewModel.onPhoneChanged(it) },
                        label = { Text("Mobile Number (10 Digits) *") },
                        placeholder = { Text("e.g. 9876543210") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF2563EB)) },
                        isError = formState.duplicatePhoneWarning,
                        supportingText = {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                if (formState.duplicatePhoneWarning) {
                                    Text("⚠️ Phone number already used by another person", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                                } else if (formState.phoneNumber.length == 10) {
                                    Text("✓ Valid 10 digits", color = Color(0xFF059669))
                                } else {
                                    Text("10 digit mobile number")
                                }
                                Text("${formState.phoneNumber.length}/10")
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Gender Selector
                    Text("Gender", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Male", "Female", "Other").forEach { g ->
                            val isSelected = formState.gender.equals(g, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onGenderChanged(g) },
                                label = { Text(g, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFDBEAFE),
                                    selectedLabelColor = Color(0xFF1E40AF)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Date of Birth & Age (Auto-calculated, read-only)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = formState.dob,
                            onValueChange = { viewModel.onDobChanged(it) },
                            label = { Text("Date of Birth") },
                            placeholder = { Text("DD.MM.YYYY") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    val cal = Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val formatted = String.format("%02d.%02d.%04d", d, m + 1, y)
                                            viewModel.onDobChanged(formatted)
                                        },
                                        cal.get(Calendar.YEAR) - 30,
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date", tint = Color(0xFF2563EB))
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier.weight(1.5f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Read-only Age field: cannot be typed or touched directly, computed strictly from DOB
                        OutlinedTextField(
                            value = if (formState.age.isNotBlank()) "${formState.age} yrs" else "--",
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Age (Auto)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color(0xFF1E293B),
                                disabledBorderColor = Color(0xFFCBD5E1),
                                disabledLabelColor = Color(0xFF64748B),
                                disabledContainerColor = Color(0xFFF1F5F9)
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // CARD 2: NOMINEE (FAMILY MEMBER)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FormSectionHeader(
                        number = "2",
                        title = "Nominee Details",
                        subtitle = "Family member for insurance claim"
                    )

                    OutlinedTextField(
                        value = formState.nominee,
                        onValueChange = { viewModel.onNomineeChanged(it) },
                        label = { Text("Nominee Full Name") },
                        placeholder = { Text("e.g. Spouse / Parent name") },
                        leadingIcon = { Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF059669)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Text("Relation with Person", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                    val relations = listOf("Wife", "Husband", "Mother", "Father", "Son", "Daughter", "Brother", "Sister")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        relations.forEach { r ->
                            val isSelected = formState.relation.equals(r, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onRelationSelected(r) },
                                label = { Text(r, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFD1FAE5),
                                    selectedLabelColor = Color(0xFF065F46)
                                )
                            )
                        }
                    }
                }
            }

            // CARD 3: ADDRESS
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FormSectionHeader(
                        number = "3",
                        title = "Address",
                        subtitle = "Village, town, or city"
                    )

                    OutlinedTextField(
                        value = formState.address,
                        onValueChange = { viewModel.onAddressChanged(it) },
                        label = { Text("Address / Village") },
                        placeholder = { Text("Enter address") },
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF7C3AED)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Optional toggle for repeat entries at the same village camp
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Keep address for next person",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B),
                                    maxLines = 1
                                )
                                Text(
                                    "Avoid typing the same village again",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1
                                )
                            }
                            Switch(
                                checked = formState.isStickyAddressEnabled,
                                onCheckedChange = { viewModel.toggleStickyAddress(it) }
                            )
                        }
                    }
                }
            }

            // CARD 4: SIMPLE QUESTIONS
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
                    FormSectionHeader(
                        number = "4",
                        title = "Additional Information",
                        subtitle = "Policy check questions"
                    )

                    // Accident Previous
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                "Previous major accident?",
                                fontSize = 13.sp,
                                color = Color(0xFF374151),
                                maxLines = 1
                            )
                            Text("Default: No", fontSize = 11.sp, color = Color(0xFF9CA3AF), maxLines = 1)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = formState.accidentPrevious == "No",
                                onClick = { viewModel.onAccidentPreviousChanged("No") },
                                label = { Text("No") }
                            )
                            FilterChip(
                                selected = formState.accidentPrevious == "Yes",
                                onClick = { viewModel.onAccidentPreviousChanged("Yes") },
                                label = { Text("Yes") }
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // Existing Policy
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                "Has existing insurance?",
                                fontSize = 13.sp,
                                color = Color(0xFF374151),
                                maxLines = 1
                            )
                            Text("Default: No", fontSize = 11.sp, color = Color(0xFF9CA3AF), maxLines = 1)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = formState.existingPolicy == "No",
                                onClick = { viewModel.onExistingPolicyChanged("No") },
                                label = { Text("No") }
                            )
                            FilterChip(
                                selected = formState.existingPolicy == "Yes",
                                onClick = { viewModel.onExistingPolicyChanged("Yes") },
                                label = { Text("Yes") }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FormSectionHeader(
    number: String,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E40AF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )
            Text(
                subtitle,
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
        }
    }
}
