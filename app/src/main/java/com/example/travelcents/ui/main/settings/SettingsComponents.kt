package com.example.travelcents.ui.main.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

// Uppercase section label
@Composable
fun SettingHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = TripWizardColors.OnSurfaceVariant,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = modifier.padding(start = 4.dp, bottom = 6.dp, top = 20.dp)
    )
}

// Rounded card grouping settings rows
@Composable
fun SettingCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DeepSea2)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        content = content
    )
}

// Toggle row
@Composable
fun SwitchSettingItem(
    title: String,
    subtitle: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(text = title, color = DeepSea5, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        color = DeepSea4,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = DeepSea5,
                    checkedTrackColor = DeepSea3,
                    uncheckedThumbColor = DeepSea4,
                    uncheckedTrackColor = TripWizardColors.ContainerHigh,
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
        if (showDivider) {
            HorizontalDivider(color = DeepSea3.copy(alpha = 0.3f), thickness = 0.5.dp)
        }
    }
}

// Static label + value row
@Composable
fun SettingRow(
    label: String,
    value: String = "",
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = DeepSea5, fontSize = 15.sp)
            if (value.isNotEmpty()) {
                Text(text = value, color = DeepSea4, fontSize = 14.sp)
            }
        }
        if (showDivider) {
            HorizontalDivider(color = DeepSea3.copy(alpha = 0.3f), thickness = 0.5.dp)
        }
    }
}
