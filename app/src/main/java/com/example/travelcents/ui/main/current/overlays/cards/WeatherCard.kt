package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.ATTR_WEATHER_CONDITION
import com.example.travelcents.data.trip.model.ATTR_WEATHER_PRECIP_PCT
import com.example.travelcents.data.trip.model.ATTR_WEATHER_SUMMARY
import com.example.travelcents.data.trip.model.ATTR_WEATHER_TEMP_C
import com.example.travelcents.data.trip.model.ATTR_WEATHER_WIND_KPH
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue

@Composable
fun WeatherCard(event: TravelEvent) {
    val condition = event.detailValue(ATTR_WEATHER_CONDITION)?.takeIf { it.isNotBlank() }
    val temperatureC = event.detailValue(ATTR_WEATHER_TEMP_C)?.toIntOrNull()
    val precipPct = event.detailValue(ATTR_WEATHER_PRECIP_PCT)?.toIntOrNull()
    val windKph = event.detailValue(ATTR_WEATHER_WIND_KPH)?.toIntOrNull()
    val summary = event.detailValue(ATTR_WEATHER_SUMMARY)?.takeIf { it.isNotBlank() }

    if (condition == null && temperatureC == null && precipPct == null && windKph == null && summary == null) {
        return
    }

    DetailCardFrame(accent = CardCoral) {
        DetailCardHeader(
            eyebrow = "Weather",
            title = condition?.let { "$it around your dining time" } ?: "Patio forecast"
        )

        val badges = listOfNotNull(
            temperatureC?.let { "${it}C" },
            precipPct?.let { "$it% precip" },
            windKph?.let { "$it km/h wind" }
        )
        if (badges.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            DetailBadgeRow(
                badges = badges,
                accent = CardCoral
            )
        }

        summary?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = it,
                color = CardTextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}
