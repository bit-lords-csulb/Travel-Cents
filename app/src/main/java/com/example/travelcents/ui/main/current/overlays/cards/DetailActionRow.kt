package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DetailActionRow(
    type: String,
    onDirections: () -> Unit,
    onEdit: () -> Unit
) {
    val accent = accentForType(type)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DetailActionChip(
            modifier = Modifier.weight(1f),
            label = "Directions",
            filled = true,
            type = type,
            icon = Icons.Default.Directions,
            onClick = onDirections
        )
        DetailActionChip(
            modifier = Modifier.weight(1f),
            label = "Edit",
            filled = false,
            type = type,
            icon = Icons.Default.Edit,
            onClick = onEdit
        )
    }
}

@Composable
fun TicketmasterActionRow(
    onDirections: () -> Unit,
    onBook: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DetailActionChip(
            modifier = Modifier.weight(1f),
            label = "Directions",
            filled = true,
            type = "activity",
            icon = Icons.Default.Directions,
            onClick = onDirections
        )
        DetailActionChip(
            modifier = Modifier.weight(1f),
            label = "Book",
            filled = false,
            type = "activity",
            icon = Icons.Default.ConfirmationNumber,
            onClick = onBook
        )
    }
}

@Composable
private fun DetailActionChip(
    modifier: Modifier,
    label: String,
    filled: Boolean,
    type: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val accent = accentForType(type)
    val baseModifier = modifier
        .clip(CircleShape)
        .clickable(onClick = onClick)
    Row(
        modifier = if (filled) {
            baseModifier.background(accentGradientForType(type), CircleShape)
        } else {
            baseModifier.background(CardSurfaceHigh, CircleShape)
        }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (filled) CardBackground else accent
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = if (filled) CardBackground else CardText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
