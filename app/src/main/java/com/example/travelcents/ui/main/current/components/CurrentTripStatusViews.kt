package com.example.travelcents.ui.main.current

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5

@Composable
fun CurrentTripMessageCard(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isError) Color(0xFF3B1722) else DeepSea2)
            .border(
                width = 1.dp,
                color = if (isError) Color(0xFF8C3951) else DeepSea3.copy(alpha = 0.65f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            color = if (isError) Color(0xFFFFB4C7) else DeepSea5,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(22.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss message",
                tint = if (isError) Color(0xFFFFB4C7) else DeepSea4,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun CurrentTripLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = DeepSea4)
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = "Loading your trip...",
            color = DeepSea5,
            fontSize = 14.sp
        )
    }
}

@Composable
fun CurrentTripEmptyState(
    title: String,
    body: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(DeepSea2),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.EditCalendar,
                contentDescription = null,
                tint = DeepSea4,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.size(18.dp))
        Text(
            text = title,
            color = DeepSea5,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = body,
            color = DeepSea4,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DeletePlanDialog(
    plan: EditablePlan,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepSea2,
        title = {
            Text(
                text = "Delete Plan?",
                color = DeepSea5,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = "Remove ${plan.title.ifBlank { "this event" }} from your trip calendar?",
                color = DeepSea4
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirmDelete) {
                Text(text = "DELETE", color = Color(0xFFE77D90))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "CANCEL", color = DeepSea5)
            }
        }
    )
}
