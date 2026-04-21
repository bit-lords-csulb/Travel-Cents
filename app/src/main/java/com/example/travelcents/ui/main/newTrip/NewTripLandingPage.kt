package com.example.travelcents.ui.main.newTrip

import androidx.compose.foundation.background
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.ui.components.TcButton
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.theme.TravelCentsFonts

@Composable
fun NewTripLandingPage(
    modifier: Modifier = Modifier,
    onPlanTripClick: () -> Unit,
    onAiChatClick: () -> Unit
) {
    ProvideTextStyle(value = TextStyle(fontFamily = TravelCentsFonts.Body)) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(DeepSea1)
        ) {
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(DeepSea3, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = DeepSea5,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "PLAN YOUR NEXT TRIP",
                    fontSize = 11.sp,
                    color = DeepSea5.copy(alpha = 0.72f),
                    letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Travel smarter with TravelCents",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepSea5,
                    textAlign = TextAlign.Center,
                    lineHeight = 42.sp,
                    fontFamily = TravelCentsFonts.Headline
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Start a guided trip plan or jump straight into AI travel help. Both paths use the same shared itinerary flow.",
                    fontSize = 15.sp,
                    color = DeepSea5.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                TcButton(
                    onClick = onPlanTripClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Plan a Trip",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TcButton(
                    onClick = onAiChatClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepSea3,
                        contentColor = DeepSea5,
                        disabledContainerColor = DeepSea3.copy(alpha = 0.45f),
                        disabledContentColor = DeepSea5.copy(alpha = 0.7f)
                    )
                ) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Open AI Chat",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = DeepSea5.copy(alpha = 0.55f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "You can switch between planning and chat any time.",
                        fontSize = 13.sp,
                        color = DeepSea5.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

