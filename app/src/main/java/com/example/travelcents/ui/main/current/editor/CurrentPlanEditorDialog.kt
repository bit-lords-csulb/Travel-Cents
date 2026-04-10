package com.example.travelcents.ui.main.current

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import java.util.Calendar
import java.util.Locale

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
    var reviewsExpanded by remember { mutableStateOf(false) }

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
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = DeepSea2,
        unfocusedContainerColor = DeepSea2,
        focusedTextColor = DeepSea5,
        unfocusedTextColor = DeepSea5,
        focusedBorderColor = DeepSea3,
        unfocusedBorderColor = DeepSea3.copy(alpha = 0.7f),
        focusedLabelColor = DeepSea5,
        unfocusedLabelColor = DeepSea4,
        cursorColor = DeepSea5
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = DeepSea1
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Close", color = DeepSea4) }
                    Text(
                        text = if (initialPlan.eventId == null) "ADD PLAN" else "EDIT PLAN",
                        color = DeepSea5,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.3.sp
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Spacer(modifier = Modifier.height(18.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        validationMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                    placeholder = { Text("e.g. Scuba Diving") },
                    singleLine = true,
                    colors = textFieldColors,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlanPickerField(
                        modifier = Modifier.weight(1f),
                        label = "Date",
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
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlanPickerField(
                        modifier = Modifier.weight(1f),
                        label = "Start Time",
                        value = time,
                        placeholder = "Select time",
                        icon = Icons.Default.AccessTime,
                        onClick = {
                            showPlanTimePicker(context, time) {
                                time = it
                                validationMessage = null
                            }
                        }
                    )
                    PlanPickerField(
                        modifier = Modifier.weight(1f),
                        label = "End Time",
                        value = endTime,
                        placeholder = "Optional",
                        icon = Icons.Default.AccessTime,
                        onClick = {
                            showPlanTimePicker(context, endTime.ifBlank { plusMinutes(time, 60) }) {
                                endTime = it
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("TIME ZONE", color = DeepSea4, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTimeZoneLabel(
                        timeZoneId = timeZoneId,
                        date = date.ifBlank { todayIsoDate() },
                        time = time.ifBlank { "12:00 PM" }
                    ),
                    color = DeepSea5,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Location") },
                    placeholder = { Text("Beach, resort, restaurant...") },
                    singleLine = true,
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(14.dp))

                when (planType) {
                    "flight" -> {
                        PlanDetailFields(
                            textFieldColors = textFieldColors,
                            fields = listOf(
                                PlanDetailField("Airline", airline, { airline = it }, "Delta"),
                                PlanDetailField("Flight Number", flightNumber, { flightNumber = it }, "DL 123"),
                                PlanDetailField("Origin Airport", originAirport, { originAirport = it }, "LAX"),
                                PlanDetailField("Destination Airport", destinationAirport, { destinationAirport = it }, "JFK"),
                                PlanDetailField("Total Price", totalPrice, { totalPrice = it }, "399"),
                                PlanDetailField("Trip Segment", tripSegment, { tripSegment = it }, "outbound / return")
                            )
                        )
                    }
                    "hotel" -> {
                        PlanDetailFields(
                            textFieldColors = textFieldColors,
                            fields = listOf(
                                PlanDetailField("Hotel Name", hotelName, { hotelName = it }, "The Carlyle"),
                                PlanDetailField("Address", hotelAddress, { hotelAddress = it }, "35 E 76th St"),
                                PlanDetailField("Rating", hotelRating, { hotelRating = it }, "4.7"),
                                PlanDetailField("Check-In", checkIn, { checkIn = it }, "3:00 PM"),
                                PlanDetailField("Check-Out", checkOut, { checkOut = it }, "11:00 AM")
                            )
                        )
                    }
                    "restaurant", "dining", "food" -> {
                        PlanDetailFields(
                            textFieldColors = textFieldColors,
                            fields = listOf(
                                PlanDetailField("Restaurant Name", restaurantName, { restaurantName = it }, "Via Carota"),
                                PlanDetailField("Cuisine", cuisine, { cuisine = it }, "Italian")
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    label = { Text("Notes") },
                    placeholder = { Text("Add booking notes or reminders") },
                    maxLines = 5,
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(18.dp))
                Text("LABEL COLOR", color = DeepSea4, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colorOptions.forEach { (key, color) ->
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (key == colorKey) 2.dp else 1.dp,
                                    color = if (key == colorKey) DeepSea5 else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorKey = key }
                        )
                    }
                }

                if (initialPlan.existingDetails["yelp_id"].orEmpty().isNotBlank()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    YelpReviewsSection(
                        reviews = yelpReviews,
                        loading = reviewsLoading,
                        expanded = reviewsExpanded,
                        onToggle = { reviewsExpanded = !reviewsExpanded }
                    )
                }

                validationMessage?.let { message ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = message, color = Color(0xFFFFB4C7), fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (canShowAlternatives) {
                        Button(
                            onClick = onAlternatives,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepSea2, contentColor = DeepSea5)
                        ) {
                            Text("Alternatives", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (initialPlan.eventId != null) {
                        Button(
                            onClick = { onDelete(initialPlan) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3B2637),
                                contentColor = Color(0xFFFFB4C7)
                            )
                        ) {
                            Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Button(
                        onClick = {
                            when {
                                title.isBlank() -> validationMessage = "A title is required."
                                date.isBlank() -> validationMessage = "Select a date for this plan."
                                time.isBlank() -> validationMessage = "Select a time for this plan."
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
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepSea3, contentColor = DeepSea5)
                    ) {
                        Text(
                            text = if (initialPlan.eventId == null) "Save" else "Update",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanDetailFields(
    textFieldColors: androidx.compose.material3.TextFieldColors,
    fields: List<PlanDetailField>
) {
    fields.forEach { field ->
        OutlinedTextField(
            value = field.value,
            onValueChange = field.onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(field.label) },
            placeholder = { Text(field.placeholder) },
            singleLine = true,
            colors = textFieldColors
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun YelpReviewsSection(
    reviews: List<YelpReview>,
    loading: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        color = DeepSea2,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Yelp Reviews",
                    color = DeepSea5,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (expanded) "Hide" else "Show",
                    color = DeepSea4,
                    fontSize = 12.sp
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                when {
                    loading -> Text("Loading reviews...", color = DeepSea4, fontSize = 12.sp)
                    reviews.isEmpty() -> Text("No reviews available.", color = DeepSea4, fontSize = 12.sp)
                    else -> reviews.forEach { review ->
                        Surface(
                            color = DeepSea1,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = review.user?.name ?: "Reviewer",
                                    color = DeepSea5,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Rating ${review.rating}/5", color = DeepSea4, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = review.text,
                                    color = DeepSea5,
                                    fontSize = 12.sp,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanPickerField(
    modifier: Modifier,
    label: String,
    value: String,
    placeholder: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(modifier = modifier) {
        Text(text = label, color = DeepSea4, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DeepSea2)
                .border(1.dp, DeepSea3.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value.ifBlank { placeholder },
                color = if (value.isBlank()) DeepSea4.copy(alpha = 0.65f) else DeepSea5,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = label,
                tint = DeepSea4,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

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
