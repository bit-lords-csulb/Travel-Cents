package com.example.travelcents.ui.main.current

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.travelcents.ui.main.newTrip.TripWizardColors
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5

private data class DisplayModeOption(
    val mode: CurrentDisplayMode,
    val label: String
)

@Composable
fun CurrentTripModeSwitcher(
    selectedMode: CurrentDisplayMode,
    onModeSelected: (CurrentDisplayMode) -> Unit
) {
    val tabs = remember {
        listOf(
            DisplayModeOption(CurrentDisplayMode.DAY, "Day"),
            DisplayModeOption(CurrentDisplayMode.WEEK, "Week")
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepSea2, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        tabs.forEach { tab ->
            val isSelected = selectedMode == tab.mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) TripWizardColors.SurfaceBright else Color.Transparent
                    )
                    .clickable(enabled = !isSelected) { onModeSelected(tab.mode) }
                    .then(
                        if (isSelected) Modifier.border(1.dp, TripWizardColors.Blue.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label,
                    color = if (isSelected) DeepSea5 else DeepSea4,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

