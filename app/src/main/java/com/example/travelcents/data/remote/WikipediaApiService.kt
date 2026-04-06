package com.example.travelcents.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface WikipediaApiService {
    // Wikipedia API: free, no key required
    // GET /w/api.php?action=query&titles=Maui&prop=pageimages&format=json&pithumbsize=600
    // Response: { "query": { "pages": { "<id>": { "thumbnail": { "source": "https://..." } } } } }
    @GET("w/api.php")
    suspend fun getPageImage(
        @Query("action") action: String,
        @Query("titles") titles: String,
        @Query("prop") prop: String,
        @Query("format") format: String,
        @Query("pithumbsize") size: Int
    ): WikipediaImageResponse
}

data class WikipediaImageResponse(
    val query: WikipediaQuery?
)

data class WikipediaQuery(
    val pages: Map<String, WikipediaPage>?
)

data class WikipediaPage(
    val thumbnail: WikipediaThumbnail?
)

data class WikipediaThumbnail(
    val source: String?
)
