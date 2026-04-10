package com.example.travelcents.ui.main.current

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5

private data class DisplayModeOption(
    val mode: CurrentDisplayMode,
    val label: String
)

@Composable
fun CurrentDisplayModeTabs(
    selectedMode: CurrentDisplayMode,
    onModeSelected: (CurrentDisplayMode) -> Unit
) {
    val tabs = remember {
        listOf(
            DisplayModeOption(CurrentDisplayMode.ITINERARY, "Itinerary"),
            DisplayModeOption(CurrentDisplayMode.DAY, "Day View"),
            DisplayModeOption(CurrentDisplayMode.WEEK, "Week View")
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepSea2, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEach { tab ->
            val isSelected = selectedMode == tab.mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) DeepSea3 else Color.Transparent)
                    .clickable(enabled = !isSelected) { onModeSelected(tab.mode) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label,
                    color = if (isSelected) DeepSea5 else DeepSea4,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}
