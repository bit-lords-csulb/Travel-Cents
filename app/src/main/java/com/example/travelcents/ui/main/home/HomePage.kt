package com.example.travelcents.ui.main.home

import android.util.Log
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.travelcents.BuildConfig
import com.example.travelcents.data.model.Itinerary
import com.example.travelcents.ui.components.ProfileAvatar
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val Primary = Color(0xFF64B5F6)
private val PrimaryDim = Color(0xFF54A7E7)
private val SurfaceBright = Color(0xFF243447)

// ─────────────────────────────────────────────────────────────
// Entry point
// ─────────────────────────────────────────────────────────────

@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    onTripClick: (String) -> Unit = {},
    onProfileClick: () -> Unit = {},
    homeViewModel: HomeViewModel = viewModel(),
    currencyViewModel: CurrencyViewModel = viewModel()
) {
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSea1)
            .verticalScroll(rememberScrollState())
    ) {
        HomeHeader(
            profileImageUrl = homeUiState.profile.profileImageUrl,
            isProfileLoading = homeUiState.profile.isLoading,
            onProfileClick = onProfileClick
        )

        Spacer(modifier = Modifier.height(4.dp))

        TripsCarousel(
            trips = homeUiState.trips,
            tripImages = homeUiState.tripImages,
            isLoading = homeUiState.isLoading,
            onTripClick = onTripClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SavedPlacesWidget(modifier = Modifier.weight(1f).aspectRatio(1f))
                CurrencyConverterCard(
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    viewModel = currencyViewModel
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
private fun HomeHeader(
    profileImageUrl: String,
    isProfileLoading: Boolean,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                    .clickable(onClick = onProfileClick),
                contentAlignment = Alignment.Center
            ) {
                ProfileAvatar(
                    photoUrl = profileImageUrl,
                    contentDescription = "Profile",
                    modifier = Modifier.fillMaxSize(),
                    borderColor = Primary.copy(alpha = 0.3f),
                    backgroundColor = DeepSea2,
                    placeholderTint = DeepSea4,
                    isLoading = isProfileLoading
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
    onTripClick: (String) -> Unit = {}
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
                        imageUrl = tripImages[trips[page].itineraryId],
                        isCurrent = page == pagerState.currentPage,
                        onClick = { onTripClick(trips[page].itineraryId) }
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
                                .background(if (isSelected) Primary else DeepSea3)
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
private fun TripCard(trip: Itinerary, imageUrl: String?, isCurrent: Boolean, onClick: () -> Unit = {}) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val countdownDays: Long? = runCatching {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        ChronoUnit.DAYS.between(today, LocalDate.parse(trip.dateFrom, fmt))
    }.getOrNull()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 5f)
            .clip(RoundedCornerShape(24.dp))
            .alpha(if (isCurrent) 1f else 0.6f)
            .clickable(enabled = isCurrent) { onClick() }
    ) {
        if (imageUrl != null) {
            val request = remember(imageUrl) {
                ImageRequest.Builder(context)
                    .data(imageUrl)
                    .setHeader("User-Agent", WIKIMEDIA_IMAGE_USER_AGENT)
                    .setHeader("Api-User-Agent", WIKIMEDIA_IMAGE_USER_AGENT)
                    .crossfade(true)
                    .listener(
                        onError = { request, result ->
                            Log.w(
                                "HomePage",
                                "Trip card image failed for '${trip.destination}' url='${request.data}': ${result.throwable.message}"
                            )
                        }
                    )
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = trip.destination,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(colors = listOf(DeepSea3, SurfaceBright, DeepSea2))
                    )
            )
        }

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

        // Countdown badge
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

        // Trip name + flight info
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

            if (trip != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                Text(text = "No upcoming trips", color = DeepSea4, fontSize = 13.sp)
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
            .background(Brush.horizontalGradient(listOf(Primary, PrimaryDim)))
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

private const val WIKIMEDIA_CONTACT_URL = "https://github.com/bit-lords-csulb/Travel-Cents"
private val WIKIMEDIA_IMAGE_USER_AGENT =
    "TravelCents/${BuildConfig.VERSION_NAME} (Android app; $WIKIMEDIA_CONTACT_URL)"
