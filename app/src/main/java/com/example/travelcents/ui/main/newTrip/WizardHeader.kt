package com.example.travelcents.ui.main.newTrip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.theme.TravelCentsFonts

@Composable
fun WizardHeader(
    currentStep: Int,
    title: String,
    onBack: () -> Unit,
    onClose: () -> Unit,
    totalSteps: Int = 5
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF010E24))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TripWizardColors.Blue)
            }
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = DeepSea5,
                fontFamily = TravelCentsFonts.Headline,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "Close", tint = TripWizardColors.Blue)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(totalSteps) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (index < currentStep) TripWizardColors.Blue
                            else TripWizardColors.ContainerHighest
                        )
                )
            }
        }
    }
}
