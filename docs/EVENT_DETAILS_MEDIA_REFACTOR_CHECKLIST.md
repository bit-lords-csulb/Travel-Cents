# Event Details Media Refactor Checklist

Updated: 2026-04-16

Related plan: [EVENT_DETAILS_MEDIA_REFACTOR_PLAN.md](C:\Users\Zaher503\AndroidStudioProjects\Travel-Cents\docs\EVENT_DETAILS_MEDIA_REFACTOR_PLAN.md)

## How To Use This Checklist

- Mark items as `[x]` when completed.
- Keep phases in order unless a later item is clearly independent.
- If scope changes, update this file instead of leaving work tracked only in chat.

## Phase 0: Guardrails And Baseline

### Goals

- Confirm current behavior before refactoring.
- Avoid regressions while changing data flow and UI structure.

### Checklist

- [x] Capture current Yelp restaurant/activity generation behavior for a 7-day trip:
  - verify requested pool size for restaurants
  - verify requested pool size for activities
  - verify how many unique businesses are actually used after distribution
- [x] Capture current hotel media behavior:
  - verify selected hotel event `photoUrls`
  - verify hotel option `photoUrls`
  - verify current local image download behavior
- [x] Capture current Yelp media/detail behavior:
  - verify which fields are currently persisted from search
  - verify what `getBusinessDetail()` actually adds under Base plan
  - verify which screens trigger detail enrichment today
- [x] Document the current Firestore event shape for:
  - hotel event
  - Yelp restaurant event
  - Yelp activity event
- [x] Add or update tests around:
  - hotel image mapping from Serp payload
  - Yelp business detail mapping
  - pool distribution logic

## Phase 1: Data Contract Cleanup

### Goals

- Standardize event detail keys.
- Separate shared remote metadata from device-local cache state.

### Checklist

- [x] Standardize hotel detail keys in `SerpRepository.kt` to `attr_*` names where appropriate
- [x] Standardize Yelp detail keys in `YelpRepository.kt` to `attr_*`
- [x] Persist these Yelp Base-plan fields on events and options:
  - [x] `attr_business_name`
  - [x] `attr_business_address`
  - [x] `attr_categories`
  - [x] `attr_phone`
  - [x] `attr_hours_summary`
  - [x] `attr_hours_raw`
  - [x] `attr_average_rating`
  - [x] `attr_review_count`
  - [x] `attr_is_closed`
  - [x] `attr_yelp_url`
  - [x] `attr_profile_photo_url`
  - [x] `attr_has_reservations`
  - [x] `attr_has_waitlist`
  - [x] `attr_has_request_a_quote`
  - [x] `attr_has_food_order`
  - [x] `attr_menu_url`
- [x] Persist static map metadata fields:
  - [x] `attr_latitude`
  - [x] `attr_longitude`
  - [x] `attr_static_map_url`
  - [x] `attr_static_map_provider`
- [x] Audit `TravelEvent.toFirestoreMap()` usage and decide whether `localImagePath` should stop being written as shared state
- [x] Audit `EventOption.toMap()` usage and decide whether `localImagePath` should stop being written as shared state
- [x] Verify Firestore load paths still deserialize:
  - [x] current-trip flow
  - [x] itinerary flow
  - [x] Serp cache flow

## Phase 2: Yelp Pooling And Generation Fixes

### Goals

- Restore intended `days * 5` behavior.
- Stop wasting fetched businesses through overlapping alternatives.
- Preserve every loaded Yelp business as a saved backup option for the relevant generated events.
- Respect Yelp Search paging limits when trip size requires more than one request.

### Checklist

- [x] Replace restaurant distribution logic with chunked 5-per-day grouping
- [x] Replace activity distribution logic with chunked 5-per-day grouping
- [x] Change activity pool request from fixed `20` to `tripDates.size * 5`
- [x] Reuse paged `fetchBusinessPool()` logic for activities
- [x] Ensure restaurant pool still requests `tripDates.size * 5`
- [x] Ensure later days do not unnecessarily reuse earlier businesses as alternatives
- [x] Preserve all loaded restaurant businesses as saved options on each generated restaurant event
- [x] Preserve all loaded activity businesses as saved options on each generated activity event
- [x] Confirm Yelp Search paging assumptions for large trips:
  - [x] max `50` results per individual request
  - [x] page additional results with `offset`
  - [x] cap a single originating query at `240` total results
- [x] Split oversized restaurant/activity pool loads across multiple Yelp Search requests when target count exceeds one page
- [ ] Add fallback behavior for undersized pools:
  - [ ] smaller option groups allowed
  - [ ] duplication only when unavoidable
- [ ] Verify 7-day trip target behavior:
  - [ ] restaurants request about 35 businesses
  - [ ] activities request about 35 businesses
  - [ ] generated default selections stay varied by day while full backup pools remain available

## Phase 3: Yelp Detail Enrichment

### Goals

- Use Yelp Base-plan business details fully.
- Make enriched details available in Firestore and on every device.

### Checklist

- [x] Expand `YelpBusiness` mapping usage to read supported Base-plan fields from Business Details
- [x] Add a shared enrichment function for Yelp-backed events
- [ ] Use that shared enrichment path in:
  - [x] `CurrentTripViewModel`
  - [x] `ItineraryViewModel`
- [x] Persist enriched Yelp attributes back to Firestore
- [x] Persist any supported shared media fields back to Firestore
- [ ] Ensure enrichment happens from a predictable trigger:
  - [x] background on trip load for visible Yelp events
  - [x] fallback on card open if missing
- [ ] Verify enrichment survives:
  - [ ] app reload
  - [ ] trip switch
  - [ ] second device load

## Phase 4: Static Map Pipeline

### Goals

- Replace the fake location background with a real static map image.

### Checklist

- [x] Choose static map provider and document attribution requirements
- [x] Add static map URL builder utility
- [ ] Use existing coordinates from:
  - [x] Yelp businesses
  - [x] Serp hotels
- [x] Persist `attr_static_map_url` on events/options where coordinates exist
- [x] Add address-only fallback when coordinates are missing
- [x] Download/cache static map images locally per device
- [ ] Verify static maps render in:
  - [x] current-trip event details
  - [x] itinerary event details
- [ ] Verify a second device can display the same static map from Firestore metadata

## Phase 5: Hotel Media Everywhere

### Goals

- Make hotel galleries reliable without extra Serp calls.

### Checklist

- [x] Verify selected hotel event preserves all returned Serp image URLs
- [x] Verify hotel options preserve all returned Serp image URLs
- [x] Ensure hotel option selection copies:
  - [x] `imageUrl`
  - [x] `photoUrls`
- [x] Extend local download flow to include selected hotel gallery images
- [x] Add background prefetch for selected hotel galleries on trip load
- [x] Add background prefetch when user switches hotel options
- [ ] Verify hotel galleries are available in:
  - [ ] current-trip screen
  - [ ] itinerary screen
  - [ ] expanded detail views
  - [ ] after trip reload
  - [ ] on second device after Firestore sync and local download

## Phase 6: Media Cache Refactor

### Goals

- Support hero images, galleries, and static maps with the same local cache strategy.

### Checklist

- [ ] Extend `ImageCacheManager.kt` to download lists of URLs, not just hero images
- [ ] Decide URL-to-file naming strategy for gallery and static-map assets
- [ ] Support local caching for:
  - [ ] event hero images
  - [ ] event gallery images
  - [ ] option gallery images
  - [ ] static map images
- [ ] Ensure cache writes are idempotent
- [ ] Ensure cache lookups prefer local files before remote URLs
- [ ] Ensure local cache remains per-device only
- [ ] Verify no shared logic assumes another device can use a saved local path

## Phase 7: Event Details UI Refactor

### Goals

- Replace the monolithic detail layout with type-specific cards.

### Checklist

- [ ] Create `ui/main/current/overlays/cards/`
- [ ] Create shared card primitives:
  - [ ] `DetailCardFrame.kt`
  - [ ] `DetailCardHeader.kt`
  - [ ] `DetailBadgeRow.kt`
  - [ ] `DetailLinkRow.kt`
  - [ ] `StaticMapCard.kt`
- [ ] Create flight-specific cards:
  - [ ] `FlightTimingCard.kt`
  - [ ] `FlightRouteCard.kt`
  - [ ] `FlightPricingCard.kt`
- [ ] Create hotel-specific cards:
  - [ ] `HotelStayCard.kt`
  - [ ] `HotelPricingCard.kt`
  - [ ] `HotelAmenitiesCard.kt`
- [ ] Create restaurant-specific cards:
  - [ ] `RestaurantSummaryCard.kt`
  - [ ] `RestaurantServicesCard.kt`
  - [ ] `RestaurantHoursCard.kt`
- [ ] Create activity-specific cards:
  - [ ] `ActivitySummaryCard.kt`
  - [ ] `ActivityHoursCard.kt`
- [ ] Create shared cards:
  - [ ] `LocationMapCard.kt`
  - [ ] `ReviewsCard.kt`
- [ ] Refactor `CurrentTripEventDetailsDialog.kt` into a coordinator/composer only
- [ ] Replace oversized top cards with compact summary cards
- [ ] Remove `Place Info`
- [ ] Replace `Experience` with useful type-specific cards
- [ ] Reduce map card height and densify address row

## Phase 8: ViewModel Integration Parity

### Goals

- Ensure current-trip and itinerary flows behave the same.

### Checklist

- [ ] Current-trip flow uses shared media/detail enrichment pipeline
- [ ] Itinerary flow uses shared media/detail enrichment pipeline
- [ ] Current-trip flow uses shared static-map handling
- [ ] Itinerary flow uses shared static-map handling
- [ ] Current-trip flow supports hotel gallery prefetch
- [ ] Itinerary flow supports hotel gallery prefetch
- [ ] Verify no screen is left on the old one-image-only path

## Phase 9: Firestore And Performance Cleanup

### Goals

- Keep the solution lean and query-safe.

### Checklist

- [ ] Add Firestore index exemptions for large/non-query fields:
  - [ ] `photoUrls`
  - [ ] `attr_hours_raw`
  - [ ] `attr_static_map_url`
  - [ ] any retained local-only path fields
- [ ] Confirm event documents stay comfortably below Firestore document size limits
- [ ] Confirm option documents stay comfortably below Firestore document size limits
- [ ] Confirm batch writes remain within Firestore request size limits
- [ ] Confirm no new per-hotel Serp calls were introduced
- [ ] Confirm Yelp enrichment is not requesting unsupported Base-plan fields

## Phase 10: Verification And Polish

### Goals

- Ensure the finished system behaves correctly end to end.

### Checklist

- [ ] Generate a fresh trip and verify hotel galleries exist in Firestore metadata
- [ ] Generate a fresh trip and verify Yelp `attr_*` details exist in Firestore metadata
- [ ] Verify selected hotel card opens with gallery available
- [ ] Verify restaurant detail page shows useful cards instead of empty sections
- [ ] Verify activity detail page shows useful cards instead of empty sections
- [ ] Verify static map appears instead of fake hero-image background
- [ ] Verify reviews still load correctly
- [ ] Verify switching hotel options updates gallery correctly
- [ ] Verify switching between trips does not leave stale media/details behind
- [ ] Verify second device behavior:
  - [ ] Firestore metadata loads correctly
  - [ ] remote images download correctly
  - [ ] static maps download correctly
  - [ ] hotel galleries appear
  - [ ] Yelp details appear
- [ ] Run compile/test validation
- [ ] Update the main plan doc if implementation decisions changed

## Deferred / Out Of Scope For This Pass

- [ ] Yelp retention/scrubbing policy enforcement
- [ ] Extra Serp photo endpoints per hotel
- [ ] Flight static maps
- [ ] Large model/data layer rewrite away from `details: Map<String, String>`
