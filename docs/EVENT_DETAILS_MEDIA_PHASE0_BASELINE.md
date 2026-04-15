# Event Details Media Phase 0 Baseline

Updated: 2026-04-15

This document captures the current pre-refactor behavior for Event Details media, Yelp metadata, and Firestore serialization.

## 7-Day Yelp Generation Baseline

- Restaurant pool request size: `tripDates.size * 5`, so a 7-day trip requests `35` businesses in [`NewTripViewModel.kt`](C:/Users/Zaher503/AndroidStudioProjects/Travel-Cents/app/src/main/java/com/example/travelcents/ui/main/newTrip/NewTripViewModel.kt:171).
- Activity pool request size: fixed `20` businesses regardless of trip length in [`YelpRepository.kt`](C:/Users/Zaher503/AndroidStudioProjects/Travel-Cents/app/src/main/java/com/example/travelcents/data/trip/remote/YelpRepository.kt:183).
- Distribution logic: `distributePoolToEvents()` assigns day `i` as the primary and then fills the other four slots from the start of the remaining pool, which heavily overlaps alternatives across days in [`YelpRepository.kt`](C:/Users/Zaher503/AndroidStudioProjects/Travel-Cents/app/src/main/java/com/example/travelcents/data/trip/remote/YelpRepository.kt:242).
- Effective unique-business usage for a 7-day / 35-business restaurant pool: only `7` unique businesses appear across all generated option sets under the current distribution logic.
- If the pool is smaller than the trip length, later days are dropped entirely instead of receiving reduced option groups.

## Hotel Media Baseline

- Serp hotel search already keeps all selected hotel image URLs in the event `photoUrls` list and uses the first one as the event `imageUrl` in [`SerpRepository.kt`](C:/Users/Zaher503/AndroidStudioProjects/Travel-Cents/app/src/main/java/com/example/travelcents/data/trip/remote/SerpRepository.kt:459).
- Hotel options also keep all mapped image URLs in `photoUrls`, with `original_image` preferred over `thumbnail`.
- Trip generation only downloads hero images from `allEvents.map { it.imageUrl }`; gallery images are not prefetched in [`NewTripViewModel.kt`](C:/Users/Zaher503/AndroidStudioProjects/Travel-Cents/app/src/main/java/com/example/travelcents/ui/main/newTrip/NewTripViewModel.kt:223).
- The downloaded local path is written back only to the selected event via `localImagePath`; options do not get gallery-local cache paths during trip creation.

## Yelp Media And Detail Baseline

- Yelp search persistence on selected restaurant/activity events is currently limited to:
  - name (`restaurant_name` or `activity_name`)
  - `price_tier`
  - `rating`
  - `review_count`
  - `address`
  - `categories`
  - `yelp_id`
- Yelp option persistence adds only a small amount beyond the event fields:
  - `name`
  - `phone`
  - `distance_m`
  - `delivery`
  - `pickup`
- `getBusinessDetail()` currently returns the raw `YelpBusiness` detail payload, including `hours`, `photos`, `coordinates`, `is_closed`, and `attributes`, but there is no shared enrichment pipeline that maps those fields into Firestore today.
- Current detail enrichment trigger:
  - `CurrentTripScreen` calls `fetchYelpPhotos()` when an event details dialog opens.
  - `fetchYelpPhotos()` only runs in the current-trip flow, only when the event has `<= 1` photo, and only updates in-memory `photoUrls`.
  - It does not persist enriched photos or metadata back to Firestore.
  - The itinerary flow does not trigger `getBusinessDetail()` today.

## Firestore Event Shape Baseline

- Event documents are written from `TravelEvent.toFirestoreMap()` and always include:
  - `eventId`
  - `type`
  - `itineraryId`
  - `tz`
  - `date`
  - `startTime`
  - `endTime`
  - `imageUrl`
  - `localImagePath`
  - `photoUrls`
  - every entry from `details`
- Option documents are written from `EventOption.toMap()` and always include:
  - `optionId`
  - `eventId`
  - `source`
  - `selected`
  - `votes`
  - `imageUrl`
  - `localImagePath`
  - `photoUrls`
  - every entry from `details`

### Current hotel event detail keys

- `hotel_name`
- `check_in_date`
- `check_out_date`
- `rating`
- `review_count`
- `hotel_class`
- `rate_per_night`
- `group_rate_per_night`
- `rooms_needed`
- `rate_per_night_display`
- `deal`
- `amenities`

### Current Yelp restaurant event detail keys

- `restaurant_name`
- `price_tier`
- `rating`
- `review_count`
- `address`
- `categories`
- `yelp_id`

### Current Yelp activity event detail keys

- `activity_name`
- `price_tier`
- `rating`
- `review_count`
- `address`
- `categories`
- `yelp_id`

## Firestore Load Path Baseline

- `CurrentTripViewModel` and `ItineraryViewModel` both deserialize `localImagePath` and `photoUrls` directly from Firestore.
- Both flows also treat `imageUrl` as a local path if it starts with `/` or `file:/`, which preserves older mixed-state records.
- `SerpCache` serializes cached events through `TravelEvent.toCacheMap()` and deserializes them with `TravelEvent.fromCacheMap()`, so the cache path also currently preserves `localImagePath` as if it were shared event state.

## Baseline Tests Added

- `SerpHotelMappingTest`
- `YelpBusinessDetailMappingTest`
- `YelpPoolDistributionTest`
