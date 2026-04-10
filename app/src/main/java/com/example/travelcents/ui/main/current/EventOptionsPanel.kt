package com.example.travelcents.ui.main.current

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelcents.data.model.EventOption
import com.example.travelcents.data.model.TravelEvent

private val BgColor = Color(0xFF010E24)
private val SurfaceHigh = Color(0xFF0B203D)
private val SurfaceHighest = Color(0xFF102645)
private val BlueAccent = Color(0xFF64B5F6)
private val OnSurface = Color(0xFFDBE6FF)
private val OnSurfaceVar = Color(0xFF9EABC8)
private val OutlineVar = Color(0xFF3B4861)
private val ErrorRed = Color(0xFFFF716C)
private val TertiaryPurple = Color(0xFFB5A0FF)

private fun formatDurationMinutes(rawMinutes: String?): String? {
    val totalMinutes = rawMinutes?.toIntOrNull() ?: return null
    if (totalMinutes <= 0) return null
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

// Format price/rating for each event type
private fun formatOptionSubtitle(event: TravelEvent, opt: EventOption): String {
    return when (event.type.lowercase()) {
        "flight" -> {
            val price = opt.details["total_price"] ?: opt.details["price"] ?: ""
            val cabin = opt.details["cabin_class"] ?: "Economy"
            val duration = formatDurationMinutes(opt.details["total_duration"] ?: opt.details["duration"]) ?: ""
            listOfNotNull(
                if (price.isNotBlank()) "\$$price" else null,
                cabin,
                if (duration.isNotBlank()) duration else null
            ).joinToString(" · ")
        }
        "hotel" -> {
            val rate = opt.details["rate_per_night"] ?: ""
            val rating = opt.details["overall_rating"] ?: opt.details["rating"] ?: ""
            val rooms = opt.details["rooms_needed"] ?: ""
            listOfNotNull(
                if (rate.isNotBlank()) "\$$rate/night" else null,
                if (rating.isNotBlank()) "★$rating" else null,
                if (rooms.isNotBlank() && rooms != "1") "($rooms rooms)" else null
            ).joinToString(" · ")
        }
        "restaurant", "dining", "food" -> {
            val tier = opt.details["price_tier"] ?: ""
            val rating = opt.details["rating"] ?: ""
            val reviews = opt.details["review_count"] ?: ""
            listOfNotNull(
                tier.ifBlank { null },
                if (rating.isNotBlank()) "★$rating" else null,
                if (reviews.isNotBlank()) "$reviews reviews" else null
            ).joinToString(" · ")
        }
        else -> {
            val isFree = opt.details["is_free"] == "true"
            val cost = opt.details["cost"] ?: ""
            val costMax = opt.details["cost_max"] ?: ""
            val tier = opt.details["price_tier"] ?: ""
            val rating = opt.details["rating"] ?: ""
            listOfNotNull(
                when {
                    isFree -> "Free"
                    cost.isNotBlank() && costMax.isNotBlank() -> "\$$cost–\$$costMax"
                    cost.isNotBlank() -> "\$$cost"
                    tier.isNotBlank() -> tier
                    else -> null
                },
                if (rating.isNotBlank()) "★$rating" else null
            ).joinToString(" · ")
        }
    }
}

private fun optionName(event: TravelEvent, opt: EventOption): String {
    return when (event.type.lowercase()) {
        "flight" -> listOfNotNull(
            opt.details["title"]?.takeIf { it.isNotBlank() },
            listOfNotNull(
                opt.details["airline"]?.takeIf { it.isNotBlank() },
                opt.details["flight_number"]?.takeIf { it.isNotBlank() }
            ).joinToString(" ").takeIf { it.isNotBlank() }
        ).firstOrNull()

        "hotel" -> opt.details["hotel_name"] ?: opt.details["name"]
        "restaurant", "dining", "food" -> opt.details["restaurant_name"] ?: opt.details["name"]
        else -> opt.details["activity_name"] ?: opt.details["title"] ?: opt.details["name"]
    } ?: "Option"
}

private fun eventTypeColor(type: String): Color = when (type.lowercase()) {
    "flight" -> BlueAccent
    "hotel" -> TertiaryPurple
    "restaurant", "dining", "food" -> ErrorRed
    else -> Color(0xFFD5E3FB)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventOptionsPanel(
    event: TravelEvent,
    options: List<EventOption>,
    rejectedIds: Set<String>,
    onSelect: (optionId: String) -> Unit,
    onReject: (optionId: String) -> Unit,
    onDismiss: () -> Unit,
    selectedNamesElsewhere: Set<String> = emptySet()
) {
    val typeColor = eventTypeColor(event.type)
    val activeOptions = options.filter { opt ->
        opt.optionId !in rejectedIds && !opt.selected
    }
    val rejectedOptions = options.filter { it.optionId in rejectedIds }
    var rejectedExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceHigh,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(OutlineVar)
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = typeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = event.type.uppercase(),
                        color = typeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Choose an option",
                    color = OnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(color = OutlineVar.copy(alpha = 0.3f))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Active options
                if (activeOptions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No alternative options left for this slot.",
                                color = OnSurfaceVar,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    items(activeOptions, key = { it.optionId }) { opt ->
                        OptionRow(
                            event = event,
                            opt = opt,
                            typeColor = typeColor,
                            isSelected = opt.selected,
                            isRejected = false,
                            isChosenElsewhere = optionName(event, opt) in selectedNamesElsewhere,
                            onSelect = {
                                onSelect(opt.optionId)
                                onDismiss()
                            },
                            onReject = { onReject(opt.optionId) }
                        )
                    }
                }

                // Previously Rejected accordion
                if (rejectedOptions.isNotEmpty()) {
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            color = OutlineVar.copy(alpha = 0.25f)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { rejectedExpanded = !rejectedExpanded }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Previously Rejected (${rejectedOptions.size})",
                                color = OnSurfaceVar,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (rejectedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = OnSurfaceVar,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (rejectedExpanded) {
                        items(rejectedOptions, key = { "rej_${it.optionId}" }) { opt ->
                            OptionRow(
                                event = event,
                                opt = opt,
                                typeColor = typeColor.copy(alpha = 0.5f),
                                isSelected = false,
                                isRejected = true,
                                isChosenElsewhere = optionName(event, opt) in selectedNamesElsewhere,
                                onSelect = {
                                    onSelect(opt.optionId)
                                    onDismiss()
                                },
                                onReject = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionRow(
    event: TravelEvent,
    opt: EventOption,
    typeColor: Color,
    isSelected: Boolean,
    isRejected: Boolean,
    isChosenElsewhere: Boolean,
    onSelect: () -> Unit,
    onReject: () -> Unit
) {
    val imageSource = opt.localImagePath.ifBlank { opt.imageUrl }
    val subtitle = formatOptionSubtitle(event, opt)
    val name = optionName(event, opt)
    val chosenElsewhereLabel = when (event.type.lowercase()) {
        "restaurant", "dining", "food", "activity" -> "Chosen on another day"
        else -> "Chosen elsewhere"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceHighest),
            contentAlignment = Alignment.Center
        ) {
            if (imageSource.isNotBlank()) {
                AsyncImage(
                    model = imageSource,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = event.type.take(1).uppercase(),
                    color = typeColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name + subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = if (isRejected) OnSurfaceVar else OnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (isChosenElsewhere) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = typeColor.copy(alpha = if (isRejected) 0.10f else 0.18f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = chosenElsewhereLabel,
                        color = if (isRejected) OnSurfaceVar else typeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = if (isRejected) OnSurfaceVar.copy(alpha = 0.6f) else OnSurfaceVar,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Selected check or reject button
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(typeColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = typeColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else if (!isRejected) {
            TextButton(
                onClick = onReject,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "Skip",
                    color = OnSurfaceVar.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
