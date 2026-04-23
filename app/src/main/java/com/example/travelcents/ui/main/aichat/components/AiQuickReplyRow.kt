package com.example.travelcents.ui.main.aichat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.ai.chat.AiChatQuickReply
import com.example.travelcents.ui.main.newTrip.TripWizardColors
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.theme.TravelCentsFonts

@Composable
fun AiQuickReplyRow(
    replies: List<AiChatQuickReply>,
    onReplySelected: (AiChatQuickReply) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(replies, key = { it.id }) { reply ->
            SuggestionChip(
                onClick = { onReplySelected(reply) },
                label = {
                    Text(
                        text = reply.label,
                        fontFamily = TravelCentsFonts.Body,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = TripWizardColors.ContainerHigh,
                    labelColor = DeepSea5
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = TripWizardColors.SurfaceBright
                )
            )
        }
    }
}
