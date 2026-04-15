package com.example.travelcents.ui.main.newTrip

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.ui.components.TcButton
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.theme.TravelCentsFonts
import java.util.Calendar


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
    var displayYear by remember { mutableIntStateOf(now.get(Calendar.YEAR)) }
    var displayMonth by remember { mutableIntStateOf(now.get(Calendar.MONTH)) }
    var startDate by remember { mutableStateOf(viewModel.dateFrom.toCalDate()) }
    var endDate by remember { mutableStateOf(viewModel.dateTo.toCalDate()) }
    var showMonthYearPicker by remember { mutableStateOf(false) }

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

    ProvideTextStyle(value = TextStyle(fontFamily = TravelCentsFonts.Body)) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(DeepSea1)
        ) {
        // Top bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF010E24))
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TripWizardColors.Blue)
                    }
                    Text("Step 2 of 5", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DeepSea5)
                }
                IconButton(onClick = onCloseClick) {
                    Icon(Icons.Default.Close, "Close", tint = TripWizardColors.Blue)
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(TripWizardColors.ContainerHigh)) {
                Box(
                    modifier = Modifier.fillMaxWidth(0.4f).height(3.dp)
                        .background(Brush.horizontalGradient(listOf(TripWizardColors.Blue, TripWizardColors.Blue.copy(alpha = 0.7f))))
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
                letterSpacing = (-1).sp,
                fontFamily = TravelCentsFonts.Headline
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Select your dates and TravelCents will optimize your daily schedule.",
                fontSize = 14.sp,
                color = TripWizardColors.OnSurfaceVariant,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(20.dp))

            // Calendar card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(TripWizardColors.ContainerLow)
                    .padding(20.dp)
            ) {
                Column {
                    // Month header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showMonthYearPicker = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "${monthNames[displayMonth]} $displayYear",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepSea5
                            )
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Pick month/year",
                                tint = TripWizardColors.Blue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(TripWizardColors.ContainerHighest, RoundedCornerShape(8.dp))
                                    .clickable { prevMonth() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ChevronLeft, null, tint = TripWizardColors.Blue, modifier = Modifier.size(20.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(TripWizardColors.ContainerHighest, RoundedCornerShape(8.dp))
                                    .clickable { nextMonth() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ChevronRight, null, tint = TripWizardColors.Blue, modifier = Modifier.size(20.dp))
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
                                color = TripWizardColors.OnSurfaceVariant.copy(alpha = 0.6f),
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
                        .background(TripWizardColors.SecondaryContainer.copy(alpha = 0.3f))
                        .border(1.dp, TripWizardColors.ContainerHighest.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
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
                                .background(TripWizardColors.Blue.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CalendarToday, null, tint = TripWizardColors.Blue, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(
                                "SELECTED DURATION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TripWizardColors.OnSurfaceVariant,
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
                        color = TripWizardColors.Blue,
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

        // Month/year picker dialog
        if (showMonthYearPicker) {
            S2MonthYearPickerDialog(
                currentMonth = displayMonth,
                currentYear = displayYear,
                onConfirm = { month, year ->
                    displayMonth = month
                    displayYear = year
                    showMonthYearPicker = false
                },
                onDismiss = { showMonthYearPicker = false }
            )
        }

        // Continue button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepSea1)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            TcButton(
                onClick = onContinueClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = startDate != null && endDate != null
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
}

@Composable
private fun S2ProgressWidget(stepsComplete: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TripWizardColors.ContainerHigh.copy(alpha = 0.5f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("YOUR PROGRESS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TripWizardColors.OnSurfaceVariant, letterSpacing = 1.5.sp)
                Box(
                    modifier = Modifier
                        .background(TripWizardColors.Blue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .border(1.dp, TripWizardColors.Blue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("${stepsComplete * 20}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TripWizardColors.Blue)
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
                            .background(if (i < stepsComplete) TripWizardColors.Blue else TripWizardColors.ContainerHighest)
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
    val rangeColor = TripWizardColors.Blue.copy(alpha = 0.2f)

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
                        Modifier.background(TripWizardColors.Blue, CircleShape)
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

@Composable
private fun S2MonthYearPickerDialog(
    currentMonth: Int,
    currentYear: Int,
    onConfirm: (month: Int, year: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var pickerMonth by remember { mutableIntStateOf(currentMonth) }
    var pickerYear by remember { mutableIntStateOf(currentYear) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0B203D),
        shape = RoundedCornerShape(20.dp),
        title = {
            // Year selector row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { pickerYear-- }) {
                    Icon(Icons.Default.ChevronLeft, null, tint = TripWizardColors.Blue)
                }
                Text(
                    pickerYear.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = { pickerYear++ }) {
                    Icon(Icons.Default.ChevronRight, null, tint = TripWizardColors.Blue)
                }
            }
        },
        text = {
            // 3×4 month grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(200.dp)
            ) {
                itemsIndexed(shortMonthNames) { index, name ->
                    val selected = index == pickerMonth
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) TripWizardColors.Blue else Color(0xFF152C4E))
                            .clickable { pickerMonth = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            name,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Color(0xFF001627) else Color(0xFF9EABC8)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pickerMonth, pickerYear) }) {
                Text("Done", color = TripWizardColors.Blue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF9EABC8))
            }
        }
    )
}
