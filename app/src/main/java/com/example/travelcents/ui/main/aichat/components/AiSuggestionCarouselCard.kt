package com.example.travelcents.ui.main.aichat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelcents.data.ai.chat.AiChatItem
import com.example.travelcents.data.ai.chat.SuggestionItem
import com.example.travelcents.ui.main.newTrip.TripWizardColors
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.theme.TravelCentsFonts

@Composable
fun AiSuggestionCarouselCard(
    card: AiChatItem.SuggestionCarouselCard,
    onAdd: (SuggestionItem) -> Unit,
    onBookmark: (SuggestionItem) -> Unit,
    onSkip: (SuggestionItem) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = TripWizardColors.ContainerLow,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = card.label,
                color = DeepSea5,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = TravelCentsFonts.Headline
            )

            if (card.suggestions.isEmpty()) {
                Text(
                    text = if (card.exhausted) "No more options for this slot." else "Looking for options...",
                    color = DeepSea4,
                    fontSize = 12.sp,
                    fontFamily = TravelCentsFonts.Body
                )
            }

            card.suggestions.forEach { suggestion ->
                SuggestionRow(
                    suggestion = suggestion,
                    onAdd = { onAdd(suggestion) },
                    onBookmark = { onBookmark(suggestion) },
                    onSkip = { onSkip(suggestion) }
                )
            }

            if (card.hasMore && !card.exhausted) {
                Surface(
                    onClick = onLoadMore,
                    color = TripWizardColors.Blue.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Load more options",
                        color = TripWizardColors.Blue,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        fontFamily = TravelCentsFonts.Body,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestion: SuggestionItem,
    onAdd: () -> Unit,
    onBookmark: () -> Unit,
    onSkip: () -> Unit
) {
    Surface(
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (!suggestion.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = suggestion.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = suggestion.name,
                        color = DeepSea5,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = TravelCentsFonts.Body
                    )
                    Text(
                        text = suggestion.subtitle,
                        color = DeepSea4,
                        fontSize = 12.sp,
                        fontFamily = TravelCentsFonts.Body
                    )
                    if (suggestion.address.isNotBlank()) {
                        Text(
                            text = suggestion.address,
                            color = DeepSea4.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontFamily = TravelCentsFonts.Body
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionActionButton("Add", Color(0xFF4DB6AC), onAdd)
                SuggestionActionButton("Bookmark", TripWizardColors.Blue, onBookmark)
                SuggestionActionButton("Skip", Color(0xFFFF8A65), onSkip)
            }
        }
    }
}

@Composable
private fun SuggestionActionButton(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = label,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontFamily = TravelCentsFonts.Body,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
