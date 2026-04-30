package com.example.travelcents.data.ai.chat

import com.example.travelcents.data.trip.TripAccessRole
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

const val PREVIEW_TRIP_STATUS = "preview"

object AiCuratedTripToItineraryMapper {

    fun map(
        starter: AiCuratedTripStarter,
        intakeProfile: AiTripIntakeProfile,
        viewerUid: String?,
        today: LocalDate = LocalDate.now()
    ): PreviewTrip {
        val ownerUid = viewerUid?.takeIf { it.isNotBlank() } ?: PREVIEW_OWNER_PLACEHOLDER
        val tripKey = TripKey(ownerUid = ownerUid, tripId = "${PREVIEW_TRIP_ID_PREFIX}${UUID.randomUUID()}")
        val durationDays = starter.durationDays.coerceAtLeast(1)
        val dateFrom = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dateTo = today.plusDays((durationDays - 1).toLong())
            .format(DateTimeFormatter.ISO_LOCAL_DATE)

        val events = buildPreviewEvents(
            starter = starter,
            tripId = tripKey.tripId,
            startDate = today,
            durationDays = durationDays
        )

        val (adults, children) = inferTravelerCounts(intakeProfile)
        val itinerary = Itinerary(
            itineraryId = tripKey.tripId,
            userId = ownerUid,
            tripName = starter.title.ifBlank { starter.destination.ifBlank { "AI Trip Preview" } },
            destination = starter.destination,
            origin = intakeProfile.origin,
            dateFrom = dateFrom,
            dateTo = dateTo,
            durationDays = durationDays,
            currency = "USD",
            travelStyle = starter.travelStyle.ifBlank { "comfort" },
            adults = adults,
            children = children,
            createdAt = Instant.now().toString(),
            status = PREVIEW_TRIP_STATUS,
            eventIds = events.map(TravelEvent::eventId),
            homeImageUrl = starter.heroImageUrl.orEmpty(),
            ownerUid = ownerUid,
            memberUids = listOf(ownerUid),
            roleByUid = mapOf(ownerUid to TripAccessRole.OWNER.wireValue)
        )

        return PreviewTrip(tripKey = tripKey, itinerary = itinerary, events = events)
    }

    private fun buildPreviewEvents(
        starter: AiCuratedTripStarter,
        tripId: String,
        startDate: LocalDate,
        durationDays: Int
    ): List<TravelEvent> {
        val seed = starter.seedId?.let(AiCuratedTripSeedCatalog::findSeed)
            ?: AiCuratedTripSeedCatalog.findSeedByDestination(starter.destination)
        val suggestions = seed?.placeSuggestions.orEmpty()
        if (suggestions.isEmpty()) return emptyList()

        return suggestions.mapIndexed { index, suggestion ->
            val dayOffset = (index % durationDays).toLong()
            val eventDate = startDate.plusDays(dayOffset)
                .format(DateTimeFormatter.ISO_LOCAL_DATE)
            val startTime = previewStartTime(index)

            TravelEvent(
                eventId = "${PREVIEW_EVENT_ID_PREFIX}${UUID.randomUUID()}",
                type = "activity",
                itineraryId = tripId,
                date = eventDate,
                startTime = startTime,
                endTime = previewEndTime(startTime),
                imageUrl = "",
                details = buildMap {
                    put("title", suggestion.name)
                    put("activity_name", suggestion.name)
                    if (suggestion.area.isNotBlank()) {
                        put("location", suggestion.area)
                    }
                    if (suggestion.summary.isNotBlank()) {
                        put("description", suggestion.summary)
                    }
                    if (suggestion.category.isNotBlank()) {
                        put("category", suggestion.category)
                    }
                    put("colorKey", "lightBlue")
                    put("sortOrder", (index / durationDays).toString())
                }
            )
        }
    }

    private fun previewStartTime(index: Int): String {
        val slot = index / 3
        val baseHour = when (index % 3) {
            0 -> 10
            1 -> 14
            else -> 18
        }
        val hour = (baseHour + slot).coerceIn(8, 21)
        return "%02d:00".format(hour)
    }

    private fun previewEndTime(startTime: String): String {
        val parts = startTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return "11:00"
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val nextHour = (hour + 2).coerceAtMost(23)
        return "%02d:%02d".format(nextHour, minute)
    }

    private fun inferTravelerCounts(intakeProfile: AiTripIntakeProfile): Pair<Int, Int> {
        val party = intakeProfile.partySummary.lowercase()
        return when {
            intakeProfile.tripType == AiTripType.SOLO || "solo" in party -> 1 to 0
            intakeProfile.tripType == AiTripType.ROMANTIC || "two adults" in party || "for two" in party -> 2 to 0
            intakeProfile.tripType == AiTripType.FAMILY || "family" in party || "kids" in party || "children" in party -> 2 to 2
            intakeProfile.tripType == AiTripType.FRIENDS || "friends" in party || "group" in party -> 4 to 0
            else -> 2 to 0
        }
    }

    private const val PREVIEW_OWNER_PLACEHOLDER = "preview"
    const val PREVIEW_TRIP_ID_PREFIX = "preview_"
    const val PREVIEW_EVENT_ID_PREFIX = "preview_event_"
}

data class PreviewTrip(
    val tripKey: TripKey,
    val itinerary: Itinerary,
    val events: List<TravelEvent>
)
