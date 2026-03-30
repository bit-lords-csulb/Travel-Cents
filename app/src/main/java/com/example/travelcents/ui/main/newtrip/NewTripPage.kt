package com.example.travelcents.ui.main.newtrip

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewTripPage(
    modifier: Modifier = Modifier,
    viewModel: NewTripViewModel,
    onChatClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = DeepSea5,
        unfocusedTextColor = DeepSea5,
        cursorColor = DeepSea5,
        focusedBorderColor = DeepSea3,
        unfocusedBorderColor = DeepSea4,
        focusedLabelColor = DeepSea5,
        unfocusedLabelColor = DeepSea4
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSea1)
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Plan a New Trip",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = DeepSea5
        )

        // Origin
        OutlinedTextField(
            value = viewModel.origin,
            onValueChange = { viewModel.origin = it },
            label = { Text("Origin") },
            placeholder = { Text("e.g. Los Angeles, USA", color = DeepSea4.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = textFieldColors
        )

        // Destination
        OutlinedTextField(
            value = viewModel.destination,
            onValueChange = { viewModel.destination = it },
            label = { Text("Destination") },
            placeholder = { Text("e.g. Tokyo, Japan", color = DeepSea4.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = textFieldColors
        )

        // Date pickers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DateField(
                label = "From",
                value = viewModel.dateFrom,
                modifier = Modifier.weight(1f),
                onClick = {
                    showDatePicker(context) { viewModel.dateFrom = it }
                }
            )
            DateField(
                label = "To",
                value = viewModel.dateTo,
                modifier = Modifier.weight(1f),
                onClick = {
                    showDatePicker(context) { viewModel.dateTo = it }
                }
            )
        }

        // Travelers
        SectionLabel("Travelers")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            NumberStepper(
                label = "Adults",
                value = viewModel.adults,
                onMinus = { if (viewModel.adults > 1) viewModel.adults-- },
                onPlus = { viewModel.adults++ }
            )
            NumberStepper(
                label = "Children",
                value = viewModel.children,
                onMinus = { if (viewModel.children > 0) viewModel.children-- },
                onPlus = { viewModel.children++ }
            )
        }

        // Travel style
        SectionLabel("Travel Style")
        val styles = listOf("budget", "comfort", "luxury")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            styles.forEachIndexed { index, style ->
                SegmentedButton(
                    selected = viewModel.travelStyle == style,
                    onClick = { viewModel.travelStyle = style },
                    shape = SegmentedButtonDefaults.itemShape(index, styles.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = DeepSea3,
                        activeContentColor = DeepSea5,
                        inactiveContainerColor = DeepSea2,
                        inactiveContentColor = DeepSea4
                    )
                ) {
                    Text(style.replaceFirstChar { it.uppercase() })
                }
            }
        }

        // Budget + Currency
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = viewModel.budgetTotal,
                onValueChange = { viewModel.budgetTotal = it },
                label = { Text("Budget") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = textFieldColors
            )
            OutlinedTextField(
                value = viewModel.currency,
                onValueChange = { viewModel.currency = it.uppercase().take(3) },
                label = { Text("Currency") },
                modifier = Modifier.width(100.dp),
                singleLine = true,
                colors = textFieldColors
            )
        }

        // Dietary preferences
        SectionLabel("Dietary Preferences")
        val dietaryOptions = listOf("Vegetarian", "Vegan", "Halal", "Gluten-Free", "None")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            dietaryOptions.forEach { option ->
                val selected = option.lowercase() in viewModel.dietary
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.toggleDietary(option.lowercase()) },
                    label = { Text(option) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DeepSea3,
                        selectedLabelColor = DeepSea5,
                        containerColor = DeepSea2,
                        labelColor = DeepSea4
                    )
                )
            }
        }

        // Interests
        SectionLabel("Interests")
        val interestOptions = listOf("Culture", "Food", "Nature", "Adventure", "Nightlife", "Shopping", "History")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            interestOptions.forEach { option ->
                val selected = option.lowercase() in viewModel.interests
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.toggleInterest(option.lowercase()) },
                    label = { Text(option) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DeepSea3,
                        selectedLabelColor = DeepSea5,
                        containerColor = DeepSea2,
                        labelColor = DeepSea4
                    )
                )
            }
        }

        // Special requests
        OutlinedTextField(
            value = viewModel.specialRequests,
            onValueChange = { viewModel.specialRequests = it },
            label = { Text("Special Requests") },
            placeholder = { Text("e.g. Anniversary dinner on day 3", color = DeepSea4.copy(alpha = 0.5f)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            maxLines = 4,
            colors = textFieldColors
        )

        // State feedback
        when (val state = uiState) {
            is TripUiState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = DeepSea3)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(state.statusMessage, color = DeepSea4, fontSize = 14.sp)
                    }
                }
            }
            is TripUiState.Error -> {
                Text(
                    text = state.message,
                    color = Color(0xFFEF5350),
                    fontSize = 14.sp
                )
            }
            is TripUiState.Success -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepSea2, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Trip created: ${state.itinerary.tripName}",
                            color = Color(0xFF66BB6A),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${state.itinerary.origin} → ${state.itinerary.destination}",
                            color = DeepSea4,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${state.itinerary.dateFrom} to ${state.itinerary.dateTo} · ${state.events.size} events",
                            color = DeepSea4,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            is TripUiState.Idle -> {}
        }

        // Generate button
        Button(
            onClick = { viewModel.generateTrip() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = uiState !is TripUiState.Loading,
            colors = ButtonDefaults.buttonColors(
                containerColor = DeepSea3,
                contentColor = DeepSea5,
                disabledContainerColor = DeepSea3.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (uiState is TripUiState.Loading) "Generating..." else "Generate Trip",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // AI chat alternative
        OutlinedButton(
            onClick = onChatClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepSea4),
            border = androidx.compose.foundation.BorderStroke(1.dp, DeepSea3),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Plan with AI Chat instead",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = DeepSea5,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun DateField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        placeholder = { Text("YYYY-MM-DD", color = DeepSea4.copy(alpha = 0.5f)) },
        modifier = modifier.clickable { onClick() },
        readOnly = true,
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = onClick) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = "Pick date",
                    tint = DeepSea4,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = DeepSea5,
            unfocusedTextColor = DeepSea5,
            cursorColor = DeepSea5,
            focusedBorderColor = DeepSea3,
            unfocusedBorderColor = DeepSea4,
            focusedLabelColor = DeepSea5,
            unfocusedLabelColor = DeepSea4
        )
    )
}

@Composable
private fun NumberStepper(
    label: String,
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = DeepSea4, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMinus, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = DeepSea4)
            }
            Text(
                text = "$value",
                color = DeepSea5,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            IconButton(onClick = onPlus, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Increase", tint = DeepSea4)
            }
        }
    }
}

private fun showDatePicker(
    context: android.content.Context,
    onDateSelected: (String) -> Unit
) {
    val calendar = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, day ->
            val formatted = "%04d-%02d-%02d".format(year, month + 1, day)
            onDateSelected(formatted)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}