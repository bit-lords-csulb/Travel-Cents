# Ticketmaster Integration — Phased Plan

Adds Ticketmaster Discovery API as a third event-ideation source alongside the
existing Yelp business/events and SerpAPI flight/hotel layers. Goal: surface
concerts, sports, theater, and family events into both the AI chat suggestions
and the trip-generation pipeline, while keeping the data layer modular and
reusing the existing `TravelEvent` / `EventOption` contract.

Constraints carried from the rest of the codebase (see CLAUDE.md):

- Every new typed field lives in `EventDetailContract.kt` as an `ATTR_*`
  constant and is read via `detailValue(ATTR_X, "legacy_key")`.
- Every new sub-card early-returns when its data is absent. No empty cards.
- Firestore documents stay flat `Map<String, String>` under
  `TravelEvent.details`.
- Each phase is independently shippable and reversible.

---

## Phase 0 — API key + config wiring

Goal: key reachable from code, nothing else changes.

Files:
- `local.properties` — add `TICKETMASTER_API_KEY=...` (user, not committed).
- `app/build.gradle.kts` — add a `buildConfigField("String",
  "TICKETMASTER_API_KEY", ...)` line next to the existing `SERP_API_KEY` /
  `YELP_API_KEY` entries.
- `CLAUDE.md` — add the new env var to the "API Key Setup" section.
- `docs/Ticketmaster-Integration-Plan.md` — this file.

Exit criteria: `BuildConfig.TICKETMASTER_API_KEY` compiles and resolves at
runtime. No behavior change.

---

## Phase 1 — Data layer (DTOs + Retrofit + repository)

Goal: a self-contained repository that can search Ticketmaster and return
`List<TravelEvent>` shaped like the output of `YelpRepository.searchEvents`.

New files (mirroring the Yelp layout under `data/trip/`):

- `data/trip/model/TicketmasterModels.kt` — response DTOs for the Discovery
  `/events.json` endpoint. Only keep fields we render: `id`, `name`, `url`,
  `info`, `dates.start.localDate`, `dates.start.localTime`, `images[]`,
  `classifications[].segment/genre`, `priceRanges[]`, `_embedded.venues[]`
  (name, address, city, latitude, longitude, timezone).
- `data/trip/remote/TicketmasterApiService.kt` — one Retrofit interface:
  ```kotlin
  interface TicketmasterApiService {
      @GET("discovery/v2/events.json")
      suspend fun searchEvents(@QueryMap params: Map<String, String>): TmSearchResponse
  }
  ```
- `data/trip/remote/TicketmasterRepository.kt` — `object` singleton built like
  `YelpRepository`: OkHttp + Retrofit, `apikey` as a query param
  (Ticketmaster uses query-string auth, not a header), plus two public
  functions:

  ```kotlin
  suspend fun searchEventsForTrip(
      location: String,
      startDate: String,
      endDate: String,
      itineraryId: String,
      classification: String? = null,   // "music", "sports", "arts", ...
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

  Private mapper `ticketmasterToTravelEvent(tm: TmEvent, itineraryId: String)`
  produces a `TravelEvent` with `type = "activity"` (matches the existing
  Yelp events branch so all current cards and filters keep working) and
  populates:

  | Field | Source | Storage key |
  |---|---|---|
  | date / startTime | `dates.start.localDate` / `localTime` | `date`, `startTime` |
  | tz | `dates.timezone` | `tz` |
  | imageUrl | largest `images[]` entry | `imageUrl` |
  | venue name | `_embedded.venues[0].name` | `ATTR_VENUE_NAME` (new) |
  | address | `_embedded.venues[0].address.line1` | `ATTR_BUSINESS_ADDRESS` |
  | lat / lng | venue coords | `ATTR_LATITUDE`, `ATTR_LONGITUDE` |
  | classification | `classifications[0].segment.name` | `ATTR_CATEGORIES` |
  | price min / max | `priceRanges[0].min/max` + currency | `ATTR_TICKET_PRICE_MIN`, `ATTR_TICKET_PRICE_MAX`, `ATTR_TICKET_CURRENCY` (all new) |
  | booking url | `url` | `ATTR_BOOKING_URL` (already exists for hotels) |
  | provider id | `id` | `ATTR_TICKETMASTER_EVENT_ID` (new) |
  | description | `info` or `pleaseNote` (truncated 200) | `description` |

- `data/trip/model/EventDetailContract.kt` — add:
  ```
  ATTR_VENUE_NAME
  ATTR_TICKET_PRICE_MIN
  ATTR_TICKET_PRICE_MAX
  ATTR_TICKET_CURRENCY
  ATTR_TICKETMASTER_EVENT_ID
  ```

Exit criteria: a one-off debug call to
`TicketmasterRepository.searchEventsForTrip("Chicago", "2026-05-01",
"2026-05-05", itineraryId)` returns a non-empty list locally. No UI wired
yet. Reverting the repo touches no other code path.

---

## Phase 2 — Trip generation pipeline

Goal: Ticketmaster events appear on generated itineraries alongside Yelp
events, without breaking the existing flow.

Edits in `ui/main/newTrip/NewTripViewModel.kt`:

- In Step 4 (activities block around line 205–252), extend the existing
  `async { YelpRepository.searchEvents(...) }` with a second parallel async:

  ```kotlin
  val tmEventsDeferred = async {
      TicketmasterRepository.searchEventsForTrip(
          location = itinerary.destination,
          startDate = flightArrivalDate,
          endDate = request.dateTo,
          itineraryId = itinerary.itineraryId,
          classification = interestsToClassification(request.interests)
      )
  }
  ```

- Merge + dedupe: union Yelp + Ticketmaster events, de-dupe by
  `(dateTimeLocal, venueName)` fuzzy key — Ticketmaster wins when both have
  the same show (it carries ticket pricing and booking URL).
- Pass the merged list through the existing `filterEventsBeforeTime` /
  `filterEventsAfterTime` flight-window filters so post-landing / pre-takeoff
  clamping still works.
- No change to `allEvents` assembly below, since Ticketmaster events share
  `type = "activity"`.

New helper (private in the ViewModel, or under `data/ai/repository/` if it
needs LLM access):

```kotlin
private fun interestsToClassification(interests: List<String>): String? {
    // "music", "sports", "arts", "family", "film"
    // null = search across all categories
}
```

Exit criteria: generating a trip for a destination with strong Ticketmaster
coverage (e.g. New York, Chicago) produces at least one TM-sourced activity
event, its expanded card shows price range and booking URL, and no duplicate
Yelp/TM pair appears in the list.

---

## Phase 3 — Card rendering (minimal surface)

Goal: the new TM-only fields render cleanly in the existing activity card
stack, without adding a new event type.

Edits:

- `ui/main/current/overlays/cards/` — add `TicketPricingCard.kt`: shows
  `ATTR_TICKET_PRICE_MIN`–`ATTR_TICKET_PRICE_MAX` with currency label. Wraps
  in `DetailCardFrame(accent = CardMint)` (activity accent). Early-returns
  when no price data.
- Same folder — add `VenueCard.kt`: shows `ATTR_VENUE_NAME` + address + map
  link. Early-returns when no venue.
- `ui/main/current/overlays/CurrentTripEventDetailsDialog.kt` — in the
  `activity` branch of `EventDetailCardStack`, append `VenueCard` and
  `TicketPricingCard` after the existing cards. Both self-hide when empty so
  Yelp activities are unaffected.
- Reuse the existing `DetailLinkRow` for the "Buy tickets" CTA backed by
  `ATTR_BOOKING_URL`.

No new event type is introduced — the activity stack is the fallback for
generic events per the card matrix in CLAUDE.md.

Exit criteria: a Ticketmaster-sourced activity shows venue + price + tickets
CTA; a Yelp-sourced activity is visually unchanged.

---

## Phase 4 — AI chat integration

Goal: `AiTripChatPage` can suggest real concerts / games / shows with live
dates + prices, not hallucinated ones.

Option A (simplest, ship first): tool-free text injection.

- In `ui/main/aichat/AiChatViewModel.kt`, when the user's message matches a
  light intent filter ("concert", "show", "game", "tickets", "sports",
  "theater") and the active chat has a resolved destination + date window,
  pre-fetch `TicketmasterRepository.searchEventsForChat(...)` and inject the
  top 5 results into the system prompt as a compact bulleted block ("Real
  Ticketmaster options for your trip: ..."). The LLM then grounds its
  response in real events.
- Render each surfaced event as an AI follow-up card (reusing the existing
  AI Chat Overhaul card layer) so users can tap-to-add — same UX the plan
  already targets for recommendations.

Option B (Phase 4b, defer): full tool-use. Add a `search_ticketmaster` tool
schema to the LLM's tools list, route tool calls through
`TicketmasterRepository`, render results as cards. Only pursue once Option A
proves users want this.

Exit criteria (Option A): in the AI chat, asking "any good concerts while
I'm in Nashville?" returns real upcoming shows with venues, dates, and a
"Buy tickets" card action that opens the TM url.

---

## Phase 5 — Polish

Only tackle after Phase 2–4 ship and get used:

- In-memory cache for chat-side searches (60s TTL) — mirror `SerpCache.kt`.
- Persist cached trip-side results under `data/trip/local/` if TM rate
  limiting bites (free tier is 5 req/s, 5000/day — fine for current volume).
- Add a "Source: Ticketmaster" chip to the activity card header when
  `ATTR_TICKETMASTER_EVENT_ID` is present, matching the existing "Source:
  Yelp" treatment.
- Extend `interestsToClassification()` to multi-classification queries
  (`classificationName=music,sports`) if users want blended results.
- Localize the currency rendering in `TicketPricingCard` through
  `CurrencyRateCache` for travelers in non-USD destinations.

---

## Rollback notes

Each phase can be reverted independently:

- Phase 1 leaves no call sites outside itself — delete the three files.
- Phase 2 is one `async` + one merge helper; guard it behind a
  `BuildConfig.TICKETMASTER_API_KEY.isNotBlank()` check so an unset key
  silently falls back to Yelp-only.
- Phase 3 cards self-hide when fields are missing; leaving them in place
  after a Phase 2 revert is harmless.
- Phase 4 should ship behind the same empty-key guard.
