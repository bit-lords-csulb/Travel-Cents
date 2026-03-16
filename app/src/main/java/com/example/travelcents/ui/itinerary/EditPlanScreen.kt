package com.example.travelcents.ui.itinerary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlanScreen(
    eventId: String? = null,
    tripId: String? = null,
    onBackClick: () -> Unit,
    viewModel: EditPlanViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // --- PICKER STATES ---
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(eventId, tripId) {
        viewModel.loadEventDetails(eventId, tripId)
    }

    // --- DELETE DIALOG ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Event?") },
            text = { Text("Are you sure you want to remove this from your itinerary? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteEvent(eventId, tripId) {
                        showDeleteDialog = false
                        onBackClick()
                    }
                }) { Text("DELETE", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("CANCEL") }
            }
        )
    }

    // --- NATIVE DATE PICKER ---
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        formatter.timeZone = TimeZone.getTimeZone("UTC")
                        viewModel.date = formatter.format(Date(millis))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("CANCEL") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // --- NATIVE START TIME PICKER ---
    if (showStartTimePicker) {
        val parts = viewModel.time.split(":")
        val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 12
        val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val startTimeState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute
        )
        TimePickerDialog(
            onCancel = { showStartTimePicker = false },
            onConfirm = {
                val h = startTimeState.hour.toString().padStart(2, '0')
                val m = startTimeState.minute.toString().padStart(2, '0')
                viewModel.time = "$h:$m"
                showStartTimePicker = false
            }
        ) {
            TimeInput(state = startTimeState)
        }
    }

    // --- NATIVE END TIME PICKER ---
    if (showEndTimePicker) {
        val parts = viewModel.endTime.split(":")
        val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 12
        val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val endTimeState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute
        )
        TimePickerDialog(
            onCancel = { showEndTimePicker = false },
            onConfirm = {
                val h = endTimeState.hour.toString().padStart(2, '0')
                val m = endTimeState.minute.toString().padStart(2, '0')
                viewModel.endTime = "$h:$m"
                showEndTimePicker = false
            }
        ) {
            TimeInput(state = endTimeState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1B2A))
            .padding(24.dp)
            .verticalScroll(scrollState)
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))

                val headerText = if (eventId == "new") "NEW EVENT" else "EDIT PLAN"
                Text(headerText, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (eventId != "new") {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Event", tint = Color(0xFFEF4444))
                    }
                }
                IconButton(
                    onClick = {
                        viewModel.updateEvent(eventId, tripId) { onBackClick() }
                    }
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Save Event", tint = Color(0xFF3A86FF))
                }
            }
        }

        // --- DYNAMIC FORM FIELDS ---

        // 1. EVENT TYPE DROPDOWN
        val eventTypes = listOf("Flight", "Hotel", "Restaurant", "Activity")
        val displayType = viewModel.type.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }.ifEmpty { "Activity" }

        DropdownEditField(
            label = "Event Type",
            options = eventTypes,
            selectedValue = displayType,
            onValueChange = { selected -> viewModel.type = selected.lowercase() }
        )

        // 2. MAIN TITLE
        val titleLabel = when (viewModel.type.lowercase()) {
            "flight" -> "Flight Name"
            "hotel" -> "Hotel Name"
            "restaurant" -> "Restaurant Name"
            else -> "Title"
        }
        CustomEditField(label = titleLabel, value = viewModel.title, onValueChange = { viewModel.title = it })

        // 3. DATE, START TIME, END TIME
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ClickableEditField(
                label = "Date",
                value = viewModel.date,
                onClick = { showDatePicker = true },
                modifier = Modifier.weight(1f),
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.Gray) }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val startLabel = if (viewModel.type.lowercase() == "hotel") "Check-in" else "Start Time"
            val endLabel = if (viewModel.type.lowercase() == "hotel") "Check-out" else "End Time"

            ClickableEditField(
                label = startLabel,
                value = viewModel.time,
                onClick = { showStartTimePicker = true },
                modifier = Modifier.weight(1f),
                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Gray) }
            )
            ClickableEditField(
                label = endLabel,
                value = viewModel.endTime,
                onClick = { showEndTimePicker = true },
                modifier = Modifier.weight(1f),
                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Gray) }
            )
        }

        // 4. TYPE-SPECIFIC FIELDS
        when (viewModel.type.lowercase()) {
            "flight" -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CustomEditField(label = "Airline", value = viewModel.airline, onValueChange = { viewModel.airline = it }, modifier = Modifier.weight(1f))
                    CustomEditField(label = "Flight #", value = viewModel.flightNumber, onValueChange = { viewModel.flightNumber = it }, modifier = Modifier.weight(1f))
                }
                CustomEditField(label = "Destination Airport", value = viewModel.location, onValueChange = { viewModel.location = it }, leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray) })
            }
            "restaurant" -> {
                CustomEditField(label = "Cuisine", value = viewModel.cuisine, onValueChange = { viewModel.cuisine = it })
                CustomEditField(label = "Location / Address", value = viewModel.location, onValueChange = { viewModel.location = it }, leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray) })
            }
            else -> {
                CustomEditField(label = "Location / Address", value = viewModel.location, onValueChange = { viewModel.location = it }, leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray) })
            }
        }

        // 5. NOTES
        CustomEditField(label = "Notes", value = viewModel.notes, onValueChange = { viewModel.notes = it }, singleLine = false, modifier = Modifier.height(120.dp), leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = Color.Gray) })

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// --- HELPER COMPONENTS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownEditField(
    label: String,
    options: List<String>,
    selectedValue: String,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()) {
        Text(text = label.uppercase(), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            TextField(
                value = selectedValue,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E293B), unfocusedContainerColor = Color(0xFF1E293B),
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF1E293B))
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = Color.White) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomEditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true
) {
    Column(modifier = modifier.padding(bottom = 16.dp)) {
        Text(text = label.uppercase(), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            leadingIcon = leadingIcon,
            singleLine = singleLine,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1E293B), unfocusedContainerColor = Color(0xFF1E293B),
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun ClickableEditField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier.padding(bottom = 16.dp)) {
        Text(text = label.uppercase(), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        Box(modifier = Modifier.clickable { onClick() }) {
            TextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                leadingIcon = leadingIcon,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    disabledContainerColor = Color(0xFF1E293B),
                    disabledTextColor = Color.White,
                    disabledIndicatorColor = Color.Transparent,
                    disabledLeadingIconColor = Color.Gray
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            Box(modifier = Modifier.matchParentSize().clickable { onClick() })
        }
    }
}

@Composable
fun TimePickerDialog(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        dismissButton = { TextButton(onClick = onCancel) { Text("CANCEL") } },
        confirmButton = { TextButton(onClick = onConfirm) { Text("OK") } },
        text = { content() }
    )
}