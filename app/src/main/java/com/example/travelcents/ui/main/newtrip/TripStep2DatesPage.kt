package com.example.travelcents.ui.main.newtrip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea5
import java.util.Calendar

private val S2Blue = Color(0xFF64B5F6)
private val S2ContainerHigh = Color(0xFF0B203D)
private val S2ContainerHighest = Color(0xFF102645)
private val S2ContainerLow = Color(0xFF02132B)
private val S2SurfaceBright = Color(0xFF152C4E)
private val S2OnSurfaceVariant = Color(0xFF9EABC8)
private val S2SecondaryContainer = Color(0xFF3A485B)

private data class CalDate(val year: Int, val month: Int, val day: Int) : Comparable<CalDate> {
    override fun compareTo(other: CalDate) =
        compareValuesBy(this, other, CalDate::year, CalDate::month, CalDate::day)
    fun toYMD() = "%04d-%02d-%02d".format(year, month + 1, day)
}

private fun String.toCalDate(): CalDate? {
    if (isBlank()) return null
    return try {
        val p = split("-")
        CalDate(p[0].toInt(), p[1].toInt() - 1, p[2].toInt())
    } catch (e: Exception) { null }
}

private val monthNames = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)
private val shortMonthNames = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)
private val dayHeaders = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

@Composable
fun TripStep2DatesPage(
    modifier: Modifier = Modifier,
    viewModel: NewTripViewModel,
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    val now = remember { Calendar.getInstance() }
    var displayYear by remember { mutableStateOf(now.get(Calendar.YEAR)) }
    var displayMonth by remember { mutableStateOf(now.get(Calendar.MONTH)) }
    var startDate by remember { mutableStateOf(viewModel.dateFrom.toCalDate()) }
    var endDate by remember { mutableStateOf(viewModel.dateTo.toCalDate()) }

    fun syncDates() {
        viewModel.dateFrom = startDate?.toYMD() ?: ""
        viewModel.dateTo = endDate?.toYMD() ?: ""
    }

    fun onDayClick(day: Int) {
        val tapped = CalDate(displayYear, displayMonth, day)
        when {
            startDate == null -> { startDate = tapped; endDate = null }
            endDate == null -> {
                when {
                    tapped == startDate -> { startDate = null; endDate = null }
                    tapped > startDate!! -> { endDate = tapped }
                    else -> { startDate = tapped; endDate = null }
                }
            }
            else -> { startDate = tapped; endDate = null }
        }
        syncDates()
    }

    fun prevMonth() {
        if (displayMonth == 0) { displayMonth = 11; displayYear-- }
        else displayMonth--
    }

    fun nextMonth() {
        if (displayMonth == 11) { displayMonth = 0; displayYear++ }
        else displayMonth++
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSea1)
    ) {
        // Top bar
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF010E24))) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = S2Blue)
                    }
                    Text("Step 2 of 5", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DeepSea5)
                }
                IconButton(onClick = onCloseClick) {
                    Icon(Icons.Default.Close, "Close", tint = S2Blue)
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(S2ContainerHigh)) {
                Box(
                    modifier = Modifier.fillMaxWidth(0.4f).height(3.dp)
                        .background(Brush.horizontalGradient(listOf(S2Blue, S2Blue.copy(alpha = 0.7f))))
                )
            }
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Progress widget
            S2ProgressWidget(stepsComplete = 2)
            Spacer(Modifier.height(20.dp))

            // Hero
            Text(
                text = "When are you traveling?",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepSea5,
                letterSpacing = (-1).sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Select your dates and TravelCents will optimize your daily schedule.",
                fontSize = 14.sp,
                color = S2OnSurfaceVariant,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(20.dp))

            // Calendar card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(S2ContainerLow)
                    .padding(20.dp)
            ) {
                Column {
                    // Month header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${monthNames[displayMonth]} $displayYear",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepSea5
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(S2ContainerHighest, RoundedCornerShape(8.dp))
                                    .clickable { prevMonth() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ChevronLeft, null, tint = S2Blue, modifier = Modifier.size(20.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(S2ContainerHighest, RoundedCornerShape(8.dp))
                                    .clickable { nextMonth() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ChevronRight, null, tint = S2Blue, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    // Day-of-week headers
                    Row(modifier = Modifier.fillMaxWidth()) {
                        dayHeaders.forEach { label ->
                            Text(
                                text = label,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = S2OnSurfaceVariant.copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))

                    // Calendar cells
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, displayYear)
                        set(Calendar.MONTH, displayMonth)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    val firstDow = cal.get(Calendar.DAY_OF_WEEK) - 1
                    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val cells: List<Int?> = List(firstDow) { null } + (1..daysInMonth).map { it }
                    val padded = cells + List((7 - cells.size % 7) % 7) { null }
                    val weeks = padded.chunked(7)

                    weeks.forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            week.forEach { day ->
                                if (day == null) {
                                    Spacer(Modifier.weight(1f).height(40.dp))
                                } else {
                                    val current = CalDate(displayYear, displayMonth, day)
                                    val isStart = startDate == current
                                    val isEnd = endDate == current
                                    val hasRange = startDate != null && endDate != null
                                    val isInRange = hasRange && current > startDate!! && current < endDate!!
                                    S2DayCell(
                                        day = day,
                                        isStart = isStart,
                                        isEnd = isEnd,
                                        isInRange = isInRange,
                                        hasRange = hasRange,
                                        onClick = { onDayClick(day) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Summary box (shown when at least start date is selected)
            if (startDate != null) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(S2SecondaryContainer.copy(alpha = 0.3f))
                        .border(1.dp, S2ContainerHighest.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(S2Blue.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CalendarToday, null, tint = S2Blue, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(
                                "SELECTED DURATION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = S2OnSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            val s = startDate!!
                            val label = if (endDate != null) {
                                val e = endDate!!
                                val cal1 = Calendar.getInstance().apply { set(s.year, s.month, s.day, 0, 0, 0) }
                                val cal2 = Calendar.getInstance().apply { set(e.year, e.month, e.day, 0, 0, 0) }
                                val nights = ((cal2.timeInMillis - cal1.timeInMillis) / 86400000L).toInt()
                                "${shortMonthNames[s.month]} ${s.day} – ${shortMonthNames[e.month]} ${e.day}  ($nights night${if (nights != 1) "s" else ""})"
                            } else {
                                "${shortMonthNames[s.month]} ${s.day}  (tap end date)"
                            }
                            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DeepSea5)
                        }
                    }
                    Text(
                        "Clear",
                        color = S2Blue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { startDate = null; endDate = null; syncDates() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // Continue button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepSea1)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = onContinueClick,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = startDate != null && endDate != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = S2Blue,
                    contentColor = Color(0xFF001627),
                    disabledContainerColor = S2Blue.copy(alpha = 0.25f),
                    disabledContentColor = DeepSea5.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Continue to Travelers", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun S2ProgressWidget(stepsComplete: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(S2ContainerHigh.copy(alpha = 0.5f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("YOUR PROGRESS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = S2OnSurfaceVariant, letterSpacing = 1.5.sp)
                Box(
                    modifier = Modifier
                        .background(S2Blue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .border(1.dp, S2Blue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("${stepsComplete * 20}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = S2Blue)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(5) { i ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (i < stepsComplete) S2Blue else S2ContainerHighest)
                    )
                }
            }
        }
    }
}

@Composable
private fun S2DayCell(
    day: Int,
    isStart: Boolean,
    isEnd: Boolean,
    isInRange: Boolean,
    hasRange: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rangeColor = S2Blue.copy(alpha = 0.2f)

    Box(
        modifier = modifier
            .height(40.dp)
            .clickable { onClick() }
    ) {
        // Range tint background
        if (isStart && hasRange) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .align(Alignment.CenterEnd)
                    .background(rangeColor)
            )
        }
        if (isEnd && hasRange) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .align(Alignment.CenterStart)
                    .background(rangeColor)
            )
        }
        if (isInRange) {
            Box(modifier = Modifier.fillMaxSize().background(rangeColor))
        }

        // Day circle / number
        Box(
            modifier = Modifier
                .size(34.dp)
                .align(Alignment.Center)
                .then(
                    if (isStart || isEnd)
                        Modifier.background(S2Blue, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.toString(),
                fontSize = 13.sp,
                fontWeight = if (isStart || isEnd) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isStart || isEnd -> Color(0xFF001627)
                    isInRange -> DeepSea5
                    else -> DeepSea5.copy(alpha = 0.85f)
                },
                textAlign = TextAlign.Center
            )
        }
    }
}
