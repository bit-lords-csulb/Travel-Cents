package com.example.travelcents.ui.main.current

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.advisory.AdvisorySeverity
import com.example.travelcents.data.trip.advisory.TripAdvisory
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_ADDRESS
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.ATTR_PRICE_TIER
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.data.trip.model.displayName
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5

private val AdvisoryAmber = Color(0xFFFFC857)
private val AdvisoryCoral = Color(0xFFFF716C)
private val AdvisoryGreen = Color(0xFF79E2A0)

@Composable
fun CurrentTripDemoAdvisoryToggle(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = DeepSea2,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DeepSea3.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(
                        color = if (enabled) AdvisoryAmber.copy(alpha = 0.18f) else DeepSea3.copy(alpha = 0.28f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (enabled) AdvisoryAmber else DeepSea4,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Demo updates",
                    color = DeepSea5,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (enabled) "Will check when itinerary opens" else "Off",
                    color = DeepSea4,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = DeepSea1,
                    checkedTrackColor = AdvisoryAmber,
                    uncheckedThumbColor = DeepSea4,
                    uncheckedTrackColor = DeepSea3
                )
            )
        }
    }
}

@Composable
fun CurrentTripAdvisoryStrip(
    advisory: TripAdvisory,
    canEditTrip: Boolean,
    onReview: () -> Unit,
    onDismiss: () -> Unit
) {
    val accent = advisoryAccent(advisory.severity)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 36.dp, bottom = 10.dp),
        color = DeepSea2,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = advisory.title,
                        color = DeepSea5,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = advisory.contextSummary,
                        color = DeepSea4,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onReview,
                    enabled = canEditTrip,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accent)
                ) {
                    Text(text = "Review", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = "Dismiss", color = DeepSea4, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripAdvisoryReviewSheet(
    advisory: TripAdvisory,
    event: TravelEvent,
    canEditTrip: Boolean,
    isLoadingSuggestions: Boolean = false,
    onReplaceOption: (optionId: String) -> Unit,
    onSaveOption: (optionId: String) -> Unit,
    onDismissAdvisory: () -> Unit,
    onDismiss: () -> Unit
) {
    val accent = advisoryAccent(advisory.severity)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DeepSea1,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(DeepSea3, RoundedCornerShape(999.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = advisory.title,
                color = DeepSea5,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = advisory.message,
                color = DeepSea4,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                color = DeepSea2,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DeepSea3.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Current plan",
                        color = DeepSea4,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = event.displayName().orEmpty().ifBlank { event.details["title"].orEmpty() },
                        color = DeepSea5,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (event.startTime.isNotBlank()) {
                        Text(
                            text = event.startTime,
                            color = DeepSea4,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Suggested updates",
                color = DeepSea5,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (isLoadingSuggestions) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = accent,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Finding weather-safe alternatives...",
                        color = DeepSea4,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            } else if (advisory.suggestedOptions.isEmpty()) {
                Text(
                    text = "No live inventory alternatives found yet.",
                    color = DeepSea4,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            } else {
                advisory.suggestedOptions.forEachIndexed { index, option ->
                    AdvisoryOptionRow(
                        option = option,
                        accent = accent,
                        canEditTrip = canEditTrip,
                        onReplace = {
                            onReplaceOption(option.optionId)
                            onDismiss()
                        },
                        onSave = {
                            onSaveOption(option.optionId)
                            onDismiss()
                        }
                    )
                    if (index != advisory.suggestedOptions.lastIndex) {
                        HorizontalDivider(color = DeepSea3.copy(alpha = 0.3f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = {
                    onDismissAdvisory()
                    onDismiss()
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = "Dismiss suggestion", color = DeepSea4)
            }
        }
    }
}

@Composable
private fun AdvisoryOptionRow(
    option: EventOption,
    accent: Color,
    canEditTrip: Boolean,
    onReplace: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val title = option.detailValue(ATTR_BUSINESS_NAME, "activity_name", "title", "name")
            ?.takeIf { it.isNotBlank() }
            ?: "Suggested option"
        val subtitle = listOfNotNull(
            option.detailValue(ATTR_BUSINESS_ADDRESS, "address")?.takeIf { it.isNotBlank() },
            option.detailValue(ATTR_PRICE_TIER, "price_tier")?.takeIf { it.isNotBlank() }
        ).joinToString(" - ")

        Text(
            text = title,
            color = DeepSea5,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        option.details["description"]?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                color = DeepSea4,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                color = DeepSea4,
                fontSize = 11.sp
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onReplace,
                enabled = canEditTrip,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = DeepSea1
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = "Replace plan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onSave,
                enabled = canEditTrip,
                border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = "Save option", fontSize = 12.sp)
            }
        }
    }
}

private fun advisoryAccent(severity: AdvisorySeverity): Color {
    return when (severity) {
        AdvisorySeverity.HIGH -> AdvisoryCoral
        AdvisorySeverity.MEDIUM -> AdvisoryAmber
        AdvisorySeverity.LOW -> AdvisoryGreen
    }
}
