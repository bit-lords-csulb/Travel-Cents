package com.example.travelcents.ui.main

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.travelcents.data.local.UserSettings
import com.example.travelcents.data.model.Itinerary
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

// Accent colors that mirror the HTML template's primary palette
private val Primary = Color(0xFF64B5F6)
private val PrimaryDim = Color(0xFF54A7E7)
private val SurfaceBright = Color(0xFF243447)

// ─────────────────────────────────────────────────────────────
// Entry point
// ─────────────────────────────────────────────────────────────

@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    userSettings: UserSettings,
    homeViewModel: HomeViewModel = viewModel(),
    currencyViewModel: CurrencyViewModel = viewModel()
) {
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    // Sync currency if user settings change.
    // Ensure one side is always USD as the app is US-based.
    LaunchedEffect(userSettings.currency) {
        if (userSettings.currency != "USD") {
            // If user is in a non-US region (e.g. UK), default to Regional -> USD
            currencyViewModel.onFromCurrencyChange(userSettings.currency)
            currencyViewModel.onToCurrencyChange("USD")
        } else {
            // If user is in US, default to USD -> EUR
            currencyViewModel.onFromCurrencyChange("USD")
            if (currencyViewModel.toCurrency == "USD") {
                currencyViewModel.onToCurrencyChange("EUR")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSea1)
            .verticalScroll(rememberScrollState())
    ) {
        HomeHeader()

        Spacer(modifier = Modifier.height(4.dp))

        TripsCarousel(
            trips = homeUiState.trips,
            tripImages = homeUiState.tripImages,
            isLoading = homeUiState.isLoading,
            dateFormat = userSettings.dateFormat
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WeatherWidget(
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    tempUnit = userSettings.temperatureUnit,
                    region = userSettings.region,
                    country = userSettings.country
                )
                CurrencyWidget(
                    modifier = Modifier.weight(1f).aspectRatio(1f),
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
                    onSwap = currencyViewModel::swap,
                    countryCode = userSettings.countryCode
                )
            }

            TripStatusWidget(trip = homeUiState.trips.firstOrNull())

            DocumentsWidget()
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DeepSea2)
                    .border(1.5.dp, Primary.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = DeepSea4,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = "My Trips",
                color = Primary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = Primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Trips Carousel
// ─────────────────────────────────────────────────────────────

@Composable
private fun TripsCarousel(
    trips: List<Itinerary>,
    tripImages: Map<String, String>,
    isLoading: Boolean,
    dateFormat: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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

                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(end = 48.dp),
                    pageSpacing = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    TripCard(
                        trip = trips[page],
                        imageUrl = tripImages[trips[page].destination],
                        isCurrent = page == pagerState.currentPage,
                        dateFormat = dateFormat
                    )
                }

                // Pill-dot pager indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(trips.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val indicatorWidth by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 6.dp,
                            label = "indicator_$index"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .height(6.dp)
                                .width(indicatorWidth)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Primary
                                    else DeepSea3
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
            .padding(horizontal = 16.dp)
            .aspectRatio(4f / 5f)
            .clip(RoundedCornerShape(24.dp))
            .background(DeepSea2),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun TripCard(trip: Itinerary, imageUrl: String?, isCurrent: Boolean, dateFormat: String) {
    val today = LocalDate.now()
    val countdownDays: Long? = runCatching {
        // Assume database date format is yyyy-MM-dd
        val dbFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        ChronoUnit.DAYS.between(today, LocalDate.parse(trip.dateFrom, dbFmt))
    }.getOrNull()

    val imageSeed = abs(trip.destination.hashCode() % 1000)
    val resolvedImageUrl = imageUrl ?: "https://picsum.photos/seed/$imageSeed/400/500"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 5f)
            .clip(RoundedCornerShape(24.dp))
            .alpha(if (isCurrent) 1f else 0.6f)
    ) {
        AsyncImage(
            model = resolvedImageUrl,
            contentDescription = trip.destination,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xE6000000)),
                        startY = 80f
                    )
                )
        )

        // Countdown badge (top-right frosted pill)
        if (countdownDays != null && countdownDays >= 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(50.dp))
                    .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Countdown $countdownDays days",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
        }

        // Trip name + flight info (bottom-left)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text(
                text = trip.tripName,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 32.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            val flightLine = when {
                trip.originIata.isNotBlank() && trip.destinationIata.isNotBlank() ->
                    "${trip.originIata} → ${trip.destinationIata}"
                trip.destinationIata.isNotBlank() -> "Flight to ${trip.destinationIata}"
                else -> null
            }
            if (flightLine != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlightTakeoff,
                        contentDescription = null,
                        tint = DeepSea4,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = flightLine,
                        color = DeepSea4,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Weather Widget (added for regional impact)
// ─────────────────────────────────────────────────────────────

@Composable
private fun WeatherWidget(modifier: Modifier = Modifier, tempUnit: String, region: String, country: String) {
    val location = if (region.isNotBlank()) region else country
    
    // Simulate weather conditions based on location
    val condition = when {
        location.contains("London", ignoreCase = true) || location.contains("UK", ignoreCase = true) -> "Rainy"
        location.contains("Seattle", ignoreCase = true) || location.contains("Paris", ignoreCase = true) -> "Cloudy"
        location.contains("Tokyo", ignoreCase = true) -> "Partly Cloudy"
        else -> "Sunny"
    }
    
    val (weatherIcon, iconColor) = when (condition) {
        "Rainy" -> Icons.Default.Thunderstorm to Color(0xFF90A4AE)
        "Cloudy" -> Icons.Default.Cloud to Color(0xFFB0BEC5)
        "Partly Cloudy" -> Icons.Default.WbCloudy to Color(0xFFFFD54F)
        else -> Icons.Default.WbSunny to Color(0xFFFFD54F)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSea2)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Weather",
                        color = DeepSea5,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = location,
                        color = DeepSea4,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
                Icon(
                    imageVector = weatherIcon,
                    contentDescription = condition,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                // Simulate slightly different weather if a region is specified
                val baseTemp = if (condition == "Rainy") 15 else if (condition == "Cloudy") 18 else 22
                val temp = if (tempUnit == "Fahrenheit") "${(baseTemp * 9/5) + 32}°F" else "${baseTemp}°C"
                Text(
                    text = temp,
                    color = DeepSea5,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = condition,
                    color = DeepSea4,
                    fontSize = 12.sp
                )
            }
            
            val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"))
            Text(
                text = "Local Time: $currentTime",
                color = DeepSea4,
                fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Saved Places Widget
// ─────────────────────────────────────────────────────────────

@Composable
private fun SavedPlacesWidget(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSea2)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "Saved Places",
                    color = DeepSea5,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            // 2 × 2 photo grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AsyncImage(
                        model = "https://picsum.photos/seed/paris/100/100",
                        contentDescription = "Paris",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                    )
                    AsyncImage(
                        model = "https://picsum.photos/seed/cairo/100/100",
                        contentDescription = "Cairo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AsyncImage(
                        model = "https://picsum.photos/seed/agra/100/100",
                        contentDescription = "Agra",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(SurfaceBright, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+12",
                            color = Primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Currency Widget
// ─────────────────────────────────────────────────────────────

private fun currencySymbol(code: String) = when (code) {
    "USD" -> "$"; "CAD" -> "CA$"; "AUD" -> "A$"; "NZD" -> "NZ$"
    "SGD" -> "S$"; "HKD" -> "HK$"; "MXN" -> "MX$"
    "EUR" -> "€"
    "GBP" -> "£"
    "JPY" -> "¥"; "CNY" -> "¥"
    "KRW" -> "₩"
    "INR" -> "₹"
    "BRL" -> "R$"
    "TRY" -> "₺"
    "ILS" -> "₪"
    "PHP" -> "₱"
    "THB" -> "฿"
    "PLN" -> "zł"
    "ZAR" -> "R"
    "IDR" -> "Rp"
    "MYR" -> "RM"
    "CHF" -> "Fr"
    "SEK" -> "kr"; "NOK" -> "kr"; "DKK" -> "kr"; "ISK" -> "kr"
    "HUF" -> "Ft"
    "CZK" -> "Kč"
    "RON" -> "lei"
    "BGN" -> "лв"
    else -> ""
}

@Composable
private fun CurrencyWidget(
    modifier: Modifier = Modifier,
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
    onSwap: () -> Unit,
    countryCode: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSea2)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "Currency",
                    color = DeepSea5,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onSwap,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Swap currencies",
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // FROM row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceBright, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CurrencyCodeDropdown(
                    selected = fromCurrency,
                    currencies = currencies,
                    recentCurrencies = recentCurrencies,
                    labelColor = DeepSea4,
                    onSelect = onFromCurrencyChange
                )
                Box(
                    modifier = Modifier
                        .height(14.dp)
                        .width(1.dp)
                        .background(DeepSea4.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.weight(1f))
                val fromSymbol = currencySymbol(fromCurrency)
                if (fromSymbol.isNotEmpty()) {
                    Text(
                        text = fromSymbol,
                        color = DeepSea4,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                // Box(IntrinsicSize.Min) makes the field shrink to its text width
                Box(modifier = Modifier.width(IntrinsicSize.Min).widthIn(min = 24.dp, max = 80.dp)) {
                    BasicTextField(
                        value = amount,
                        onValueChange = onAmountChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = TextStyle(
                            color = DeepSea5,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End
                        ),
                        cursorBrush = SolidColor(Primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // TO row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                    .border(1.dp, Primary.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CurrencyCodeDropdown(
                    selected = toCurrency,
                    currencies = currencies,
                    recentCurrencies = recentCurrencies,
                    labelColor = Primary,
                    onSelect = onToCurrencyChange
                )
                Box(
                    modifier = Modifier
                        .height(14.dp)
                        .width(1.dp)
                        .background(Primary.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.weight(1f))
                val toSymbol = currencySymbol(toCurrency)
                when {
                    isLoading -> CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Primary,
                        strokeWidth = 2.dp
                    )
                    error != null -> Text(
                        text = "—",
                        color = Color(0xFFFF6B6B),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    result != null -> {
                        if (toSymbol.isNotEmpty()) {
                            Text(
                                text = toSymbol,
                                color = Primary.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // Impacting numerical formats based on locale/country
                        val locale = Locale("", countryCode)
                        val formattedResult = try {
                            val formatter = NumberFormat.getNumberInstance(locale)
                            formatter.minimumFractionDigits = 2
                            formatter.maximumFractionDigits = 2
                            formatter.format(result)
                        } catch (e: Exception) {
                            "%.2f".format(result)
                        }

                        Text(
                            text = formattedResult,
                            color = Primary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    else -> Text(
                        text = "—",
                        color = Primary.copy(alpha = 0.5f),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrencyCodeDropdown(
    selected: String,
    currencies: List<String>,
    recentCurrencies: List<String>,
    labelColor: Color,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Text(
            text = selected,
            color = labelColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            maxLines = 1,
            modifier = Modifier
                .widthIn(min = 28.dp)
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.height(200.dp),
            containerColor = DeepSea2
        ) {
            if (recentCurrencies.isNotEmpty()) {
                recentCurrencies.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency, color = DeepSea5, fontSize = 11.sp) },
                        onClick = { onSelect(currency); expanded = false },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
                HorizontalDivider(color = DeepSea4.copy(alpha = 0.2f))
            }
            currencies.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency, color = DeepSea5, fontSize = 11.sp) },
                    onClick = { onSelect(currency); expanded = false },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Trip Status Widget
// ─────────────────────────────────────────────────────────────

@Composable
private fun TripStatusWidget(trip: Itinerary?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSea2)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF4ADE80), CircleShape)
                    )
                    Text(
                        text = "STATUS: ON TIME",
                        color = DeepSea4,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = null,
                    tint = DeepSea4,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Flight route
            if (trip != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Origin
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = trip.originIata.ifBlank { "—" },
                            color = DeepSea5,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = trip.origin.take(12).uppercase(),
                            color = DeepSea4,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Flight line with plane icon
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        HorizontalDivider(color = DeepSea3, thickness = 1.dp)
                        Box(
                            modifier = Modifier
                                .background(DeepSea2)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flight,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Destination
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = trip.destinationIata.ifBlank { "—" },
                            color = DeepSea5,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = trip.destination.take(12).uppercase(),
                            color = DeepSea4,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Destination info card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceBright, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Primary.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hotel,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DESTINATION",
                            color = DeepSea4,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = trip.destination,
                            color = DeepSea5,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = DeepSea4,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Text(
                    text = "No upcoming trips",
                    color = DeepSea4,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Documents Widget
// ─────────────────────────────────────────────────────────────

@Composable
private fun DocumentsWidget() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(listOf(Primary, PrimaryDim))
            )
            .clickable { }
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column {
                    Text(
                        text = "Your Documents",
                        color = Color(0xFF00253D),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tickets, Passports & Visas",
                        color = Color(0xFF00253D).copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
