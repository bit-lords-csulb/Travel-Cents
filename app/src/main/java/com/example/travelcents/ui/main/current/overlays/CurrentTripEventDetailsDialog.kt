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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.travelcents.data.trip.model.ATTR_HOTEL_DETAIL_URL
import com.example.travelcents.data.trip.model.ATTR_YELP_URL
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.ATTR_TICKETMASTER_EVENT_ID
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.YelpReview
import com.example.travelcents.data.trip.model.detailValue
import com.example.travelcents.ui.main.current.overlays.cards.ActivityHoursCard
import com.example.travelcents.ui.main.current.overlays.cards.ActivitySummaryCard
import com.example.travelcents.ui.main.current.overlays.cards.CardBackground
import com.example.travelcents.ui.main.current.overlays.cards.CardCoral
import com.example.travelcents.ui.main.current.overlays.cards.CardLavender
import com.example.travelcents.ui.main.current.overlays.cards.CardSurface
import com.example.travelcents.ui.main.current.overlays.cards.CardText
import com.example.travelcents.ui.main.current.overlays.cards.CardTextMuted
import com.example.travelcents.ui.main.current.overlays.cards.CurrencyCostCard
import com.example.travelcents.ui.main.current.overlays.cards.DetailActionRow
import com.example.travelcents.ui.main.current.overlays.cards.EventSummaryCard
import com.example.travelcents.ui.main.current.overlays.cards.FlightBookingOffersCard
import com.example.travelcents.ui.main.current.overlays.cards.FlightLegsCard
import com.example.travelcents.ui.main.current.overlays.cards.FlightPricingCard
import com.example.travelcents.ui.main.current.overlays.cards.FlightRouteCard
import com.example.travelcents.ui.main.current.overlays.cards.FlightStatusCard
import com.example.travelcents.ui.main.current.overlays.cards.FlightTimingCard
import com.example.travelcents.ui.main.current.overlays.cards.LoyaltyCard
import com.example.travelcents.ui.main.current.overlays.cards.HotelAmenitiesCard
import com.example.travelcents.ui.main.current.overlays.cards.HotelBookingCard
import com.example.travelcents.ui.main.current.overlays.cards.HotelOverviewCard
import com.example.travelcents.ui.main.current.overlays.cards.HotelPricingCard
import com.example.travelcents.ui.main.current.overlays.cards.HotelStayCard
import com.example.travelcents.ui.main.current.overlays.cards.LocationMapCard
import com.example.travelcents.ui.main.current.overlays.cards.NeighborhoodCard
import com.example.travelcents.ui.main.current.overlays.cards.RestaurantHoursCard
import com.example.travelcents.ui.main.current.overlays.cards.RestaurantMenuCard
import com.example.travelcents.ui.main.current.overlays.cards.RestaurantOverviewCard
import com.example.travelcents.ui.main.current.overlays.cards.RestaurantReservationsCard
import com.example.travelcents.ui.main.current.overlays.cards.RestaurantServicesCard
import com.example.travelcents.ui.main.current.overlays.cards.ReviewsCard
import com.example.travelcents.ui.main.current.overlays.cards.TransportCard
import com.example.travelcents.ui.main.current.overlays.cards.WaitTimeCard
import com.example.travelcents.ui.main.current.overlays.cards.WeatherCard
import com.example.travelcents.ui.main.current.overlays.cards.TicketPricingCard
import com.example.travelcents.ui.main.current.overlays.cards.VenueCard
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
import com.example.travelcents.ui.modules.TripPhotoGalleryDialog
import com.example.travelcents.ui.modules.galleryPhotoModels
import com.example.travelcents.ui.modules.hasStaticMapSource
import com.example.travelcents.ui.modules.heroImageModel

@Composable
fun CurrentTripEventDetailsDialog(
    event: TravelEvent,
    tripDestination: String,
    tripAdults: Int,
    tripChildren: Int,
    currentOptions: List<EventOption>,
    yelpReviews: List<YelpReview>,
    reviewsLoading: Boolean,
    canEditTrip: Boolean,
    canShowAlternatives: Boolean = currentOptions.size > 1,
    weatherAlertMessage: String? = null,
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
    val hotelReviewsUrl = remember(event, mapsUrl) {
        event.detailValue(ATTR_HOTEL_DETAIL_URL)?.takeIf { it.isNotBlank() } ?: mapsUrl
    }
    val directionsUrl = remember(mapsQuery) { googleMapsDirectionsUrl(mapsQuery) }
    val locationLabel = remember(event) { eventLocationLabel(event) }
    val timeSummary = remember(event) { eventTimeSummary(event) }
    val durationSummary = remember(event) { eventDurationSummary(event) }
    val ratingLabel = remember(event, yelpReviews) { eventRatingLabel(event, yelpReviews) }
    val reviewCountLabel = remember(event, yelpReviews) { eventReviewCountLabel(event, yelpReviews) }
    val reviewUrl = remember(event, yelpReviews) {
        yelpReviews.firstOrNull()?.url?.takeIf { it.isNotBlank() }
            ?: event.detailValue(ATTR_YELP_URL)
    }
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
                    weatherAlertMessage?.let { message ->
                        Surface(
                            color = CardSurface,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                CardCoral.copy(alpha = 0.35f)
                            )
                        ) {
                            Text(
                                text = message,
                                color = CardText,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }
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
                        tripDestination = tripDestination,
                        tripAdults = tripAdults,
                        tripChildren = tripChildren,
                        locationLabel = locationLabel,
                        mapsUrl = mapsUrl,
                        hotelReviewsUrl = hotelReviewsUrl,
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
    tripDestination: String,
    tripAdults: Int,
    tripChildren: Int,
    locationLabel: String,
    mapsUrl: String,
    hotelReviewsUrl: String,
    websiteLabel: String,
    officialUrl: String?,
    ratingLabel: String,
    reviewCountLabel: String,
    yelpReviews: List<YelpReview>,
    reviewsLoading: Boolean,
    reviewUrl: String?
) {
    val uriHandler = LocalUriHandler.current
    val showLocationCard = event.hasStaticMapSource() ||
        locationLabel != "Location information unavailable"
    val isTicketmasterBacked = !event.detailValue(ATTR_TICKETMASTER_EVENT_ID).isNullOrBlank()
    val showActivityReviews = reviewsLoading ||
        yelpReviews.isNotEmpty() ||
        reviewCountLabel.isNotBlank() ||
        (ratingLabel.isNotBlank() && ratingLabel != "N/A")

    when (event.type.lowercase()) {
        "flight" -> {
            FlightTimingCard(event)
            FlightRouteCard(event)
            FlightStatusCard(event)
            FlightLegsCard(event)
            FlightPricingCard(event)
            FlightBookingOffersCard(event = event, adults = tripAdults)
            LoyaltyCard(event)
        }
        "hotel" -> {
            HotelStayCard(event)
            HotelOverviewCard(
                event = event,
                onOpenReviews = { uriHandler.openUri(hotelReviewsUrl) }
            )
            HotelPricingCard(event)
            HotelBookingCard(event)
            HotelAmenitiesCard(event)
            if (showLocationCard) {
                LocationMapCard(
                    title = "Map & Coordinates",
                    event = event,
                    locationLabel = locationLabel,
                    onOpenMaps = { uriHandler.openUri(mapsUrl) }
                )
            }
        }
        "restaurant", "dining", "food" -> {
            val restaurantReviewsUrl = yelpReviews.firstOrNull()?.url?.takeIf { it.isNotBlank() }
                ?: event.detailValue(ATTR_YELP_URL)?.takeIf { it.isNotBlank() }
            RestaurantMenuCard(event)
            RestaurantOverviewCard(
                event = event,
                onOpenReviews = restaurantReviewsUrl?.let { { uriHandler.openUri(it) } }
            )
            RestaurantReservationsCard(
                event = event,
                tripDestination = tripDestination,
                tripAdults = tripAdults,
                tripChildren = tripChildren
            )
            RestaurantServicesCard(event)
            RestaurantHoursCard(event)
            if (showLocationCard) {
                TransportCard(event)
            }
            if (showLocationCard) {
                LocationMapCard(
                    event = event,
                    locationLabel = locationLabel,
                    onOpenMaps = { uriHandler.openUri(mapsUrl) }
                )
            }
            WaitTimeCard(event)
            NeighborhoodCard(event)
            WeatherCard(event)
            CurrencyCostCard(event)
        }
        else -> {
            ActivitySummaryCard(event)
            ActivityHoursCard(event)
            VenueCard(event)
            TicketPricingCard(event)
            val isBookable = event.isNativeBookable?.toString() == "true"
            val bookingUrl = event.bookingUrl
            if (isBookable && !bookingUrl.isNullOrBlank()) {
                com.example.travelcents.ui.main.current.overlays.cards.DetailLinkRow(
                    label = "★ INSTANT BOOKING",
                    value = "Book on Viator",
                    accent = com.example.travelcents.ui.main.current.overlays.cards.CardMint,
                    onClick = { uriHandler.openUri(bookingUrl) }
                )
            } else if (isTicketmasterBacked && officialUrl != null) {
                com.example.travelcents.ui.main.current.overlays.cards.DetailLinkRow(
                    label = "Tickets",
                    value = "Buy tickets",
                    onClick = { uriHandler.openUri(officialUrl) }
                )
            }
            if (showLocationCard) {
                LocationMapCard(
                    event = event,
                    locationLabel = locationLabel,
                    onOpenMaps = { uriHandler.openUri(mapsUrl) }
                )
            }
            if (showActivityReviews) {
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
}
