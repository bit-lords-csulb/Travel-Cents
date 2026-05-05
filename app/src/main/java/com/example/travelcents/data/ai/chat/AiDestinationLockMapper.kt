package com.example.travelcents.data.ai.chat

import com.example.travelcents.data.ai.chat.AiCuratedTripToItineraryMapper.PREVIEW_EVENT_ID_PREFIX
import com.example.travelcents.data.ai.chat.AiCuratedTripToItineraryMapper.PREVIEW_TRIP_ID_PREFIX
import com.example.travelcents.data.trip.TripAccessRole
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

object AiDestinationLockMapper {

    fun map(
        destination: String,
        intakeProfile: AiTripIntakeProfile,
        viewerUid: String?,
        today: LocalDate = LocalDate.now()
    ): PreviewTrip {
        val ownerUid = viewerUid?.takeIf { it.isNotBlank() } ?: "preview"
        val tripId = "${PREVIEW_TRIP_ID_PREFIX}${UUID.randomUUID()}"
        val tripKey = TripKey(ownerUid = ownerUid, tripId = tripId)

        // Default dates — Phase 4 will replace these via LLM clarification
        val dateFrom = today.plusDays(14).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dateTo = today.plusDays(17).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val durationDays = 4

        val cityName = destination.substringBefore(",").trim().ifBlank { destination }

        val events = buildPlaceholderFlights(tripId = tripId, dateFrom = dateFrom, dateTo = dateTo)
        val (adults, children) = inferTravelerCounts(intakeProfile)

        val itinerary = Itinerary(
            itineraryId = tripId,
            userId = ownerUid,
            tripName = "Trip to $cityName",
            destination = destination,
            origin = intakeProfile.origin,
            dateFrom = dateFrom,
            dateTo = dateTo,
            durationDays = durationDays,
            currency = "USD",
            travelStyle = "comfort",
            adults = adults,
            children = children,
            createdAt = Instant.now().toString(),
            status = PREVIEW_TRIP_STATUS,
            eventIds = events.map(TravelEvent::eventId),
            homeImageUrl = "",
            ownerUid = ownerUid,
            memberUids = listOf(ownerUid),
            roleByUid = mapOf(ownerUid to TripAccessRole.OWNER.wireValue)
        )

        return PreviewTrip(tripKey = tripKey, itinerary = itinerary, events = events)
    }

    private fun buildPlaceholderFlights(
        tripId: String,
        dateFrom: String,
        dateTo: String
    ): List<TravelEvent> = listOf(
        TravelEvent(
            eventId = "${PREVIEW_EVENT_ID_PREFIX}${UUID.randomUUID()}",
            type = "flight",
            itineraryId = tripId,
            date = dateFrom,
            startTime = "08:00",
            endTime = "12:00",
            imageUrl = "",
            details = mapOf("title" to "Outbound flight")
        ),
        TravelEvent(
            eventId = "${PREVIEW_EVENT_ID_PREFIX}${UUID.randomUUID()}",
            type = "flight",
            itineraryId = tripId,
            date = dateTo,
            startTime = "14:00",
            endTime = "18:00",
            imageUrl = "",
            details = mapOf("title" to "Return flight")
        )
    )

    private fun inferTravelerCounts(intakeProfile: AiTripIntakeProfile): Pair<Int, Int> {
        val party = intakeProfile.partySummary.lowercase()
        return when {
            intakeProfile.tripType == AiTripType.SOLO || "solo" in party -> 1 to 0
            intakeProfile.tripType == AiTripType.ROMANTIC || "two adults" in party || "for two" in party -> 2 to 0
            intakeProfile.tripType == AiTripType.FAMILY || "family" in party || "kids" in party -> 2 to 2
            intakeProfile.tripType == AiTripType.FRIENDS || "friends" in party || "group" in party -> 4 to 0
            else -> 2 to 0
        }
    }
}
