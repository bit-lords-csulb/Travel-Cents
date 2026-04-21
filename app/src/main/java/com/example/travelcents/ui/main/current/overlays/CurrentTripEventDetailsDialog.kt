package com.example.travelcents.ui.main.current

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.YelpReview
import com.example.travelcents.ui.main.current.overlays.cards.ActivityHoursCard
import com.example.travelcents.ui.main.current.overlays.cards.ActivitySummaryCard
import com.example.travelcents.ui.main.current.overlays.cards.CardBackground
import com.example.travelcents.ui.main.current.overlays.cards.CardCoral
import com.example.travelcents.ui.main.current.overlays.cards.CardLavender
import com.example.travelcents.ui.main.current.overlays.cards.CardSurface
import com.example.travelcents.ui.main.current.overlays.cards.CardText
import com.example.travelcents.ui.main.current.overlays.cards.CardTextMuted
import com.example.travelcents.ui.main.current.overlays.cards.DetailActionRow
import com.example.travelcents.ui.main.current.overlays.cards.EventSummaryCard
import com.example.travelcents.ui.main.current.overlays.cards.FlightPricingCard
import com.example.travelcents.ui.main.current.overlays.cards.FlightRouteCard
import com.example.travelcents.ui.main.current.overlays.cards.FlightTimingCard
import com.example.travelcents.ui.main.current.overlays.cards.HotelAmenitiesCard
import com.example.travelcents.ui.main.current.overlays.cards.HotelOverviewCard
import com.example.travelcents.ui.main.current.overlays.cards.HotelPricingCard
import com.example.travelcents.ui.main.current.overlays.cards.HotelStayCard
import com.example.travelcents.ui.main.current.overlays.cards.LocationMapCard
import com.example.travelcents.ui.main.current.overlays.cards.RestaurantHoursCard
import com.example.travelcents.ui.main.current.overlays.cards.RestaurantServicesCard
import com.example.travelcents.ui.main.current.overlays.cards.RestaurantSummaryCard
import com.example.travelcents.ui.main.current.overlays.cards.ReviewsCard
import com.example.travelcents.ui.main.current.overlays.cards.compactHostLabel
import com.example.travelcents.ui.main.current.overlays.cards.eventDurationSummary
import com.example.travelcents.ui.main.current.overlays.cards.eventLocationLabel
import com.example.travelcents.ui.main.current.overlays.cards.eventMapsQuery
import com.example.travelcents.ui.main.current.overlays.cards.eventOfficialUrl
import com.example.travelcents.ui.main.current.overlays.cards.eventRatingLabel
import com.example.travelcents.ui.main.current.overlays.cards.eventReviewCountLabel
import com.example.travelcents.ui.main.current.overlays.cards.eventTimeSummary
import com.example.travelcents.ui.main.current.overlays.cards.googleMapsDirectionsUrl
import com.example.travelcents.ui.main.current.overlays.cards.googleMapsSearchUrl
import com.example.travelcents.ui.modules.embeddedMapUrl
import com.example.travelcents.ui.modules.TripPhotoGalleryDialog
import com.example.travelcents.ui.modules.galleryPhotoModels
import com.example.travelcents.ui.modules.heroImageModel
import com.example.travelcents.ui.modules.rememberStaticMapModel

@Composable
fun CurrentTripEventDetailsDialog(
    event: TravelEvent,
    currentOptions: List<EventOption>,
    yelpReviews: List<YelpReview>,
    reviewsLoading: Boolean,
    canEditTrip: Boolean,
    canShowAlternatives: Boolean = currentOptions.size > 1,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAlternatives: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val photos = remember(event, context) { event.galleryPhotoModels(context) }
    val heroImage = remember(event, context) { event.heroImageModel(context) }
    val officialUrl = remember(event) { eventOfficialUrl(event) }
    val mapsQuery = remember(event) { eventMapsQuery(event) }
    val mapsUrl = remember(mapsQuery) { googleMapsSearchUrl(mapsQuery) }
    val directionsUrl = remember(mapsQuery) { googleMapsDirectionsUrl(mapsQuery) }
    val locationLabel = remember(event) { eventLocationLabel(event) }
    val staticMapModel = rememberStaticMapModel(event)
    val embeddedMapUrl = remember(event) { event.embeddedMapUrl() }
    val timeSummary = remember(event) { eventTimeSummary(event) }
    val durationSummary = remember(event) { eventDurationSummary(event) }
    val ratingLabel = remember(event, yelpReviews) { eventRatingLabel(event, yelpReviews) }
    val reviewCountLabel = remember(event, yelpReviews) { eventReviewCountLabel(event, yelpReviews) }
    val reviewUrl = yelpReviews.firstOrNull()?.url?.takeIf { it.isNotBlank() } ?: officialUrl
    val websiteLabel = remember(officialUrl, mapsUrl) {
        officialUrl?.let(::compactHostLabel) ?: compactHostLabel(mapsUrl)
    }
    var menuExpanded by remember { mutableStateOf(false) }
    var showGallery by remember(event.eventId) { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = CardBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBackground)
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
                                tint = CardLavender
                            )
                        }
                        Text(
                            text = "Event Details",
                            color = CardText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (canEditTrip) {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More actions",
                                    tint = CardTextMuted
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                containerColor = CardSurface
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit details", color = CardText) },
                                    onClick = {
                                        menuExpanded = false
                                        onEdit()
                                    }
                                )
                                if (onAlternatives != null && canShowAlternatives) {
                                    DropdownMenuItem(
                                        text = { Text("Change option", color = CardText) },
                                        onClick = {
                                            menuExpanded = false
                                            onAlternatives()
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Delete plan", color = CardCoral) },
                                    onClick = {
                                        menuExpanded = false
                                        onDelete()
                                    }
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EventSummaryCard(
                        event = event,
                        heroImage = heroImage,
                        photoCount = photos.size,
                        timeSummary = timeSummary,
                        durationSummary = durationSummary,
                        onOpenGallery = if (photos.isNotEmpty()) ({ showGallery = true }) else null
                    )

                    DetailActionRow(
                        type = event.type,
                        onDirections = { uriHandler.openUri(directionsUrl) },
                        onEdit = onEdit
                    )

                    EventDetailCardStack(
                        event = event,
                        staticMapModel = staticMapModel,
                        embeddedMapUrl = embeddedMapUrl,
                        locationLabel = locationLabel,
                        mapsUrl = mapsUrl,
                        websiteLabel = websiteLabel,
                        officialUrl = officialUrl,
                        ratingLabel = ratingLabel,
                        reviewCountLabel = reviewCountLabel,
                        yelpReviews = yelpReviews,
                        reviewsLoading = reviewsLoading,
                        reviewUrl = reviewUrl
                    )

                    Spacer(modifier = Modifier.padding(bottom = 12.dp))
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
private fun EventDetailCardStack(
    event: TravelEvent,
    staticMapModel: String?,
    embeddedMapUrl: String?,
    locationLabel: String,
    mapsUrl: String,
    websiteLabel: String,
    officialUrl: String?,
    ratingLabel: String,
    reviewCountLabel: String,
    yelpReviews: List<YelpReview>,
    reviewsLoading: Boolean,
    reviewUrl: String?
) {
    val uriHandler = LocalUriHandler.current
    val showLocationCard = !staticMapModel.isNullOrBlank() ||
        !embeddedMapUrl.isNullOrBlank() ||
        locationLabel != "Location information unavailable"

    when (event.type.lowercase()) {
        "flight" -> {
            FlightTimingCard(event)
            FlightRouteCard(event)
            FlightPricingCard(event)
        }
        "hotel" -> {
            HotelStayCard(event)
            HotelOverviewCard(
                event = event,
                onOpenReviews = { uriHandler.openUri(mapsUrl) }
            )
            HotelPricingCard(event)
            HotelAmenitiesCard(event)
            if (showLocationCard) {
                LocationMapCard(
                    title = "Map & Coordinates",
                    locationLabel = locationLabel,
                    staticMapModel = staticMapModel,
                    embeddedMapUrl = embeddedMapUrl,
                    onOpenMaps = { uriHandler.openUri(mapsUrl) }
                )
            }
        }
        "restaurant", "dining", "food" -> {
            RestaurantSummaryCard(event)
            RestaurantServicesCard(event)
            RestaurantHoursCard(event)
            LocationMapCard(
                locationLabel = locationLabel,
                staticMapModel = staticMapModel,
                embeddedMapUrl = embeddedMapUrl,
                onOpenMaps = { uriHandler.openUri(mapsUrl) }
            )
            ReviewsCard(
                ratingLabel = ratingLabel,
                reviewCountLabel = reviewCountLabel,
                reviews = yelpReviews,
                reviewsLoading = reviewsLoading,
                onReadAll = reviewUrl?.let { { uriHandler.openUri(it) } }
            )
        }
        else -> {
            ActivitySummaryCard(event)
            ActivityHoursCard(event)
            if (officialUrl != null) {
                com.example.travelcents.ui.main.current.overlays.cards.DetailLinkRow(
                    label = "Source",
                    value = websiteLabel,
                    onClick = { uriHandler.openUri(officialUrl) }
                )
            }
            LocationMapCard(
                locationLabel = locationLabel,
                staticMapModel = staticMapModel,
                embeddedMapUrl = embeddedMapUrl,
                onOpenMaps = { uriHandler.openUri(mapsUrl) }
            )
            ReviewsCard(
                ratingLabel = ratingLabel,
                reviewCountLabel = reviewCountLabel,
                reviews = yelpReviews,
                reviewsLoading = reviewsLoading,
                onReadAll = reviewUrl?.let { { uriHandler.openUri(it) } }
            )
        }
    }
}
