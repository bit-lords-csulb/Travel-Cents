package com.example.travelcents.ui.main.aichat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.ai.chat.AiChatItem
import com.example.travelcents.data.ai.chat.DiscoveryTrack
import com.example.travelcents.ui.main.newTrip.TripWizardColors
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.theme.TravelCentsFonts

@Composable
fun AiPreferenceQuestionCard(
    card: AiChatItem.PreferenceQuestionCard,
    enabled: Boolean,
    onSubmit: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = remember(card.id) { mutableStateListOf<String>().apply { addAll(card.answerSummarySelections()) } }
    val accent = when (card.track) {
        DiscoveryTrack.FOOD -> Color(0xFFFF8A65)
        DiscoveryTrack.ACTIVITIES -> Color(0xFF4DB6AC)
        DiscoveryTrack.EVENTS -> Color(0xFF9575CD)
        DiscoveryTrack.NOT_STARTED,
        DiscoveryTrack.COMPLETE -> TripWizardColors.Blue
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = TripWizardColors.ContainerLow,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = card.question,
                color = DeepSea5,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = TravelCentsFonts.Headline
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                card.options.forEach { option ->
                    val isSelected = option in selected || card.answerSummary.contains(option)
                    FilterChip(
                        selected = isSelected,
                        enabled = enabled && !card.answered,
                        onClick = {
                            if (!enabled || card.answered) return@FilterChip
                            if (card.multiSelect) {
                                if (option in selected) selected.remove(option) else selected.add(option)
                            } else {
                                selected.clear()
                                selected.add(option)
                                onSubmit(selected.toList())
                            }
                        },
                        label = {
                            Text(
                                text = option,
                                fontFamily = TravelCentsFonts.Body
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.copy(alpha = 0.22f),
                            selectedLabelColor = accent,
                            containerColor = Color.White.copy(alpha = 0.04f),
                            labelColor = DeepSea5
                        )
                    )
                }
            }

            when {
                card.answered -> {
                    Text(
                        text = card.answerSummary,
                        color = DeepSea4,
                        fontSize = 12.sp,
                        fontFamily = TravelCentsFonts.Body
                    )
                }

                card.multiSelect -> {
                    Surface(
                        onClick = { onSubmit(selected.toList()) },
                        enabled = enabled && selected.isNotEmpty(),
                        color = accent.copy(alpha = 0.18f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Done",
                            color = accent,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            fontFamily = TravelCentsFonts.Body,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private fun AiChatItem.PreferenceQuestionCard.answerSummarySelections(): List<String> {
    return answerSummary.substringAfter(": ", "").split(", ")
        .map(String::trim)
        .filter(String::isNotBlank)
}
