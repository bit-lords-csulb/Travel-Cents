package com.example.travelcents.data.ai.chat

import com.example.travelcents.data.trip.TripAccessRole
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.TripRepository
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCuratedTripCatalogTest {

    private val catalog = AiCuratedTripCatalog(tripRepository = FakeTripRepository())

    @Test
    fun recommendSeededStarterRow_returnsSeededHotspots_whenSignalsMatchPopularDestinations() {
        val row = catalog.recommendSeededStarterRow(
            profile = AiTravelerProfile(
                travelPace = "Relaxed",
                budgetSummary = "Comfort leaning",
                interests = listOf("Beach", "Food"),
                notes = listOf("Want somewhere warm")
            )
        )

        assertNotNull(row)
        assertEquals("Popular trips", row?.title)
        assertTrue(row?.trips?.isNotEmpty() == true)
        assertTrue(row?.trips?.all { starter -> starter.source == AiCuratedTripSource.SEEDED } == true)
        assertTrue(
            row?.trips?.any { starter ->
                starter.destination.contains("Bali", ignoreCase = true) ||
                    starter.destination.contains("Honolulu", ignoreCase = true) ||
                    starter.destination.contains("Cancun", ignoreCase = true)
            } == true
        )
    }

    @Test
    fun recommendSeededStarterRow_returnsEditableSeededStarter_forExplicitHotspotDestination() {
        val row = catalog.recommendSeededStarterRow(
            profile = AiTravelerProfile(
                destination = "Tokyo, Japan",
                interests = listOf("Food", "Nightlife")
            )
        )

        val starter = row?.trips?.singleOrNull()
        assertNotNull(starter)
        assertEquals("Popular trips in Tokyo", row?.title)
        assertEquals(AiCuratedTripSource.SEEDED, starter?.source)
        assertEquals(listOf(4, 5, 7), starter?.durationOptions)
        assertEquals("Tokyo, Japan", starter?.destination)
    }

    @Test
    fun recommendSeededStarterRow_returnsDestinationSpecificSeededStarter() {
        val row = catalog.recommendSeededStarterRow(
            profile = AiTravelerProfile(
                destination = "Paris, France",
                interests = listOf("Culture", "Food")
            )
        )

        assertNotNull(row)
        assertEquals("Popular trips in Paris", row?.title)
        assertEquals(1, row?.trips?.size)
        assertEquals("paris_romance_cafes", row?.trips?.firstOrNull()?.seedId)
    }

    @Test
    fun adjustStarterDuration_updatesSeededStarterToSelectedDuration() {
        val seed = AiCuratedTripSeedCatalog.findSeed("bali_temple_beach")
        val starter = seed!!.toStarter(matchReason = "Fits your vibe", durationDays = 4)

        val adjusted = catalog.adjustStarterDuration(starter, 7)

        assertEquals(starter.id, adjusted.id)
        assertEquals(7, adjusted.durationDays)
        assertTrue(adjusted.title.startsWith("7-day Bali"))
        assertEquals(AiCuratedTripSource.SEEDED, adjusted.source)
    }

    @Test
    fun recommendPlaceRecommendations_returnsSeededPlaces_whenDestinationMatchesSeed() {
        val row = catalog.recommendPlaceRecommendations(
            profile = AiTravelerProfile(
                destination = "Bangkok",
                travelPace = "Relaxed",
                interests = listOf("Food", "Culture")
            )
        )

        assertNotNull(row)
        assertTrue(row?.title?.contains("Bangkok") == true)
        assertEquals(3, row?.recommendations?.size)
        assertTrue(
            row?.recommendations?.any { recommendation ->
                recommendation.name == "Ari" || recommendation.name == "Old Town"
            } == true
        )
    }
}

private class FakeTripRepository(
    private val summaries: List<Itinerary> = emptyList()
) : TripRepository {
    override suspend fun getLatestActiveTripKey(viewerUid: String): TripKey? = null

    override suspend fun getTripSummaries(viewerUid: String): List<Itinerary> = summaries

    override suspend fun getTripSummary(key: TripKey): Itinerary? = summaries.firstOrNull { itinerary ->
        itinerary.ownerUid == key.ownerUid && itinerary.itineraryId == key.tripId
    }

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
