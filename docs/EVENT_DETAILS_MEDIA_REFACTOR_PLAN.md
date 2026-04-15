# Event Details Media Refactor Plan

Updated: 2026-04-15

## Goals

- Show richer, reliable media for hotels, restaurants, and activities.
- Replace the fake location background with a real static map image.
- Persist shareable media metadata in Firestore so galleries work across devices.
- Cache media locally per device so cards open quickly after first load.
- Expand Event Details into type-specific cards instead of one generic layout.
- Use Yelp Base plan details fully and avoid extra Serp usage that would exceed the monthly budget.

## Constraints

### Yelp

- The current Yelp plan is Base.
- Only the following business details should be assumed available and persisted:
  - business name
  - business address
  - categories
  - phone number
  - hours of operation
  - average star rating
  - review count
  - closure status
  - Yelp profile URL
  - business profile photo
  - Yelp Reservations
  - Yelp Waitlist
  - Yelp Request a Quote
  - Yelp food order
  - Yelp menu URL
- Do not plan around unsupported photo/gallery fields for Yelp beyond what is actually available under Base.
- Ignore the 24-hour storage limit in this implementation plan for now. Data scrubbing will be handled separately later.

### Serp

- Serp usage is capped at 200 calls/month.
- Do not introduce follow-up hotel-photo calls per hotel.
- Use only the hotel image URLs already returned by the main `google_hotels` search response.

### Firestore

- Firestore should store media metadata, not image binaries.
- Shared state across devices should be remote URLs and structured attributes.
- Device-local file paths should not be treated as shared/canonical data.

## Current Problems

### 1. Hotel images are only partially utilized

- `SerpRepository.searchHotels()` already maps hotel `images` into event and option `photoUrls`.
- Only the selected event hero image is downloaded locally during trip generation.
- Galleries are not consistently prefetched locally.
- Option galleries are not guaranteed to be ready when the user switches hotels.

### 2. Yelp event data is too thin

- The current Yelp mapping stores only a small subset of available business metadata.
- Many Base-plan fields are not persisted into event details.
- Event Details therefore has empty or low-value sections.

### 3. Yelp image handling is inconsistent

- Search/distribution currently relies on one `imageUrl`.
- Extra Yelp media enrichment is not a reliable shared-data pipeline today.
- Old and new trip flows are not aligned.

### 4. Location card is not a real map

- The current location block uses the event hero image as a dimmed background.
- That is misleading and wastes space.

### 5. Event Details layout is too generic

- `Timing` and `Place Info` are oversized.
- `Place Info` overlaps with top action buttons and directions behavior.
- `Experience` is weak and often empty.
- Hotels, restaurants, and activities all need different information density and different card sets.

### 6. Yelp pool distribution is not matching product intent

- Restaurants still request `tripDates.size * 5`, but distribution wastes a large part of the pool by reusing alternatives.
- Activities no longer request `tripDates.size * 5`; they currently use a fixed pool of `20`.
- For a week-long trip, the intended behavior is roughly `35 restaurant businesses + 35 activity businesses`, but the current implementation does not reliably produce that.

## Media Storage Model

### Shared across devices

Persist to Firestore:

- `imageUrl`
- `photoUrls`
- static map image URL
- structured `attr_*` fields
- provider identifiers required for refresh or linking

This shared metadata is what makes the same gallery and the same detail cards available on another device.

### Stored locally per device

Downloaded media should be cached under app internal storage:

- `files/trip_images/{tripId}/...`

This is what is already done today for hero images.

### Important behavior

- Saving a remote image URL in Firestore does not store the image binary.
- Another device that loads the trip reads the URL from Firestore and performs a normal image download from that URL.
- That is not another Yelp business-data call and not another Serp hotel-data call. It is only a standard image fetch.

## Data Model Plan

Keep using `details: Map<String, String>` for compatibility, but standardize attribute names.

### Yelp restaurant/activity event attributes

Persist these on each Yelp-backed event and Yelp option:

- `attr_business_name`
- `attr_business_address`
- `attr_categories`
- `attr_phone`
- `attr_hours_summary`
- `attr_hours_raw`
- `attr_average_rating`
- `attr_review_count`
- `attr_is_closed`
- `attr_yelp_url`
- `attr_profile_photo_url`
- `attr_has_reservations`
- `attr_has_waitlist`
- `attr_has_request_a_quote`
- `attr_has_food_order`
- `attr_menu_url`

Continue to store:

- `yelp_id`
- `imageUrl`
- `photoUrls`

### Hotel event attributes

Keep and standardize the hotel fields already available from Serp:

- `attr_hotel_name`
- `attr_hotel_rating`
- `attr_review_count`
- `attr_hotel_class`
- `attr_check_in_time`
- `attr_check_out_time`
- `attr_amenities`
- `attr_rate_per_night`
- `attr_group_rate_per_night`
- `attr_rooms_needed`
- `attr_booking_url`

### Static map metadata

Persist:

- `attr_latitude`
- `attr_longitude`
- `attr_static_map_url`
- `attr_static_map_provider`

## Static Map Plan

### Goal

Replace the current fake location background with a real static map image.

### Approach

- Use an OSM-based static map provider.
- Build a map image URL from stored coordinates.
- Save that URL in Firestore so all devices can render the same location preview.
- Download/cache the map image locally the same way other media is cached.

### Coordinate sources

- Yelp businesses already expose coordinates.
- Serp hotels already expose GPS coordinates.
- Flights can continue to use a simpler location presentation unless airport coordinates are added later.

### Fallback

- If coordinates are missing, show a compact address-only location card with the Open in Maps action.
- Do not fall back to the hero photo as a fake map.

## Serp Hotel Image Plan

### Guiding rule

Do not add per-hotel follow-up Serp calls.

### Implementation

1. Keep using only the image URLs returned in the main hotel search response.
2. Ensure those image URLs survive:
   - event creation
   - Firestore save/load
   - option selection
   - current-trip flow
   - itinerary flow
3. Download locally:
   - selected hotel hero image
   - selected hotel gallery images
4. When the user selects a different hotel option:
   - copy over the option `photoUrls`
   - begin local background download for that option gallery
5. Prefetch selected hotel gallery on trip load so hotel cards open with images immediately.

## Yelp Business Details Plan

### Scope under Base plan

Use the 15 supported business details fully and stop planning around unsupported fields that clog the system.

### Implementation

1. Expand Yelp mapping so Business Details populates all supported `attr_*` fields.
2. Persist those attributes into Firestore on the event and option records.
3. Reuse the existing `image_url` as the shared profile photo field.
4. If `photoUrls` is not meaningfully richer under Base, do not create fake multi-photo complexity.
5. Focus on richer detail cards and link/actions support rather than unsupported media assumptions.

## Event Details Refactor Plan

Refactor the current detail layout into reusable type-specific cards.

### Target folder structure

Create a new package:

- `ui/main/current/overlays/cards/`

Create shared card primitives:

- `DetailCardFrame.kt`
- `DetailCardHeader.kt`
- `DetailBadgeRow.kt`
- `DetailLinkRow.kt`
- `StaticMapCard.kt`

Create type-specific cards:

- `FlightTimingCard.kt`
- `FlightRouteCard.kt`
- `FlightPricingCard.kt`
- `HotelStayCard.kt`
- `HotelPricingCard.kt`
- `HotelAmenitiesCard.kt`
- `RestaurantSummaryCard.kt`
- `RestaurantServicesCard.kt`
- `RestaurantHoursCard.kt`
- `ActivitySummaryCard.kt`
- `ActivityHoursCard.kt`
- `LocationMapCard.kt`
- `ReviewsCard.kt`

### Layout changes

#### Replace current generic sections

- Shrink `Timing` into a compact summary card.
- Remove the oversized `Place Info` card.
- Replace `Experience` with useful type-specific cards.
- Keep `Location`, but make it smaller and use a real static map.

#### Hotel detail page

Recommended cards:

- `HotelStayCard`
- `HotelPricingCard`
- `HotelAmenitiesCard`
- `LocationMapCard`
- `ReviewsCard`

#### Restaurant detail page

Recommended cards:

- `RestaurantSummaryCard`
- `RestaurantServicesCard`
- `RestaurantHoursCard`
- `LocationMapCard`
- `ReviewsCard`

#### Activity detail page

Recommended cards:

- `ActivitySummaryCard`
- `ActivityHoursCard`
- `LocationMapCard`
- `ReviewsCard`

#### Flight detail page

Recommended cards:

- `FlightTimingCard`
- `FlightRouteCard`
- `FlightPricingCard`

Flights do not need the same location-map treatment in this phase.

### Compact sizing changes

- Reduce height and padding for the summary cards at the top.
- Reduce the map card height.
- Make address rows denser.
- Use badges and action rows instead of large text blocks.

## File-Level Refactor Plan

### Data layer

- Update `data/trip/remote/YelpRepository.kt`
  - map all Base-plan business details
  - standardize `attr_*` keys
  - fix pool sizing and distribution
- Update `data/trip/remote/SerpRepository.kt`
  - preserve hotel coordinates
  - standardize hotel detail keys
- Update `data/media/ImageCacheManager.kt`
  - support batch download of gallery URLs and static map URLs

### Models

- Update `data/trip/model/TravelEvent.kt`
- Update `data/trip/model/EventOption.kt`

No large structural model rewrite is required yet; the existing maps can still carry this data.

### ViewModels

- Update `ui/main/current/CurrentTripViewModel.kt`
  - background prefetch for selected hotel galleries
  - shared detail enrichment behavior
  - static map/local media caching hooks
- Update `ui/main/itinerary/ItineraryViewModel.kt`
  - keep parity with current-trip behavior
  - use the same photo/static-map enrichment pipeline
- Update `ui/main/newTrip/NewTripViewModel.kt`
  - download selected event hero images
  - download selected hotel galleries
  - download static map images
  - fix pooled Yelp restaurant/activity generation

### UI

- Slim down `ui/main/current/overlays/CurrentTripEventDetailsDialog.kt`
  - turn it into a coordinator/composer only
  - move card rendering into the new `cards/` files

## Yelp Pooling Fix Plan

### Intended behavior

For a 7-day trip:

- restaurants target around `35` businesses
- activities target around `35` businesses

### Current restaurant issue

- The current restaurant request count is still `days * 5`.
- But the distribution logic reuses businesses as alternatives, so much of the pool is wasted.

### Current activity issue

- Activities currently use a fixed pool of `20`.
- This is below the intended weekly target for many trips.

### New distribution logic

Replace the current overlapping alternative logic with chunked groups.

For each day:

- day 1 gets businesses `0..4`
- day 2 gets businesses `5..9`
- day 3 gets businesses `10..14`

Each day becomes one event with:

- one selected primary place
- four unique alternatives

### Request changes

- Restaurants: keep `days * 5`
- Activities: change to `days * 5`
- Use the same paged pool fetch logic for both

### Fallback handling

- If the pool is smaller than requested, still create as many day groups as possible.
- Prefer unique businesses over duplicated alternatives.

## Firestore Plan

### What should be stored

Store:

- remote image URLs
- gallery `photoUrls`
- static map image URL
- `attr_*` metadata

### What should not be shared as canonical state

Do not rely on:

- `localImagePath`

That path is device-specific.

### Indexing

Add index exemptions for large/non-query fields:

- `photoUrls`
- `attr_hours_raw`
- `attr_static_map_url`
- any local-only path fields that remain

## Delivery Order

1. Fix Yelp pool sizing and chunked distribution.
2. Expand Yelp Base-plan business detail mapping into `attr_*`.
3. Standardize hotel detail attributes and preserve hotel coordinates.
4. Add static map URL generation and persistence.
5. Extend local media caching for galleries and static maps.
6. Refactor Event Details into shared and type-specific card files.
7. Make current-trip and itinerary flows use the same detail/media behavior.
8. Verify cross-device behavior:
   - Firestore contains shared URLs and attrs
   - second device downloads and shows the same media

## Success Criteria

- Hotels show their returned gallery images everywhere without extra Serp calls.
- Yelp restaurant/activity cards show all supported Base-plan business details.
- Event Details uses real, smaller static map cards instead of a fake background image.
- Hotels, restaurants, activities, and flights each render their own card sets.
- Shared media metadata works across devices.
- Local cache makes relevant cards open quickly after first sync.
- Restaurant and activity generation again reflects the intended `days * 5` pooled behavior.
