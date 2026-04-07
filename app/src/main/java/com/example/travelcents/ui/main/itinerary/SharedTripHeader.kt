package com.example.travelcents.ui.main.itinerary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.ui.theme.DeepSea3

@Composable
fun SharedTripHeader(
    tripTitle: String,
    dateRange: String,
    primaryActionLabel: String,
    onPrimaryActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    canAdd: Boolean = true,
    onAddClick: () -> Unit = {},
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = tripTitle.uppercase(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = dateRange,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            TripHeaderActionButton(
                enabled = canAdd,
                onClick = onAddClick
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TripHeaderPill(
                label = primaryActionLabel,
                onClick = onPrimaryActionClick
            )

            if (secondaryActionLabel != null && onSecondaryActionClick != null) {
                TripHeaderPill(
                    label = secondaryActionLabel,
                    onClick = onSecondaryActionClick
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
    }
}

@Composable
private fun TripHeaderActionButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (enabled) DeepSea3 else DeepSea3.copy(alpha = 0.35f),
        modifier = Modifier
            .size(40.dp)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "+",
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.45f),
                fontSize = 24.sp
            )
        }
    }
}

@Composable
private fun TripHeaderPill(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
