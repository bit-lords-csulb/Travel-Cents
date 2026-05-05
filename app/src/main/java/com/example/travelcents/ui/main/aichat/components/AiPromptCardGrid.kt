package com.example.travelcents.ui.main.aichat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.ai.chat.AiChatCardOption
import com.example.travelcents.ui.main.newTrip.TripWizardColors
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.theme.TravelCentsFonts

@Composable
fun AiPromptCardGrid(
    title: String,
    subtitle: String,
    options: List<AiChatCardOption>,
    selectedOptionIds: Set<String>,
    onOptionClick: (AiChatCardOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (title.isNotBlank() || subtitle.isNotBlank()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (title.isNotBlank()) {
                    Text(
                        text = title,
                        color = DeepSea5,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = TravelCentsFonts.Headline
                    )
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = DeepSea4,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontFamily = TravelCentsFonts.Body
                    )
                }
            }
        }

        options.chunked(2).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowOptions.forEach { option ->
                    StarterPromptCard(
                        option = option,
                        selected = option.id in selectedOptionIds,
                        onClick = { onOptionClick(option) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (rowOptions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StarterPromptCard(
    option: AiChatCardOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val meta = starterCardMeta(option)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) {
            TripWizardColors.ContainerHighest
        } else {
            TripWizardColors.ContainerLow
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                meta.tint.copy(alpha = 0.5f)
            } else {
                Color.White.copy(alpha = 0.08f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = if (selected) {
                        Brush.linearGradient(
                            colors = listOf(
                                meta.tint.copy(alpha = 0.14f),
                                Color.Transparent
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent
                            )
                        )
                    }
                )
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        color = meta.tint.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(6.dp)
            ) {
                Icon(
                    imageVector = meta.icon,
                    contentDescription = null,
                    tint = meta.tint,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                text = option.label,
                color = if (selected) DeepSea5 else DeepSea4,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = TravelCentsFonts.Body
            )
        }
    }
}

private data class StarterCardMeta(
    val icon: ImageVector,
    val tint: Color
)

private fun starterCardMeta(option: AiChatCardOption): StarterCardMeta {
    return when {
        option.id.contains("beach_getaway") -> StarterCardMeta(Icons.Outlined.BeachAccess, Color(0xFF7DE1FF))
        option.id.contains("nature_escape") -> StarterCardMeta(Icons.Outlined.Forest, Color(0xFF85D59F))
        option.id.contains("city_break") -> StarterCardMeta(Icons.Outlined.FlightTakeoff, Color(0xFFB6C6ED))
        option.id.contains("food_trip") -> StarterCardMeta(Icons.Outlined.Restaurant, Color(0xFF00B0D6))
        option.id.contains("romantic_trip") -> StarterCardMeta(Icons.Outlined.FavoriteBorder, Color(0xFFFFA0B8))
        option.id.contains("family_trip") -> StarterCardMeta(Icons.Outlined.FamilyRestroom, Color(0xFF9EDB9A))
        option.id.contains("road_trip") -> StarterCardMeta(Icons.Outlined.DirectionsCar, Color(0xFFFFC774))
        option.id.contains("spa_retreat") -> StarterCardMeta(Icons.Outlined.SelfImprovement, Color(0xFF9FD8C8))
        option.id.contains("girls_trip") -> StarterCardMeta(Icons.Outlined.Celebration, Color(0xFFFF9DCB))
        option.id.contains("adventure_trip") -> StarterCardMeta(Icons.Outlined.AutoAwesome, Color(0xFFFFA86E))
        option.id.contains("arts_culture") -> StarterCardMeta(Icons.Outlined.Palette, Color(0xFFA9B2FF))
        option.id.contains("nightlife_escape") -> StarterCardMeta(Icons.Outlined.Celebration, Color(0xFFFF7AC8))
        option.id.contains("pet_friendly") -> StarterCardMeta(Icons.Outlined.Pets, Color(0xFFC6A6FF))
        option.id.contains("shopping_weekend") -> StarterCardMeta(Icons.Outlined.ShoppingBag, Color(0xFFFFB98B))
        option.id.contains("wellness_reset") -> StarterCardMeta(Icons.Outlined.SelfImprovement, Color(0xFF8EDBC4))
        option.id.contains("island_hopping") -> StarterCardMeta(Icons.Outlined.WbSunny, Color(0xFFFFD76A))
        option.id.contains("coffee_crawl") -> StarterCardMeta(Icons.Outlined.Coffee, Color(0xFFC58C5A))
        else -> StarterCardMeta(Icons.Outlined.AutoAwesome, TripWizardColors.Blue)
    }
}
