package com.example.travelcents.data.ai.chat

import com.example.travelcents.data.trip.FirestoreTripRepository
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.TripRepository
import com.example.travelcents.data.trip.model.Itinerary
import kotlin.math.abs

class AiCuratedTripCatalog(
    private val tripRepository: TripRepository = FirestoreTripRepository()
) {
    suspend fun recommendTrips(
        profile: AiTravelerProfile,
        viewerUid: String?
    ): AiCuratedTripRow? {
        val curatedTrips = viewerUid
            ?.takeIf { it.isNotBlank() }
            ?.let { uid -> loadCuratedTrips(profile, uid) }
            .orEmpty()

        return if (curatedTrips.isNotEmpty()) {
            AiCuratedTripRow(
                title = "Start from a saved trip",
                subtitle = "These curated trips match the direction you've set so far.",
                trips = curatedTrips.take(3)
            )
        } else {
            buildGeneratedStarter(profile)?.let { starter ->
                AiCuratedTripRow(
                    title = "Start from scratch",
                    subtitle = "I couldn't find a saved match, so here's a fresh starter.",
                    trips = listOf(starter)
                )
            }
        }
    }

    private suspend fun loadCuratedTrips(
        profile: AiTravelerProfile,
        viewerUid: String
    ): List<AiCuratedTripStarter> {
        val destinationKey = normalizeDestinationKey(profile.destination)
        if (destinationKey.isBlank()) return emptyList()

        return runCatching {
            tripRepository.getTripSummaries(viewerUid)
        }.getOrElse {
            emptyList()
        }.filterNot { itinerary ->
            itinerary.status.equals("archived", ignoreCase = true)
        }.mapNotNull { itinerary ->
            val score = scoreTrip(profile, itinerary, destinationKey)
            if (score <= 0) return@mapNotNull null

            ScoredTrip(
                score = score,
                starter = itinerary.toStarter(
                    matchReason = buildMatchReason(profile, itinerary)
                )
            )
        }.sortedWith(
            compareByDescending<ScoredTrip> { scored -> scored.score }
                .thenByDescending { scored -> scored.starter.durationDays }
                .thenBy { scored -> scored.starter.title }
        ).map { scored -> scored.starter }
            .distinctBy { starter -> starter.tripKey?.let { "${it.ownerUid}:${it.tripId}" } ?: starter.id }
    }

    private fun scoreTrip(
        profile: AiTravelerProfile,
        itinerary: Itinerary,
        destinationKey: String
    ): Int {
        val itineraryDestinationKey = normalizeDestinationKey(itinerary.destination)
        val destinationScore = when {
            itineraryDestinationKey == destinationKey -> 90
            itinerary.destination.contains(profile.destination, ignoreCase = true) -> 70
            profile.destination.contains(itinerary.destination, ignoreCase = true) -> 55
            else -> 0
        }
        if (destinationScore == 0) return 0

        var score = destinationScore

        val requestedStyle = inferTravelStyle(profile)
        if (requestedStyle.isNotBlank() && itinerary.travelStyle.equals(requestedStyle, ignoreCase = true)) {
            score += 18
        }

        val requestedDays = inferDurationDays(profile)
        if (requestedDays > 0 && itinerary.durationDays > 0) {
            val distance = abs(itinerary.durationDays - requestedDays)
            score += when {
                distance == 0 -> 14
                distance == 1 -> 10
                distance <= 2 -> 6
                else -> 0
            }
        }

        if (profile.partySummary.contains("family", ignoreCase = true) && itinerary.children > 0) {
            score += 10
        }

        if (profile.partySummary.contains("two", ignoreCase = true) && itinerary.adults == 2 && itinerary.children == 0) {
            score += 8
        }

        return score
    }

    private fun buildMatchReason(
        profile: AiTravelerProfile,
        itinerary: Itinerary
    ): String {
        val reasons = buildList {
            add("Matches ${itinerary.destination}")

            val requestedStyle = inferTravelStyle(profile)
            if (requestedStyle.isNotBlank() && itinerary.travelStyle.equals(requestedStyle, ignoreCase = true)) {
                add(styleLabel(itinerary.travelStyle))
            }

            val requestedDays = inferDurationDays(profile)
            if (requestedDays > 0 && itinerary.durationDays > 0) {
                add("${itinerary.durationDays}-day fit")
            }
        }

        return reasons.joinToString(" • ")
    }

    private fun Itinerary.toStarter(matchReason: String): AiCuratedTripStarter {
        return AiCuratedTripStarter(
            title = tripName,
            destination = destination,
            durationDays = durationDays.coerceAtLeast(1),
            travelStyle = travelStyle.ifBlank { "comfort" },
            summary = buildSummary(this),
            matchReason = matchReason,
            source = AiCuratedTripSource.FIRESTORE,
            tripKey = TripKey(ownerUid = ownerUid, tripId = itineraryId)
        )
    }

    private fun buildGeneratedStarter(profile: AiTravelerProfile): AiCuratedTripStarter? {
        val destination = profile.destination.ifBlank { return null }
        val style = inferTravelStyle(profile).ifBlank { "comfort" }
        val durationDays = inferDurationDays(profile).coerceAtLeast(4)
        val summary = buildString {
            append("${durationDays}-day ")
            append(destination)
            append(" starter")
            profile.travelPace.takeIf { it.isNotBlank() }?.let { pace ->
                append(" with a ")
                append(pace.lowercase())
                append(" pace")
            }
            val highlights = (profile.interests + profile.cuisinePreferences)
                .distinct()
                .take(2)
            if (highlights.isNotEmpty()) {
                append(" focused on ")
                append(highlights.joinToString(" and ") { it.lowercase() })
            }
        }

        return AiCuratedTripStarter(
            title = "${durationDays}-day ${destination}",
            destination = destination,
            durationDays = durationDays,
            travelStyle = style,
            summary = summary,
            matchReason = "Fresh starter built from your current chat signals.",
            source = AiCuratedTripSource.GENERATED
        )
    }

    private fun buildSummary(itinerary: Itinerary): String {
        val style = styleLabel(itinerary.travelStyle)
        return buildString {
            append("${itinerary.durationDays.coerceAtLeast(1)}-day ")
            append(itinerary.destination)
            if (style.isNotBlank()) {
                append(" • ")
                append(style)
            }
            if (itinerary.adults > 0 || itinerary.children > 0) {
                append(" • ")
                append(
                    when {
                        itinerary.children > 0 -> "family-ready"
                        itinerary.adults == 2 -> "for two"
                        itinerary.adults > 2 -> "group-friendly"
                        else -> "solo-friendly"
                    }
                )
            }
        }
    }

    private fun inferTravelStyle(profile: AiTravelerProfile): String {
        val budget = profile.budgetSummary.lowercase()
        return when {
            "luxury" in budget -> "luxury"
            "budget" in budget -> "budget"
            "comfort" in budget || "splurge" in budget -> "comfort"
            else -> ""
        }
    }

    private fun inferDurationDays(profile: AiTravelerProfile): Int {
        val dateWindow = profile.dateWindow.lowercase()
        return when {
            "weekend" in dateWindow -> 3
            "week" in dateWindow -> 7
            "summer" in dateWindow || "spring" in dateWindow || "fall" in dateWindow || "winter" in dateWindow -> 6
            else -> 4
        }
    }

    private fun styleLabel(rawStyle: String): String {
        return when (rawStyle.lowercase()) {
            "budget" -> "Budget"
            "luxury" -> "Luxury"
            "comfort" -> "Comfort"
            else -> rawStyle.replaceFirstChar { it.uppercase() }
        }
    }

    private fun normalizeDestinationKey(destination: String): String {
        return destination
            .substringBefore(",")
            .trim()
            .lowercase()
    }

    private data class ScoredTrip(
        val score: Int,
        val starter: AiCuratedTripStarter
    )
}
