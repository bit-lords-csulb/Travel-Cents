package com.example.travelcents.data.ai.chat

import java.util.Locale

object AiRecommendationMapper {
    fun destinationRowFromEngine(
        recommendations: List<AiDestinationRecommendationMatch>
    ): AiDestinationRecommendationRow? {
        val mapped = recommendations
            .filter { recommendation ->
                recommendation.destination.isNotBlank() &&
                    (recommendation.summary.isNotBlank() || recommendation.matchReason.isNotBlank())
            }
            .map { recommendation ->
                AiDestinationRecommendation(
                    id = sanitizeRecommendationId(recommendation.seedId ?: recommendation.destination, "destination"),
                    destination = recommendation.destination.trim(),
                    summary = recommendation.summary.trim(),
                    matchReason = recommendation.matchReason.trim(),
                    seedId = recommendation.seedId
                )
            }
            .distinctBy { recommendation -> normalizeDestinationKey(recommendation.destination) }
            .take(3)

        if (mapped.size < 2) return null

        return AiDestinationRecommendationRow(
            title = "Good destination fits",
            subtitle = "These locations match the direction you have set so far.",
            recommendations = mapped
        )
    }

    fun destinationRowFromIntake(
        recommendations: List<AiTripIntakeDestinationRecommendation>
    ): AiDestinationRecommendationRow? {
        val mapped = recommendations
            .filter { recommendation ->
                recommendation.destination.isNotBlank() &&
                    (recommendation.summary.isNotBlank() || recommendation.reason.isNotBlank())
            }
            .map { recommendation ->
                val destination = recommendation.destination.trim()
                AiDestinationRecommendation(
                    id = sanitizeRecommendationId(recommendation.id, "destination"),
                    destination = destination,
                    summary = recommendation.summary.trim(),
                    matchReason = recommendation.reason.trim().ifBlank {
                        "Fits the direction you have set so far."
                    },
                    seedId = AiCuratedTripSeedCatalog.findSeedByDestination(destination)?.id
                )
            }
            .distinctBy { recommendation -> normalizeDestinationKey(recommendation.destination) }
            .take(3)

        if (mapped.size < 2) return null

        return AiDestinationRecommendationRow(
            title = "Places worth considering",
            subtitle = "These destinations fit what the chat knows so far.",
            recommendations = mapped
        )
    }

    fun placeRowFromIntake(
        recommendations: List<AiTripIntakePlaceRecommendation>
    ): AiPlaceRecommendationRow? {
        val mapped = recommendations
            .filter { recommendation ->
                recommendation.name.isNotBlank() &&
                    recommendation.category.isNotBlank() &&
                    (recommendation.summary.isNotBlank() || recommendation.reason.isNotBlank())
            }
            .map { recommendation ->
                AiPlaceRecommendation(
                    id = sanitizeRecommendationId(recommendation.id, "place"),
                    name = recommendation.name.trim(),
                    category = recommendation.category.trim(),
                    area = recommendation.area.trim(),
                    summary = recommendation.summary.trim(),
                    matchReason = recommendation.reason.trim()
                )
            }
            .distinctBy { recommendation -> recommendation.name.lowercase(Locale.US) }
            .take(3)

        if (mapped.size < 2) return null

        return AiPlaceRecommendationRow(
            title = "Places to consider",
            subtitle = "These spots match the destination and the vibe you have set.",
            recommendations = mapped,
            actionLabels = listOf("Add", "Save", "Swap"),
            actionsEnabled = false
        )
    }

    private fun sanitizeRecommendationId(
        rawId: String,
        prefix: String
    ): String {
        val normalized = rawId
            .trim()
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
        return if (normalized.isBlank()) "${prefix}_option" else normalized
    }

    private fun normalizeDestinationKey(destination: String): String {
        return destination.substringBefore(",").trim().lowercase(Locale.US)
    }
}
