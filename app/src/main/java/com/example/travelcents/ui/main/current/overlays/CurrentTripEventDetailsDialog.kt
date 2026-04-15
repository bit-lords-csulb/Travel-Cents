package com.example.travelcents.ui.main.current

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.YelpReview
import com.example.travelcents.data.trip.model.ATTR_AVERAGE_RATING
import com.example.travelcents.data.trip.model.ATTR_BOOKING_URL
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_ADDRESS
import com.example.travelcents.data.trip.model.ATTR_BUSINESS_NAME
import com.example.travelcents.data.trip.model.ATTR_CATEGORIES
import com.example.travelcents.data.trip.model.ATTR_HOTEL_NAME
import com.example.travelcents.data.trip.model.ATTR_HOTEL_RATING
import com.example.travelcents.data.trip.model.ATTR_MENU_URL
import com.example.travelcents.data.trip.model.ATTR_REVIEW_COUNT
import com.example.travelcents.data.trip.model.ATTR_YELP_URL
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.ui.modules.PhotoGalleryButton
import com.example.travelcents.ui.modules.TripPhotoGalleryDialog
import com.example.travelcents.ui.modules.formatDisplayTime
import com.example.travelcents.ui.modules.formatDisplayTimeRange
import com.example.travelcents.ui.modules.galleryPhotoModels
import com.example.travelcents.ui.modules.heroImageModel
import com.example.travelcents.ui.modules.parseFlexibleTime
import java.time.Duration
import java.util.Locale

private val DetailBackground = Color(0xFF0B1326)
private val DetailSurfaceLowest = Color(0xFF060E20)
private val DetailSurfaceLow = Color(0xFF131B2E)
private val DetailSurfaceHigh = Color(0xFF222A3D)
private val DetailSurfaceHighest = Color(0xFF2D3449)
private val DetailPrimary = Color(0xFFCEBDFF)
private val DetailPrimaryStrong = Color(0xFFA78BFA)
private val DetailOnSurface = Color(0xFFDAE2FD)
private val DetailOnSurfaceVariant = Color(0xFFCAC4D4)
private val DetailOutline = Color(0xFF494552)
private val DetailTertiary = Color(0xFFDBC839)

@Composable
fun CurrentTripEventDetailsDialog(
    event: TravelEvent,
    currentOptions: List<EventOption>,
    yelpReviews: List<YelpReview>,
    reviewsLoading: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAlternatives: (() -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    val photos = remember(event) { event.galleryPhotoModels() }
    val heroImage = remember(event) { event.heroImageModel() }
    val officialUrl = remember(event) { eventOfficialUrl(event) }
    val reviewUrl = yelpReviews.firstOrNull()?.url?.takeIf { it.isNotBlank() } ?: officialUrl
    val mapsQuery = remember(event) { eventMapsQuery(event) }
    val mapsUrl = remember(mapsQuery) { googleMapsSearchUrl(mapsQuery) }
    val directionsUrl = remember(mapsQuery) { googleMapsDirectionsUrl(mapsQuery) }
    val locationLabel = remember(event) { eventLocationLabel(event) }
    val experienceText = remember(event) { eventExperienceText(event) }
    val timeSummary = remember(event) { eventTimeSummary(event) }
    val durationSummary = remember(event) { eventDurationSummary(event) }
    val ratingLabel = remember(event, yelpReviews) { eventRatingLabel(event, yelpReviews) }
    val reviewCountLabel = remember(event, yelpReviews) { eventReviewCountLabel(event, yelpReviews) }
    val websiteLabel = remember(officialUrl, mapsUrl) {
        officialUrl?.let(::compactHostLabel) ?: compactHostLabel(mapsUrl)
    }
    var menuExpanded by remember { mutableStateOf(false) }
    @Suppress("UNUSED_VALUE") // Compose state — reads happen on recomposition, IDE can't see them
    var showGallery by remember(event.eventId) { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DetailBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DetailBackground)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = DetailPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Event Details",
                            color = DetailPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More actions",
                                tint = DetailOnSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            containerColor = DetailSurfaceLow
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit details", color = DetailOnSurface) },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                            if (onAlternatives != null && currentOptions.size > 1) {
                                DropdownMenuItem(
                                    text = { Text("Change option", color = DetailOnSurface) },
                                    onClick = {
                                        menuExpanded = false
                                        onAlternatives()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete plan", color = Color(0xFFFFB4AB)) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    EventHeroSection(
                        event = event,
                        heroImage = heroImage,
                        photoCount = photos.size,
                        onOpenGallery = if (photos.isNotEmpty()) ({ showGallery = true }) else null
                    )

                    if (event.type.equals("flight", ignoreCase = true)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(26.dp)
                        ) {
                            QuickActionsSection(
                                onDirections = { uriHandler.openUri(directionsUrl) },
                                onEdit = onEdit
                            )

                            FlightRouteCard(event = event)

                            FlightInfoGrid(event = event)

                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(26.dp)
                        ) {
                            QuickActionsSection(
                                onDirections = { uriHandler.openUri(directionsUrl) },
                                onEdit = onEdit
                            )

                            HighlightGrid(
                                timeSummary = timeSummary,
                                durationSummary = durationSummary,
                                websiteLabel = websiteLabel,
                                officialUrl = officialUrl ?: mapsUrl,
                                hasOfficialSite = officialUrl != null
                            )

                            LocationSection(
                                heroImage = heroImage,
                                locationLabel = locationLabel,
                                onOpenMaps = { uriHandler.openUri(mapsUrl) }
                            )

                            DetailTextSection(body = experienceText)

                            ReviewsSection(
                                ratingLabel = ratingLabel,
                                reviewCountLabel = reviewCountLabel,
                                reviews = yelpReviews,
                                reviewsLoading = reviewsLoading,
                                onReadAll = reviewUrl?.let { { uriHandler.openUri(it) } }
                            )

                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }
                }
            }
        }
    }

    if (showGallery && photos.isNotEmpty()) {
        TripPhotoGalleryDialog(
            photos = photos,
            onDismiss = { showGallery = false }
        )
    }
}

@Composable
private fun EventHeroSection(
    event: TravelEvent,
    heroImage: String?,
    photoCount: Int,
    onOpenGallery: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
    ) {
        if (!heroImage.isNullOrBlank()) {
            AsyncImage(
                model = heroImage,
                contentDescription = eventTitle(event),
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (onOpenGallery != null) Modifier.clickable(onClick = onOpenGallery) else Modifier),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(DetailPrimaryStrong.copy(alpha = 0.85f), DetailBackground)
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, DetailBackground.copy(alpha = 0.22f), DetailBackground)
                    )
                )
        )

        if (photoCount > 1) {
            Surface(
                color = DetailSurfaceHighest.copy(alpha = 0.72f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = "$photoCount PHOTOS",
                    color = DetailOnSurface,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        if (onOpenGallery != null) {
            PhotoGalleryButton(
                photoCount = photoCount,
                onClick = onOpenGallery,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
            Surface(
                color = DetailSurfaceHighest.copy(alpha = 0.75f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = event.type.uppercase(Locale.US),
                    color = DetailPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = eventTitle(event),
                color = DetailOnSurface,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun QuickActionsSection(
    onDirections: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionButton(
            modifier = Modifier.weight(1f),
            filled = true,
            icon = Icons.Default.Directions,
            label = "Get Directions",
            onClick = onDirections
        )
        QuickActionButton(
            modifier = Modifier.weight(1f),
            filled = false,
            icon = Icons.Default.Edit,
            label = "Edit Details",
            onClick = onEdit
        )
    }
}

@Composable
private fun QuickActionButton(
    modifier: Modifier,
    filled: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val containerModifier = if (filled) {
        Modifier.background(
            brush = Brush.linearGradient(
                colors = listOf(DetailPrimary, DetailPrimaryStrong)
            ),
            shape = CircleShape
        )
    } else {
        Modifier.background(DetailSurfaceHigh, CircleShape)
    }

    Row(
        modifier = modifier
            .then(containerModifier)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (filled) DetailBackground else DetailPrimary
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = if (filled) DetailBackground else DetailPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            softWrap = false
        )
    }
}

@Composable
private fun HighlightGrid(
    timeSummary: String,
    durationSummary: String,
    websiteLabel: String,
    officialUrl: String,
    hasOfficialSite: Boolean
) {
    val uriHandler = LocalUriHandler.current

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val wideLayout = maxWidth >= 620.dp

        if (wideLayout) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HighlightCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Schedule,
                    eyebrow = "Timing",
                    title = timeSummary,
                    supporting = durationSummary
                )
                HighlightCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Language,
                    eyebrow = if (hasOfficialSite) "Official Site" else "Place Info",
                    title = websiteLabel,
                    supporting = if (hasOfficialSite) "Booked: Yes" else "Open for extra details",
                    onClick = { uriHandler.openUri(officialUrl) }
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HighlightCard(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Schedule,
                    eyebrow = "Timing",
                    title = timeSummary,
                    supporting = durationSummary
                )
                HighlightCard(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Language,
                    eyebrow = if (hasOfficialSite) "Official Site" else "Place Info",
                    title = websiteLabel,
                    supporting = if (hasOfficialSite) "Booked: Yes" else "Open for extra details",
                    onClick = { uriHandler.openUri(officialUrl) }
                )
            }
        }
    }
}

@Composable
private fun HighlightCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    eyebrow: String,
    title: String,
    supporting: String,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(DetailSurfaceLow)
            .then(clickableModifier)
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(DetailSurfaceHighest, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DetailPrimary
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = eyebrow.uppercase(Locale.US),
                color = DetailOnSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Text(
                text = title,
                color = if (onClick != null) DetailPrimary else DetailOnSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = supporting,
                color = DetailOnSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun LocationSection(
    heroImage: String?,
    locationLabel: String,
    onOpenMaps: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Location",
                color = DetailOnSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.clickable(onClick = onOpenMaps),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Open in Maps",
                    color = DetailPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = DetailPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DetailSurfaceLowest)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(192.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DetailSurfaceHigh)
            ) {
                if (!heroImage.isNullOrBlank()) {
                    AsyncImage(
                        model = heroImage,
                        contentDescription = locationLabel,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.colorMatrix(
                            ColorMatrix().apply { setToSaturation(0f) }
                        ),
                        alpha = 0.48f
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    DetailBackground.copy(alpha = 0.15f),
                                    DetailSurfaceHigh.copy(alpha = 0.75f)
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .align(Alignment.Center)
                        .background(DetailPrimary.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = DetailPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    tint = DetailOnSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = locationLabel,
                    color = DetailOnSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DetailTextSection(
    body: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Experience",
            color = DetailOnSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DetailSurfaceLow)
                .padding(20.dp)
        ) {
            Text(
                text = body,
                color = DetailOnSurfaceVariant,
                fontSize = 15.sp,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
private fun ReviewsSection(
    ratingLabel: String,
    reviewCountLabel: String,
    reviews: List<YelpReview>,
    reviewsLoading: Boolean,
    onReadAll: (() -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Guest Reviews",
                color = DetailOnSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Surface(
                color = DetailSurfaceHighest,
                shape = RoundedCornerShape(999.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = DetailTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = ratingLabel,
                        color = DetailOnSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (reviewCountLabel.isNotBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "($reviewCountLabel)",
                            color = DetailOnSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        when {
            reviewsLoading -> {
                ReviewStateCard("Loading guest reviews...")
            }
            reviews.isEmpty() -> {
                ReviewStateCard("No guest reviews are available for this event yet.")
            }
            else -> {
                reviews.take(2).forEach { review ->
                    ReviewCard(review = review)
                }
            }
        }

        if (onReadAll != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, DetailOutline.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .clickable(onClick = onReadAll)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (reviewCountLabel.isBlank()) "Read All Reviews" else "Read All $reviewCountLabel Reviews",
                    color = DetailPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun ReviewStateCard(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DetailSurfaceLow)
            .padding(20.dp)
    ) {
        Text(
            text = message,
            color = DetailOnSurfaceVariant,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ReviewCard(review: YelpReview) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DetailSurfaceLow)
            .border(1.dp, DetailPrimary.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!review.user?.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = review.user!!.imageUrl,
                        contentDescription = review.user!!.name.ifBlank { "Reviewer" },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(DetailSurfaceHighest, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = review.user?.name?.take(1)?.uppercase(Locale.US) ?: "R",
                            color = DetailOnSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.user?.name ?: "Verified Traveler",
                        color = DetailOnSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = review.timeCreated.take(10),
                        color = DetailOnSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.3.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(review.rating.coerceIn(0, 5)) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = DetailTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Text(
            text = "\"${review.text}\"",
            color = DetailOnSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
    }
}

private fun eventOfficialUrl(event: TravelEvent): String? {
    return listOf(
        event.detailValue(ATTR_BOOKING_URL, "booking_url"),
        event.details["tickets_url"],
        event.detailValue(ATTR_MENU_URL, "yelp_menu_url"),
        event.detailValue(ATTR_YELP_URL),
        event.details["website"],
        event.details["url"]
    ).firstOrNull { !it.isNullOrBlank() }
}

private fun eventMapsQuery(event: TravelEvent): String {
    return listOf(
        event.detailValue(ATTR_BUSINESS_ADDRESS, "address"),
        event.details["location"],
        event.details["destination_airport"],
        event.detailValue(ATTR_HOTEL_NAME, "hotel_name"),
        event.detailValue(ATTR_BUSINESS_NAME, "restaurant_name", "activity_name"),
        event.details["title"],
        eventTitle(event)
    ).firstOrNull { !it.isNullOrBlank() } ?: eventTitle(event)
}

private fun eventLocationLabel(event: TravelEvent): String {
    return listOf(
        event.detailValue(ATTR_BUSINESS_ADDRESS, "address"),
        event.details["location"],
        event.details["destination_airport"],
        event.details["origin_airport"],
        event.detailValue(ATTR_HOTEL_NAME, "hotel_name"),
        event.detailValue(ATTR_BUSINESS_NAME, "restaurant_name")
    ).firstOrNull { !it.isNullOrBlank() } ?: "Location information unavailable"
}

private fun eventExperienceText(event: TravelEvent): String {
    return listOf(
        event.details["description"],
        event.details["notes"],
        eventSubtitle(event).takeIf { it != "Tap to edit details" },
        event.detailValue(ATTR_CATEGORIES, "categories"),
        event.details["cuisine"]
    ).firstOrNull { !it.isNullOrBlank() }
        ?: "Open the editor to add notes, links, and more context for this event."
}

private fun formatFlightDuration(minStr: String?): String? {
    val min = minStr?.toIntOrNull() ?: return null
    if (min <= 0) return null
    val h = min / 60
    val m = min % 60
    return if (m == 0) "${h}h" else "${h}h ${m}m"
}

private fun eventTimeSummary(event: TravelEvent): String {
    if (event.type.equals("flight", ignoreCase = true)) {
        val datePrefix = event.date.takeIf { it.isNotBlank() }?.let { "$it  " }.orEmpty()
        val departure = formatDisplayTime(event.startTime)
        val arrival = formatDisplayTime(event.endTime)
        val arrivalDate = event.details["arrival_date"]?.takeIf { it.isNotBlank() }
        return if (!arrivalDate.isNullOrBlank() && arrivalDate != event.date) {
            "$datePrefix$departure - $arrivalDate  $arrival"
        } else {
            "$datePrefix$departure - $arrival"
        }
    }

    val datePrefix = event.date.takeIf { it.isNotBlank() }?.let { "$it  " }.orEmpty()
    return datePrefix + formatDisplayTimeRange(event.startTime, event.endTime)
}

private fun eventDurationSummary(event: TravelEvent): String {
    val explicitDuration = formatFlightDuration(
        event.details["flight_duration_min"]
            ?: event.details["total_duration"]
            ?: event.details["duration"]
    )
    if (explicitDuration != null) {
        return "Duration: $explicitDuration"
    }

    val start = parseFlexibleTime(event.startTime)
    val end = parseFlexibleTime(event.endTime)
    return if (start != null && end != null && end.isAfter(start)) {
        val duration = Duration.between(start, end)
        val totalMinutes = duration.toMinutes()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        buildString {
            append("Duration: ")
            if (hours > 0) {
                append(hours)
                append(" hour")
                if (hours != 1L) append('s')
            }
            if (minutes > 0) {
                if (hours > 0) append(' ')
                append(minutes)
                append(" min")
            }
        }
    } else {
        "Start: ${formatDisplayTime(event.startTime)}"
    }
}

private fun eventRatingLabel(event: TravelEvent, reviews: List<YelpReview>): String {
    return event.detailValue(ATTR_HOTEL_RATING, ATTR_AVERAGE_RATING, "overall_rating", "rating")
        ?: if (reviews.isNotEmpty()) {
            "%.1f".format(Locale.US, reviews.map { it.rating }.average())
        } else {
            "N/A"
        }
}

private fun eventReviewCountLabel(event: TravelEvent, reviews: List<YelpReview>): String {
    return event.detailValue(ATTR_REVIEW_COUNT, "review_count")
        ?: event.details["reviews"]
        ?: if (reviews.isNotEmpty()) reviews.size.toString() else ""
}

private fun googleMapsSearchUrl(query: String): String {
    return "https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}"
}

private fun googleMapsDirectionsUrl(query: String): String {
    return "https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(query)}&travelmode=driving"
}

private fun compactHostLabel(url: String): String {
    val host = url.toUri().host.orEmpty().removePrefix("www.")
    return host.ifBlank { "Open Link" }
}

