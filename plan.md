# Travel Cents — Master Implementation Plan

> Created: 2026-03-30
> Updated: 2026-03-31
> Branch: new-new-trip-layout

---

## Phase 0 — Completed Work (v1 New Trip Flow Overhaul)

| # | Task | Files |
|---|------|-------|
| 1 | Landing page header + X/close navigates to landing, back arrow goes one step back | `NewTripLandingPage.kt` |
| 2 | Step 1: destination autocomplete (150 cities), origin field with GPS/geocoder, fallback card for unknown destinations | `TripStep1DestinationPage.kt`, `NewTripViewModel.kt` |
| 3 | Step 2: month/year picker dialog (3×4 grid + year arrows), chevron single-step nav retained | `TripStep2DatesPage.kt` |
| 4 | Step 3: children/pets toggle → +/- stepper pattern, pets shown separately from total | `TripStep3TravelersPage.kt` |
| 5 | Step 4: budget text field synced with slider, $50 increments, tier badges | `TripStep4BudgetPage.kt` |
| 6 | Step 5: 20 interest cards, search bar with `+ Add "X"` chip, custom tags, ~80 keyword vocabulary | `TripStep5InterestsPage.kt` |
| 7 | Generation loading screen: 5 animated step cards, error state, "View My Trip →" button | `TripGeneratingPage.kt`, `NewTripViewModel.kt`, `MainScaffold.kt` |
| 8 | Trip preview page: summary header, timeline with day grouping, multi-trip switcher, archive/delete | `FinalPlan.kt` |
| 9 | AI chat screen: flat header matching wizard style, gradient accent line | `AiTripChatPage.kt` |
| 10 | Unified "Continue to X" button labels across all 5 steps | All step pages |
| 11 | Login screen Shift+Tab reverse focus navigation | `LoginPage.kt` |

---

## Phase 1 — Performance & Image Loading Fixes

### Task 1.1 — Bundle Static Images (Steps 1, 5, Landing)
**Decision:** Bundle all wizard static images as APK drawable resources. Remove all runtime URL fetching for these images. Images were directly sourced and added to `res/drawable-nodpi/` (git history recovery steps were skipped — not needed).
**Files:** `TripStep1DestinationPage.kt`, `TripStep5InterestsPage.kt`, `NewTripLandingPage.kt`, `res/drawable-nodpi/`

- [x] Source and add to `res/drawable-nodpi/`: 6 destination images (Paris, Tokyo, Bali, New York, London, Dubai) + 2 landing page card images
- [x] Source and add to `res/drawable-nodpi/`: 20 interest images — replaced all 8 Google Aida URLs and all 12 LoremFlickr placeholders with local assets
- [x] Replace every `AsyncImage(model = "https://...")` for these images with `painterResource(R.drawable.*)`
- [x] Remove LoremFlickr URLs from `TripStep5InterestsPage.kt` entirely
- [x] Remove Google Aida CDN URLs from all wizard pages

### Task 1.2 — Reduce Main Thread Work
**Problem:** `Choreographer: Skipped 137 frames!` — heavy work on main thread during navigation.
**Files:** `ChatsViewModel.kt`, `FriendsViewModel.kt`, `ItineraryViewModel.kt`, `MainScaffold.kt`

- [x] Move Firestore listener setup out of `init{}` blocks — defer to first collection in `viewModelScope.launch`
- [x] Fix `FriendsViewModel` O(n) nested listeners — batch friend lookups into a single `whereIn` query
- [x] Fix `FirestoreRepository.listenToDirectChatPreviews()` N+1 problem — batch user name lookups
- [x] Remove eager `sharedItineraryViewModel.loadTrip()` from `MainScaffold` LaunchedEffect — load only when navigating to trip screen
- [x] `ItineraryViewModel.fetchAllTrips()` — add `.whereEqualTo("userId", uid)` server-side filter instead of client-side

### Task 1.3 — Reduce Logging Overhead
**Problem:** `HttpLoggingInterceptor.Level.BODY` serializes full request/response bodies in all builds.
**Files:** `GroqRepository.kt`, `SerpRepository.kt`

- [x] Gate logging level behind `BuildConfig.DEBUG`: `BODY` in debug, `NONE` in release
- [x] Apply to both OkHttpClient instances (Groq + SerpAPI)

---

## Phase 2 — Event Management Pipeline

### Task 2.1 — Data Model: Multi-Option Events
**Problem:** Current `TravelEvent` holds a single option. Need to support multiple alternatives per event slot.
**Files:** `TravelEvent.kt` (new fields), new model `EventOption.kt`

- [x] Create `EventOption` data class: `optionId`, `eventId`, `source` (serp/yelp/groq), `selected` (Boolean), `votes` (Map<userId, vote>), `imageUrl`, `localImagePath`, `details` (Map)
- [x] No `rejected` field persisted — rejection state is session-only (in-memory `Set<optionId>` in ViewModel)
- [x] **Price display fields** to always populate in `details` map where available:
  - Flights: `price` (numeric, total round-trip), `price_level` ("low"/"typical"/"high" from SerpAPI insights)
  - Hotels: `rate_per_night` (numeric, per room), `group_rate_per_night` (numeric, × rooms needed), `rooms_needed` (int), `total_rate` (full stay, grouped), `deal` (string label if present e.g. "20% off")
  - Restaurants: `price_tier` ($–$$$$), `rating` (float), `review_count`
  - Activities: `price_tier` ($–$$$$) if available, `cost` + `cost_max` (numeric) for Yelp Events, `is_free` (boolean)
- [x] All price fields stored as strings in `details` map for Firestore compatibility; parse to numeric only at display time
- [x] Add `imageUrl` field to `TravelEvent`
- [x] Add `options: List<EventOption>` to `TravelEvent` (or store as subcollection in Firestore)
- [x] Define Firestore schema: `users/{uid}/trips/{tripId}/events/{eventId}/options/{optionId}`

### Task 2.2 — SerpAPI: Extract Multiple Flights + Images
**Problem:** `.take(1)` discards all but cheapest flight; `thumbnail`/airline logo not extracted.
**Files:** `SerpRepository.kt`, `SerpModels.kt`

- [x] Remove `.take(1)` entirely — store ALL flight options returned by the call
- [x] Add `airline_logo` to `SerpFlightOption`; add `legroom`, `often_delayed_by_over_30_min`, `overnight`, `plane_and_crew_by`, `extensions[]` to `SerpFlightLeg`
- [x] Add `carbon_emissions` {this_flight, typical_for_this_route, difference_percent} to flight option model
- [x] Add `price_insights` {lowest_price, price_level, typical_price_range} to flight response model
- [x] Map `airline_logo` URL → `EventOption.imageUrl`
- [x] Default selection: first result from `best_flights` (Google's top pick). If `best_flights` absent, first from `other_flights`.
- [x] `stops=2` on first attempt, `stops=3` on metro fallbacks (per Task 2.4 fallback chain)
- [x] Return full `List<EventOption>` — all options available for user to browse and swap

### Task 2.3 — SerpAPI: Extract Multiple Hotels + Images
**Note:** 1 call returns ~20 hotels. Default sort: `sort_by=8` (highest rated). Prices are per room — NOT per person.
**Files:** `SerpRepository.kt`, `SerpModels.kt`

- [x] Remove `.take(1)` — store ALL hotels from page 1 (~20).
- [ ] Fetch page 2 via `next_page_token` for trips >7 days — not yet implemented.
- [x] Use `sort_by=8` (highest rated) as default. Pass `adults` count from the trip request.
- [x] **Group pricing**: `roomsNeeded = adults / 2` (integer division, minimum 1). Compute `group_rate_per_night = rate_per_night.extracted_lowest × roomsNeeded`. Store both values in `EventOption.details`. Display as e.g. "**$420/night** (3 rooms)". No rounding, no ceiling — keep it simple.
- [x] Add full hotel model fields: `images[]` {thumbnail, original_image}, `overall_rating`, `reviews`, `reviews_breakdown[]`, `amenities[]`, `excluded_amenities[]`, `check_in_time`, `check_out_time`, `hotel_class`, `extracted_hotel_class`, `location_rating`, `deal`, `deal_description`, `eco_certified`, `gps_coordinates`, `nearby_places[]`
- [x] Add `prices[]` per hotel: {source, logo, link (booking URL), rate_per_night, total_rate, free_cancellation, discount_remarks}
- [x] Map `images[0].thumbnail` → `EventOption.imageUrl`; store `images[0].original_image` for expanded card view
- [x] Default selection: highest-rated hotel within budget (already sorted by `sort_by=8`, so first result)
- [x] Booking link: store cheapest `prices[].link` in `EventOption.details["booking_url"]`
- [ ] Room type details (king/queen/suite): deferred — requires extra `property_token` call (1 credit each). Implement in Phase 5 if needed.

### Task 2.4 — Flight Search: Fallback for Empty Results
**Decision:** Use comma-separated metro airports + stops parameter. Max 3 attempts, all single calls.
**Files:** `SerpRepository.kt`, `NewTripViewModel.kt`

- [x] Default `stops=2` on first attempt, `stops=3` on metro fallbacks
- [x] Build a metro airport map (e.g., `"SJC" → "SJC,SFO,OAK"`, `"NYC" → "JFK,EWR,LGA"`) for top 30 metros
- [x] Fallback sequence (each is 1 API call):
  1. Origin IATA + destination IATA + `stops=2`
  2. Metro origin + destination IATA + `stops=3`
  3. Metro origin + metro destination + `stops=3`
  4. If all empty: insert a "No flights found" placeholder card with a Google Flights deep link
- [x] Groq already provides IATA codes — use them directly; only expand to metro if results are empty
- [ ] Update `TripGeneratingPage.kt` to show "Searching alternate airports…" during fallback — silently retried in `SerpRepository`, no mid-step UI update yet

### Task 2.5 — Yelp Integration for Restaurants AND Activities
**Decision:** Yelp replaces Groq-generated restaurant names. Also covers activities via `arts,museums,tours,landmarks` categories. Groq still generates the plan skeleton and neighborhood context.
**Files:** new `YelpRepository.kt`, `NewTripViewModel.kt`

- [x] Create `YelpRepository.kt` with three methods:
  - `searchRestaurants(location, dietaryCategories, limit=5)` → `GET /v3/businesses/search`
  - `searchActivities(location, limit=5)` → `GET /v3/businesses/search`
  - `searchEvents(location, startDate, endDate, limit=20)` → `GET /v3/events`
- [x] **Business search: extract per result** — `id`, `name`, `image_url`, `rating`, `review_count`, `price`, `categories[]`, `address`, `phone`, `transactions[]`, `distance`
- [x] **Restaurants**: 1 call per day, `limit=5`. Pass dietary preference as additional `categories`. A 7-day trip = 7 restaurant calls, ~35 options total.
- [x] **Activities**: 1 separate call per day, `limit=5`, `categories=arts,museums,tours,landmarks`. A 7-day trip = 7 activity calls, ~35 options total.
- [x] **Events**: 1 call per trip (not per day). `GET /v3/events`, date range = full trip duration. Extract: `name`, `image_url`, `category`, `description`, `is_free`, `cost`, `cost_max`, `tickets_url`, `time_start`, `time_end`, `location`.
- [ ] For activities Yelp can't cover (national parks, beaches): Groq-generated fallback with typed placeholder icon — not yet implemented.
- [x] **Lazy detail fetch on card expand** (Task 3.3): `getBusinessDetail(yelpId)` and `getBusinessReviews(yelpId, limit=3)` methods exist in `YelpRepository`; caching wired in `ItineraryViewModel`.

#### Dietary Preferences Integration
- [x] Add a `dietary: List<String>` field to `TravelRequest` and `NewTripViewModel`
- [ ] Expose dietary selection UI in **Step 5 (Interests)** — no matches found in `TripStep5InterestsPage.kt`; deferred to Task 4.5
- [x] When dietary prefs are set, append the matching Yelp category alias to the restaurant search `categories` param (Vegan/Vegetarian/Halal/Kosher/Gluten-Free → yelp aliases)
- [x] **Note**: these categories return restaurants that *are* that type (e.g., fully vegan), not restaurants that merely *have* vegan options.
- [ ] Pass dietary prefs string to Groq in the itinerary prompt — not yet confirmed in `GroqRepository`.

### Task 2.6 — Local Image Storage
**Decision:** Download all event option images to app internal storage when the trip is finalized. Persists until app uninstall. No external storage or gallery writes.
**Files:** new `ImageCacheManager.kt`, `TravelEvent.kt`, `EventOption.kt`

- [x] Create `ImageCacheManager`: takes a list of URLs, downloads each to `context.filesDir/trip_images/{tripId}/`, returns local file paths
- [x] Called in pipeline at `DOWNLOADING_IMAGES` step — batch downloads all selected + alternative option image URLs
- [x] Store local path in `EventOption.localImagePath` in Firestore
- [ ] Coil: configure a custom `ImageLoader` that checks `localImagePath` first, then falls back to URL, then to a typed vector placeholder — not yet implemented
- [x] When a trip is deleted: call `ImageCacheManager.deleteTripImages()` — wired in `ItineraryViewModel.deleteTrip()`; option subcollection docs are also removed before event deletion
- [ ] When an option is swapped: download new option image if not already cached — not yet implemented

### Task 2.7 — Pipeline Orchestration Update
**Problem:** `NewTripViewModel.generateTrip()` needs to accommodate multi-option flow.
**Files:** `NewTripViewModel.kt`

- [x] Update generation pipeline:
  1. `CRAFTING_ITINERARY` — Groq generates metadata, IATA codes, day-by-day skeleton
  2. `SEARCHING_FLIGHTS` + `FINDING_HOTELS` — parallel (2 SerpAPI calls). Hotels `sort_by=8`. Group pricing calculated client-side.
  3. `FINDING_RESTAURANTS` — 1 Yelp call per day, `limit=5`, dietary categories appended. Parallelized across all days.
  4. `FINDING_ACTIVITIES` — 1 Yelp call per day, `limit=5`, `categories=arts,museums,tours,landmarks`. + 1 Yelp Events call for full trip range.
  5. `DOWNLOADING_IMAGES` — Batch download all option `image_url`s
  6. `SAVING` — Persist to Firestore with options subcollection
  7. `COMPLETE`
- [x] Update `TripGeneratingPage.kt` step labels — all 7 steps rendered with animated card UI
- [x] Total calls per 7-day trip: 2 SerpAPI + up to 2 fallback + 7 Yelp restaurants + 7 Yelp activities + 1 Yelp events + 1 Groq = ~20 calls

---

## Phase 3 — Event Selection UX

### Task 3.1 — Option Selection UI (Change/Swap Panel)
**Problem:** User needs to pick from multiple options per event slot and reorganize cards.
**Files:** new `EventOptionsPanel.kt`, `FinalPlan.kt`, `ItineraryViewModel.kt`

- [x] Each event card shows a "Change" button (small, secondary style) in the top-right corner
- [x] Show option count badge on cards with alternatives (e.g., "3 options")
- [x] Tapping "Change" opens a semi-full-screen bottom panel (`ModalBottomSheet`) showing:
  - List of all available alternatives. Each row: thumbnail image, name, price (formatted by type)
  - **Price formatting per type**: Flight → "$1,240 · Economy · 14h 20m"; Hotel → "$140/night · ★4.5 · (3 rooms)"; Restaurant → "$$$ · ★4.2 · 380 reviews"; Activity → "Free" / "$25–$50" / "$$$"
  - Tapping an alternative swaps it in as the selected option
  - Collapsed "Previously Rejected" accordion section at the bottom — shows rejected options for this slot that can still be picked
- [x] Drag-to-reorder: cards within a day are drag-reorderable (long-press to lift, drag to position) — `sh.calvin.reorderable:reorderable:2.4.0`
- [ ] Drag-to-change-day: deferred to Phase 5
- [x] Selection and reorder changes update Firestore and in-memory state
- [x] Rejected options tracked in ViewModel as `Set<optionId>` per event slot (session only, not persisted)

### Task 3.2 — Trip Sharing via Chat
**Decision:** No group voting on trip events. Group voting system stays as-is (for group event coordination only). This task covers trip sharing as a rich link card in any chat.
**Files:** `FinalPlan.kt`, `Message.kt`, `ChatPage.kt`, `DirectChatPage.kt`, `ItineraryViewModel.kt`

- [x] Add a "Share" icon button to `FinalPlanPage` top bar
- [x] Tapping opens a bottom sheet: pick a chat (group or DM) from the user's chat list
- [x] Sends a structured trip card message to the selected chat: trip name, destination, date range, cover image
- [x] Store `sharedTripId` + `ownerUid` + trip metadata in the chat message Firestore document
- [x] In chat: render trip card messages with a distinct card composable (not a plain text bubble)
- [ ] Tapping the trip card in chat → opens `FinalPlanPage` in read-only mode — navigation wiring deferred (requires nav graph update)
- [ ] Read-only view: deferred with navigation wiring

### Task 3.3 — Event Card Expandable Detail View
**Decision:** Tapping a card opens a Dialog overlay (semi-full-screen) within `FinalPlanPage`.
**Files:** `FinalPlan.kt`, new `ExpandedEventCard.kt`, `ItineraryViewModel.kt`, `YelpApiService.kt`, `YelpRepository.kt`

- [x] Tapping a card opens `ExpandedEventCard` Dialog overlay
- [x] Expanded view shows — all from stored data:
  - Full-size hero image
  - **Flight**: airline + flight number, route, times, duration, aircraft, legroom, Wi-Fi/power, total price, price level badge, carbon emissions delta
  - **Hotel**: name, star class, rating, rate/night, group total rate (× rooms), deal label, check-in/out times, amenities, booking link
  - **Restaurant**: price tier, rating + review count, address, categories, phone, delivery availability, View Menu link
  - **Activity**: price (free/range/tier), rating, address, categories, tickets link
- [x] Yelp reviews: lazily fetch `/v3/businesses/{id}/reviews` only on first expand (3 excerpts). Cache in ViewModel.
- [x] "Change" button inside expanded view opens the options panel (Task 3.1)
- [x] Inline edit fields for title, time, notes — saves to Firestore on dismiss
- [x] Tap outside to dismiss
- [x] Only one card expanded at a time (state tracked in FinalPlan)

---

## Phase 3 — Post-Completion Refinements

> Added: 2026-04-02. These issues were discovered after Phase 3 was first marked complete. Address before moving to Phase 4.

### Task 3.4 — FinalPlan Header & Navigation Redesign
**Problem:** Top bar is cluttered (trip-switcher dropdown, clipped trip name). Bottom nav is absent from this screen.
**Files:** `FinalPlan.kt`, `MainScaffold.kt` (nav graph), `ItineraryViewModel.kt`

- [ ] Remove the trip-name dropdown / multi-trip switcher from the top bar entirely
- [ ] Move the trip name to a full-width heading row immediately below the top bar, above the first day card — no clipping, large readable text
- [ ] Top bar left: back arrow only
- [ ] Top bar right: single triple-dot (`MoreVert`) icon that opens a `DropdownMenu` with:
  - Share Trip (wires into Task 3.2 share sheet)
  - Reorder Events (toggles jiggle mode — see Task 3.7)
  - Archive Trip
  - Delete Trip (confirmation dialog required)
- [x] Add `BottomNavigationBar` to `FinalPlan` scaffold so the five main tabs remain accessible without pressing back

### Task 3.5 — Card Content & Swap Quality Fixes
**Problem:** Cards show the event type tag instead of the real name; swapping an option does not update the card; change menu shows "Option" instead of real names; already-selected options appear in the change menu.
**Files:** `FinalPlan.kt`, `EventOptionsPanel.kt`, `ItineraryViewModel.kt`

- [x] **Primary card label**: render the event's real name (hotel name, restaurant name, activity name, airline+flight number) as the largest text on the card — the type tag is secondary/small
- [x] **Remove option-count badge** from cards — replace with a plain "Change" button only
- [x] **Change menu row content**: each alternative must show its real name + at least one supporting detail (price, rating, or duration depending on type) — never show "Option N" as the label
- [x] **Post-swap card update**: when the user picks an alternative in the change menu or expanded view, update the in-memory selected option immediately so the card reflects the new name, time, image, and price — not the original
- [ ] **Exclude already-selected**: the change menu must not list the currently active option; the already-selected option for any other slot must also be excluded (no repeats across days in the same pool)
- [ ] **Two activities per day**: pipeline should aim for 2 activity slots per day and 1 restaurant slot (see also Task 3.6)

### Task 3.6 — No-Duplicate Daily Recommendations
**Problem:** The same restaurant and activity appear on every day of the trip. Root cause is likely Yelp results are fetched once but the same top result is assigned to every day rather than cycling.
**Files:** `NewTripViewModel.kt`, `GroqRepository.kt`, `YelpRepository.kt`

- [ ] After fetching Yelp pools for restaurants and activities, distribute them round-robin across days — day 1 gets option[0], day 2 gets option[1], etc. Never assign the same business as the primary slot on more than one day
- [ ] Same deduplication applies to change-menu alternatives: if a business is already the primary on another day, move it to that day's alternatives list only
- [ ] Groq itinerary prompt: explicitly instruct the model not to suggest the same restaurant or activity name more than once across the plan skeleton
- [ ] If the Yelp pool is smaller than the number of days, wrap around only for alternatives — not for primary selections; fall back to Groq-generated name + typed placeholder for overflow primaries

### Task 3.7 — Explicit Reorder Mode (Jiggle Mode)
**Problem:** Long-press drag-to-reorder silently exists but doesn't work reliably and offers no visual feedback that items are movable.
**Files:** `FinalPlan.kt`, `EventOptionsPanel.kt` (if reorderable list is there), `build.gradle.kts`

- [ ] Reorder mode is off by default — activated only via "Reorder Events" in the triple-dot menu (Task 3.4)
- [ ] When active, cards enter a **jiggle mode**: subtle continuous rotation/wobble animation on each card (similar to iOS home screen) + a visible "Done" button or banner indicating edit mode is on
- [ ] Use `sh.calvin.reorderable:reorderable:2.4.0` — confirm dependency is in `build.gradle.kts`
- [ ] Exiting jiggle mode: tap "Done" banner or re-tap "Reorder Events" in triple-dot; persists reorder to Firestore on exit
- [ ] Drag handles are shown only while jiggle mode is active — hidden otherwise to reduce visual noise

### Task 3.8 — API Response Debugging Investigation
**Problem:** (a) Flights are missing from Day 1 of generated trips. (b) Unclear which event data comes from real API calls vs Groq hallucination. (c) Yelp is not cycling properly. Root cause must be confirmed before fixes are written.
**Files:** `test.py` (external debugger, root), `enhancement_plan.md`, `NewTripViewModel.kt`, `SerpRepository.kt`, `YelpRepository.kt`, `GroqRepository.kt`, `ImageCacheManager.kt`, `EventOption.kt`, `FinalPlan.kt`, `ExpandedEventCard.kt`

- [x] **Create `test.py`** — a standalone Python script that reproduces the full trip generation pipeline outside Android:
  - Takes a sample trip request (origin, destination, dates, budget, adults)
  - Calls Groq with the same system prompt the app uses and records the structured output
  - Calls SerpAPI flights + hotels with the same params the app uses and records full JSON responses
  - Calls Yelp restaurants + activities + events in the same order and concurrency pattern as the app
  - Emits a chronological request/response timeline to Markdown + JSON
  - Downloads hotel images into a per-run folder and reports image counts + storage location
- [x] **Flight missing root cause**: confirm whether `SEARCHING_FLIGHTS` runs before Groq returns IATA codes, and log the exact IATA strings used in the flight query
  - Result: not a sequencing issue. Groq returns before Serp starts, the exact IATA query succeeds (`LAX` → `JFK`), and the Day 1 flight issue is downstream parsing/mapping.
  - Confirmed parser issue: the live Serp response exposes flight times under `departure_airport.time` / `arrival_airport.time`, while current app models expect `departureTime` / `arrivalTime`. This explains blank flight times even when a valid flight is returned.
- [x] **Root-cause audit completed** via `enhancement_plan.md`
  - Confirmed: Groq is only used for itinerary metadata in the current pipeline, not for day-level restaurant/activity generation
  - Confirmed: Yelp fan-out is too aggressive on long trips and causes many `429` responses
  - Confirmed: Yelp primaries repeat because each day independently picks `response.businesses.first()`
  - Confirmed: hotel image downloading dominates generation time
- [x] **Groq vs real data source tracing at the data layer**
  - `EventOption.source` already exists and is populated with `"serp"` / `"yelp"` / `"groq"`
  - The debug pipeline report now makes the provider source explicit in request/response summaries
- [ ] **Groq vs real data labeling in debug UI**
  - Add a small debug-only source label on cards / expanded cards so in-app inspection matches the external report
- [ ] Once root causes are confirmed, implement the targeted fixes below and update Tasks 2.4, 3.5, and 3.6 as those code changes land

#### Task 3.8.1 — Investigation Output Summary

- [x] Latest successful adversarial audit completed against the live pipeline using `test.py` and documented in `enhancement_plan.md`
- [x] Evidence captured:
  - 1 Groq call, 2 Serp calls, 33 Yelp calls on a 16-day trip
  - repeated Yelp restaurant/activity primaries across days
  - many Yelp `429` failures causing missing day content
  - Yelp Events returned no usable output in the audited run
  - hotel image prefetch stage consumed the majority of total runtime
- [x] Conclusion:
  - The current pipeline is optimized in some local areas, but it is architecturally over-fetching Yelp and over-downloading hotel images

#### Task 3.8.2 — Order Of Operations To Speed Up Generation

- [ ] **Step 1: Fix correctness before optimization**
  - Update `SerpRepository.kt` flight parsing to read the actual live response time fields
  - Validate Day 1 grouping after the parser fix using `test.py`
- [ ] **Step 2: Reduce Yelp call count**
  - Replace per-day Yelp restaurant/activity searches in `NewTripViewModel.kt` and `YelpRepository.kt` with pooled searches per trip or per area cluster
  - Distribute Yelp results locally across day slots instead of asking Yelp the same question once per day
- [ ] **Step 3: Deduplicate and rank locally**
  - In `NewTripViewModel.kt`, assign primaries round-robin by business id so no restaurant/activity becomes the primary on multiple days unless the pool is exhausted
  - Improve hotel selection locally in `SerpRepository.kt` using travel-style-aware ranking instead of “first result wins”
- [ ] **Step 4: Remove unnecessary critical-path downloads**
  - Change `ImageCacheManager.kt` and the image download stage in `NewTripViewModel.kt` so generation prefetches only selected hero images
  - Defer alternative hotel gallery images until expanded view / gallery view
- [ ] **Step 5: Reduce or remove non-essential model work**
  - Move deterministic itinerary fields out of `GroqRepository.kt`
  - Generate locally where possible: `itinerary_id`, `created_at`, `duration_days`, `status`, trip title, and day slot structure
  - Replace Groq IATA inference later with a deterministic lookup source if coverage is acceptable
- [ ] **Step 6: Keep the debugger aligned with production**
  - Update `test.py` whenever pipeline order, request shape, or image strategy changes
  - Add summary metrics for missing days, provider errors, duplicate primaries, and image-stage time

#### Task 3.8.3 — Concrete File Change Map

- [ ] `app/src/main/java/com/example/travelcents/data/remote/SerpRepository.kt`
  - Fix flight time parsing against the real Serp response shape
  - Improve hotel ranking and selected-option choice
- [ ] `app/src/main/java/com/example/travelcents/ui/main/newtrip/NewTripViewModel.kt`
  - Replace per-day Yelp fan-out with pooled fetch + local distribution
  - Deduplicate restaurant/activity primaries across the trip
  - Reduce the image download work performed in the critical path
- [ ] `app/src/main/java/com/example/travelcents/data/remote/YelpRepository.kt`
  - Add pooled search entry points and support bounded concurrency / backoff if per-day fallback is still needed
  - Preserve richer failure information instead of silently returning `null`
- [ ] `app/src/main/java/com/example/travelcents/data/ImageCacheManager.kt`
  - Move from eager full-gallery prefetch to selected-image prefetch + lazy gallery fetch
  - Add bounded concurrency if multiple downloads are still required
- [ ] `app/src/main/java/com/example/travelcents/data/remote/GroqRepository.kt`
  - Reduce the Groq responsibility to only the fields that cannot be generated deterministically
  - Remove locally derivable metadata from the model call over time
- [ ] `app/src/main/java/com/example/travelcents/data/model/EventOption.kt`
  - Keep `source` as the canonical provider marker for debug tooling and UI labels
- [ ] `app/src/main/java/com/example/travelcents/ui/main/itinerary/FinalPlan.kt`
  - Add debug-only provider source labels when running debug builds
- [ ] `app/src/main/java/com/example/travelcents/ui/main/itinerary/ExpandedEventCard.kt`
  - Add debug-only provider source labels and ensure lazy image/gallery strategy is reflected in the UI
- [ ] `test.py`
  - Keep as the external regression harness for request ordering, missing-day detection, provider attribution, and image-stage timing
- [x] `enhancement_plan.md`
  - Adversarial audit completed and committed to planning artifacts

### Task 3.9 — Expanded Card Overlay Fixes
**Problem:** Expanded card shows a black/opaque screen behind it instead of the dimmed original screen. Also the card lacks the richness expected from the stored Yelp / SerpAPI data.
**Files:** `FinalPlan.kt`, `ExpandedEventCard.kt`

- [ ] Replace full-screen `Dialog` with a `ModalBottomSheet` — the itinerary remains visible (dimmed) behind the sheet, consistent with the change menu (Task 3.1)
- [ ] Hero image area: display full stored image, not a small thumbnail
- [ ] Show all stored `EventOption.details` fields — surface every non-null detail in a readable layout (hours, amenities, price breakdown, emissions, booking URL, etc.)
- [ ] "X" / close affordance: if the event has more than one photo, replace the X button with a **2×2 grid icon** (indicating a gallery); tapping it opens a full-screen swipeable image carousel. X still appears for single-photo events.
- [ ] Yelp reviews: show the 3 cached review excerpts (already lazily fetched per Task 3.3); render as quote cards

### Task 3.10 — Multi-Photo Gallery for Events
**Problem:** Hotels should return many photos from SerpAPI but only one is shown. Gallery navigation is missing.
**Files:** `ExpandedEventCard.kt`, `SerpModels.kt`, `SerpRepository.kt`, new `ImageGalleryScreen.kt`

- [ ] SerpAPI hotel `images[]` is already modeled (Task 2.3) — verify all images are actually stored in Firestore after pipeline runs (see Task 3.8 debugging)
- [ ] In expanded card: show a horizontal pager (HorizontalPager) of all stored images with a dot indicator
- [ ] **Gallery icon trigger** (per Task 3.9): tapping the 2×2 grid icon opens `ImageGalleryScreen` — a full-screen composable with swipe-to-navigate and an image counter ("3 / 8")
- [ ] `ImageGalleryScreen` is a Dialog overlay (not a navigation destination) so back-press returns to the expanded card, not the itinerary

### Task 3.11 — Trip Sharing: Navigation Wiring + Read-Only View + Bug Fixes
**Problem:** (a) Tapping a shared trip card in chat does nothing (navigation deferred in 3.2). (b) The wrong trip title is shown to the receiver ("New York City Getaway" instead of the shared Madrid trip). (c) Receiver has no way to save the trip to their own profile.
**Files:** `FinalPlan.kt`, `ChatPage.kt`, `DirectChatPage.kt`, `ItineraryViewModel.kt`, `TravelCentsNavigation.kt`, `MainScaffold.kt`, `Message.kt`, `FirestoreRepository.kt`

- [ ] **Fix wrong-trip bug**: the shared message must store the `ownerUid` + `tripId` that was explicitly shared, not infer it from the receiver's current active trip. When rendering the trip card in chat, read name/destination/dates from the message document itself (denormalized fields) — never from the receiver's own trip list
- [ ] **Navigation wiring**: add a route `finalPlan/{ownerUid}/{tripId}?readOnly=true` to the nav graph. Tapping the trip card in chat navigates to this route. `ItineraryViewModel` loads the trip by `ownerUid/tripId` (cross-user read — confirm Firestore rules allow it, or add a `sharedWith` field)
- [ ] **Read-only mode**: when `readOnly=true`, disable all inline edit fields, hide the "Delete Trip" and "Archive Trip" menu options, and replace the "Reorder Events" option with a disabled/hidden state
- [ ] **Save to My Trips**: in read-only mode, show a "Save to My Trips" primary button (fixed at the bottom or in the triple-dot menu). Tapping deep-copies the trip + all events/options into the receiver's own `users/{uid}/trips/` tree in Firestore with a new `tripId`
- [ ] **Re-share from read-only view**: "Share Trip" option remains active in read-only mode so the receiver can forward it to other chats

### Task 3.12 — UI Density & Spacing Fixes
**Problem:** Cards within a day are too far apart; day separators add too much vertical space; overall the timeline feels slow to scroll.
**Files:** `FinalPlan.kt`

- [x] Reduce vertical gap between event cards within a single day to 8dp (from current spacing)
- [x] Reduce vertical gap between the last card of one day and the day header of the next day to 16dp
- [x] Day headers: reduce padding top/bottom
- [x] Audit all `Spacer` calls in the timeline composable and halve any value above 12dp
- [ ] After layout changes: run on a device and confirm the full 7-day itinerary is scrollable without feeling bloated

---

## Phase 4 — UI Standardization

### Task 4.1 — Unify Bottom Button Layout
**Problem:** Bottom bar styling (height, radius, padding) is inconsistent across steps.
**Files:** All step pages

- [ ] Define a single `WizardBottomBar` composable used by all steps
- [ ] Standard layout: full-width primary "Continue to X" button
- [ ] Save Draft: either remove from Step 4 or add consistently to all steps (per Q6 answer)
- [ ] Consistent height (52dp), corner radius (16dp), horizontal padding (16dp)

### Task 4.2 — Unify Text Input Styling
**Problem:** Step 5 search bar uses transparent containers + 12dp radius; Steps 1/3 use `SurfaceBright` + 16dp radius; Step 4 uses `BasicTextField` with 52sp font.
**Files:** All step pages, new `TravelCentsTextField.kt`

- [ ] Create a shared `TravelCentsTextField` composable with standard styling:
  - Container: `SurfaceBright` background
  - Corner radius: 16dp
  - Cursor: blue accent
  - Label + placeholder support
  - Optional leading icon
  - Transparent indicator lines
- [ ] Step 4 budget field is intentionally unique (currency input) — keep `BasicTextField` but align colors/radius
- [ ] Step 5 search bar: switch from transparent to `SurfaceBright` to match others
- [x] Standardize color variable names — consolidated into `TripWizardColors` (done in Task 4.3)

### Task 4.3 — Consolidate Color Definitions
**Problem:** Each step page re-declares identical colors with different prefixes (S1Blue, S2Blue, S3Blue... all = `0xFF64B5F6`).
**Files:** All step pages, new `TripWizardTheme.kt`

- [x] Create a single `TripWizardColors` object with shared color definitions
- [x] Replace all S1*/S2*/S3*/S4*/S5* references across step pages
- [x] Single source of truth for the wizard color palette

### Task 4.5 — Step 5: Add Dietary Restrictions Section
**Decision:** Add dietary preferences as a second optional section below the interests grid on Step 5. Same page, no new step needed. Keeps the wizard at 5 steps.
**Files:** `TripStep5InterestsPage.kt`, `NewTripViewModel.kt`, `TravelRequest.kt` (or equivalent model)

- [ ] Below the interests grid, add a clearly separated section: "DIETARY PREFERENCES" label + subtitle "Optional — affects restaurant suggestions"
- [ ] Display as a horizontal wrap of toggle chips (similar to interest cards but smaller pill style): **Vegan**, **Vegetarian**, **Halal**, **Kosher**, **Gluten-Free**
- [ ] Multi-select allowed. Unselected by default.
- [ ] Selected chips use the same blue accent / border style as selected interests
- [ ] Store selected dietary prefs in `NewTripViewModel` as `List<String>` (Yelp category aliases: `vegan`, `vegetarian`, `halal`, `kosher`, `gluten_free`)
- [ ] Pass dietary prefs into `TravelRequest` so they flow through to `YelpRepository` and Groq prompt
- [ ] Update "Continue to Generate" button to remain enabled regardless of dietary selection (it's optional)

### Task 4.4 — Remove Save Draft
**Decision:** Remove the feature entirely for now.
**Files:** `TripStep4BudgetPage.kt`, `NewTripViewModel.kt`, `MainScaffold.kt`

- [x] Delete "Save Draft" button from `TripStep4BudgetPage.kt` bottom bar
- [x] Remove `onSaveDraftClick` callback parameter from `TripStep4BudgetPage`
- [x] Remove the corresponding `navController.navigate(MainRoutes.Home)` handler from `MainScaffold.kt`
- [x] Step 4 bottom bar becomes single-button layout matching all other steps

---

## Phase 5 — Polish & Edge Cases

### Task 5.1 — Placeholder & Error States
**Files:** All event card composables, `FinalPlanPage.kt`

- [ ] No-flights-found card with actionable fallback (manual airport entry or link to Google Flights)
- [ ] No-hotels-found card with fallback
- [ ] Image load failure: show a typed placeholder icon (plane for flight, bed for hotel, fork for restaurant)
- [ ] Network error during generation: retry button per failed step (not full restart)

### Task 5.2 — Offline Support for Saved Trips
**Files:** `ImageCacheManager.kt`, `ItineraryViewModel.kt`

- [ ] Saved trips with locally cached images viewable offline
- [ ] Firestore offline persistence already enabled by default — verify it works for trip data
- [ ] Show "offline" indicator when viewing cached data

### Task 5.3 — Image Cleanup & Storage Management
**Files:** `ImageCacheManager.kt`

- [ ] Delete locally stored images when a trip is deleted or archived
- [ ] Track total image cache size
- [ ] Optional: settings toggle to clear image cache

---

## Phase 6 — Chat & Messaging Improvements

### Task 6.1 — Unified Chat List Screen
**Decision:** Merge group chats and DMs into a single screen. Clear visual distinction between the two types.
**Files:** `ChatsPage.kt`, `ChatsViewModel.kt`

- [ ] Replace the current split-tab or separate list views with a single unified list showing both DMs and group chats
- [ ] Visually distinguish DM entries from group chat entries: DMs show a circular avatar, group chats show a multi-person icon or stacked avatars
- [ ] Add a filter chip row at the top: **All** / **Direct** / **Groups** — so users can still narrow if desired
- [ ] Sort by most recent message across both types
- [ ] Show unread count badges on individual chat rows

### Task 6.2 — Nav Bar Message Notifications
**Decision:** Show a badge on the Chat nav bar icon when there are unread messages or DMs.
**Files:** `MainScaffold.kt`, `ChatsViewModel.kt`

- [ ] Track total unread message count across all chats (DMs + groups) in `ChatsViewModel`
- [ ] Pass the count to the bottom nav bar Chat icon and render a badge (show count if ≤9, show "9+" beyond that)
- [ ] Badge clears when the user opens the chat list and all chats are marked read
- [ ] Use existing Firestore listeners — extend the unread count logic rather than adding new listeners

### Task 6.3 — Chat Management (Delete & Archive)
**Problem:** No way for users to remove chats from their view, and no archive concept exists.
**Files:** `ChatsPage.kt`, `ChatsViewModel.kt`, `FirestoreRepository.kt`

#### Delete
- [ ] Long-press (or swipe action) on a chat row reveals a **Delete** option
- [ ] **DMs**: removing a DM hides it from the current user's chat list. If the other user has also deleted it and no messages remain unread, delete the chat document from Firestore entirely. If the other user still has it, only remove the current user's reference.
- [ ] **Group chats**: the user leaves the group. If they are the last member, delete the group chat document and all its messages from Firestore.
- [ ] Show a confirmation dialog before deletion

#### Archive
- [ ] Long-press (or swipe) also exposes an **Archive** option
- [ ] Archived chats disappear from the main list but are accessible via an "Archived" section at the bottom of the chat list (collapsed by default, tapping expands it)
- [ ] Archived state is stored per-user in Firestore (`users/{uid}/archivedChats/{chatId}`)
- [ ] Un-archive from the archived section via long-press → **Unarchive**
- [ ] New messages in an archived chat automatically un-archive it and surface it in the main list

### Task 6.4 — New Group Chat Creation Screen UI Overhaul
**Problem:** The + button on the top right of the chat screen opens a group creation flow with dead buttons and inconsistent UI.
**Files:** `CreateGroupChatPage.kt` (or equivalent), related ViewModel

- [ ] Audit and remove all non-functional buttons from the creation screen
- [ ] Match the input field and button styling to the rest of the app (use `TravelCentsTextField` or equivalent)
- [ ] Step 1: group name input (single clear field, hint text)
- [ ] Step 2: member search/selection — searchable list of friends, each row with avatar + name + checkbox; selected members shown as chips above the list
- [ ] Step 3: optional group icon/avatar selection
- [ ] Replace any broken confirmation flow with a clear "Create Group" primary button at the bottom
- [ ] On success, navigate directly into the new group chat

### Task 6.5 — User Profile Cards in Chat
**Decision:** Tapping a user's name or avatar in a DM or group chat opens a profile card / bottom sheet.
**Files:** `ChatMessageBubble.kt` (or equivalent), new `UserProfileSheet.kt`, `ProfileViewModel.kt`

- [ ] In DM chat header: tapping the other user's name opens their profile card
- [ ] In group chat: tapping a message sender's name or avatar opens their profile card
- [ ] Profile card shows: profile picture, display name, username, mutual trips (if any), and a "Send DM" button
- [ ] Implemented as a `ModalBottomSheet` — no full navigation away from the chat
- [ ] Profile data fetched lazily from Firestore on first open; cache in ViewModel for the session

### Task 6.6 — Shared Chat Input Box
**Decision:** Unify the message input composable used across all chat contexts.
**Files:** All chat screen files that have their own input box implementation

- [ ] Audit how many different chat input implementations exist (group chat, DM, AI chat)
- [ ] Create a single shared `ChatInputBox` composable with: text field, send button, optional attachment/emoji actions
- [ ] Replace all per-screen input implementations with the shared composable
- [ ] Match the visual style to the existing AI chat input box which is the most polished version

---

## Phase 7 — Profile & Identity

### Task 7.1 — Profile Picture Upload & Display
**Files:** `ProfilePage.kt`, `ProfileViewModel.kt`, `FirestoreRepository.kt`, Firebase Storage

- [ ] Add a profile picture upload flow on the profile page: tapping the avatar opens image picker (gallery or camera)
- [ ] Upload the selected image to Firebase Storage under `profile_pictures/{uid}.jpg`; store the download URL in the user's Firestore document
- [ ] Display profile pictures in: chat message avatars, group chat member lists, chat list rows, profile cards (Task 6.5), and the profile page itself
- [ ] Show a placeholder initial-letter avatar when no profile picture is set
- [ ] Crop the image to a circle on upload; compress to reasonable size before uploading

### Task 7.2 — Extended Profile Identifiers
**Files:** `ProfilePage.kt`, `ProfileViewModel.kt`, Firestore `users/{uid}` document

- [ ] Add editable profile fields: **username** (unique handle, e.g. @zaher), **display name**, **phone number**, **current city/location**, **bio** (short text), **website** (optional)
- [ ] Username uniqueness: validate against Firestore on input; show inline error if taken
- [ ] Phone number: store as-is; not required; no verification for now
- [ ] Current location: free-text city field (not GPS); user types it manually
- [ ] Display the relevant fields on the public-facing profile card (Task 6.5); respect a future privacy toggle before showing phone/location

---

## Phase 8 — Settings Page Overhaul

### Task 8.1 — Fix Account Deletion & Logout
**Problem:** Account deletion breaks access without actually deleting the account from Firestore. Logout causes login to fail on the next attempt.
**Files:** `SettingsPage.kt`, `SettingsViewModel.kt`, `AuthModel.kt`, `FirestoreRepository.kt`

- [ ] **Account deletion**: implement a full deletion flow — delete the user's Firestore document (`users/{uid}`) and all subcollections, then call `FirebaseAuth.currentUser?.delete()`. Handle re-authentication requirement (Firebase requires recent login before delete — prompt for password re-entry or Google re-auth).
- [ ] **Cascade deletion**: currently only `users/{uid}` is deleted — any other collections storing user data (trips, expenses, etc.) are orphaned. Audit all Firestore collections for user-owned documents and delete them as part of the flow.
- [ ] **Deletion error feedback**: the current failure path (`task.isSuccessful == false`) silently leaves the dialog open with no message. Show a clear error (e.g. "Re-authentication required — please log out and back in first") so the user knows what to do.
- [ ] Move the delete account button out of the main settings flow into a clearly marked **Danger Zone** section at the bottom, styled with a red/destructive color and a confirmation dialog requiring the user to type "DELETE" to confirm.
- [ ] **Logout**: diagnose why login fails after logout (likely a stale Firestore listener or ViewModel state not cleared on sign-out). Fix by clearing all ViewModel state and cancelling listeners on sign-out before navigating to the login screen.
- [ ] Ensure `FirebaseAuth.signOut()` is called before any navigation so the auth state is clean when the login screen loads.

### Task 8.2 — Fix Preferences Persistence
**Problem:** Preference toggle switches reset to default when navigating away — state is not saved.
**Files:** `SettingsPage.kt`, `SettingsViewModel.kt`, Firestore `users/{uid}/preferences` or shared prefs

- [ ] Identify all toggle switches on the settings page and their intended behavior
- [ ] Persist each preference: use either `DataStore<Preferences>` (preferred for simple key-value) or a `preferences` sub-document in Firestore if the prefs need to sync across devices
- [ ] Load saved preference values on ViewModel init so toggles reflect the correct state when navigating back to settings
- [ ] Remove any switches that have no backing functionality — either implement them or delete them

### Task 8.3 — Settings Page Reorganization & UI Polish
**Files:** `SettingsPage.kt`, `SettingsViewModel.kt`

- [ ] **Default tab**: start the user on the left tab (or reorder tabs so the primary settings tab is first/left)
- [ ] **Unify styling**: match card styles, typography, spacing, and colors to the rest of the app — remove any jarring visual differences
- [ ] **Reorganize sections** into clear groups: Account, Notifications, Privacy & Security, Preferences, About, Danger Zone
- [ ] **Privacy & Security section**: add "Do not share my data with third parties" toggle, and a "Data & Privacy" info screen (static text for now)
- [ ] **Remove or implement** any placeholder settings items that currently do nothing
- [ ] **Profile section in settings**: show profile picture, display name, username; tapping navigates to the full profile edit screen (Phase 7)

---

## Execution Order

```
Phase 1 (Performance) — No new features, just faster
  1.1  Preload/bundle static images
  1.2  Fix main thread blockers
  1.3  Reduce logging overhead

Phase 2 (Event Pipeline) — Core new functionality
  2.1  Data model for multi-option events
  2.2  SerpAPI multi-flight extraction
  2.3  SerpAPI multi-hotel extraction
  2.4  Flight empty result fallback
  2.5  Yelp restaurant integration
  2.6  Local image storage
  2.7  Pipeline orchestration update

Phase 3 (Selection UX — original)
  3.1  Change/swap panel + drag-to-reorder
  3.2  Trip sharing via chat
  3.3  Event card expandable detail view

Phase 3 (Post-Completion Refinements) — address before Phase 4
  3.8  API response debugging investigation  ← start here, informs all fixes below
  3.4  FinalPlan header & navigation redesign (bottom nav, trip name placement, triple-dot)
  3.5  Card content & swap quality fixes (real names, post-swap update, no duplicates in menu)
  3.6  No-duplicate daily recommendations (round-robin Yelp pool distribution)
  3.7  Explicit reorder mode / jiggle mode
  3.9  Expanded card overlay fix (ModalBottomSheet, not black Dialog)
  3.10 Multi-photo gallery (HorizontalPager + full-screen gallery screen)
  3.11 Trip sharing navigation wiring + read-only view + wrong-trip bug fix
  3.12 UI density & spacing fixes

Phase 4 (UI Standardization) — Can run parallel to Phase 2
  4.1  Unified bottom button bar
  4.2  Unified text input component
  4.3  Consolidated color definitions
  4.4  Remove Save Draft
  4.5  Step 5: Dietary restrictions section

Phase 5 (Polish) — After core features stable
  5.1  Placeholder & error states
  5.2  Offline support
  5.3  Image cleanup

Phase 6 (Chat & Messaging)
  6.1  Unified chat list (DMs + groups merged)
  6.2  Nav bar unread notification badge
  6.3  Delete & archive chats
  6.4  New group chat creation screen UI overhaul
  6.5  User profile cards in chat
  6.6  Shared chat input composable

Phase 7 (Profile & Identity)
  7.1  Profile picture upload & display
  7.2  Extended profile identifiers (username, phone, location, bio)

Phase 8 (Settings Overhaul)
  8.1  Fix account deletion & logout
  8.2  Fix preferences persistence
  8.3  Settings reorganization & UI polish
```

---

## Files Touched Summary

| File | Tasks |
|---|---|
| `TravelEvent.kt` | 2.1, 2.6 |
| `EventOption.kt` *(new)* | 2.1 |
| `SerpRepository.kt` | 2.2, 2.3, 2.4 |
| `SerpModels.kt` | 2.2, 2.3 |
| `YelpRepository.kt` *(new)* | 2.5 |
| `ImageCacheManager.kt` *(new)* | 2.6, 5.3 |
| `NewTripViewModel.kt` | 2.4, 2.5, 2.7, 4.4, 4.5 |
| `TravelRequest.kt` | 2.5, 4.5 |
| `TripGeneratingPage.kt` | 2.7 |
| `FinalPlanPage.kt` | 3.1, 3.3, 5.1 |
| `EventOptionsSheet.kt` *(new)* | 3.1 |
| `EventVotingScreen.kt` *(new)* | 3.2 |
| `EditPlanScreen.kt` | 3.3 |
| `MainScaffold.kt` | 3.3, 1.2 |
| `TravelCentsTextField.kt` *(new)* | 4.2 |
| `TripWizardColors.kt` *(new)* | 4.3 |
| `WizardBottomBar.kt` *(new)* | 4.1 |
| All step pages | 4.1, 4.2, 4.3 |
| `TripStep1DestinationPage.kt` | 1.1 |
| `TripStep5InterestsPage.kt` | 1.1, 4.5 |
| `NewTripLandingPage.kt` | 1.1, 4.4 |
| `ChatsViewModel.kt` | 1.2 |
| `FriendsViewModel.kt` | 1.2 |
| `ItineraryViewModel.kt` | 1.2 |
| `GroqRepository.kt` | 1.3 |
| `ItineraryScreen.kt` | 5.1 |
| `ChatsPage.kt` | 6.1, 6.2, 6.3, 6.4 |
| `ChatsViewModel.kt` | 1.2, 6.1, 6.2, 6.3 |
| `CreateGroupChatPage.kt` | 6.4 |
| `ChatMessageBubble.kt` | 6.5 |
| `UserProfileSheet.kt` *(new)* | 6.5 |
| `ChatInputBox.kt` *(new)* | 6.6 |
| `ProfilePage.kt` | 7.1, 7.2 |
| `ProfileViewModel.kt` | 6.5, 7.1, 7.2 |
| `SettingsPage.kt` | 8.1, 8.2, 8.3 |
| `SettingsViewModel.kt` | 8.1, 8.2, 8.3 |
| `AuthModel.kt` | 8.1 |
| `FinalPlan.kt` | 3.4, 3.5, 3.7, 3.9, 3.10, 3.12 |
| `ExpandedEventCard.kt` | 3.9, 3.10 |
| `EventOptionsPanel.kt` | 3.5, 3.7 |
| `ItineraryViewModel.kt` | 3.5, 3.11 |
| `TravelCentsNavigation.kt` | 3.4, 3.11 |
| `MainScaffold.kt` | 3.4, 3.11 |
| `ChatPage.kt` | 3.11 |
| `DirectChatPage.kt` | 3.11 |
| `Message.kt` | 3.11 |
| `FirestoreRepository.kt` | 3.11 |
| `NewTripViewModel.kt` | 3.6 |
| `YelpRepository.kt` | 3.6 |
| `GroqRepository.kt` | 3.6, 3.8 |
| `SerpRepository.kt` | 3.8 |
| `ImageGalleryScreen.kt` *(new)* | 3.10 |
| `debug/test_pipeline.py` *(new, external)* | 3.8 |

---

## API Budget (Dev/Free Tier)

| API | Free Tier | Calls Per Trip | Trips Before Limit |
|-----|-----------|----------------|-------------------|
| SerpAPI | 100 searches/month | 2 (flights + hotels) | ~50/month |
| Yelp Fusion | 500 calls/day | ~7–14 (2–3 per restaurant slot × days) | ~35–70/day |
| Groq | 30 req/min, 14.4K/day | 2 (metadata + activities) | ~7,200/day |
| Total per trip | — | ~11–18 API calls | — |

**Dev strategy:** Mock SerpAPI responses locally with JSON fixtures to preserve the 100/month limit. Yelp and Groq are generous enough for unrestricted dev use.

---

## Reserved Space — Future Tasks

### Phase 6 — TBD
*(Reserved for additional tasks discovered during implementation)*

- [ ] _
- [ ] _
- [ ] _
- [ ] _
- [ ] _

### Phase 7 — TBD

- [ ] _
- [ ] _
- [ ] _
- [ ] _
- [ ] _
