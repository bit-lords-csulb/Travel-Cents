# Ticketmaster Discovery API — Integration Plan

## Overview

Ticketmaster Discovery API v2 is a free REST API that returns professionally organized, ticketed events (concerts, sports, theatre, festivals, exhibitions). Spain, France, and most of Europe are first-class markets with direct Ticketmaster coverage. No subscription required — just a free API key.

**Base URL:** `https://app.ticketmaster.com/discovery/v2/`  
**Auth:** Query param `?apikey=YOUR_KEY` on every request  
**Format:** JSON only  
**Register:** https://developer.ticketmaster.com/

---

## API Registration & OAuth

When registering, the developer portal asks for a **Redirect URL**. This is an OAuth 2.0 concept — after a user logs in to authorize an app, their browser is redirected to this URL carrying an auth token.

**For Travel Cents, OAuth is not needed.** The Discovery API is entirely read-only and uses only the API key. The redirect URL field exists because Ticketmaster's registration form is shared with their Commerce API (which does require OAuth). Just enter `https://localhost` to get past the form — it will never be triggered.

**What you actually use:**
```
https://app.ticketmaster.com/discovery/v2/events.json?apikey=YOUR_KEY&...
```
No OAuth flow, no user login, no redirect. Just the API key as a query param.

---

## Ticket Purchasing / Booking

**Ticket purchasing is not available to independent developers.** Ticketmaster has a separate **Commerce API** for buying tickets, but access is restricted to a small number of approved commercial partners (large travel platforms, banks, etc.) and requires a formal agreement with Ticketmaster. It cannot be applied for as an individual developer.

**The standard pattern instead:** Every event object returns a `url` field — a direct link to that event's purchase page on Ticketmaster. Deep-link the user there from your app:

```kotlin
// Open the Ticketmaster purchase page for an event
fun openEventPurchasePage(context: Context, event: TmEvent) {
    event.url?.let { url ->
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}
```

This means Travel Cents surfaces the event and price range, then hands off to Ticketmaster for the actual transaction. No OAuth, no Commerce API access required.

---

## Rate Limits

| Limit | Value |
|---|---|
| Requests per day | 5,000 |
| Requests per second | 5 |
| Max results per request (`size`) | **200** |
| Default results per request | 20 |
| Max total results per search | **1,000** (`size × page < 1000`) |

> To exceed the 1,000-result cap, narrow the query with shorter date windows, classification filters, or multiple targeted requests.

---

## Can You Request 100 Events in Paris on June 13, 2026?

**Yes.** This works perfectly with a single call:

```
GET https://app.ticketmaster.com/discovery/v2/events.json
  ?apikey=YOUR_KEY
  &city=Paris
  &countryCode=FR
  &startDateTime=2026-06-13T00:00:00Z
  &endDateTime=2026-06-13T23:59:59Z
  &size=100
  &page=0
  &sort=date,asc
```

This returns up to 100 events happening in Paris on that specific date in a single API call. If more than 100 exist, increment `page=1` for the next batch (still costs 1 request).

---

## All Event Discovery Endpoints

### 1. Search Events (Primary Discovery Endpoint)

```
GET /discovery/v2/events.json
```

**All search parameters:**

| Parameter | Type | Description |
|---|---|---|
| `keyword` | string | Full-text search across name, description |
| `city` | string | City name (e.g. `Madrid`, `Paris`) |
| `countryCode` | string | ISO 3166-1 alpha-2 (e.g. `ES`, `FR`, `GB`) |
| `stateCode` | string | US/CA state code |
| `postalCode` | string | Zip/postal code |
| `latlong` | string | `lat,lon` (e.g. `40.4168,-3.7038`) |
| `radius` | integer | Search radius in km/miles from `latlong` |
| `unit` | string | `km` or `miles` |
| `startDateTime` | string | ISO 8601 UTC (e.g. `2026-06-13T00:00:00Z`) |
| `endDateTime` | string | ISO 8601 UTC (e.g. `2026-06-13T23:59:59Z`) |
| `localStartDateTime` | string | Local time range `2026-06-13T00:00:00,2026-06-13T23:59:59` |
| `classificationName` | string | `Music`, `Sports`, `Arts & Theatre`, `Film`, `Miscellaneous` |
| `classificationId` | string | Classification ID (more precise than name) |
| `attractionId` | string | Filter by specific artist/team ID |
| `venueId` | string | Filter by specific venue ID |
| `promoterId` | string | Filter by promoter |
| `segmentId` | string | Segment (top-level genre bucket) |
| `genreId` | string | Genre filter |
| `subGenreId` | string | Sub-genre filter |
| `source` | string | `ticketmaster`, `universe`, `frontgate`, `tmr` |
| `onsaleStartDateTime` | string | Filter by when tickets go on sale |
| `onsaleEndDateTime` | string | Filter by end of ticket sale window |
| `sort` | string | `name,asc` / `name,desc` / `date,asc` / `date,desc` / `relevance,asc` / `relevance,desc` / `distance,asc` |
| `size` | integer | Results per page (default: 20, **max: 200**) |
| `page` | integer | Page number, 0-indexed |
| `locale` | string | Response locale (e.g. `en-us`, `es-es`) |
| `includeTBA` | string | Include events with TBA dates: `yes` / `no` / `only` |
| `includeTBD` | string | Include events with TBD dates: `yes` / `no` / `only` |
| `includeTest` | string | Include test events: `yes` / `no` / `only` |

### 2. Get Event by ID

```
GET /discovery/v2/events/{eventId}.json?apikey=YOUR_KEY
```

Returns the complete event object for a known event ID. Use this after search to refresh data.

### 3. Search Venues

```
GET /discovery/v2/venues.json?apikey=YOUR_KEY&keyword=Bernabeu&countryCode=ES
```

Useful for getting a `venueId` to then feed back into event search.

### 4. Get Venue by ID

```
GET /discovery/v2/venues/{venueId}.json?apikey=YOUR_KEY
```

### 5. Search Attractions (Artists / Teams)

```
GET /discovery/v2/attractions.json?apikey=YOUR_KEY&keyword=Coldplay
```

Returns artist/team profiles. Use the returned `attractionId` to find all their events.

### 6. Get Attraction by ID

```
GET /discovery/v2/attractions/{attractionId}.json?apikey=YOUR_KEY
```

### 7. Search Classifications

```
GET /discovery/v2/classifications.json?apikey=YOUR_KEY
```

Returns the full taxonomy of segments → genres → sub-genres. Useful for populating filter dropdowns.

---

## Full Event Response Object

Every field returned by `GET /discovery/v2/events.json`:

```json
{
  "_embedded": {
    "events": [
      {
        "id": "vvG1zZ93p-kGZ9",
        "name": "Rosalía – MOTOMAMI World Tour",
        "type": "event",
        "url": "https://www.ticketmaster.es/event/...",
        "locale": "es-es",
        "test": false,
        "info": "Doors open 30 minutes before showtime.",
        "pleaseNote": "No re-entry permitted.",

        "dates": {
          "start": {
            "localDate": "2026-06-13",
            "localTime": "21:00:00",
            "dateTime": "2026-06-13T19:00:00Z",
            "dateTBD": false,
            "dateTBA": false,
            "timeTBA": false,
            "noSpecificTime": false
          },
          "end": {
            "localDate": "2026-06-13",
            "localTime": "23:30:00",
            "dateTime": "2026-06-13T21:30:00Z",
            "approximate": false,
            "noSpecificTime": false
          },
          "timezone": "Europe/Madrid",
          "status": {
            "code": "onsale"
          },
          "spanMultipleDays": false
        },

        "sales": {
          "public": {
            "startDateTime": "2026-01-15T10:00:00Z",
            "startTBD": false,
            "endDateTime": "2026-06-13T18:00:00Z"
          },
          "presales": [
            {
              "name": "Fan Club Presale",
              "startDateTime": "2026-01-13T10:00:00Z",
              "endDateTime": "2026-01-14T23:59:00Z"
            }
          ]
        },

        "images": [
          {
            "ratio": "16_9",
            "url": "https://s1.ticketm.net/dam/a/abc/event-16_9.jpg",
            "width": 1024,
            "height": 576,
            "fallback": false
          },
          {
            "ratio": "4_3",
            "url": "https://s1.ticketm.net/dam/a/abc/event-4_3.jpg",
            "width": 305,
            "height": 225,
            "fallback": false
          },
          {
            "ratio": "3_2",
            "url": "https://s1.ticketm.net/dam/a/abc/event-3_2.jpg",
            "width": 305,
            "height": 203,
            "fallback": false
          }
        ],

        "classifications": [
          {
            "primary": true,
            "segment": { "id": "KZFzniwnSyZfZ7v7nJ", "name": "Music" },
            "genre": { "id": "KnvZfZ7vAeA", "name": "Pop" },
            "subGenre": { "id": "KZazBEonSMnZfZ7vk1E", "name": "Pop" },
            "type": { "id": "KZAyXgnZfZ7v7nI", "name": "Undefined" },
            "subType": { "id": "KZFzBErXgnZfZ7vAd7", "name": "Undefined" },
            "family": false
          }
        ],

        "priceRanges": [
          {
            "type": "standard",
            "currency": "EUR",
            "min": 45.0,
            "max": 120.0
          }
        ],

        "promoter": {
          "id": "653",
          "name": "LIVE NATION SPAIN",
          "description": "LIVE NATION SPAIN / NTL / 00653"
        },

        "seatmap": {
          "staticUrl": "https://maps.ticketmaster.es/..."
        },

        "_embedded": {
          "venues": [
            {
              "id": "KovZpZAaJkdA",
              "name": "WiZink Center",
              "type": "venue",
              "url": "https://www.ticketmaster.es/venue/...",
              "locale": "es-es",
              "postalCode": "28037",
              "timezone": "Europe/Madrid",
              "city": { "name": "Madrid" },
              "state": { "name": "Community of Madrid", "stateCode": "MD" },
              "country": { "name": "Spain", "countryCode": "ES" },
              "address": { "line1": "Avenida de Felipe II" },
              "location": {
                "longitude": "-3.6779",
                "latitude": "40.4361"
              },
              "markets": [{ "name": "Spain", "id": "423" }],
              "dmas": [{ "id": 10423 }],
              "generalInfo": {
                "generalRule": "No professional cameras.",
                "childRule": "All ages welcome."
              },
              "upcomingEvents": { "_total": 24, "ticketmaster": 24 },
              "images": [...]
            }
          ],
          "attractions": [
            {
              "id": "K8vZ9171Jo0",
              "name": "Rosalía",
              "type": "attraction",
              "url": "https://www.ticketmaster.es/artist/...",
              "images": [
                {
                  "ratio": "16_9",
                  "url": "https://s1.ticketm.net/dam/a/abc/artist-16_9.jpg",
                  "width": 1024,
                  "height": 576,
                  "fallback": false
                }
              ],
              "classifications": [
                {
                  "primary": true,
                  "segment": { "id": "KZFzniwnSyZfZ7v7nJ", "name": "Music" },
                  "genre": { "id": "KnvZfZ7vAeA", "name": "Pop" },
                  "subGenre": { "id": "KZazBEonSMnZfZ7vk1E", "name": "Pop" }
                }
              ],
              "upcomingEvents": { "_total": 12, "ticketmaster": 12 }
            }
          ]
        },

        "_links": {
          "self": { "href": "/discovery/v2/events/vvG1zZ93p-kGZ9?locale=*" }
        }
      }
    ]
  },

  "_links": {
    "first": { "href": "/discovery/v2/events.json?...&page=0" },
    "self": { "href": "/discovery/v2/events.json?...&page=0" },
    "next": { "href": "/discovery/v2/events.json?...&page=1" },
    "last": { "href": "/discovery/v2/events.json?...&page=4" }
  },

  "page": {
    "size": 100,
    "totalElements": 347,
    "totalPages": 4,
    "number": 0
  }
}
```

**`dates.status.code` values:**
- `onsale` — tickets available now
- `offsale` — tickets not currently available
- `cancelled` — event cancelled
- `postponed` — date TBD
- `rescheduled` — new date set

---

## Kotlin / Retrofit Integration

### Data Models

```kotlin
// TicketmasterModels.kt

data class TicketmasterResponse(
    @SerializedName("_embedded") val embedded: EventsEmbedded?,
    val page: PageInfo
)

data class EventsEmbedded(
    val events: List<TmEvent>
)

data class TmEvent(
    val id: String,
    val name: String,
    val url: String?,
    val info: String?,
    val pleaseNote: String?,
    val dates: TmDates,
    val images: List<TmImage>,
    val classifications: List<TmClassification>,
    val priceRanges: List<TmPriceRange>?,
    val seatmap: TmSeatmap?,
    @SerializedName("_embedded") val embedded: TmEventEmbedded?
)

data class TmDates(
    val start: TmDateStart,
    val end: TmDateEnd?,
    val timezone: String?,
    val status: TmStatus?
)

data class TmDateStart(
    val localDate: String,
    val localTime: String?,
    val dateTime: String?,
    val dateTBD: Boolean = false,
    val dateTBA: Boolean = false,
    val timeTBA: Boolean = false
)

data class TmDateEnd(
    val localDate: String?,
    val localTime: String?,
    val dateTime: String?,
    val approximate: Boolean = false
)

data class TmStatus(val code: String)

data class TmImage(
    val ratio: String,
    val url: String,
    val width: Int,
    val height: Int,
    val fallback: Boolean
)

data class TmClassification(
    val primary: Boolean = false,
    val segment: TmGenreNode?,
    val genre: TmGenreNode?,
    val subGenre: TmGenreNode?
)

data class TmGenreNode(val id: String, val name: String)

data class TmPriceRange(
    val type: String,
    val currency: String,
    val min: Double,
    val max: Double
)

data class TmSeatmap(val staticUrl: String?)

data class TmEventEmbedded(
    val venues: List<TmVenue>?,
    val attractions: List<TmAttraction>?
)

data class TmVenue(
    val id: String,
    val name: String,
    val url: String?,
    val postalCode: String?,
    val timezone: String?,
    val city: TmCity?,
    val country: TmCountry?,
    val address: TmAddress?,
    val location: TmLocation?,
    val generalInfo: TmGeneralInfo?
)

data class TmCity(val name: String)
data class TmCountry(val name: String, val countryCode: String)
data class TmAddress(val line1: String?, val line2: String?)
data class TmLocation(val longitude: String, val latitude: String)
data class TmGeneralInfo(val generalRule: String?, val childRule: String?)

data class TmAttraction(
    val id: String,
    val name: String,
    val url: String?,
    val images: List<TmImage>?,
    val classifications: List<TmClassification>?
)

data class PageInfo(
    val size: Int,
    val totalElements: Int,
    val totalPages: Int,
    val number: Int
)
```

### Retrofit API Interface

```kotlin
// TicketmasterApiService.kt

interface TicketmasterApiService {

    @GET("discovery/v2/events.json")
    suspend fun searchEvents(
        @Query("apikey") apiKey: String = BuildConfig.TICKETMASTER_API_KEY,
        @Query("city") city: String? = null,
        @Query("countryCode") countryCode: String? = null,
        @Query("latlong") latlong: String? = null,
        @Query("radius") radius: Int? = null,
        @Query("unit") unit: String? = "km",
        @Query("keyword") keyword: String? = null,
        @Query("classificationName") classificationName: String? = null,
        @Query("startDateTime") startDateTime: String? = null,
        @Query("endDateTime") endDateTime: String? = null,
        @Query("sort") sort: String = "date,asc",
        @Query("size") size: Int = 20,
        @Query("page") page: Int = 0
    ): Response<TicketmasterResponse>

    @GET("discovery/v2/events/{id}.json")
    suspend fun getEvent(
        @Path("id") eventId: String,
        @Query("apikey") apiKey: String = BuildConfig.TICKETMASTER_API_KEY
    ): Response<TmEvent>

    @GET("discovery/v2/venues.json")
    suspend fun searchVenues(
        @Query("apikey") apiKey: String = BuildConfig.TICKETMASTER_API_KEY,
        @Query("keyword") keyword: String,
        @Query("countryCode") countryCode: String? = null
    ): Response<VenueSearchResponse>
}
```

### Retrofit Client Setup

```kotlin
// TicketmasterClient.kt

object TicketmasterClient {
    private const val BASE_URL = "https://app.ticketmaster.com/"

    val service: TicketmasterApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TicketmasterApiService::class.java)
    }
}
```

### Repository

```kotlin
// TicketmasterRepository.kt

class TicketmasterRepository {

    private val api = TicketmasterClient.service

    // 100 events in Paris on June 13, 2026
    suspend fun getParisEventsJune13(): List<TmEvent> {
        val response = api.searchEvents(
            city = "Paris",
            countryCode = "FR",
            startDateTime = "2026-06-13T00:00:00Z",
            endDateTime = "2026-06-13T23:59:59Z",
            size = 100,
            page = 0,
            sort = "date,asc"
        )
        return response.body()?.embedded?.events ?: emptyList()
    }

    // Generic city + date range search for itinerary generation
    suspend fun searchEventsForItinerary(
        city: String,
        countryCode: String,
        startDate: String,   // "2026-06-13T00:00:00Z"
        endDate: String,     // "2026-06-15T23:59:59Z"
        category: String? = null,
        pageSize: Int = 50,
        page: Int = 0
    ): Result<TicketmasterResponse> = runCatching {
        val response = api.searchEvents(
            city = city,
            countryCode = countryCode,
            startDateTime = startDate,
            endDateTime = endDate,
            classificationName = category,
            size = pageSize,
            page = page
        )
        response.body() ?: throw Exception("Empty response")
    }

    // Lat/lon search — useful when user drops a pin on the map
    suspend fun searchEventsByLocation(
        lat: Double,
        lon: Double,
        radiusKm: Int = 20,
        startDateTime: String,
        endDateTime: String,
        size: Int = 50
    ): List<TmEvent> {
        val response = api.searchEvents(
            latlong = "$lat,$lon",
            radius = radiusKm,
            unit = "km",
            startDateTime = startDateTime,
            endDateTime = endDateTime,
            size = size
        )
        return response.body()?.embedded?.events ?: emptyList()
    }

    // Music events only
    suspend fun getMusicEvents(city: String, countryCode: String, startDateTime: String, endDateTime: String) =
        searchEventsForItinerary(city, countryCode, startDateTime, endDateTime, category = "Music")

    // Sports events only
    suspend fun getSportsEvents(city: String, countryCode: String, startDateTime: String, endDateTime: String) =
        searchEventsForItinerary(city, countryCode, startDateTime, endDateTime, category = "Sports")

    // Fetch all pages up to the 1000-result cap
    suspend fun fetchAllEvents(
        city: String,
        countryCode: String,
        startDateTime: String,
        endDateTime: String
    ): List<TmEvent> {
        val allEvents = mutableListOf<TmEvent>()
        var page = 0
        val pageSize = 200

        while (true) {
            val response = api.searchEvents(
                city = city,
                countryCode = countryCode,
                startDateTime = startDateTime,
                endDateTime = endDateTime,
                size = pageSize,
                page = page
            )
            val body = response.body() ?: break
            val events = body.embedded?.events ?: break
            allEvents.addAll(events)

            val pageInfo = body.page
            if (page >= pageInfo.totalPages - 1) break
            if ((page + 1) * pageSize >= 1000) break  // hard cap
            page++
        }

        return allEvents
    }
}
```

### ViewModel Usage

```kotlin
// Example in ItineraryViewModel.kt

viewModelScope.launch {
    val events = ticketmasterRepo.searchEventsForItinerary(
        city = destination.city,
        countryCode = destination.countryCode,
        startDate = trip.startDate.toIso8601Utc(),
        endDate = trip.endDate.toIso8601Utc(),
        pageSize = 100
    ).getOrElse { emptyList() }

    _uiState.update { it.copy(ticketmasterEvents = events) }
}
```

---

## Key Limitations

| Limitation | Detail |
|---|---|
| Only ticketed events | No free community events, no small local gatherings |
| 1,000 result cap | Can't page beyond result #1,000 per query |
| Max 200 per request | Need multiple calls to get up to 1,000 |
| 5,000 req/day | ~25 full paginated searches of 200 per call |
| Ticketmaster coverage only | Events on Ticketmaster.es / Ticketweb; not all venues use it |
| No "what's on tonight" for free events | Bars, free concerts, street events not covered |
| No in-app ticket purchasing | Commerce API is partner-only; deep-link to `event.url` instead |
| OAuth not needed | Discovery API is read-only; ignore the redirect URL during registration |

---

## Recommended Usage in Travel Cents

- Use as the **primary source for major events**: concerts, sports matches, theatre shows, music festivals
- Filter by `classificationName` to show categorized tabs (Music / Sports / Arts)
- Use `priceRanges` to show budget estimates in the itinerary
- Use `_embedded.venues[0].location` (lat/lon) to plot events on a map
- Cache results per city+date window to stay within the 5,000/day limit
- Pair with Meetup API for community/social events to fill gaps
