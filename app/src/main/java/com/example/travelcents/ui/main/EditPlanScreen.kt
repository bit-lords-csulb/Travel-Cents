package com.example.travelcents.ui.itinerary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditPlanScreen(
    eventId: String? = null,
    tripId: String? = null,
    onBackClick: () -> Unit,
    viewModel: EditPlanViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    // 1. Fetch data using BOTH IDs when screen opens
    LaunchedEffect(eventId, tripId) {
        viewModel.loadEventDetails(eventId, tripId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1B2A))
            .padding(24.dp)
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "EDIT PLAN",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        // --- TITLE ---
        CustomEditField(
            label = "Title",
            value = viewModel.title,
            onValueChange = { viewModel.title = it }
        )

        // --- DATE & TIME ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CustomEditField(
                label = "Date",
                value = viewModel.date,
                onValueChange = { viewModel.date = it },
                modifier = Modifier.weight(1f),
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.Gray) }
            )
            CustomEditField(
                label = "Time",
                value = viewModel.time,
                onValueChange = { viewModel.time = it },
                modifier = Modifier.weight(1f),
                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Gray) }
            )
        }

        // --- LOCATION ---
        CustomEditField(
            label = "Location",
            value = viewModel.location,
            onValueChange = { viewModel.location = it },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray) }
        )

        // --- NOTES ---
        CustomEditField(
            label = "Notes",
            value = viewModel.notes,
            onValueChange = { viewModel.notes = it },
            singleLine = false,
            modifier = Modifier.height(120.dp),
            leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = Color.Gray) }
        )

        Spacer(modifier = Modifier.weight(1f))

        // --- UPDATE BUTTON ---
        Button(
            onClick = {
                viewModel.updateEvent(eventId, tripId) {
                    onBackClick()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A86FF)),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Text("UPDATE PLAN", fontWeight = FontWeight.Bold, color = Color.White)
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
        Text(
            text = label.uppercase(),
            color = Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            leadingIcon = leadingIcon,
            singleLine = singleLine,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        )
    }
}