package com.example.travelcents.ui.main.current

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.travelcents.data.model.EventOption
import com.example.travelcents.data.model.YelpReview
import com.example.travelcents.ui.modules.defaultPlanTimeZoneId
import com.example.travelcents.ui.modules.formatDisplayTime
import com.example.travelcents.ui.modules.formatTimeZoneLabel
import com.example.travelcents.ui.modules.normalizeDate
import com.example.travelcents.ui.modules.parseFlexibleTime
import com.example.travelcents.ui.modules.parseIsoDate
import com.example.travelcents.ui.modules.plusMinutes
import com.example.travelcents.ui.modules.todayIsoDate
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import java.util.Calendar
import java.util.Locale

// Design tokens matching the HTML spec
private val EditorBg = Color(0xFF0B1326)
private val SurfaceLowest = Color(0xFF060E20)
private val SurfaceContainerHigh = Color(0xFF222A3D)
private val SurfaceContainerHighest = Color(0xFF2D3449)
private val PrimaryAccent = Color(0xFFCEBDFF)
private val PrimaryContainer = Color(0xFFA78BFA)
private val OnSurface = Color(0xFFDAE2FD)
private val OnSurfaceVariant = Color(0xFFCAC4D4)
private val ErrorContainer = Color(0xFF93000A)
private val OnErrorContainer = Color(0xFFFFDAD6)

private val GradientAccent = Brush.linearGradient(listOf(PrimaryAccent, PrimaryContainer))

private data class PlanDetailField(
    val label: String,
    val value: String,
    val onValueChange: (String) -> Unit,
    val placeholder: String
)

@Composable
fun CurrentPlanEditorDialog(
    initialPlan: EditablePlan,
    currentOptions: List<EventOption>,
    yelpReviews: List<YelpReview>,
    reviewsLoading: Boolean,
    onDismiss: () -> Unit,
    onSave: (EditablePlan) -> Unit,
    onDelete: (EditablePlan) -> Unit,
    onAlternatives: () -> Unit
) {
    val context = LocalContext.current
    var planType by remember(initialPlan) { mutableStateOf(initialPlan.type.lowercase(Locale.US).ifBlank { "activity" }) }
    var title by remember(initialPlan) { mutableStateOf(initialPlan.title) }
    var date by remember(initialPlan) { mutableStateOf(normalizeDate(initialPlan.date)) }
    var time by remember(initialPlan) { mutableStateOf(formatDisplayTime(initialPlan.startTime)) }
    var endTime by remember(initialPlan) {
        mutableStateOf(initialPlan.endTime.takeIf { it.isNotBlank() }?.let(::formatDisplayTime).orEmpty())
    }
    var location by remember(initialPlan) { mutableStateOf(initialPlan.location) }
    var notes by remember(initialPlan) { mutableStateOf(initialPlan.notes) }
    var colorKey by remember(initialPlan) { mutableStateOf(initialPlan.colorKey.ifBlank { "rose" }) }
    val timeZoneId = remember(initialPlan) { initialPlan.timeZoneId.ifBlank { defaultPlanTimeZoneId() } }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    var airline by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["airline"].orEmpty()) }
    var flightNumber by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["flight_number"].orEmpty()) }
    var originAirport by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["origin_airport"].orEmpty()) }
    var destinationAirport by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["destination_airport"].orEmpty()) }
    var totalPrice by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["total_price"].orEmpty()) }
    var tripSegment by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["trip_segment"].orEmpty()) }
    var hotelName by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["hotel_name"].orEmpty()) }
    var hotelAddress by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["address"].orEmpty()) }
    var hotelRating by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["rating"].orEmpty()) }
    var checkIn by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["check_in"].orEmpty()) }
    var checkOut by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["check_out"].orEmpty()) }
    var restaurantName by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["restaurant_name"].orEmpty()) }
    var cuisine by remember(initialPlan) { mutableStateOf(initialPlan.existingDetails["cuisine"].orEmpty()) }

    val canShowAlternatives = initialPlan.eventId != null && currentOptions.size > 1
    val colorOptions = remember {
        listOf(
            "rose" to Color(0xFFFF677C),
            "teal" to Color(0xFF4CA7C5),
            "olive" to Color(0xFF8A9365),
            "plum" to Color(0xFF5A2A7B),
            "lavender" to Color(0xFF6D5D8E),
            "cyan" to Color(0xFF268C95)
        )
    }
    val activeColorName = remember(colorKey) {
        colorKey.replaceFirstChar { it.uppercase() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = EditorBg
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Top bar ──────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EditorBg)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = PrimaryAccent)
                    }
                    Text(
                        text = if (initialPlan.eventId == null) "ADD PLAN" else "EDIT EVENT",
                        color = OnSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    )
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = PrimaryAccent)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(SurfaceContainerHigh)
                        ) {
                            if (canShowAlternatives) {
                                DropdownMenuItem(
                                    text = { Text("Alternatives", color = OnSurface) },
                                    onClick = { menuExpanded = false; onAlternatives() }
                                )
                            }
                        }
                    }
                }

                // ── Scrollable content ───────────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                ) {
                    // Hero section: type label + big title + image
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = planType.uppercase(Locale.US),
                        color = PrimaryAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it.filter { c -> c != '\n' }; validationMessage = null },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = OnSurface,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 34.sp
                        ),
                        cursorBrush = SolidColor(PrimaryAccent),
                        decorationBox = { inner ->
                            Box {
                                if (title.isEmpty()) {
                                    Text(
                                        "Event title",
                                        color = OnSurfaceVariant.copy(alpha = 0.5f),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                inner()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Hero image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(192.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(SurfaceContainerHigh)
                    ) {
                        if (initialPlan.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = initialPlan.imageUrl,
                                contentDescription = title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, EditorBg.copy(alpha = 0.75f))
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Date + timezone row
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        EditorPickerField(
                            modifier = Modifier.weight(1f),
                            label = "DATE",
                            value = date,
                            placeholder = "YYYY-MM-DD",
                            icon = Icons.Default.CalendarToday,
                            onClick = {
                                showPlanDatePicker(context, date) {
                                    date = it
                                    validationMessage = null
                                }
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            EditorFieldLabel("TIME ZONE")
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceLowest)
                                    .padding(horizontal = 14.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Language,
                                    contentDescription = null,
                                    tint = PrimaryAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = formatTimeZoneLabel(
                                        timeZoneId = timeZoneId,
                                        date = date.ifBlank { todayIsoDate() },
                                        time = time.ifBlank { "12:00 PM" }
                                    ),
                                    color = OnSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Start + End time row
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        EditorPickerField(
                            modifier = Modifier.weight(1f),
                            label = "START TIME",
                            value = time,
                            placeholder = "Select time",
                            icon = Icons.Default.AccessTime,
                            onClick = {
                                showPlanTimePicker(context, time) { time = it; validationMessage = null }
                            }
                        )
                        EditorPickerField(
                            modifier = Modifier.weight(1f),
                            label = "END TIME",
                            value = endTime,
                            placeholder = "Optional",
                            icon = Icons.Default.AccessTime,
                            onClick = {
                                showPlanTimePicker(context, endTime.ifBlank { plusMinutes(time, 60) }) { endTime = it }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Location
                    EditorFieldLabel("LOCATION")
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceLowest)
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = PrimaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        EditorTextField(
                            value = location,
                            onValueChange = { location = it },
                            placeholder = "Beach, resort, restaurant...",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Map,
                            contentDescription = null,
                            tint = PrimaryAccent.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Type-specific detail fields
                    when (planType) {
                        "flight" -> PlanDetailFields(
                            fields = listOf(
                                PlanDetailField("Airline", airline, { airline = it }, "Delta"),
                                PlanDetailField("Flight Number", flightNumber, { flightNumber = it }, "DL 123"),
                                PlanDetailField("Origin Airport", originAirport, { originAirport = it }, "LAX"),
                                PlanDetailField("Destination Airport", destinationAirport, { destinationAirport = it }, "JFK"),
                                PlanDetailField("Total Price", totalPrice, { totalPrice = it }, "399"),
                                PlanDetailField("Trip Segment", tripSegment, { tripSegment = it }, "outbound / return")
                            )
                        )
                        "hotel" -> PlanDetailFields(
                            fields = listOf(
                                PlanDetailField("Hotel Name", hotelName, { hotelName = it }, "The Carlyle"),
                                PlanDetailField("Address", hotelAddress, { hotelAddress = it }, "35 E 76th St"),
                                PlanDetailField("Rating", hotelRating, { hotelRating = it }, "4.7"),
                                PlanDetailField("Check-In", checkIn, { checkIn = it }, "3:00 PM"),
                                PlanDetailField("Check-Out", checkOut, { checkOut = it }, "11:00 AM")
                            )
                        )
                        "restaurant", "dining", "food" -> PlanDetailFields(
                            fields = listOf(
                                PlanDetailField("Restaurant Name", restaurantName, { restaurantName = it }, "Via Carota"),
                                PlanDetailField("Cuisine", cuisine, { cuisine = it }, "Italian")
                            )
                        )
                    }

                    // Notes
                    EditorFieldLabel("NOTES")
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceLowest)
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        if (notes.isEmpty()) {
                            Text(
                                "Add booking notes or reminders",
                                color = OnSurfaceVariant.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            textStyle = TextStyle(color = OnSurface, fontSize = 14.sp, lineHeight = 22.sp),
                            cursorBrush = SolidColor(PrimaryAccent),
                            modifier = Modifier.fillMaxWidth().height(96.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Label color
                    EditorFieldLabel("LABEL COLOR")
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceLowest)
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        colorOptions.forEach { (key, color) ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (key == colorKey) Modifier.border(2.dp, OnSurface, CircleShape) else Modifier
                                    )
                                    .clickable { colorKey = key }
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = activeColorName,
                            color = OnSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Yelp reviews (shown always if yelp_id exists)
                    val yelpId = initialPlan.existingDetails["yelp_id"].orEmpty()
                    if (yelpId.isNotBlank()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        EditorYelpReviews(
                            rating = initialPlan.existingDetails["rating"],
                            reviews = yelpReviews,
                            loading = reviewsLoading
                        )
                    }

                    validationMessage?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(it, color = Color(0xFFFFB4C7), fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(100.dp))
                }

                // ── Sticky footer ────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EditorBg.copy(alpha = 0.9f))
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (initialPlan.eventId != null) {
                        Button(
                            onClick = { onDelete(initialPlan) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ErrorContainer,
                                contentColor = OnErrorContainer
                            ),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            Text(
                                "DELETE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .clip(RoundedCornerShape(50))
                            .background(GradientAccent)
                            .clickable {
                                when {
                                    title.isBlank() -> validationMessage = "A title is required."
                                    date.isBlank() -> validationMessage = "Select a date."
                                    time.isBlank() -> validationMessage = "Select a start time."
                                    else -> {
                                        val mergedDetails = initialPlan.existingDetails.toMutableMap().apply {
                                            applyPlanDetail("airline", airline, planType == "flight")
                                            applyPlanDetail("flight_number", flightNumber, planType == "flight")
                                            applyPlanDetail("origin_airport", originAirport, planType == "flight")
                                            applyPlanDetail("destination_airport", destinationAirport, planType == "flight")
                                            applyPlanDetail("total_price", totalPrice, planType == "flight")
                                            applyPlanDetail("trip_segment", tripSegment, planType == "flight")
                                            applyPlanDetail("hotel_name", hotelName, planType == "hotel")
                                            applyPlanDetail("address", hotelAddress, planType == "hotel")
                                            applyPlanDetail("rating", hotelRating, planType == "hotel")
                                            applyPlanDetail("check_in", checkIn, planType == "hotel")
                                            applyPlanDetail("check_out", checkOut, planType == "hotel")
                                            applyPlanDetail("restaurant_name", restaurantName.ifBlank { title.trim() }, planType == "restaurant" || planType == "dining" || planType == "food")
                                            applyPlanDetail("cuisine", cuisine, planType == "restaurant" || planType == "dining" || planType == "food")
                                            applyPlanDetail("activity_name", title.trim(), planType == "activity")
                                        }
                                        onSave(
                                            initialPlan.copy(
                                                type = planType,
                                                title = title.trim(),
                                                date = date,
                                                startTime = time,
                                                endTime = endTime,
                                                timeZoneId = timeZoneId,
                                                location = location.trim(),
                                                notes = notes.trim(),
                                                colorKey = colorKey,
                                                existingDetails = mergedDetails
                                            )
                                        )
                                    }
                                }
                            }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (initialPlan.eventId == null) "SAVE" else "UPDATE EVENT",
                            color = Color(0xFF21005E),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }
    }
}

// ── Yelp reviews section ──────────────────────────────────────────────────────

@Composable
private fun EditorYelpReviews(
    rating: String?,
    reviews: List<YelpReview>,
    loading: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Yelp Reviews",
                color = OnSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (!rating.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(rating, color = PrimaryAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    repeat(5) { i ->
                        val filled = (rating.toDoubleOrNull() ?: 0.0) >= (i + 1)
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = if (filled) PrimaryAccent else OnSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        when {
            loading -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = PrimaryAccent, strokeWidth = 2.dp)
                    Text("Loading reviews…", color = DeepSea4, fontSize = 12.sp)
                }
            }
            reviews.isEmpty() -> {
                Text("No reviews available.", color = DeepSea4, fontSize = 13.sp)
            }
            else -> reviews.forEach { review ->
                EditorReviewCard(review)
            }
        }
    }
}

@Composable
private fun EditorReviewCard(review: YelpReview) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerHigh)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = review.user?.name?.take(1)?.uppercase(Locale.US) ?: "R",
                        color = PrimaryAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(review.user?.name ?: "Reviewer", color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(review.timeCreated.take(10), color = OnSurfaceVariant, fontSize = 10.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(5) { i ->
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = if (i < review.rating) PrimaryAccent else OnSurfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        Text(review.text, color = OnSurfaceVariant, fontSize = 13.sp, lineHeight = 20.sp)
    }
}

// ── Form field helpers ────────────────────────────────────────────────────────

@Composable
private fun EditorFieldLabel(text: String) {
    Text(
        text = text,
        color = OnSurfaceVariant,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.6.sp
    )
}

@Composable
private fun EditorPickerField(
    modifier: Modifier,
    label: String,
    value: String,
    placeholder: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(modifier = modifier) {
        EditorFieldLabel(label)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceLowest)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = PrimaryAccent, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value.ifBlank { placeholder },
                color = if (value.isBlank()) OnSurfaceVariant.copy(alpha = 0.5f) else OnSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium),
        cursorBrush = SolidColor(PrimaryAccent),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        decorationBox = { inner ->
            Box(modifier = modifier) {
                if (value.isEmpty()) {
                    Text(placeholder, color = OnSurfaceVariant.copy(alpha = 0.5f), fontSize = 14.sp)
                }
                inner()
            }
        },
        modifier = modifier
    )
}

@Composable
private fun PlanDetailFields(fields: List<PlanDetailField>) {
    fields.forEach { field ->
        EditorFieldLabel(field.label.uppercase(Locale.US))
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceLowest)
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorTextField(
                value = field.value,
                onValueChange = field.onValueChange,
                placeholder = field.placeholder,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

// ── Native pickers (unchanged logic) ─────────────────────────────────────────

private fun MutableMap<String, String>.applyPlanDetail(key: String, value: String, enabled: Boolean) {
    if (!enabled || value.isBlank()) remove(key) else put(key, value.trim())
}

private fun showPlanDatePicker(
    context: android.content.Context,
    initialDate: String = "",
    onDateSelected: (String) -> Unit
) {
    val initial = parseIsoDate(initialDate)
    val calendar = Calendar.getInstance().apply {
        if (initial != null) set(initial.year, initial.monthValue - 1, initial.dayOfMonth)
    }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth -> onDateSelected("%04d-%02d-%02d".format(year, month + 1, dayOfMonth)) },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun showPlanTimePicker(
    context: android.content.Context,
    initialTime: String = "",
    onTimeSelected: (String) -> Unit
) {
    val initial = parseFlexibleTime(initialTime)
    val calendar = Calendar.getInstance().apply {
        if (initial != null) {
            set(Calendar.HOUR_OF_DAY, initial.hour)
            set(Calendar.MINUTE, initial.minute)
        }
    }
    TimePickerDialog(
        context,
        { _, hourOfDay, minute -> onTimeSelected(formatDisplayTime("%02d:%02d".format(hourOfDay, minute))) },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    ).show()
}
