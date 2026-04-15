package com.example.travelcents.data.trip.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface WikipediaApiService {
    // MediaWiki API: free, no key required.
    // We use both page-image lookup and search so trip cards can resolve a
    // sensible image even when the exact stored destination text is not a page title.
    @GET("w/api.php")
    suspend fun getPageImage(
        @Query("action") action: String,
        @Query("titles") titles: String,
        @Query("prop") prop: String,
        @Query("format") format: String,
        @Query("pithumbsize") size: Int,
        @Query("piprop") imageProps: String,
        @Query("redirects") redirects: Int
    ): WikipediaImageResponse

    @GET("w/api.php")
    suspend fun searchPages(
        @Query("action") action: String,
        @Query("list") list: String,
        @Query("srsearch") searchQuery: String,
        @Query("format") format: String,
        @Query("srlimit") limit: Int,
        @Query("srnamespace") namespace: Int
    ): WikipediaSearchResponse
}

data class WikipediaImageResponse(
    val query: WikipediaQuery?
)

data class WikipediaSearchResponse(
    val query: WikipediaSearchQuery?
)

data class WikipediaQuery(
    val pages: Map<String, WikipediaPage>?
)

data class WikipediaSearchQuery(
    val search: List<WikipediaSearchResult>?
)

data class WikipediaSearchResult(
    val title: String?
)

data class WikipediaPage(
    val title: String?,
    val pageimage: String?,
    val original: WikipediaOriginal?,
    val thumbnail: WikipediaThumbnail?
)

data class WikipediaThumbnail(
    val source: String?
)

data class WikipediaOriginal(
    val source: String?
)


