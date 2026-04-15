package com.example.travelcents.ui.main.itinerary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.YelpReview
import com.example.travelcents.ui.modules.galleryPhotoModels

private val SurfaceCard = Color(0xFF0B203D)
private val SurfaceHighest = Color(0xFF102645)
private val BlueAccent = Color(0xFF64B5F6)
private val OnSurface = Color(0xFFDBE6FF)
private val OnSurfaceVar = Color(0xFF9EABC8)
private val OutlineVar = Color(0xFF3B4861)
private val ErrorRed = Color(0xFFFF716C)
private val TertiaryPurple = Color(0xFFB5A0FF)
private val GreenAccent = Color(0xFF81C784)

private fun expandedTypeColor(type: String): Color = when (type.lowercase()) {
    "flight" -> BlueAccent
    "hotel" -> TertiaryPurple
    "restaurant", "dining", "food" -> ErrorRed
    else -> Color(0xFFD5E3FB)
}

private fun formatFlightDuration(minStr: String?): String {
    val min = minStr?.toIntOrNull() ?: return "—"
    val h = min / 60
    val m = min % 60
    return if (m == 0) "${h}h" else "${h}h ${m}m"
}

@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedEventCard(
    event: TravelEvent,
    yelpReviews: List<YelpReview>,
    isLoadingReviews: Boolean,
    onChangeClick: () -> Unit,
    onDismiss: () -> Unit,
    onSaveEdits: (title: String, startTime: String, notes: String) -> Unit
) {
    val typeColor = expandedTypeColor(event.type)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val photos = remember(event) { event.galleryPhotoModels() }
    var showGallery by remember { mutableStateOf(false) }

    // Editable fields pre-filled from event
    val existingTitle = when (event.type.lowercase()) {
        "hotel" -> event.details["hotel_name"] ?: event.details["title"] ?: event.details["name"]
        "restaurant", "dining", "food" -> event.details["restaurant_name"] ?: event.details["title"] ?: event.details["name"]
        else -> event.details["title"]
            ?: event.details["activity_name"]
            ?: event.details["restaurant_name"]
            ?: event.details["name"]
    } ?: event.type.replaceFirstChar { it.uppercase() }
    val existingNotes = event.details["description"] ?: event.details["notes"] ?: ""

    var editTitle by remember(event.eventId, existingTitle) { mutableStateOf(existingTitle) }
    var editTime by remember(event.eventId, event.startTime) { mutableStateOf(event.startTime) }
    var editNotes by remember(event.eventId, existingNotes) { mutableStateOf(existingNotes) }
    var isEditing by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    ModalBottomSheet(
        onDismissRequest = {
            if (isEditing) onSaveEdits(editTitle, editTime, editNotes)
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = SurfaceCard,
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Hero image
            Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                if (photos.isNotEmpty()) {
                    AsyncImage(
                        model = photos.first(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(SurfaceHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = event.type.take(1).uppercase(),
                            color = typeColor,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                // Overlay: close/gallery icon on the left, edit + change on the right
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (photos.size > 1) {
                        IconButton(
                            onClick = { showGallery = true },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.GridView, contentDescription = "View photos", tint = Color.White)
                        }
                    } else {
                        IconButton(
                            onClick = {
                                if (isEditing) onSaveEdits(editTitle, editTime, editNotes)
                                onDismiss()
                            },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { isEditing = !isEditing },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                        }
                        Button(
                            onClick = onChangeClick,
                            colors = ButtonDefaults.buttonColors(containerColor = typeColor),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Change", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Scrollable detail content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
                    .navigationBarsPadding()
            ) {
                // Type badge + time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = typeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(100.dp)
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
                    if (isEditing) {
                        InlineEditField(
                            value = editTime,
                            onValueChange = { editTime = it },
                            placeholder = "HH:MM",
                            fontSize = 11.sp,
                            color = OnSurfaceVar
                        )
                    } else {
                        Text(text = event.startTime, color = OnSurfaceVar, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                if (isEditing) {
                    InlineEditField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        placeholder = "Event title",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                } else {
                    Text(text = editTitle, color = OnSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = OutlineVar.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

                // Type-specific details
                when (event.type.lowercase()) {
                    "flight" -> FlightDetails(event)
                    "hotel" -> HotelDetails(event)
                    "restaurant", "dining", "food" -> RestaurantDetails(event, yelpReviews, isLoadingReviews, uriHandler)
                    else -> ActivityDetails(event, yelpReviews, isLoadingReviews, uriHandler)
                }

                // Notes (editable)
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = OutlineVar.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "NOTES", color = OnSurfaceVar.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(6.dp))
                if (isEditing) {
                    InlineEditField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        placeholder = "Add notes…",
                        fontSize = 13.sp,
                        color = OnSurfaceVar,
                        multiline = true
                    )
                } else {
                    Text(
                        text = editNotes.ifBlank { "No notes." },
                        color = if (editNotes.isBlank()) OnSurfaceVar.copy(alpha = 0.4f) else OnSurfaceVar,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }

                if (isEditing) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            onSaveEdits(editTitle, editTime, editNotes)
                            isEditing = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save changes", fontWeight = FontWeight.Bold, color = Color(0xFF010E24))
                    }
                }
            }
        }
    }

    // Full-screen photo gallery overlay
    if (showGallery && photos.size > 1) {
        val pagerState = rememberPagerState(pageCount = { photos.size })
        Dialog(
            onDismissRequest = { showGallery = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    AsyncImage(
                        model = photos[page],
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                // Close button (top-end)
                IconButton(
                    onClick = { showGallery = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close gallery", tint = Color.White)
                }
                // Page counter (bottom-center)
                Text(
                    text = "${pagerState.currentPage + 1} / ${photos.size}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ── Inline editable text field ────────────────────────────────────────────────

@Composable
private fun InlineEditField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    color: Color,
    fontWeight: FontWeight = FontWeight.Normal,
    multiline: Boolean = false
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = !multiline,
        textStyle = TextStyle(color = color, fontSize = fontSize, fontWeight = fontWeight),
        cursorBrush = SolidColor(BlueAccent),
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .background(BlueAccent.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .fillMaxWidth()
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, color = color.copy(alpha = 0.4f), fontSize = fontSize)
                }
                inner()
            }
        }
    )
}

// ── Type-specific detail composables ─────────────────────────────────────────

@Composable
private fun FlightDetails(event: TravelEvent) {
    val d = event.details
    val carbonDiff = d["carbon_emissions_diff_pct"] ?: d["carbon_diff_percent"]
    DetailGroup("FLIGHT INFO") {
        DetailRow("Airline", d["airline"] ?: "—")
        DetailRow("Flight", d["flight_number"] ?: "—")
        DetailRow("Route", "${d["origin_airport"] ?: "—"} → ${d["destination_airport"] ?: "—"}")
        DetailRow("Departs", event.startTime.ifBlank { d["departure_time"] ?: "—" })
        DetailRow("Arrives", buildString {
            val arrivalDate = d["arrival_date"]
            if (!arrivalDate.isNullOrBlank() && arrivalDate != event.date) {
                append(arrivalDate)
                append("  ")
            }
            append(d["arrival_time"] ?: event.endTime.ifBlank { "—" })
        })
        DetailRow("Duration", formatFlightDuration(d["flight_duration_min"] ?: d["total_duration"]))
        DetailRow("Aircraft", d["aircraft"] ?: "—")
        DetailRow("Cabin", d["cabin_class"] ?: "Economy")
        if (d["legroom"] != null) DetailRow("Legroom", d["legroom"]!!)
        if (d["wifi"] == "true") DetailRow("Wi-Fi", "Available")
        if (d["power"] == "true") DetailRow("Power outlet", "Available")
        if (d["price_level"] != null) PriceLevelBadge(d["price_level"]!!)
    }
    DetailGroup("PRICE") {
        DetailRow("Total (round-trip)", d["total_price"]?.let { "\$$it" } ?: "—")
        if (carbonDiff != null) {
            val pct = carbonDiff.toIntOrNull() ?: 0
            DetailRow("Carbon vs. typical", "${if (pct >= 0) "+$pct" else "$pct"}%",
                valueColor = if (pct < 0) GreenAccent else ErrorRed)
        }
    }
}

@Composable
private fun HotelDetails(event: TravelEvent) {
    val d = event.details
    DetailGroup("HOTEL INFO") {
        DetailRow("Name", d["hotel_name"] ?: d["title"] ?: "—")
        if (d["hotel_class"] != null) DetailRow("Class", "${"★".repeat((d["hotel_class"]!!.toIntOrNull() ?: 0).coerceIn(0, 5))} ${d["hotel_class"]}-Star")
        DetailRow("Rating", d["overall_rating"]?.let { "★$it" } ?: "—")
        DetailRow("Reviews", d["reviews"] ?: "—")
        if (d["eco_certified"] == "true") DetailRow("Eco certified", "Yes", valueColor = GreenAccent)
        if (d["check_in_time"] != null) DetailRow("Check-in", d["check_in_time"]!!)
        if (d["check_out_time"] != null) DetailRow("Check-out", d["check_out_time"]!!)
    }
    DetailGroup("PRICING") {
        DetailRow("Per room/night", d["rate_per_night"]?.let { "\$$it" } ?: "—")
        if (d["rooms_needed"] != null && (d["rooms_needed"]!!.toIntOrNull() ?: 0) > 1) {
            DetailRow("Rooms needed", d["rooms_needed"]!!)
            DetailRow("Group total/night", d["group_rate_per_night"]?.let { "\$$it" } ?: "—")
        }
        if (d["deal"] != null) DetailRow("Deal", d["deal"]!!, valueColor = GreenAccent)
        if (d["booking_url"] != null) {
            val uriHandler = LocalUriHandler.current
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { uriHandler.openUri(d["booking_url"]!!) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BlueAccent),
                border = androidx.compose.foundation.BorderStroke(1.dp, BlueAccent.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Book now", fontSize = 12.sp)
            }
        }
    }
    if (d["amenities"] != null) {
        DetailGroup("AMENITIES") {
            Text(text = d["amenities"]!!, color = OnSurfaceVar, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun RestaurantDetails(
    event: TravelEvent,
    reviews: List<YelpReview>,
    isLoadingReviews: Boolean,
    uriHandler: androidx.compose.ui.platform.UriHandler
) {
    val d = event.details
    DetailGroup("RESTAURANT INFO") {
        DetailRow("Price", d["price_tier"] ?: "Not listed")
        DetailRow("Rating", d["rating"]?.let { "★$it" } ?: "—")
        DetailRow("Reviews", d["review_count"] ?: "—")
        if (d["address"] != null) DetailRow("Address", d["address"]!!)
        if (d["categories"] != null) DetailRow("Cuisine", d["categories"]!!)
        if (d["phone"] != null) DetailRow("Phone", d["phone"]!!)
        if (d["delivery"] == "true") DetailRow("Delivery", "Available", valueColor = GreenAccent)
        if (d["yelp_menu_url"] != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "View Menu →",
                color = BlueAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { uriHandler.openUri(d["yelp_menu_url"]!!) }
            )
        }
    }
    ReviewsSection(reviews, isLoadingReviews)
}

@Composable
private fun ActivityDetails(
    event: TravelEvent,
    reviews: List<YelpReview>,
    isLoadingReviews: Boolean,
    uriHandler: androidx.compose.ui.platform.UriHandler
) {
    val d = event.details
    DetailGroup("ACTIVITY INFO") {
        val isFree = d["is_free"] == "true"
        val cost = d["cost"] ?: ""
        val costMax = d["cost_max"] ?: ""
        DetailRow("Price", when {
            isFree -> "Free"
            cost.isNotBlank() && costMax.isNotBlank() -> "\$$cost – \$$costMax"
            cost.isNotBlank() -> "\$$cost"
            d["price_tier"] != null -> d["price_tier"]!!
            else -> "—"
        })
        if (d["rating"] != null) DetailRow("Rating", "★${d["rating"]}")
        if (d["review_count"] != null) DetailRow("Reviews", d["review_count"]!!)
        if (d["address"] != null) DetailRow("Address", d["address"]!!)
        if (d["categories"] != null) DetailRow("Categories", d["categories"]!!)
        if (d["tickets_url"] != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Get Tickets →",
                color = BlueAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { uriHandler.openUri(d["tickets_url"]!!) }
            )
        }
    }
    ReviewsSection(reviews, isLoadingReviews)
}

@Composable
private fun ReviewsSection(reviews: List<YelpReview>, isLoading: Boolean) {
    if (isLoading) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = BlueAccent,
                strokeWidth = 2.dp
            )
            Text("Loading reviews…", color = OnSurfaceVar, fontSize = 12.sp)
        }
    } else if (reviews.isNotEmpty()) {
        DetailGroup("REVIEWS") {
            reviews.forEach { review ->
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = review.user?.name ?: "Guest",
                            color = OnSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "★".repeat(review.rating),
                            color = Color(0xFFFFD54F),
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = review.timeCreated.take(10),
                            color = OnSurfaceVar.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = review.text,
                        color = OnSurfaceVar,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

// ── Reusable detail layout helpers ────────────────────────────────────────────

@Composable
private fun DetailGroup(label: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        text = label,
        color = OnSurfaceVar.copy(alpha = 0.6f),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp
    )
    Spacer(modifier = Modifier.height(8.dp))
    Column(content = content)
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = OnSurface) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = OnSurfaceVar,
            fontSize = 12.sp,
            modifier = Modifier.width(120.dp)
        )
        Text(text = value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PriceLevelBadge(level: String) {
    val (text, color) = when (level.lowercase()) {
        "low" -> "Low price" to GreenAccent
        "high" -> "High price" to ErrorRed
        else -> "Typical price" to OnSurfaceVar
    }
    Spacer(modifier = Modifier.height(4.dp))
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

