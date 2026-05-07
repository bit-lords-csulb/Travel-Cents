package com.example.travelcents.ui.main.aichat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.BuildConfig
import com.example.travelcents.data.ai.chat.AiChatCardGroup
import com.example.travelcents.data.ai.chat.PlannerQuestionSource
import com.example.travelcents.data.debug.DebugPreferencesRepository
import com.example.travelcents.ui.main.newTrip.TripWizardColors
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.theme.TravelCentsFonts

@Composable
fun AiResponseCardGroup(
    group: AiChatCardGroup,
    selectedOptionIds: Set<String>,
    enabled: Boolean,
    onOptionClick: (com.example.travelcents.data.ai.chat.AiChatCardOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = TripWizardColors.ContainerLow,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = group.title,
                    color = DeepSea5,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = TravelCentsFonts.Headline
                )
                if (group.subtitle.isNotBlank()) {
                    Text(
                        text = group.subtitle,
                        color = DeepSea4,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontFamily = TravelCentsFonts.Body
                    )
                }
            }

            group.options.chunked(2).forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowOptions.forEach { option ->
                        AiOptionCard(
                            option = option,
                            selected = option.id in selectedOptionIds,
                            enabled = enabled,
                            onClick = { onOptionClick(option) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowOptions.size == 1) {
                        androidx.compose.foundation.layout.Spacer(
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Text(
                text = "You can also type your own answer below.",
                color = DeepSea4.copy(alpha = 0.7f),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                fontFamily = TravelCentsFonts.Body
            )

            val showDebugBadge by if (BuildConfig.DEBUG) {
                DebugPreferencesRepository.showAiDebugUi.collectAsState()
            } else {
                androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            }
            if (showDebugBadge) {
                val (label, color) = when (group.source) {
                    PlannerQuestionSource.LLM -> "⬡ LLM" to Color(0xFF6BCB77)
                    PlannerQuestionSource.APP_FALLBACK -> "⬡ APP_FALLBACK" to Color(0xFFFFB347)
                    PlannerQuestionSource.DISCOVERY -> "⬡ DISCOVERY" to Color(0xFF74C0FC)
                    PlannerQuestionSource.STARTER_GRID -> "⬡ STARTER" to Color(0xFFAAAAAA)
                }
                Text(
                    text = "$label · topic=${group.topicPath.ifBlank { "-" }} · multi=${group.allowMultiple}",
                    color = color.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = TravelCentsFonts.Body
                )
            }
        }
    }
}
