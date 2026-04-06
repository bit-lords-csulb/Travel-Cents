# Travel Cents — Completed Tasks Archive

> All tasks marked [x] as of 2026-04-05. For active work see `plan-pending.md`.

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

### Task 1.1 — Bundle Static Images
- [x] Source and add to `res/drawable-nodpi/`: 6 destination images + 2 landing page card images
- [x] Source and add to `res/drawable-nodpi/`: 20 interest images — replaced all 8 Google Aida URLs and all 12 LoremFlickr placeholders
- [x] Replace every `AsyncImage(model = "https://...")` for these images with `painterResource(R.drawable.*)`
- [x] Remove LoremFlickr URLs from `TripStep5InterestsPage.kt` entirely
- [x] Remove Google Aida CDN URLs from all wizard pages

### Task 1.2 — Reduce Main Thread Work
- [x] Move Firestore listener setup out of `init{}` blocks — defer to first collection in `viewModelScope.launch`
- [x] Fix `FriendsViewModel` O(n) nested listeners — batch friend lookups into a single `whereIn` query
- [x] Fix `FirestoreRepository.listenToDirectChatPreviews()` N+1 problem — batch user name lookups
- [x] Remove eager `sharedItineraryViewModel.loadTrip()` from `MainScaffold` LaunchedEffect
- [x] `ItineraryViewModel.fetchAllTrips()` — add `.whereEqualTo("userId", uid)` server-side filter

### Task 1.3 — Reduce Logging Overhead
- [x] Gate logging level behind `BuildConfig.DEBUG`: `BODY` in debug, `NONE` in release
- [x] Apply to both OkHttpClient instances (Groq + SerpAPI)

---

## Phase 2 — Event Management Pipeline

### Task 2.1 — Data Model: Multi-Option Events ✅
- [x] Create `EventOption` data class: `optionId`, `eventId`, `source`, `selected`, `votes`, `imageUrl`, `localImagePath`, `details`
- [x] No `rejected` field persisted — rejection state is session-only (`Set<optionId>` in ViewModel)
- [x] Price display fields populated in `details` map for all event types (flights, hotels, restaurants, activities)
- [x] All price fields stored as strings in `details` map; parse to numeric only at display time
- [x] Add `imageUrl` field to `TravelEvent`
- [x] Add `options: List<EventOption>` to `TravelEvent`
- [x] Define Firestore schema: `users/{uid}/trips/{tripId}/events/{eventId}/options/{optionId}`

### Task 2.2 — SerpAPI: Extract Multiple Flights + Images ✅
- [x] Remove `.take(1)` — store ALL flight options returned by the call
- [x] Add `airline_logo`, `legroom`, delay/overnight/plane metadata to flight leg model
- [x] Add `carbon_emissions` to flight option model
- [x] Add `price_insights` to flight response model
- [x] Map `airline_logo` URL → `EventOption.imageUrl`
- [x] Default selection: first result from `best_flights`; fallback to `other_flights`
- [x] `stops=2` on first attempt, `stops=3` on metro fallbacks
- [x] Return full `List<EventOption>` — all options for user to browse

### Task 2.3 — SerpAPI: Extract Multiple Hotels + Images (partial)
- [x] Remove `.take(1)` — store ALL hotels from page 1 (~20)
- [x] Use `sort_by=8` (highest rated). Pass `adults` count.
- [x] Group pricing: `roomsNeeded = adults / 2`. Compute `group_rate_per_night`. Store both values.
- [x] Add full hotel model fields: `images[]`, `overall_rating`, `reviews`, `amenities[]`, `check_in/out_time`, `hotel_class`, `deal`, `eco_certified`, `gps_coordinates`, `nearby_places[]`
- [x] Add `prices[]` per hotel: source, logo, link, rate_per_night, total_rate, free_cancellation, discount_remarks
- [x] Map `images[0].thumbnail` → `EventOption.imageUrl`; store `images[0].original_image` for expanded view
- [x] Default selection: highest-rated hotel within budget (first result from `sort_by=8`)
- [x] Booking link: store cheapest `prices[].link` in `EventOption.details["booking_url"]`

### Task 2.4 — Flight Search: Fallback for Empty Results (partial)
- [x] Default `stops=2` on first attempt, `stops=3` on metro fallbacks
- [x] Build metro airport map for top 30 metros
- [x] Fallback sequence: IATA+IATA → metro+IATA → metro+metro → "No flights found" placeholder

### Task 2.5 — Yelp Integration (partial)
- [x] Create `YelpRepository.kt` with `searchRestaurants`, `searchActivities`, `searchEvents`
- [x] Business search: extract per result — `id`, `name`, `image_url`, `rating`, `review_count`, `price`, `categories[]`, `address`, `phone`, `transactions[]`, `distance`
- [x] Restaurants: 1 call per day, `limit=5`, dietary preference as additional categories
- [x] Activities: 1 call per day, `limit=5`, `categories=arts,museums,tours,landmarks`
- [x] Events: 1 call per trip, date range = full trip duration
- [x] Lazy detail fetch on card expand: `getBusinessDetail` and `getBusinessReviews` exist; caching wired in `ItineraryViewModel`
- [x] Add `dietary: List<String>` to `TravelRequest` and `NewTripViewModel`
- [x] Expose dietary selection UI in Step 5 — preset checkboxes + `Other` field
- [x] Map dietary prefs to Yelp category aliases; pass to Groq prompt

### Task 2.6 — Local Image Storage (partial)
- [x] Create `ImageCacheManager`: downloads URLs to `context.filesDir/trip_images/{tripId}/`, returns local paths
- [x] Called in pipeline at `DOWNLOADING_IMAGES` step — batch downloads all selected + alternative image URLs
- [x] Store local path in `EventOption.localImagePath` in Firestore
- [x] When a trip is deleted: call `ImageCacheManager.deleteTripImages()` — wired in `ItineraryViewModel.deleteTrip()`

### Task 2.7 — Pipeline Orchestration Update ✅
- [x] Update generation pipeline with all 7 steps (CRAFTING → SEARCHING → FINDING_HOTELS → FINDING_RESTAURANTS → FINDING_ACTIVITIES → DOWNLOADING_IMAGES → SAVING → COMPLETE)
- [x] Update `TripGeneratingPage.kt` step labels — all 7 steps rendered with animated card UI
- [x] Total calls per 7-day trip documented (~20 calls)

---

## Phase 3 — Event Selection UX (original)

### Task 3.1 — Option Selection UI (partial)
- [x] Each event card shows a "Change" button in the top-right corner
- [x] Show option count badge on cards with alternatives
- [x] Tapping "Change" opens a `ModalBottomSheet` with alternatives list, price formatting, swap on tap, rejected accordion
- [x] Drag-to-reorder: cards within a day are drag-reorderable via `sh.calvin.reorderable:reorderable:2.4.0`
- [x] Selection and reorder changes update Firestore and in-memory state
- [x] Rejected options tracked in ViewModel as `Set<optionId>` per event slot (session only)

### Task 3.2 — Trip Sharing via Chat (partial)
- [x] Add "Share" icon button to `FinalPlanPage` top bar
- [x] Tapping opens a bottom sheet: pick a chat from the user's chat list
- [x] Sends a structured trip card message with trip name, destination, date range, cover image
- [x] Store `sharedTripId` + `ownerUid` + trip metadata in the chat message Firestore document
- [x] In chat: render trip card messages with a distinct card composable

### Task 3.3 — Event Card Expandable Detail View ✅
- [x] Tapping a card opens `ExpandedEventCard` overlay
- [x] Full-size hero image + all type-specific details (flight, hotel, restaurant, activity)
- [x] Yelp reviews: lazily fetch on first expand, cache in ViewModel
- [x] "Change" button inside expanded view opens the options panel
- [x] Inline edit fields for title, time, notes — saves to Firestore on dismiss
- [x] Tap outside to dismiss
- [x] Only one card expanded at a time

---

## Phase 3 — Post-Completion Refinements

### Task 3.4 — FinalPlan Header & Navigation Redesign ✅
- [x] Remove trip-name dropdown from top bar
- [x] Move trip name to full-width heading row below top bar
- [x] Top bar left: back arrow only
- [x] Top bar right: triple-dot menu (Share, Reorder, Archive, Delete with confirmation)
- [x] Add `BottomNavigationBar` to `FinalPlan` scaffold

### Task 3.5 — Card Content & Swap Quality Fixes (partial)
- [x] Primary card label: render real name as largest text, type tag is secondary
- [x] Remove option-count badge — replace with plain "Change" button only
- [x] Change menu row content: real name + supporting detail — never "Option N"
- [x] Post-swap card update: update in-memory selected option immediately

### Task 3.6 — No-Duplicate Daily Recommendations (partial)
- [x] After fetching Yelp pools, distribute round-robin across days — day 1 gets option[0], day 2 gets option[1], etc.
- [x] If Yelp pool smaller than days, wrap around for alternatives only; fall back to Groq-generated name for overflow primaries

### Task 3.8 — API Response Debugging Investigation (partial)
- [x] Create `test.py` — standalone Python script reproducing the full pipeline outside Android
- [x] Flight missing root cause confirmed: parser issue reading `departure_airport.time`/`arrival_airport.time` instead of flat `departureTime`/`arrivalTime`
- [x] Root-cause audit completed via `enhancement_plan.md` — Yelp fan-out, repeat primaries, hotel image download time all confirmed
- [x] `EventOption.source` already populated with `"serp"` / `"yelp"` / `"groq"`

#### Task 3.8.1 — Investigation Output Summary ✅
- [x] Adversarial audit completed against live pipeline using `test.py`; documented in `enhancement_plan.md`
- [x] Evidence captured: 1 Groq call, 2 Serp calls, 33 Yelp calls on 16-day trip; repeated primaries; many 429 failures; hotel image prefetch dominated runtime

#### Task 3.8.2 — Order Of Operations (partial)
- [x] Update `SerpRepository.kt` flight parsing to read the actual live response time fields
- [x] Ensure future trips generate both outbound and return flight events using `departure_token`
- [x] Allow Final Plan to render flight cards even when `date` is blank
- [x] Add parser/unit coverage for the live Serp flight payload shape
- [x] Replace per-day Yelp searches with pooled searches + local distribution across days
- [x] Assign primaries round-robin by business id; improve hotel selection locally
- [x] Change `ImageCacheManager.kt` so generation prefetches only selected hero images; defer alternative gallery images

#### Task 3.8.3 — Concrete File Change Map (partial)
- [x] `SerpRepository.kt`: fix flight time parsing, generate outbound + return flight events, use `departure_token`, version the flight cache
- [x] `FinalPlan.kt`: stop dropping undated flight events; group under fallback header; show all available flight info
- [x] `SerpModels.kt`: add `departure_token`, round-trip type support
- [x] `SerpFlightParsingTest.kt`: cover airport-level timestamps, legacy flat fields, `departure_token` parsing
- [x] `enhancement_plan.md`: adversarial audit committed

### Task 3.9 — Expanded Card Overlay Fixes ✅
- [x] Replace full-screen `Dialog` with `ModalBottomSheet`
- [x] Hero image area: display full stored image, not a small thumbnail
- [x] Show all stored `EventOption.details` fields in readable layout
- [x] Gallery icon (2×2 grid) vs X close based on photo count
- [x] Yelp reviews: show 3 cached review excerpts as quote cards

### Task 3.12 — UI Density & Spacing Fixes (partial)
- [x] Reduce vertical gap between event cards within a day to 8dp
- [x] Reduce vertical gap between last card of one day and next day header to 16dp
- [x] Day headers: reduce padding top/bottom
- [x] Audit all `Spacer` calls in timeline composable and halve any value above 12dp

---

## Phase 4 — UI Standardization

### Task 4.3 — Consolidate Color Definitions ✅
- [x] Create a single `TripWizardColors` object with shared color definitions
- [x] Replace all S1*/S2*/S3*/S4*/S5* references across step pages
- [x] Single source of truth for the wizard color palette

### Task 4.4 — Remove Save Draft ✅
- [x] Delete "Save Draft" button from `TripStep4BudgetPage.kt`
- [x] Remove `onSaveDraftClick` callback parameter
- [x] Remove the corresponding `navController.navigate(MainRoutes.Home)` handler from `MainScaffold.kt`
- [x] Step 4 bottom bar becomes single-button layout

### Task 4.5 — Step 5: Add Dietary Restrictions Section ✅
- [x] Below interests grid: "DIETARY RESTRICTIONS" label + subtitle
- [x] Checkbox rows for Vegan, Vegetarian, Halal, Kosher, Gluten-Free, Other (reveals text field)
- [x] Multi-select, unselected by default, blue accent styling
- [x] Store in `NewTripViewModel` as `List<String>`; pass into `TravelRequest`, `YelpRepository`, Groq prompt
- [x] "Continue to Generate" button remains enabled regardless of dietary selection

### Task 4.2 — Standardize color variable names
- [x] Consolidated into `TripWizardColors` (done in Task 4.3)
