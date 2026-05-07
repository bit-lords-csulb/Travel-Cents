package com.example.travelcents.data.sync

import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.TripRepository
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripSyncCoordinatorTest {

    @Test
    fun refreshHomeIfNeeded_fetchesTripRefsWhenManifestMatches() = runBlocking {
        val tripRefs = listOf(
            itinerary("owner-1", "trip-1", createdAt = "2026-01-01T00:00:00Z"),
            itinerary("owner-2", "trip-2", createdAt = "2026-01-02T00:00:00Z")
        )
        val local = FakeHomeSyncLocalStore(
            homeTripCount = 2,
            manifestVersion = "42"
        )
        val remote = FakeHomeSyncRemoteSource(
            manifest = TripManifestRemote(
                manifestVersion = 42L,
                tripCount = 2,
                latestActiveTripKey = TripKey("owner-1", "trip-1")
            ),
            tripRefs = tripRefs
        )
        val repository = FakeTripRepository()
        val coordinator = TripSyncCoordinator(local, remote, repository)

        coordinator.refreshHomeIfNeeded("viewer-1")

        assertEquals(1, remote.fetchManifestCalls)
        assertEquals(1, remote.fetchTripRefsCalls)
        assertTrue(local.recordedManifestChecks.contains(42L))
        assertEquals(tripRefs, local.lastReplaceRequest?.trips)
        assertEquals(42L, local.lastReplaceRequest?.manifestVersion)
    }

    @Test
    fun refreshHomeIfNeeded_replacesHomeTripsWhenManifestDiffers() = runBlocking {
        val replacementTrips = listOf(
            itinerary("owner-1", "trip-1", createdAt = "2026-01-01T00:00:00Z"),
            itinerary("owner-1", "trip-2", createdAt = "2026-01-02T00:00:00Z")
        )
        val local = FakeHomeSyncLocalStore(
            homeTripCount = 2,
            manifestVersion = "41"
        )
        val remote = FakeHomeSyncRemoteSource(
            manifest = TripManifestRemote(
                manifestVersion = 42L,
                tripCount = 2,
                latestActiveTripKey = TripKey("owner-1", "trip-2")
            ),
            tripRefs = replacementTrips
        )
        val coordinator = TripSyncCoordinator(local, remote, FakeTripRepository())

        coordinator.refreshHomeIfNeeded("viewer-1")

        assertEquals(1, remote.fetchTripRefsCalls)
        assertEquals(replacementTrips, local.lastReplaceRequest?.trips)
        assertEquals(42L, local.lastReplaceRequest?.manifestVersion)
        assertEquals(TripKey("owner-1", "trip-2"), local.lastReplaceRequest?.latestActiveTripKey)
    }

    @Test
    fun refreshHomeIfNeeded_fallsBackToLegacyTripsWhenManifestMissing() = runBlocking {
        val fallbackTrips = listOf(
            itinerary("owner-1", "trip-1", createdAt = "2026-01-01T00:00:00Z"),
            itinerary("owner-1", "trip-2", createdAt = "2026-01-03T00:00:00Z")
        )
        val local = FakeHomeSyncLocalStore()
        val remote = FakeHomeSyncRemoteSource(manifest = null)
        val repository = FakeTripRepository(tripSummaries = fallbackTrips)
        val coordinator = TripSyncCoordinator(local, remote, repository)

        coordinator.refreshHomeIfNeeded("viewer-1")

        assertEquals(1, repository.getTripSummariesCalls)
        assertEquals(fallbackTrips.sortedBy { it.dateFrom }, local.lastReplaceRequest?.trips)
        assertNull(local.lastReplaceRequest?.manifestVersion)
        assertTrue(remote.backfillRequests.contains("viewer-1"))
    }

    private fun itinerary(ownerUid: String, tripId: String, createdAt: String): Itinerary {
        return Itinerary(
            itineraryId = tripId,
            userId = ownerUid,
            tripName = "Trip $tripId",
            destination = "Tokyo",
            origin = "LAX",
            dateFrom = "2026-07-01",
            dateTo = "2026-07-05",
            durationDays = 4,
            currency = "USD",
            travelStyle = "comfort",
            adults = 2,
            children = 0,
            createdAt = createdAt,
            status = "active",
            eventIds = emptyList(),
            ownerUid = ownerUid,
            memberUids = listOf(ownerUid)
        )
    }
}

private data class ReplaceHomeRequest(
    val viewerUid: String,
    val trips: List<Itinerary>,
    val manifestVersion: Long?,
    val latestActiveTripKey: TripKey?
)

private class FakeHomeSyncLocalStore(
    private val homeTripCount: Int = 0,
    private val manifestVersion: String? = null
) : HomeSyncLocalStore {
    val recordedManifestChecks = mutableListOf<Long?>()
    var lastReplaceRequest: ReplaceHomeRequest? = null

    override suspend fun getHomeTripCount(viewerUid: String): Int = homeTripCount

    override suspend fun getManifestVersion(viewerUid: String): String? = manifestVersion

    override suspend fun replaceHomeTripSummaries(
        viewerUid: String,
        trips: List<Itinerary>,
        manifestVersion: Long?,
        latestActiveTripKey: TripKey?
    ) {
        lastReplaceRequest = ReplaceHomeRequest(
            viewerUid = viewerUid,
            trips = trips,
            manifestVersion = manifestVersion,
            latestActiveTripKey = latestActiveTripKey
        )
    }

    override suspend fun recordManifestCheck(viewerUid: String, manifestVersion: Long?) {
        recordedManifestChecks += manifestVersion
    }
}

private class FakeHomeSyncRemoteSource(
    private val manifest: TripManifestRemote?,
    private val tripRefs: List<Itinerary> = emptyList()
) : HomeSyncRemoteSource {
    var fetchManifestCalls: Int = 0
    var fetchTripRefsCalls: Int = 0
    val backfillRequests = mutableListOf<String>()

    override suspend fun fetchManifest(viewerUid: String): TripManifestRemote? {
        fetchManifestCalls += 1
        return manifest
    }

    override suspend fun fetchTripRefs(viewerUid: String): List<Itinerary> {
        fetchTripRefsCalls += 1
        return tripRefs
    }

    override suspend fun backfillTripRefsForViewer(viewerUid: String, trips: List<Itinerary>) {
        backfillRequests += viewerUid
    }
}

private class FakeTripRepository(
    private val tripSummaries: List<Itinerary> = emptyList()
) : TripRepository {
    var getTripSummariesCalls: Int = 0

    override suspend fun getLatestActiveTripKey(viewerUid: String): TripKey? = null

    override suspend fun getTripSummaries(viewerUid: String): List<Itinerary> {
        getTripSummariesCalls += 1
        return tripSummaries
    }

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
        defaultRole: com.example.travelcents.data.trip.TripAccessRole
    ) = Unit

    override suspend fun backfillOwnedTripAccess(ownerUid: String) = Unit

    override suspend fun deleteTrip(key: TripKey) = Unit
}
