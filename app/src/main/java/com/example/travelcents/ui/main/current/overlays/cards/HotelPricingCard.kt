package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.ATTR_DEAL_DESCRIPTION
import com.example.travelcents.data.trip.model.ATTR_GROUP_RATE_PER_NIGHT
import com.example.travelcents.data.trip.model.ATTR_GROUP_TOTAL_STAY_RATE
import com.example.travelcents.data.trip.model.ATTR_RATE_PER_NIGHT
import com.example.travelcents.data.trip.model.ATTR_ROOMS_NEEDED
import com.example.travelcents.data.trip.model.ATTR_TOTAL_STAY_RATE
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun HotelPricingCard(event: TravelEvent, currencyCode: String = "USD") {
    val nights = computeNights(event)
    val rooms = event.detailValue(ATTR_ROOMS_NEEDED, "rooms_needed")?.toIntOrNull() ?: 1
    val nightlyRate = event.detailValue(ATTR_RATE_PER_NIGHT, "rate_per_night")?.toDoubleOrNull()
    val totalStay = event.detailValue(ATTR_TOTAL_STAY_RATE)?.toDoubleOrNull()
        ?: nightlyRate?.let { rate -> if (nights > 0) rate * nights else null }
    val groupTotal = event.detailValue(ATTR_GROUP_TOTAL_STAY_RATE)?.toDoubleOrNull()
        ?: totalStay?.let { it * rooms }
    val groupNightly = event.detailValue(ATTR_GROUP_RATE_PER_NIGHT, "group_rate_per_night")?.toDoubleOrNull()
    val deal = event.detailValue(ATTR_DEAL_DESCRIPTION)

    if (nightlyRate == null && totalStay == null && deal == null) return

    val headerTitle = totalStay?.let { "${formatPrice(it, currencyCode)} total stay" }
        ?: nightlyRate?.let { "${formatPrice(it, currencyCode)} / night" }
        ?: "Booking details"
    val nightlySubtitle = nightlyRate?.let { rate ->
        if (nights > 0) "$nights nights × ${formatPrice(rate, currencyCode)} / night"
        else "${formatPrice(rate, currencyCode)} / night"
    }

    DetailCardFrame(accent = CardLavender) {
        DetailCardHeader(eyebrow = "Pricing", title = headerTitle)
        nightlySubtitle?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it,
                color = CardTextMuted,
                fontSize = 13.sp
            )
        }
        if (rooms > 1 && (groupTotal != null || groupNightly != null)) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Group total ($rooms rooms)",
                    color = CardTextMuted,
                    fontSize = 13.sp
                )
                Text(
                    text = groupTotal?.let { formatPrice(it, currencyCode) }
                        ?: groupNightly?.let { "${formatPrice(it, currencyCode)} / night" }
                        ?: "",
                    color = CardText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        deal?.let {
            Spacer(modifier = Modifier.height(12.dp))
            DealBanner(text = it)
        }
    }
}

@Composable
private fun DealBanner(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardLavender.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.LocalOffer,
            contentDescription = null,
            tint = CardLavender,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            color = CardText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun computeNights(event: TravelEvent): Int {
    val checkIn = event.details["check_in_date"]?.takeIf { it.isNotBlank() } ?: return 0
    val checkOut = event.details["check_out_date"]?.takeIf { it.isNotBlank() } ?: return 0
    return try {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val start = LocalDate.parse(checkIn, formatter)
        val end = LocalDate.parse(checkOut, formatter)
        ChronoUnit.DAYS.between(start, end).toInt().coerceAtLeast(0)
    } catch (_: Exception) {
        0
    }
}
