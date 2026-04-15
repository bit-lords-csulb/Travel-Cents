package com.example.travelcents.ui.main.current.header

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.theme.TravelCentsFonts

internal val CurrentTripHeroAccent = Color(0xFFCEBDFF)

private val HeroControlShape = RoundedCornerShape(12.dp)
private val HeroControlBackground = DeepSea2.copy(alpha = 0.95f)
private val HeroControlBorder = DeepSea3.copy(alpha = 0.7f)
private val HeroNavSlotWidth = 40.dp
private val HeroNavButtonSize = 32.dp

internal data class CurrentTripHeroNavAction(
    val enabled: Boolean,
    val contentDescription: String,
    val onClick: () -> Unit
)

@Composable
internal fun CurrentTripHeroLayout(
    modifier: Modifier = Modifier,
    previousAction: CurrentTripHeroNavAction? = null,
    nextAction: CurrentTripHeroNavAction? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.padding(start = 24.dp)) {
        Text(
            text = "SCHEDULED ITINERARY",
            color = CurrentTripHeroAccent,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp,
            fontFamily = TravelCentsFonts.Body
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            HeroNavigationSlot(
                action = previousAction,
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(0.dp),
                content = content
            )
            Spacer(modifier = Modifier.width(8.dp))
            HeroNavigationSlot(
                action = nextAction,
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight
            )
        }
    }
}

@Composable
private fun HeroNavigationSlot(
    action: CurrentTripHeroNavAction?,
    icon: ImageVector
) {
    Box(
        modifier = Modifier.width(HeroNavSlotWidth),
        contentAlignment = Alignment.Center
    ) {
        if (action != null) {
            Box(
                modifier = Modifier
                    .size(HeroNavButtonSize)
                    .clip(HeroControlShape)
                    .background(
                        if (action.enabled) HeroControlBackground else HeroControlBackground.copy(alpha = 0.55f),
                        HeroControlShape
                    )
                    .border(1.dp, HeroControlBorder, HeroControlShape)
                    .clickable(enabled = action.enabled, onClick = action.onClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = action.contentDescription,
                    tint = if (action.enabled) DeepSea5 else DeepSea4.copy(alpha = 0.45f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
