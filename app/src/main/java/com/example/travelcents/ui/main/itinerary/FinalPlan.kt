package com.example.travelcents.ui.main.itinerary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelcents.data.model.Itinerary
import com.example.travelcents.data.model.TravelEvent
import com.example.travelcents.ui.theme.TravelCentsTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val BackgroundColor = Color(0xFF010E24)
private val SurfaceContainerHigh = Color(0xFF0B203D)
private val SurfaceContainerHighest = Color(0xFF102645)
private val PrimaryBlue = Color(0xFF64B5F6)
private val OnSurfaceVariant = Color(0xFF9EABC8)
private val OnSurface = Color(0xFFDBE6FF)
private val OutlineVariant = Color(0xFF3B4861)
private val TertiaryColor = Color(0xFFB5A0FF)
private val ErrorColor = Color(0xFFFF716C)
private val SecondaryColor = Color(0xFFD5E3FB)

private fun eventTypeColor(type: String): Color = when (type.lowercase()) {
    "flight" -> PrimaryBlue
    "hotel" -> TertiaryColor
    "restaurant", "dining", "food" -> ErrorColor
    else -> SecondaryColor
}

private fun formatDateHeader(dateStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = sdf.parse(dateStr) ?: return dateStr
        val cal = Calendar.getInstance().also { it.time = date }
        val month = SimpleDateFormat("MMM", Locale.US).format(date).uppercase()
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val suffix = when {
            day in 11..13 -> "TH"
            day % 10 == 1 -> "ST"
            day % 10 == 2 -> "ND"
            day % 10 == 3 -> "RD"
            else -> "TH"
        }
        "$month ${day}$suffix"
    } catch (_: Exception) {
        dateStr
    }
}

private sealed interface FinalPlanItem {
    data class Header(val date: String) : FinalPlanItem
    data class EventItem(val event: TravelEvent, val isLastEvent: Boolean) : FinalPlanItem
    data object DaySpacer : FinalPlanItem
}

private fun buildPlanItems(events: List<TravelEvent>): List<FinalPlanItem> {
    val sorted = events
        .filter { it.date.isNotEmpty() }
        .sortedWith(compareBy({ it.date }, { it.startTime }))
    val grouped = sorted.groupBy { it.date }.entries.sortedBy { it.key }
    val total = sorted.size
    var count = 0
    return buildList {
        grouped.forEach { (date, dayEvents) ->
            add(FinalPlanItem.Header(formatDateHeader(date)))
            dayEvents.forEach { event ->
                count++
                add(FinalPlanItem.EventItem(event, count == total))
            }
            add(FinalPlanItem.DaySpacer)
        }
    }
}

@Composable
fun FinalPlanPage(
    itinerary: Itinerary,
    events: List<TravelEvent>,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val planItems = remember(events) { buildPlanItems(events) }

    Scaffold(
        containerColor = BackgroundColor,
        topBar = { FinalPlanTopBar(onBackClick = onBackClick) }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                ProgressWidget(destination = itinerary.destination)
                Spacer(modifier = Modifier.height(32.dp))
            }

            items(planItems) { item ->
                when (item) {
                    is FinalPlanItem.Header -> DayHeader(item.date)
                    is FinalPlanItem.EventItem -> TimelineEventCard(item.event, item.isLastEvent)
                    is FinalPlanItem.DaySpacer -> Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun FinalPlanTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF02132B))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PrimaryBlue
                )
            }
            Text(
                text = "Your Itinerary",
                color = OnSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Icon(
            imageVector = Icons.Outlined.AccountCircle,
            contentDescription = "Profile",
            tint = OnSurfaceVariant,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
private fun ProgressWidget(destination: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "STATUS",
                        color = OnSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Your Progress",
                        color = OnSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "100%",
                    color = PrimaryBlue,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color(0xFF152C4E), CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(PrimaryBlue, Color(0xFF54A7E7))
                            ),
                            shape = CircleShape
                        )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "All systems go. Your $destination journey is fully mapped.",
                color = OnSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DayHeader(date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = date,
            color = PrimaryBlue,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(OutlineVariant.copy(alpha = 0.3f))
        )
    }
}

@Composable
private fun TimelineEventCard(event: TravelEvent, isLast: Boolean) {
    val typeColor = eventTypeColor(event.type)
    val title = event.details["title"]
        ?: event.details["activity_name"]
        ?: event.type.replaceFirstChar { it.uppercase() }
    val description = event.details["notes"]
        ?: event.details["description"]
        ?: event.details["location"]
        ?: ""
    val imageUrl = event.details["imageUrl"] ?: event.details["image_url"] ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Timeline dot and connecting line
        Box(
            modifier = Modifier
                .width(40.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (!isLast) {
                Canvas(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .padding(top = 12.dp)
                ) {
                    drawLine(
                        color = OutlineVariant.copy(alpha = 0.4f),
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(24.dp)
                    .background(SurfaceContainerHighest, CircleShape)
                    .border(2.dp, PrimaryBlue.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(PrimaryBlue, CircleShape)
                )
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 24.dp)
                .clickable { },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerHighest),
            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.05f))
        ) {
            Row(modifier = Modifier.height(120.dp)) {
                if (imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Placeholder when no image is available
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                SurfaceContainerHigh,
                                RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = event.type.take(1).uppercase(),
                            color = typeColor,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(2f)
                        .padding(16.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = typeColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(100.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, typeColor.copy(alpha = 0.2f)
                            )
                        ) {
                            Text(
                                text = event.type.uppercase(),
                                color = typeColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = event.startTime,
                            color = OnSurfaceVariant,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = title,
                        color = OnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        color = OnSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview(name = "Default", showBackground = true)
@Preview(name = "Pixel 7", device = Devices.PIXEL_7, showSystemUi = true)
@Composable
fun FinalPlanPreview() {
    TravelCentsTheme(dynamicColor = false) {
        val sampleItinerary = Itinerary(
            itineraryId = "preview",
            userId = "user",
            tripName = "Tokyo Adventure",
            destination = "Tokyo",
            origin = "Los Angeles",
            dateFrom = "2025-05-01",
            dateTo = "2025-05-03",
            durationDays = 3,
            currency = "USD",
            travelStyle = "comfort",
            adults = 2,
            children = 0,
            createdAt = "",
            status = "active",
            eventIds = emptyList()
        )
        val sampleEvents = listOf(
            TravelEvent(
                "1", "flight", "preview", date = "2025-05-01", startTime = "10:30 AM",
                details = mapOf("title" to "Flight to Tokyo", "notes" to "NH802 | Seat 4A (Business Class). Departure from LAX Terminal B.")
            ),
            TravelEvent(
                "2", "hotel", "preview", date = "2025-05-02", startTime = "03:00 PM",
                details = mapOf("title" to "Park Hyatt Check-in", "notes" to "Shinjuku Park Tower. Reservation #TYO-9821-X.")
            ),
            TravelEvent(
                "3", "restaurant", "preview", date = "2025-05-02", startTime = "08:30 PM",
                details = mapOf("title" to "Sukiyabashi Jiro", "notes" to "Omakase Experience. Ginza. Formal attire required.")
            ),
            TravelEvent(
                "4", "activity", "preview", date = "2025-05-03", startTime = "09:00 AM",
                details = mapOf("title" to "Senso-ji Temple Visit", "notes" to "Asakusa. Private guide at Kaminarimon Gate.")
            )
        )
        FinalPlanPage(itinerary = sampleItinerary, events = sampleEvents)
    }
}
