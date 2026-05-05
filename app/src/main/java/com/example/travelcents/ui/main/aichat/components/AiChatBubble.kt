package com.example.travelcents.ui.main.aichat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.ai.chat.AiChatSender
import com.example.travelcents.ui.main.newTrip.TripWizardColors
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.theme.TravelCentsFonts

@Composable
fun AiChatBubble(
    text: String,
    sender: AiChatSender,
    tags: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    val isUser = sender == AiChatSender.USER

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = TripWizardColors.Blue.copy(alpha = 0.18f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = TripWizardColors.Blue,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = "TravelCents AI",
                    color = TripWizardColors.Blue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = TravelCentsFonts.Headline
                )
            }
        }

        Surface(
            color = if (isUser) TripWizardColors.ContainerHighest else TripWizardColors.ContainerLow,
            shape = RoundedCornerShape(
                topStart = 22.dp,
                topEnd = 22.dp,
                bottomStart = if (isUser) 22.dp else 8.dp,
                bottomEnd = if (isUser) 8.dp else 22.dp
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (isUser) {
                    TripWizardColors.OnSurfaceVariant.copy(alpha = 0.12f)
                } else {
                    TripWizardColors.OnSurfaceVariant.copy(alpha = 0.16f)
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 324.dp)
                    .background(
                        brush = if (isUser) {
                            Brush.linearGradient(
                                colors = listOf(
                                    TripWizardColors.OnSurfaceVariant.copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    TripWizardColors.Blue.copy(alpha = 0.06f),
                                    Color.Transparent
                                )
                            )
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isUser && tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tags.forEach { tag ->
                            UserBubbleTag(tag = tag)
                        }
                    }
                }

                if (text.isNotBlank()) {
                    Text(
                        text = text,
                        color = if (isUser) DeepSea5 else DeepSea4.copy(alpha = 0.98f),
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        fontFamily = TravelCentsFonts.Body
                    )
                }
            }
        }
    }
}

@Composable
private fun UserBubbleTag(tag: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = TripWizardColors.ContainerHighest,
        border = BorderStroke(
            width = 1.dp,
            color = TripWizardColors.Blue.copy(alpha = 0.24f)
        )
    ) {
        Text(
            text = tag,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = TripWizardColors.Blue,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = TravelCentsFonts.Body
        )
    }
}
