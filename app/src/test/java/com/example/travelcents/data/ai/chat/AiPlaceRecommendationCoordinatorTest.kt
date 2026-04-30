package com.example.travelcents.data.ai.chat

import com.example.travelcents.data.trip.TripAccessRole
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.TripRepository
import com.example.travelcents.data.trip.model.ATTR_AMENITIES
import com.example.travelcents.data.trip.model.ATTR_GROUP_RATE_PER_NIGHT
import com.example.travelcents.data.trip.model.ATTR_HOTEL_CLASS
import com.example.travelcents.data.trip.model.ATTR_HOTEL_NAME
import com.example.travelcents.data.trip.model.ATTR_HOTEL_RATING
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.TravelRequest
import com.example.travelcents.data.trip.model.YelpBusiness
import com.example.travelcents.data.trip.model.YelpCategory
import com.example.travelcents.data.trip.model.YelpLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPlaceRecommendationCoordinatorTest {
    private val curatedTripCatalog = AiCuratedTripCatalog(tripRepository = TestTripRepository())

    @Test
    fun shouldRecommendPlaces_returnsFalse_whenDestinationConfidenceTooLow() {
        val coordinator = AiPlaceRecommendationCoordinator(
            curatedTripCatalog = curatedTripCatalog,
            restaurantProvider = { _, _ -> emptyList() },
            activityProvider = { _, _ -> emptyList() },
            hotelProvider = { _, _ -> emptyList() }
        )

        val result = coordinator.shouldRecommendPlaces(
            intakeProfile = AiTripIntakeProfile(
                destination = "Tokyo, Japan",
                interests = listOf("Food"),
                confidence = mapOf("destination" to 0.4)
            ),
            profile = AiTravelerProfile(destination = "Tokyo, Japan", interests = listOf("Food"))
        )

        assertFalse(result)
    }

    @Test
    fun recommendRow_returnsRestaurantRow_whenFoodSignalsPresent() = runBlocking {
        val coordinator = AiPlaceRecommendationCoordinator(
            curatedTripCatalog = curatedTripCatalog,
            restaurantProvider = { _, _ ->
                listOf(
                    yelpBusiness("tsukiji", "Tsukiji Outer Market", "Seafood", "$$"),
                    yelpBusiness("ramen", "Ramen Hayashida", "Ramen", "$"),
                    yelpBusiness("yakitori", "Bird Land", "Yakitori", "$$$")
                )
            },
            activityProvider = { _, _ -> emptyList() },
            hotelProvider = { _, _ -> emptyList() }
        )

        val row = coordinator.recommendRow(
            intakeProfile = AiTripIntakeProfile(
                destination = "Tokyo, Japan",
                cuisinePreferences = listOf("Seafood", "Ramen"),
                confidence = mapOf("destination" to 0.9)
            ),
            profile = AiTravelerProfile(
                destination = "Tokyo, Japan",
                interests = listOf("Food"),
                cuisinePreferences = listOf("Seafood")
            )
        )

        assertNotNull(row)
        assertEquals(AiPlaceRecommendationRowType.RESTAURANTS, row?.rowType)
        assertEquals(3, row?.recommendations?.size)
        assertEquals(listOf("Add", "Save", "Swap"), row?.actionLabels)
        assertTrue(row?.recommendations?.firstOrNull()?.matchReason?.contains("Cuisine", ignoreCase = true) == true)
    }

    @Test
    fun recommendRow_returnsHotelRow_whenHotelSignalsPresent() = runBlocking {
        val coordinator = AiPlaceRecommendationCoordinator(
            curatedTripCatalog = curatedTripCatalog,
            restaurantProvider = { _, _ -> emptyList() },
            activityProvider = { _, _ -> emptyList() },
            hotelProvider = { _, _ ->
                listOf(
                    TravelEvent(
                        eventId = "hotel_event",
                        type = "hotel",
                        itineraryId = "trip",
                        details = mapOf(
                            ATTR_HOTEL_NAME to "Palace Hotel Tokyo",
                            ATTR_HOTEL_RATING to "4.8",
                            ATTR_HOTEL_CLASS to "5-star",
                            ATTR_GROUP_RATE_PER_NIGHT to "420.0",
                            ATTR_AMENITIES to "Pool, Spa, WiFi"
                        ),
                        options = listOf(
                            hotelOption("Palace Hotel Tokyo", "4.8", "5-star", "420.0", "Pool, Spa, WiFi", selected = true),
                            hotelOption("The Tokyo Edition", "4.6", "5-star", "390.0", "WiFi, Gym"),
                            hotelOption("Hotel Groove", "4.4", "4-star", "260.0", "WiFi, Lounge")
                        )
                    )
                )
            }
        )

        val row = coordinator.recommendRow(
            intakeProfile = AiTripIntakeProfile(
                destination = "Tokyo, Japan",
                budgetLevel = AiBudgetLevel.LUXURY,
                mustHaves = listOf("Hotel area"),
                confidence = mapOf("destination" to 0.95)
            ),
            profile = AiTravelerProfile(
                destination = "Tokyo, Japan",
                budgetSummary = "Luxury leaning",
                notes = listOf("Need a good hotel base")
            )
        )

        assertNotNull(row)
        assertEquals(AiPlaceRecommendationRowType.HOTELS, row?.rowType)
        assertEquals(3, row?.recommendations?.size)
        assertTrue(row?.recommendations?.firstOrNull()?.summary?.contains("rating") == true)
    }

    @Test
    fun recommendRow_usesCuratedFallback_whenProvidersReturnNothing() = runBlocking {
        val coordinator = AiPlaceRecommendationCoordinator(
            curatedTripCatalog = curatedTripCatalog,
            restaurantProvider = { _, _ -> emptyList() },
            activityProvider = { _, _ -> emptyList() },
            hotelProvider = { _: TravelRequest, _: Itinerary -> emptyList() }
        )

        val row = coordinator.recommendRow(
            intakeProfile = AiTripIntakeProfile(
                destination = "Bangkok, Thailand",
                interests = listOf("Culture"),
                confidence = mapOf("destination" to 0.9)
            ),
            profile = AiTravelerProfile(
                destination = "Bangkok, Thailand",
                interests = listOf("Culture", "Food")
            )
        )

        assertNotNull(row)
        assertTrue(
            row?.rowType == AiPlaceRecommendationRowType.NEIGHBORHOODS ||
                row?.rowType == AiPlaceRecommendationRowType.GENERAL
        )
        assertTrue(row?.recommendations?.isNotEmpty() == true)
    }

    private fun yelpBusiness(
        id: String,
        name: String,
        category: String,
        price: String
    ): YelpBusiness {
        return YelpBusiness(
            id = id,
            name = name,
            rating = 4.6,
            reviewCount = 340,
            price = price,
            categories = listOf(YelpCategory(title = category)),
            location = YelpLocation(
                displayAddress = listOf("Central ${name.substringBefore(' ')}", "Tokyo")
            )
        )
    }

    private fun hotelOption(
        name: String,
        rating: String,
        hotelClass: String,
        pricePerNight: String,
        amenities: String,
        selected: Boolean = false
    ): EventOption {
        return EventOption(
            eventId = "hotel_event",
            source = "serp",
            selected = selected,
            details = mapOf(
                ATTR_HOTEL_NAME to name,
                ATTR_HOTEL_RATING to rating,
                ATTR_HOTEL_CLASS to hotelClass,
                ATTR_GROUP_RATE_PER_NIGHT to pricePerNight,
                ATTR_AMENITIES to amenities
            )
        )
    }
}

private class TestTripRepository : TripRepository {
    override suspend fun getLatestActiveTripKey(viewerUid: String): TripKey? = null

    override suspend fun getTripSummaries(viewerUid: String): List<Itinerary> = emptyList()

    override suspend fun getTripSummary(key: TripKey): Itinerary? = null

    override fun observeTripSummary(key: TripKey): Flow<Itinerary?> = emptyFlow()

    override fun observeTripEvents(key: TripKey): Flow<List<TravelEvent>> = emptyFlow()

    override suspend fun getTripMembers(key: TripKey): List<String> = emptyList()

    override suspend fun getEventOptions(
        key: TripKey,
        eventIds: List<String>
    ): Map<String, List<EventOption>> = emptyMap()

    override suspend fun ensureTripAccess(
        key: TripKey,
        memberUids: List<String>,
        defaultRole: TripAccessRole
    ) = Unit

    override suspend fun backfillOwnedTripAccess(ownerUid: String) = Unit

    override suspend fun deleteTrip(key: TripKey) = Unit
}
