package com.example.travelcents.ui.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.BudgetStatus
import com.example.travelcents.data.trip.model.WeeklySummary
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import java.util.Locale

@Composable
fun WeeklySummaryWidget(
    summary: WeeklySummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSea2)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WEEKLY SUMMARY",
                        color = DeepSea4,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = summary.tripName,
                        color = DeepSea5,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Assignment,
                    contentDescription = null,
                    tint = Color(0xFF64B5F6),
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "${summary.weekStartDate} — ${summary.weekEndDate}",
                color = DeepSea4,
                fontSize = 12.sp
            )

            // Budget Progress
            val progress = if (summary.budget > 0) (summary.totalCost / summary.budget).toFloat() else 0f
            val statusColor = when (summary.budgetStatus) {
                BudgetStatus.UNDER_BUDGET -> Color(0xFF66BB6A)
                BudgetStatus.ON_TRACK -> Color(0xFF64B5F6)
                BudgetStatus.OVER_BUDGET -> Color(0xFFEF5350)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Spending: ${formatCurrency(summary.totalCost, summary.currency)}",
                        color = DeepSea5,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Budget: ${formatCurrency(summary.budget, summary.currency)}",
                        color = DeepSea4,
                        fontSize = 12.sp
                    )
                }
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = statusColor,
                    trackColor = DeepSea3
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusIcon = when (summary.budgetStatus) {
                        BudgetStatus.UNDER_BUDGET -> Icons.Default.CheckCircle
                        BudgetStatus.ON_TRACK -> Icons.AutoMirrored.Filled.TrendingUp
                        BudgetStatus.OVER_BUDGET -> Icons.Default.Error
                    }
                    val statusText = when (summary.budgetStatus) {
                        BudgetStatus.UNDER_BUDGET -> "Under budget! Good job."
                        BudgetStatus.ON_TRACK -> "On track with your goals."
                        BudgetStatus.OVER_BUDGET -> "Above budget this week."
                    }
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (summary.interestStats.isNotEmpty()) {
                Text(
                    text = "HIGHLIGHTS BY INTEREST",
                    color = DeepSea4,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    summary.interestStats.forEach { (interest, count) ->
                        Box(
                            modifier = Modifier
                                .background(DeepSea3, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$interest ($count)",
                                color = DeepSea5,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            if (summary.events.isNotEmpty()) {
                Text(
                    text = "TOP EVENTS",
                    color = DeepSea4,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    summary.events.take(3).forEach { event ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = event.eventTitle,
                                color = DeepSea5,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatCurrency(event.cost, summary.currency),
                                color = DeepSea4,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatCurrency(amount: Double, currencyCode: String): String {
    val symbol = when (currencyCode.uppercase(Locale.US)) {
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> "$currencyCode "
    }
    return if (amount % 1.0 == 0.0) {
        "$symbol${amount.toInt()}"
    } else {
        "$symbol${String.format(Locale.US, "%.2f", amount)}"
    }
}
