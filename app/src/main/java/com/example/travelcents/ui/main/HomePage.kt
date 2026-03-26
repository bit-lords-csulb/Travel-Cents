package com.example.travelcents.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.travelcents.data.model.Itinerary
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────
// Entry point
// ─────────────────────────────────────────────────────────────

@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = viewModel(),
    currencyViewModel: CurrencyViewModel = viewModel()
) {
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSea1)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ── Header ──────────────────────────────────────────
        Text(
            text = "My Trips",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // ── Trips carousel ──────────────────────────────────
        TripsCarousel(
            trips = homeUiState.trips,
            tripImages = homeUiState.tripImages,
            isLoading = homeUiState.isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── 2 × 2 widget grid ───────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SavedPlacesWidget()
                TripStatusWidget()
            }
            // Right column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactCurrencyWidget(
                    amount = currencyViewModel.amount,
                    fromCurrency = currencyViewModel.fromCurrency,
                    toCurrency = currencyViewModel.toCurrency,
                    result = currencyViewModel.result,
                    isLoading = currencyViewModel.isLoading,
                    error = currencyViewModel.error,
                    currencies = currencyViewModel.currencies,
                    recentCurrencies = currencyViewModel.recentCurrencies,
                    onAmountChange = currencyViewModel::onAmountChange,
                    onFromCurrencyChange = currencyViewModel::onFromCurrencyChange,
                    onToCurrencyChange = currencyViewModel::onToCurrencyChange,
                    onSwap = currencyViewModel::swap
                )
                YourDocumentsWidget()
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────────────────────
// Trips Carousel
// ─────────────────────────────────────────────────────────────

@Composable
private fun TripsCarousel(
    trips: List<Itinerary>,
    tripImages: Map<String, String>,
    isLoading: Boolean
) {
    when {
        isLoading -> CarouselPlaceholder {
            CircularProgressIndicator(color = DeepSea4, strokeWidth = 2.dp)
        }

        trips.isEmpty() -> CarouselPlaceholder {
            Text(
                text = "No trips yet. Create one from New Trip.",
                color = DeepSea4,
                fontSize = 13.sp
            )
        }

        else -> {
            val pagerState = rememberPagerState(pageCount = { trips.size })
            Column {
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(end = 32.dp),
                    pageSpacing = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    TripCard(
                        trip = trips[page],
                        imageUrl = tripImages[trips[page].destination]
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dot indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(trips.size) { index ->
                        val selected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (selected) 8.dp else 5.dp)
                                .background(
                                    color = if (selected) Color.White else DeepSea4,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CarouselPlaceholder(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DeepSea2),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun TripCard(trip: Itinerary, imageUrl: String?) {
    val today = LocalDate.now()
    val countdownDays: Long? = runCatching {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        ChronoUnit.DAYS.between(today, LocalDate.parse(trip.dateFrom, fmt))
    }.getOrNull()

    val dateRange = formatDateRange(trip.dateFrom, trip.dateTo)
    val flightLine = trip.destinationIata
        .takeIf { it.isNotBlank() }
        ?.let { "Flight to $it" }

    // Use Wikipedia image if available, fall back to picsum placeholder
    val imageSeed = abs(trip.destination.hashCode() % 1000)
    val resolvedImageUrl = imageUrl ?: "https://picsum.photos/seed/$imageSeed/400/250"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        AsyncImage(
            model = resolvedImageUrl,
            contentDescription = trip.destination,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient scrim for text legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xD0000000)),
                        startY = 60f
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = trip.tripName,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (dateRange.isNotBlank()) {
                Text(
                    text = dateRange,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )
            }
            if (flightLine != null) {
                Text(
                    text = flightLine,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )
            }
            if (countdownDays != null && countdownDays >= 0) {
                Text(
                    text = "Countdown $countdownDays days",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun formatDateRange(dateFrom: String, dateTo: String): String = runCatching {
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val from = LocalDate.parse(dateFrom, fmt)
    val to = LocalDate.parse(dateTo, fmt)
    val month = from.month.getDisplayName(DateTextStyle.FULL, Locale.US)
    "$month ${from.dayOfMonth.ordinalSuffix()} - ${to.dayOfMonth.ordinalSuffix()}"
}.getOrDefault("")

private fun Int.ordinalSuffix(): String {
    val suffix = when {
        this in 11..13 -> "th"
        this % 10 == 1 -> "st"
        this % 10 == 2 -> "nd"
        this % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$this$suffix"
}

// ─────────────────────────────────────────────────────────────
// Compact Currency Widget
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactCurrencyWidget(
    amount: String,
    fromCurrency: String,
    toCurrency: String,
    result: Double?,
    isLoading: Boolean,
    error: String?,
    currencies: List<String>,
    recentCurrencies: List<String>,
    onAmountChange: (String) -> Unit,
    onFromCurrencyChange: (String) -> Unit,
    onToCurrencyChange: (String) -> Unit,
    onSwap: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSea2)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "CURRENCY CONVERSION",
                color = DeepSea4,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // From row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 13.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = compactTextFieldColors()
                )
                CompactCurrencyDropdown(
                    selected = fromCurrency,
                    currencies = currencies,
                    recentCurrencies = recentCurrencies,
                    onSelect = onFromCurrencyChange
                )
            }

            // Swap button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onSwap,
                    modifier = Modifier
                        .size(28.dp)
                        .background(DeepSea3, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Swap currencies",
                        tint = DeepSea5,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // To row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .background(DeepSea1, RoundedCornerShape(4.dp))
                        .border(1.dp, DeepSea3, RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    when {
                        isLoading -> CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = DeepSea4,
                            strokeWidth = 2.dp
                        )
                        error != null -> Text("—", color = Color(0xFFFF6B6B), fontSize = 12.sp)
                        result != null -> Text(
                            text = "%.2f".format(result),
                            color = DeepSea5,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        else -> Text("—", color = DeepSea4, fontSize = 13.sp)
                    }
                }
                CompactCurrencyDropdown(
                    selected = toCurrency,
                    currencies = currencies,
                    recentCurrencies = recentCurrencies,
                    onSelect = onToCurrencyChange
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactCurrencyDropdown(
    selected: String,
    currencies: List<String>,
    recentCurrencies: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    LaunchedEffect(expanded) { if (!expanded) searchQuery = "" }

    val filtered = remember(searchQuery, currencies) {
        if (searchQuery.isBlank()) currencies
        else currencies.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = if (expanded) searchQuery else selected,
            onValueChange = { searchQuery = it },
            singleLine = true,
            placeholder = {
                if (expanded) Text("Search...", color = DeepSea4, fontSize = 10.sp)
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .width(80.dp)
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
            textStyle = TextStyle(fontSize = 11.sp),
            colors = compactTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.height(200.dp),
            containerColor = DeepSea2
        ) {
            if (searchQuery.isBlank() && recentCurrencies.isNotEmpty()) {
                recentCurrencies.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency, color = DeepSea5, fontSize = 11.sp) },
                        onClick = { onSelect(currency); expanded = false },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    )
                }
                HorizontalDivider(color = DeepSea4.copy(alpha = 0.2f))
            }
            filtered.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency, color = DeepSea5, fontSize = 11.sp) },
                    onClick = { onSelect(currency); expanded = false },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun compactTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = DeepSea5,
    unfocusedTextColor = DeepSea5,
    focusedBorderColor = DeepSea3,
    unfocusedBorderColor = DeepSea3,
    cursorColor = DeepSea5,
    focusedContainerColor = DeepSea1,
    unfocusedContainerColor = DeepSea1,
    focusedTrailingIconColor = DeepSea4,
    unfocusedTrailingIconColor = DeepSea4
)

// ─────────────────────────────────────────────────────────────
// Stub Widgets
// ─────────────────────────────────────────────────────────────

@Composable
private fun SavedPlacesWidget() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSea2)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Saved Places",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Coming soon", color = DeepSea4, fontSize = 11.sp)
        }
    }
}

@Composable
private fun TripStatusWidget() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSea2)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Trip Status",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Live updates for flights", color = DeepSea4, fontSize = 11.sp)
        }
    }
}

@Composable
private fun YourDocumentsWidget() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSea2)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = "Documents",
                tint = DeepSea4,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Your Documents",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
