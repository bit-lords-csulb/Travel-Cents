package com.example.travelcents.ui.main.newTrip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.R
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea5

private val LandingPrimaryBlue = Color(0xFF64B5F6)
private val LandingTertiaryPurple = Color(0xFFB5A0FF)
private val LandingCardDark = Color(0xFF0B203D)
private val LandingOutlineVariant = Color(0xFF3B4861)
private val LandingOnSurfaceVariant = Color(0xFF9EABC8)
private val LandingGlassStart = Color(0xFF102645)

@Composable
fun NewTripLandingPage(
    modifier: Modifier = Modifier,
    onPlanTripClick: () -> Unit,
    onAiChatClick: () -> Unit,
    onViewLastTripClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSea1)
    ) {
        // Top app bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF010E24))
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "New Trip Planner",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DeepSea5
            )
        }

        // Main content centered vertically
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "TravelCents AI",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepSea5,
                textAlign = TextAlign.Center,
                letterSpacing = (-1.5).sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Your intelligent travel companion for personalized trip planning and assistance.",
                fontSize = 14.sp,
                color = LandingOnSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp
            )
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "CHOOSE YOUR PATH TO BEGIN",
                fontSize = 10.sp,
                color = LandingOnSurfaceVariant.copy(alpha = 0.5f),
                letterSpacing = 2.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Planning a Trip card (glass style)
            LandingOptionCard(
                title = "Planning a Trip",
                subtitle = "Guided 5-step concierge process for your next adventure.",
                actionLabel = "GET STARTED",
                imageRes = R.drawable.landing_planning,
                icon = Icons.Outlined.CalendarToday,
                accentColor = LandingPrimaryBlue,
                cardBrush = Brush.linearGradient(
                    listOf(LandingGlassStart.copy(alpha = 0.7f), LandingCardDark.copy(alpha = 0.4f))
                ),
                onClick = onPlanTripClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // AI Chatbot card (solid style)
            LandingOptionCard(
                title = "AI Chatbot",
                subtitle = "Ask questions or get instant local recommendations via chat.",
                actionLabel = "OPEN ASSISTANT",
                imageRes = R.drawable.landing_ai_chat,
                icon = Icons.Outlined.AutoAwesome,
                accentColor = LandingTertiaryPurple,
                cardBrush = Brush.linearGradient(
                    listOf(LandingCardDark, LandingCardDark)
                ),
                onClick = onAiChatClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (onViewLastTripClick != null) {
                Spacer(modifier = Modifier.height(20.dp))
                TextButton(onClick = onViewLastTripClick) {
                    Text(
                        text = "View last trip",
                        fontSize = 13.sp,
                        color = LandingOnSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = LandingOnSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LandingOptionCard(
    title: String,
    subtitle: String,
    actionLabel: String,
    imageRes: Int,
    icon: ImageVector,
    accentColor: Color,
    cardBrush: Brush,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(cardBrush)
            .border(1.dp, LandingOutlineVariant.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
            .clickable { onClick() }
    ) {
        // Background image at low opacity
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp)),
            alpha = 0.1f
        )

        // Card content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepSea5
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = LandingOnSurfaceVariant,
                    lineHeight = 18.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 10.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
