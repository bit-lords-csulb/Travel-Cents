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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.BuildConfig
import com.example.travelcents.data.ai.chat.AiChatSender
import com.example.travelcents.data.debug.DebugPreferencesRepository
import com.example.travelcents.ui.components.ProfileAvatar
import com.example.travelcents.ui.main.newTrip.TripWizardColors
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.theme.TravelCentsFonts

@Composable
fun AiChatBubble(
    text: String,
    sender: AiChatSender,
    tags: List<String> = emptyList(),
    userDisplayName: String = "User",
    userProfileImageUrl: String = "",
    isUserProfileLoading: Boolean = false,
    showSenderHeader: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isUser = sender == AiChatSender.USER
    val resolvedUserDisplayName = userDisplayName.ifBlank { "User" }
    val showDebugTags by if (BuildConfig.DEBUG) {
        DebugPreferencesRepository.showAiDebugUi.collectAsState()
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showSenderHeader) {
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
            } else {
                Row(
                    modifier = Modifier.widthIn(max = 324.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = resolvedUserDisplayName,
                        color = DeepSea5,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = TravelCentsFonts.Headline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    ProfileAvatar(
                        photoUrl = userProfileImageUrl,
                        contentDescription = "$resolvedUserDisplayName profile picture",
                        modifier = Modifier.size(24.dp),
                        borderColor = TripWizardColors.Blue.copy(alpha = 0.28f),
                        backgroundColor = TripWizardColors.ContainerHighest,
                        placeholderTint = TripWizardColors.Blue,
                        borderWidth = 1.dp,
                        isLoading = isUserProfileLoading,
                        iconSize = 13.dp
                    )
                }
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
                    Color.White.copy(alpha = 0.04f)
                } else {
                    Color.White.copy(alpha = 0.08f)
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
                                    Color.White.copy(alpha = 0.03f),
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
                if (tags.isNotEmpty() && (isUser || showDebugTags)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tags.forEach { tag ->
                            if (isUser) UserBubbleTag(tag = tag) else DebugTag(tag = tag)
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

@Composable
private fun DebugTag(tag: String) {
    val color = when {
        tag.startsWith("APP_FALLBACK") -> Color(0xFFFFB347)
        tag.startsWith("FORCE_VISUAL") || tag.startsWith("DEAD_END") -> Color(0xFFFF6B6B)
        tag.contains("ACCEPTED") || tag.contains("OK") -> Color(0xFF6BCB77)
        tag.contains("REJECTED") -> Color(0xFFFF6B6B)
        else -> Color(0xFFAAAAAA)
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(width = 1.dp, color = color.copy(alpha = 0.4f))
    ) {
        Text(
            text = tag,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = TravelCentsFonts.Body,
            letterSpacing = 0.sp
        )
    }
}
