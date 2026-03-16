package com.example.travelcents.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun EditPlanScreen(
    eventId: String? = null, // <--- Add this line!
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // The dark navy from your mockup
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // 1. The Custom Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Arrow
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint = Color(0xFF64748B)
                )
            }

            // Centered Title
            Text(
                text = "EDIT PLAN",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp // Gives it that spaced-out, premium look
            )

            // Trash Can Icon
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete Plan",
                    tint = Color(0xFFEF4444) // A soft red
                )
            }
        }

        // We will build the custom text fields right here next!
        // Temporary state variables (we will connect these to Firebase later)
        var titleText by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("Flight to Paris") }
        var dateText by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("Feb 10") }
        var timeText by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("08:00 AM") }
        var locationText by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("LAX Terminal 4") }
        var notesText by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("Details here...") }


        // --- TITLE ---
        CustomEditField(
            label = "Title",
            value = titleText,
            onValueChange = { titleText = it }
        )


        // --- DATE & TIME (Side by Side) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CustomEditField(
                label = "Date",
                value = dateText,
                onValueChange = { dateText = it },
                modifier = Modifier.weight(1f),
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "Date") }
            )
            CustomEditField(
                label = "Time",
                value = timeText,
                onValueChange = { timeText = it },
                modifier = Modifier.weight(1f),
                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = "Time") }
            )
            CustomEditField(
                label = "Location",
                value = locationText,
                onValueChange = { locationText = it },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = "Location") }
            )

            // --- NOTES ---
            CustomEditField(
                label = "Notes",
                value = notesText,
                onValueChange = { notesText = it },
                singleLine = false, // Allows multiple lines of text
                modifier = Modifier.height(120.dp), // Makes the box taller
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = "Notes") }
            )

            // --- UPDATE BUTTON ---
            Spacer(modifier = Modifier.weight(1f)) // This physically pushes the button to the bottom of the screen

            Button(
                onClick = { /* TODO: We will wire this to Firebase later */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text("UPDATE PLAN")
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
        Text(
            text = label.uppercase(),
            color = Color.Gray,
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        TextField(
            value = value,
            onValueChange = onValueChange,
            leadingIcon = leadingIcon,
            singleLine = singleLine,
            modifier = Modifier.fillMaxWidth()
            // Colors removed completely for now!
        )
    }
}