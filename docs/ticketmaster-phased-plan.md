# Ticketmaster Discovery API — Phased Implementation Plan

This document converts [ticketmaster-api-integration.md](/C:/Users/z503/AndroidStudioProjects/Travel-Cents/docs/ticketmaster-api-integration.md) into a shippable plan for this repo.

## Core Decisions

- Use **Ticketmaster Discovery API** only.
- Use the **consumer key as the API key** via `apikey` query param.
- Do **not** use OAuth, redirect handling, or the consumer secret for Discovery reads.
- Do **not** attempt in-app ticket purchasing. Use the event `url` and deep-link out.
- Treat Ticketmaster events as `TravelEvent(type = "activity")` so they flow through the existing itinerary and detail-card stack.
- Guard every integration point with `BuildConfig.TICKETMASTER_API_KEY.isNotBlank()` so the app falls back cleanly when the key is unset.

## Out Of Scope

- Ticketmaster Commerce API
- OAuth login or redirect handling
- In-app checkout
- Partner-only inventory or reservation APIs

---

## [x] Phase 0 — Config Wiring

Goal: make the Discovery API key available from app code without changing behavior.

Files:
- `local.properties`
- `app/build.gradle.kts`
- `README.md`

Work:
- [x] Add `TICKETMASTER_API_KEY=...` to `local.properties` locally.
- [x] Add `buildConfigField("String", "TICKETMASTER_API_KEY", ...)` in `app/build.gradle.kts` next to the existing external API keys.
- [x] Add Ticketmaster setup notes to `README.md`.

Exit criteria:
- `BuildConfig.TICKETMASTER_API_KEY` compiles.
- App launches with the key set or blank.
- No runtime behavior changes yet.

---

## [x] Phase 1 — Discovery Data Layer

Goal: create a self-contained Ticketmaster integration under `data/trip/` that can search Discovery API and map results into `TravelEvent`.

Files:
- `app/src/main/java/com/example/travelcents/data/trip/model/TicketmasterModels.kt` new
- `app/src/main/java/com/example/travelcents/data/trip/remote/TicketmasterApiService.kt` new
- `app/src/main/java/com/example/travelcents/data/trip/remote/TicketmasterRepository.kt` new
- `app/src/main/java/com/example/travelcents/data/trip/model/EventDetailContract.kt`

Work:
- [x] Add DTOs for the Discovery `/events.json` response.
- [x] Keep only fields the app will use:
  - event `id`, `name`, `url`, `info`, `pleaseNote`
  - `dates.start.localDate`, `dates.start.localTime`, `dates.timezone`, `dates.status.code`
  - `images`
  - `classifications`
  - `priceRanges`
  - `_embedded.venues`
- [x] Add a Retrofit service with `@GET("discovery/v2/events.json")`.
- [x] Follow the existing `SerpRepository` / `WalkScoreRepository` pattern and send auth as query params, not headers.
- [x] Add repository methods:

```kotlin
suspend fun searchEventsForTrip(
    location: String,
    startDate: String,
    endDate: String,
    itineraryId: String,
    classification: String? = null,
    size: Int = 20
): List<TravelEvent>

suspend fun searchEventsForChat(
    location: String,
    startDate: String?,
    endDate: String?,
    keyword: String? = null,
    classification: String? = null,
    size: Int = 10
): List<TravelEvent>
```

- [x] Map each Ticketmaster event into `TravelEvent(type = "activity")`.
- [x] Extend `EventDetailContract.kt` with Ticketmaster-specific fields:
  - `ATTR_VENUE_NAME`
  - `ATTR_TICKET_PRICE_MIN`
  - `ATTR_TICKET_PRICE_MAX`
  - `ATTR_TICKET_CURRENCY`
  - `ATTR_TICKETMASTER_EVENT_ID`

Recommended detail mapping:

| Travel Cents field | Ticketmaster source |
|---|---|
| `date` | `dates.start.localDate` |
| `startTime` | `dates.start.localTime` |
| `tz` | `dates.timezone` |
| `imageUrl` | best event image |
| `ATTR_BUSINESS_NAME` | `name` |
| `ATTR_VENUE_NAME` | `_embedded.venues[0].name` |
| `ATTR_BUSINESS_ADDRESS` | `_embedded.venues[0].address.line1` |
| `ATTR_LATITUDE` / `ATTR_LONGITUDE` | venue location |
| `ATTR_CATEGORIES` | primary segment or genre |
| `ATTR_TICKET_PRICE_MIN` / `MAX` / `CURRENCY` | `priceRanges[0]` |
| `ATTR_BOOKING_URL` | `url` |
| `ATTR_TICKETMASTER_EVENT_ID` | `id` |
| `description` | `info` or `pleaseNote`, truncated |

Implementation notes:
- Base URL: `https://app.ticketmaster.com/`
- Auth: `"apikey" to BuildConfig.TICKETMASTER_API_KEY`
- Date filters: convert trip dates into Discovery-compatible UTC timestamps
- If the API key is blank, return `emptyList()`
- Catch and swallow API failures the same way the existing external repositories do

Exit criteria:
- A debug call for a strong market such as Chicago, New York, Paris, or Madrid returns non-empty `TravelEvent` results.
- The repository can be deleted without affecting other features.

---

## [x] Phase 2 — Trip Generation Pipeline

Goal: bring Ticketmaster events into itinerary generation alongside Yelp activities.

Files:
- `app/src/main/java/com/example/travelcents/ui/main/newTrip/NewTripViewModel.kt`

Current insertion point:
- Step 4 already runs Yelp activity-pool fetches and `YelpRepository.searchEvents(...)` in parallel.

Work:
- [x] Add a second async fetch in Step 4 for `TicketmasterRepository.searchEventsForTrip(...)`.
- [x] Use the same date window already computed for local events.
- [x] Add a small helper to translate trip interests into Ticketmaster classifications:
  - `music`
  - `sports`
  - `arts`
  - `family`
  - `film`
  - `null` when there is no reliable mapping
- [x] Merge Yelp and Ticketmaster event lists.
- [x] De-dupe obvious duplicates by a fuzzy key such as:
  - local date
  - local start time
  - normalized venue name or event name
- [x] Prefer the Ticketmaster copy when both sources represent the same event, because it carries booking URL and ticket pricing.
- [x] Run the merged list through the existing `filterEventsBeforeTime(...)` and `filterEventsAfterTime(...)` logic so flight-window trimming stays consistent.

Exit criteria:
- A generated trip in a major city includes at least one Ticketmaster-backed activity when available.
- Existing Yelp-only generation still works when the Ticketmaster key is blank.
- No duplicate Yelp/Ticketmaster event pair appears in the final event list.

---

## [x] Phase 3 — Current Trip Detail Cards

Goal: render the new Ticketmaster-only fields inside the existing event detail dialog without introducing a new event type.

Files:
- `app/src/main/java/com/example/travelcents/ui/main/current/overlays/cards/VenueCard.kt` new
- `app/src/main/java/com/example/travelcents/ui/main/current/overlays/cards/TicketPricingCard.kt` new
- `app/src/main/java/com/example/travelcents/ui/main/current/CurrentTripEventDetailsDialog.kt`

Work:
- [x] Add `VenueCard` to show venue name and address.
- [x] Add `TicketPricingCard` to show price range and currency.
- [x] Append both cards in the `activity` branch of `EventDetailCardStack`.
- [x] Keep both cards self-hiding when the relevant fields are absent so Yelp activities remain visually unchanged.
- [x] Reuse the existing source/official link row for the Ticketmaster booking URL.

Implementation notes:
- Follow the current overlay-card pattern:
  - early return when data is missing
  - use `DetailCardFrame`
  - keep data flat in `TravelEvent.details`

Exit criteria:
- Ticketmaster-sourced activity details show venue, map, source link, and ticket price range.
- Yelp activities continue to render without empty shells or placeholder UI.

---

## [x] Phase 4 — AI Chat Grounding

Goal: let AI chat answer live event questions with real Ticketmaster data instead of guessing.

Files:
- `app/src/main/java/com/example/travelcents/ui/main/aichat/AiChatViewModel.kt`

Work:
- [x] Add a light intent check for messages about concerts, shows, games, theatre, tickets, and sports.
- [x] When the active trip context has destination + date window, prefetch a small set of Ticketmaster results through `searchEventsForChat(...)`.
- [x] Inject a compact text block of real options into the prompt context.
- [x] Prefer a small, deterministic grounding step first. Do not introduce full tool-calling in the first pass.

Suggested first-pass behavior:
- Fetch top 5 results
- Include:
  - event name
  - date/time
  - venue
  - price range if present
  - booking URL

Exit criteria:
- Asking something like "any good concerts while I'm in Nashville?" returns real upcoming events when the trip context is known.
- Chat behavior is unchanged for unrelated prompts.

---

## [ ] Phase 5 — Hardening, Caching, And Source Attribution

Goal: make the integration production-safe and cheap enough to run regularly.

Files:
- `app/src/main/java/com/example/travelcents/data/trip/remote/TicketmasterRepository.kt`
- `app/src/main/java/com/example/travelcents/ui/main/current/overlays/cards/*`
- test files under `app/src/test/java/com/example/travelcents/...`

Work:
- [ ] Add a short in-memory cache for repeated Ticketmaster searches in chat.
- [ ] Respect Discovery rate limits:
  - 5 requests/second
  - 5,000 requests/day
  - `size * page < 1000`
- [ ] Add source attribution when `ATTR_TICKETMASTER_EVENT_ID` is present.
- [ ] Add tests for:
  - DTO mapping
  - `TravelEvent` conversion
  - merge and dedupe behavior in trip generation
  - blank-key fallback behavior

Recommended tests:
- `TicketmasterRepositoryMappingTest`
- `TicketmasterRepositoryQueryTest`
- `NewTripViewModelTicketmasterMergeTest`

Exit criteria:
- Repeat requests within the same UI flow do not spam the API.
- Mapping logic is covered by unit tests.
- Source attribution is visible for Ticketmaster-backed activities.

---

## Shipping Order

1. Phase 0: key wiring
2. Phase 1: models, Retrofit service, repository
3. Phase 2: itinerary generation integration
4. Phase 3: detail-card rendering
5. Phase 4: AI chat grounding
6. Phase 5: cache, attribution, tests

## Rollback Strategy

- Phase 1 is isolated and can be removed cleanly if needed.
- Phase 2 should always be guarded by `BuildConfig.TICKETMASTER_API_KEY.isNotBlank()`.
- Phase 3 is safe to leave in place because the cards self-hide when data is absent.
- Phase 4 should use the same empty-key guard and no-op when unavailable.
