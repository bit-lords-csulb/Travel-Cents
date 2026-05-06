package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
        ItineraryActionButton(
            label = "Directions",
            onClick = onDirections,
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Directions,
            accent = accent
        )
        ItineraryActionButton(
            label = "Edit",
            onClick = onEdit,
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Edit,
            emphasis = ItineraryActionEmphasis.Secondary,
            accent = accent
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
        ItineraryActionButton(
            label = "Directions",
            onClick = onDirections,
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Directions,
            accent = CardMint
        )
        ItineraryActionButton(
            label = "Book",
            onClick = onBook,
            modifier = Modifier.weight(1f),
            icon = Icons.Default.ConfirmationNumber,
            emphasis = ItineraryActionEmphasis.Secondary,
            accent = CardMint
        )
    }
}
