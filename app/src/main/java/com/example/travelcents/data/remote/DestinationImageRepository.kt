package com.example.travelcents.data.remote

import android.util.Log

data class DestinationImageResult(
    val imageUrl: String?,
    val matchedTitle: String?,
    val matchedQuery: String?,
    val reason: String?,
    val triedQueries: List<String>
)

class DestinationImageRepository(
    private val wikipedia: WikipediaApiService
) {
    suspend fun resolveDestinationImage(destination: String): DestinationImageResult {
        val normalizedDestination = destination.trim()
        if (normalizedDestination.isBlank()) {
            return DestinationImageResult(
                imageUrl = null,
                matchedTitle = null,
                matchedQuery = null,
                reason = "Destination was blank",
                triedQueries = emptyList()
            )
        }

        val city = normalizeCity(normalizedDestination)
        val country = normalizeCountry(normalizedDestination)
        val queryVariants = buildQueryVariants(normalizedDestination, city, country)
        val triedQueries = mutableListOf<String>()
        val candidates = LinkedHashSet<CandidateRequest>()

        queryVariants.forEach { query ->
            triedQueries += query
            candidates += CandidateRequest(requestedQuery = query, pageTitle = query)

            val searchTitles = runCatching {
                wikipedia.searchPages(
                    action = "query",
                    list = "search",
                    searchQuery = query,
                    format = "json",
                    limit = 4,
                    namespace = 0
                )
            }.onFailure { error ->
                Log.w(TAG, "Search failed for '$query': ${error.message}")
            }.getOrNull()
                ?.query?.search
                .orEmpty()
                .mapNotNull { it.title?.trim() }

            searchTitles.forEach { title ->
                candidates += CandidateRequest(requestedQuery = query, pageTitle = title)
            }
        }

        val evaluated = candidates
            .take(MAX_CANDIDATES)
            .mapNotNull { candidate ->
                fetchPageImage(candidate)?.let { page ->
                    // Prefer Wikimedia's generated thumbnail because it is a
                    // rasterized, app-friendly asset. Original files are often
                    // SVG/TIFF and may not render reliably in Coil.
                    val imageUrl = page.thumbnail?.source ?: page.original?.source
                    if (imageUrl == null) {
                        null
                    } else {
                        ScoredCandidate(
                            imageUrl = imageUrl,
                            pageTitle = page.title ?: candidate.pageTitle,
                            requestedQuery = candidate.requestedQuery,
                            score = scoreCandidate(
                                title = page.title ?: candidate.pageTitle,
                                requestedQuery = candidate.requestedQuery,
                                destination = normalizedDestination,
                                city = city,
                                country = country
                            )
                        )
                    }
                }
            }
            .sortedByDescending { it.score }

        val best = evaluated.firstOrNull()
        if (best == null) {
            return DestinationImageResult(
                imageUrl = null,
                matchedTitle = null,
                matchedQuery = null,
                reason = "No Wikimedia result had a usable image",
                triedQueries = triedQueries
            )
        }

        return DestinationImageResult(
            imageUrl = best.imageUrl,
            matchedTitle = best.pageTitle,
            matchedQuery = best.requestedQuery,
            reason = null,
            triedQueries = triedQueries
        )
    }

    private suspend fun fetchPageImage(candidate: CandidateRequest): WikipediaPage? {
        return runCatching {
            wikipedia.getPageImage(
                action = "query",
                titles = candidate.pageTitle,
                prop = "pageimages",
                format = "json",
                size = 900,
                imageProps = "original|thumbnail|name",
                redirects = 1
            )
        }.onFailure { error ->
            Log.w(TAG, "Image lookup failed for '${candidate.pageTitle}': ${error.message}")
        }.getOrNull()
            ?.query?.pages?.values
            ?.firstOrNull()
    }

    private fun buildQueryVariants(destination: String, city: String, country: String): List<String> {
        return linkedSetOf(
            destination,
            city,
            if (country.isNotBlank()) "$city, $country" else null,
            if (country.isNotBlank()) "$city $country" else null,
            "$city skyline",
            "$city landmarks",
            "$city downtown"
        )
            .filterNotNull()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
    }

    private fun normalizeCity(destination: String): String {
        val rawCity = destination.substringBefore(",").trim()
        return CITY_ALIASES[rawCity.lowercase()] ?: rawCity
    }

    private fun normalizeCountry(destination: String): String {
        val rawCountry = destination.substringAfter(",", "").trim()
        if (rawCountry.isBlank()) return ""
        return COUNTRY_ALIASES[rawCountry.lowercase()] ?: rawCountry
    }

    private fun scoreCandidate(
        title: String,
        requestedQuery: String,
        destination: String,
        city: String,
        country: String
    ): Int {
        val titleLower = title.lowercase()
        val queryLower = requestedQuery.lowercase()
        val destinationLower = destination.lowercase()
        val cityLower = city.lowercase()
        val countryLower = country.lowercase()

        var score = 0

        if (titleLower == cityLower) score += 220
        if (titleLower == destinationLower) score += 180
        if (countryLower.isNotBlank() && titleLower == "$cityLower, $countryLower") score += 160
        if (titleLower.startsWith(cityLower)) score += 80
        if (titleLower.contains(cityLower)) score += 55
        if (countryLower.isNotBlank() && titleLower.contains(countryLower)) score += 20
        if (queryLower == cityLower) score += 20
        if (queryLower == destinationLower) score += 10

        if (queryLower.contains("skyline") || queryLower.contains("landmarks") || queryLower.contains("downtown")) {
            score += 10
        }

        NEGATIVE_TITLE_HINTS.forEach { hint ->
            if (titleLower.contains(hint)) score -= 150
        }

        if (titleLower.length > 40) score -= 10
        return score
    }

    private data class CandidateRequest(
        val requestedQuery: String,
        val pageTitle: String
    )

    private data class ScoredCandidate(
        val imageUrl: String,
        val pageTitle: String,
        val requestedQuery: String,
        val score: Int
    )

    private companion object {
        private const val TAG = "DestinationImageRepo"
        private const val MAX_CANDIDATES = 12

        private val COUNTRY_ALIASES = mapOf(
            "uae" to "United Arab Emirates",
            "uk" to "United Kingdom",
            "usa" to "United States"
        )

        private val CITY_ALIASES = mapOf(
            "new york" to "New York City",
            "washington d.c." to "Washington, D.C."
        )

        private val NEGATIVE_TITLE_HINTS = listOf(
            "disambiguation",
            "list of",
            "history of",
            "economy of",
            "flag of",
            "coat of arms",
            "airport",
            "station",
            "province",
            "district",
            "county"
        )
    }
}
