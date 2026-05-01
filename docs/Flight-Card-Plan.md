# Flight Expanded Card — Phased Implementation Plan

This plan applies the [BP.md](./BP.md) blueprint to the **flight** event type.
It takes the hotel stack as the structural reference, fixes the existing
low-resolution hero image, and extends the stack with flight-specific
sub-cards: live status, seat map, baggage, boarding pass, airport amenities,
arrival-city preview.

All new services chosen below have a **generous free tier or free trial** —
no paid-only APIs. Each service entry lists the exact quota / trial terms so
we can sanity-check before signing up.

Blueprint constraints that apply to every phase:

- Every new sub-card early-returns when its data is absent. No empty cards.
- Every new field is an `ATTR_*` constant in `EventDetailContract.kt`, read
  through `detailValue(ATTR_X, "legacy_key")`.
- Every sub-card wraps in `DetailCardFrame(accent = CardSky)` and composes
  `DetailCardHeader` / `DetailBadgeRow` / `DetailLinkRow`.
- Restaurant/hotel-only sub-cards (stay, menu, reservations-for-dining,
  wait-time, neighborhood vibe) are never added to the flight branch.

## Current State

Flight branch of `EventDetailCardStack` in
`CurrentTripEventDetailsDialog.kt:261-265` today:

```
EventSummaryCard     (shared — uses airline logo PNG as hero → LOW RES)
DetailActionRow      (shared)
FlightTimingCard
FlightRouteCard
FlightPricingCard
```

`SerpRepository.kt:361` sets `event.imageUrl = selectedOption.airlineLogo` —
a 50–100 px Google Flights logo PNG. `EventSummaryCard` then blows it up to a
large rounded hero tile, which is where the pixelation complaint comes from.
`FlightTimingCard` hardcodes the origin time-zone for both depart and arrive
cells (arrival is actually in a different zone most of the time).
`FlightPricingCard` renders `"Price unavailable"` and
`"Round-trip fare shown from the selected flight option."` boilerplate even
when there's nothing useful to say — blueprint violation.

## Target State

```
EventSummaryCard                 (UPGRADED hero — see §A)
DetailActionRow                  (shared)
FlightTimingCard                 (UPGRADED — per-leg time zones, day-change indicator)
FlightRouteCard                  (UPGRADED — great-circle map, stop indicator)
FlightStatusCard                 (NEW — live status, gate, terminal, on-time %)
FlightLegsCard                   (NEW — per-leg breakdown for multi-stop itineraries)
FlightSeatCard                   (NEW — seat number, seat map preview)
FlightBaggageCard                (NEW — carry-on + checked allowance, fees)
FlightAircraftCard               (NEW — aircraft type, age, amenity icons)
FlightPricingCard                (SLIMMED — price, cabin, carbon, no boilerplate)
BookingOffersCard                (NEW — Skyscanner / Kayak / Google Flights / airline direct)
FlightWeatherCard                (NEW — destination weather at arrival time)
ArrivalCityCard                  (NEW — destination preview: time-zone delta, currency, advisory)
BoardingPassCard                 (NEW — when user pastes a PNR / pass URL)
LoyaltyCard                      (NEW — points earned for linked program)
```

Every card after `DetailActionRow` is opt-in per blueprint — if its data is
missing, the card never renders.

---

## Section A — Fixing the Flight Hero Image

This is its own section because it's the most visible problem and the fix is
independent of the new cards below. Ship Phase 1 with this in it.

### Diagnosis

- `SerpRepository.kt:361` stores `selectedOption.airlineLogo` (a tiny PNG from
  Google Flights) into `event.imageUrl`.
- `TripPhotoGallery.kt:33 heroImageModel(...)` reads `imageUrl` verbatim and
  Coil scales the 80 px logo to fill the ~140 dp × 140 dp summary tile.
- No alternate image source is consulted.

### Strategy — 4-tier hero resolution chain

Prefer the highest-quality source available, degrade gracefully. Stop at the
first source that resolves.

| Priority | Source | Why | Free? |
|:---:|---|---|---|
| 1 | **Cached destination image** already fetched by `DestinationImageRepository` for the trip | Already downloaded; same image the rest of the trip uses; zero new calls | Yes — already in-app |
| 2 | **Unsplash** photo for `"{destination_city} aerial"` or `"{destination_city} skyline"` | Consistently beautiful, 1600+ px, photographer credits built-in | Unsplash API: 50 req/hr free demo, 5,000 req/hr after free production approval |
| 3 | **Wikimedia Commons** image for the destination airport or its IATA code | Public-domain aerial photos of airports are common; no key needed | Completely free, no key, just a `User-Agent` header |
| 4 | **High-res airline logo** from **Airhex** (`logos.skyscnr.com` pattern) or the Skyscanner CDN | Not a hero image per se, but far better than the Google Flights 80 px PNG — used as a tinted background behind a gradient if tiers 1–3 all fail | Airhex: free tier for small logos (up to 100 px); paid for 300 px+. Skyscanner logos are 240 px, free via their public CDN. |

The resolved URL gets stored in the new `ATTR_HERO_IMAGE_URL` field so it
persists in Firestore and doesn't re-resolve on every open.

### Composition treatment

`EventSummaryCard` for flights should layer:

1. The resolved hero (tier 1/2/3) as the background, `ContentScale.Crop`.
2. A dark gradient overlay (we already do this for other types).
3. The high-res airline logo on top in a small rounded pill, bottom-left —
   this is where the airline branding lives, not behind a gradient.

This gives us the best of both: a pretty destination hero + clear airline
identification.

### Files touched

- `data/trip/remote/FlightHeroImageRepository.kt` — NEW. Tier resolution
  chain. Heavy use of the existing `DestinationImageRepository` in tier 1.
- `data/trip/remote/SerpRepository.kt:361` — stop using `airlineLogo` as
  `imageUrl`. Store it separately as `ATTR_AIRLINE_LOGO_URL`.
- `data/trip/model/EventDetailContract.kt` — add
  `ATTR_HERO_IMAGE_URL`, `ATTR_HERO_IMAGE_ATTRIBUTION`,
  `ATTR_AIRLINE_LOGO_URL`.
- `ui/main/current/overlays/cards/EventSummaryCard.kt` — when
  `event.type == "flight"`, render the logo pill overlay described above.
  Use the existing `HotelSummaryCard` pattern as a reference (the hotel
  summary already has a custom variant; the flight variant follows the same
  structure).
- `ui/modules/TripPhotoGallery.kt` — update `heroImageModel(...)` to prefer
  `ATTR_HERO_IMAGE_URL` over `event.imageUrl` for flight events.

### Free-tier summary

| Service | Free-tier terms | Key env var |
|---|---|---|
| Unsplash API | 50 req/hour (demo) → 5,000 req/hour (production) after a 1-form approval. Per-photo download tracking is required — trivial ping. | `UNSPLASH_ACCESS_KEY` |
| Wikimedia Commons API | Completely free, no key. Just send a descriptive `User-Agent` header. | none |
| Skyscanner logo CDN | Free, no key. URL pattern: `https://logos.skyscnr.com/images/airlines/favicon/{IATA}.png` (240 px). | none |
| Airhex | Free tier: small logo sizes up to 100 px. Paid for larger. We only need small. | `AIRHEX_API_KEY` (optional — we can skip entirely and use Skyscanner CDN) |

None of these require credit-card-on-file.

### Done when

- Opening a flight from LAX→NRT shows a high-res Tokyo skyline hero with a
  small JAL logo pill, not a giant pixelated JAL logo.
- Offline / previously-opened flights still render (the URL is persisted in
  `ATTR_HERO_IMAGE_URL` and the local cache handles the rest).
- Removing all hero tiers (tiers 1–3 all fail) gracefully falls back to a
  `CardSky → CardSkyDark` gradient with the airline logo pill on top. No
  crash, no broken image icon.

---

## Phase 0 — Pre-flight & ground rules

**Scope:** No code changes. Sign-off only.
**Exit criteria:** Team agrees on Section A hero strategy + the phase
ordering below. Confirm which free-tier API keys to provision.

Tasks:

- Create Unsplash, AviationStack, AeroDataBox, Amadeus self-service
  accounts. All four are gratis.
- Audit existing SerpAPI flight responses to list every field we already
  receive but don't surface — cheap wins for Phase 1.
- Decide whether we want Amadeus in the stack (generous free tier but
  another integration to maintain). Recommendation: **yes**, because it
  covers seat maps + airport info + flight delay prediction in a single
  API.

---

## Phase 1 — Hero image fix + existing-card cleanup

**Goal:** Ship the Section A hero image fix and bring the three existing
flight cards into blueprint shape. No new external data yet — uses what
SerpAPI already returns + the Section A services for imagery only.

**Files touched:**
- All files listed in Section A.
- `ui/main/current/overlays/cards/FlightTimingCard.kt` — use per-leg time
  zones. Add a "+1d" / "-1d" pill when arrival date ≠ departure date. Drop
  the `timezoneAbbr(event.tz)` fallback that assumes origin-zone for both.
- `ui/main/current/overlays/cards/FlightPricingCard.kt` — remove
  `"Price unavailable"` and `"Round-trip fare shown…"` boilerplate. Early
  return if no price. Keep cabin row + carbon badge.
- `ui/main/current/overlays/cards/FlightRouteCard.kt` — add a tiny
  great-circle arc SVG between the two airport codes (pure `Canvas`, no
  library). Add a stops chip ("Nonstop" / "1 stop · ORD").

**New ATTR constants:**

```kotlin
// Hero
const val ATTR_HERO_IMAGE_URL = "attr_hero_image_url"
const val ATTR_HERO_IMAGE_ATTRIBUTION = "attr_hero_image_attribution"
const val ATTR_AIRLINE_LOGO_URL = "attr_airline_logo_url"

// Route / timing
const val ATTR_DESTINATION_TZ = "attr_destination_tz"          // "Asia/Tokyo"
const val ATTR_ARRIVAL_DAY_OFFSET = "attr_arrival_day_offset"  // "1" / "-1" / "0"
const val ATTR_ORIGIN_CITY = "attr_origin_city"
const val ATTR_DESTINATION_CITY = "attr_destination_city"
const val ATTR_STOP_AIRPORTS = "attr_stop_airports"            // CSV of IATA codes
```

**Done when:**
- Blueprint violations (hardcoded "Price unavailable" / "Round-trip fare…")
  are gone.
- A LAX→NRT red-eye shows Tokyo local time for arrival with a "+1d" pill.
- Route card shows "1 stop · ICN" or "Nonstop" instead of a number.

---

## Phase 2 — FlightStatusCard (live status) + FlightLegsCard

**Goal:** The highest-utility flight add: live flight status. Users check
the itinerary the morning of the flight — showing gate / terminal / delay
inline removes a jump to the airline app.

**Files touched:**
- `data/trip/remote/FlightStatusRepository.kt` — NEW.
- `ui/main/current/overlays/cards/FlightStatusCard.kt` — NEW.
- `ui/main/current/overlays/cards/FlightLegsCard.kt` — NEW (per-leg timeline
  for multi-stop flights; uses the `leg_N_*` fields already stored by
  `SerpRepository.kt:469-480`).
- `CurrentTripEventDetailsDialog.kt` — slot both cards after
  `FlightRouteCard`.

**New ATTR constants:**

```kotlin
const val ATTR_FLIGHT_STATUS = "attr_flight_status"            // scheduled / boarding / in_air / landed / delayed / cancelled
const val ATTR_GATE = "attr_gate"
const val ATTR_TERMINAL = "attr_terminal"
const val ATTR_ARRIVAL_GATE = "attr_arrival_gate"
const val ATTR_ARRIVAL_TERMINAL = "attr_arrival_terminal"
const val ATTR_DELAY_MIN = "attr_delay_min"
const val ATTR_ONTIME_PERCENT = "attr_ontime_percent"
const val ATTR_LIVE_STATUS_UPDATED_AT = "attr_live_status_updated_at"
```

**External service — primary:**

| Service | Free tier | What it gives | Notes |
|---|---|---|---|
| **AviationStack** | 100 requests/month, HTTPS-only on paid tier but HTTP on free | Live flight status by flight number + date | Best free-tier terms for this job. 100/month is OK because we only hit it when the dialog opens on the day of travel (gated client-side). |
| **AeroDataBox** (via RapidAPI) | 500 req/month free | Flight status, airport metadata, aircraft info | Secondary source / fallback. Also covers Phase 5 aircraft lookup. |
| **OpenSky Network** | Free, no key for anonymous. Rate-limited (100 req / 24h anonymous, 1000 req / 24h free account) | Live aircraft position by ICAO24 — useful if we want a "plane is currently here" map dot, but doesn't give scheduled status | Optional stretch goal. |

Pick **AviationStack as primary**, **AeroDataBox as fallback**. Both are
free. Add `AVIATIONSTACK_KEY` and `AERODATABOX_KEY` to `local.properties`.

**Caching / refresh rules:**

- Only query when the flight is within a 24-hour window of its scheduled
  departure — outside that window, no point.
- 10-minute cache keyed on `{flight_number}|{departure_date}`.
- Refresh on dialog open if cache age > 10 min AND within the 24h window.

**FlightStatusCard content:**

- Header: eyebrow "Live", title = status label ("On time", "Delayed 35 min",
  "Landed", etc).
- Grid: Gate · Terminal · Delay · On-time %.
- "Last updated 2 min ago" footer + manual refresh icon.

Early-return if `ATTR_FLIGHT_STATUS` is blank (i.e., outside 24h window and
no cache, or both providers failed).

**FlightLegsCard content:**

- Only renders if `stops > 0`.
- Vertical timeline: each leg gets a row with airline+flight# / depart
  airport+time / arrive airport+time / layover chip ("3h 20m layover in ICN").
- Overnight / often-delayed chips from existing `leg_N_overnight` /
  `leg_N_often_delayed` fields.

**Done when:**
- A flight 3 hours from departure shows "On time · Gate 42B · Terminal 2."
- A flight a week out: status card absent.
- A LAX→ICN→NRT flight shows a 3-row timeline with the ICN layover.

---

## Phase 3 — BookingOffersCard (flight edition) + LoyaltyCard

**Goal:** Apply the hotel blueprint's multi-provider offers pattern to
flights. Surface "Book on Skyscanner / Kayak / Google Flights / Airline."
Add a small loyalty / points card when the user has linked a program.

**Files touched:**
- `data/trip/remote/FlightOffersResolver.kt` — NEW. Deterministic URL
  construction — no API calls, no keys.
- `ui/main/current/overlays/cards/FlightBookingOffersCard.kt` — NEW. Reuses
  the shared `ProviderOfferRow` extracted in the Restaurant plan Phase 3.
- `ui/main/current/overlays/cards/LoyaltyCard.kt` — NEW.
- `CurrentTripEventDetailsDialog.kt` — slot after `FlightPricingCard`.

**New ATTR constants (mirror hotel offer schema):**

```kotlin
// Reuse existing ATTR_OFFER_COUNT + offer_{i}_source / _link / _logo
const val ATTR_LOYALTY_PROGRAM = "attr_loyalty_program"        // "AAdvantage"
const val ATTR_POINTS_EARNED = "attr_points_earned"
const val ATTR_FREQUENT_FLYER_NUMBER = "attr_frequent_flyer_number"
```

**Offer provider strategy (no API keys required, all deterministic URLs):**

| Provider | URL pattern | Free? |
|---|---|---|
| Skyscanner | `skyscanner.com/transport/flights/{from}/{to}/{yymmdd}/{yymmdd}/?adults={n}` | Yes |
| Kayak | `kayak.com/flights/{from}-{to}/{yyyy-mm-dd}/{yyyy-mm-dd}/{n}adults` | Yes |
| Google Flights | `google.com/travel/flights?q=Flights+from+{from}+to+{to}+on+{date}` | Yes |
| Kiwi.com | `kiwi.com/en/search/results/{from}/{to}/{date}/{return}` | Yes |
| Airline-direct | From `airline.com` / `ATTR_AIRLINE_BOOKING_URL` if SerpAPI supplied it | — |

All five are deterministic URL templates. Zero API calls in this phase — the
URLs open the provider's search in the user's browser.

**LoyaltyCard content:**

- Header: eyebrow "Points", title = `"Earn ~${points} miles"`.
- Program chip. "Change program" link opens a future settings screen.

Early-return if no linked program and no known miles estimate.

**Phase 3b (later):** Integrate AwardHacker / seats.aero-style free APIs for
real "redeem this for X points" suggestions. Out of scope for initial
implementation.

**Done when:**
- A LAX→NRT flight card renders 4 offer rows.
- Tapping Skyscanner opens the correct dated search.
- No API keys were added for this phase.

---

## Phase 4 — FlightSeatCard + FlightBaggageCard + FlightAircraftCard

**Goal:** Fill the "about this flight" sub-cards that hotels / restaurants
already have analogs for. Seat maps, bag rules, aircraft type.

**Files touched:**
- `data/trip/remote/AmadeusRepository.kt` — NEW. Amadeus self-service
  covers all three sub-cards in this phase.
- `data/trip/remote/AeroDataBoxRepository.kt` — reuse from Phase 2 for
  aircraft registration + age lookup.
- `ui/main/current/overlays/cards/FlightSeatCard.kt` — NEW.
- `ui/main/current/overlays/cards/FlightBaggageCard.kt` — NEW.
- `ui/main/current/overlays/cards/FlightAircraftCard.kt` — NEW.
- `CurrentTripEventDetailsDialog.kt` — slot all three between
  `FlightLegsCard` and `FlightPricingCard`.

**New ATTR constants:**

```kotlin
// Seat
const val ATTR_SEAT_NUMBER = "attr_seat_number"
const val ATTR_SEAT_MAP_URL = "attr_seat_map_url"
const val ATTR_CHANGE_SEAT_URL = "attr_change_seat_url"

// Baggage
const val ATTR_CARRYON_ALLOWANCE = "attr_carryon_allowance"    // "1 × 7kg"
const val ATTR_CHECKED_ALLOWANCE = "attr_checked_allowance"    // "2 × 23kg"
const val ATTR_BAGGAGE_FEES_URL = "attr_baggage_fees_url"

// Aircraft
const val ATTR_AIRCRAFT_TYPE = "attr_aircraft_type"            // "Boeing 787-9"
const val ATTR_AIRCRAFT_REGISTRATION = "attr_aircraft_registration"  // "JA837J"
const val ATTR_AIRCRAFT_AGE_YEARS = "attr_aircraft_age_years"
const val ATTR_AIRCRAFT_AMENITIES = "attr_aircraft_amenities"  // CSV: wifi, power, ife, usb
```

**External service — primary:**

| Service | Free tier | What it gives | Notes |
|---|---|---|---|
| **Amadeus for Developers** | Self-service free tier: 10,000 transactions/month across most endpoints, 2,000/month for some. No credit card. | Seat maps, airport info, flight delay prediction, aircraft type by flight number | Best single integration for this phase. |
| **AeroDataBox** | 500 req/month (free RapidAPI plan) | Aircraft registration + age, airport metadata | Fallback + complements Amadeus aircraft data. |

**Caching rules:**

- Seat map: cached forever by `{flight_number}|{aircraft_type}` — aircraft
  rarely change for a route/flight-number.
- Baggage allowance: cached for 30 days by `{airline}|{cabin_class}|{route_region}`.
- Aircraft registration + age: cached for 24 hours by flight number + date.

**Content notes:**

- `FlightSeatCard` renders a mini seat map image (Amadeus returns a
  rendered seat map) with the user's seat highlighted. If no user seat
  is set, show a "Select seat" action that opens the airline's seat
  selection URL.
- `FlightBaggageCard` shows luggage icons and allowance per bag type. If
  Amadeus returns fee data, surface a "Extra bag: $X" row.
- `FlightAircraftCard` shows aircraft type + age + amenity icons
  (wifi / power / IFE / USB). Pulls amenities from Amadeus' aircraft
  amenities endpoint or Wikipedia's aircraft page as a fallback scrape.

**Done when:**
- A flight on a known 787-9 shows the type + age + wifi icon.
- A flight the user hasn't selected a seat for: seat card shows "Select
  seat" CTA only, no empty map.
- A flight on an obscure airline with no Amadeus coverage: all three cards
  degrade to absent.

---

## Phase 5 — Destination-aware cards: FlightWeatherCard + ArrivalCityCard

**Goal:** The flight is really about arrival. Give the user a preview of
what's waiting.

**Files touched:**
- `data/trip/remote/WeatherRepository.kt` — shared with Restaurant plan
  Phase 5; extend to support "weather at time T" not just "weather now".
- `data/trip/remote/DestinationMetadataRepository.kt` — NEW.
- `ui/main/current/overlays/cards/FlightWeatherCard.kt` — NEW.
- `ui/main/current/overlays/cards/ArrivalCityCard.kt` — NEW.
- `CurrentTripEventDetailsDialog.kt` — slot after `FlightPricingCard`.

**New ATTR constants:**

```kotlin
// Weather at arrival
const val ATTR_ARRIVAL_WEATHER_TEMP_C = "attr_arrival_weather_temp_c"
const val ATTR_ARRIVAL_WEATHER_CONDITION = "attr_arrival_weather_condition"
const val ATTR_ARRIVAL_WEATHER_SUMMARY = "attr_arrival_weather_summary"

// Destination quick facts
const val ATTR_DEST_TIMEZONE_DELTA = "attr_dest_timezone_delta"    // "+16h"
const val ATTR_DEST_CURRENCY = "attr_dest_currency"                // "JPY"
const val ATTR_DEST_FX_RATE_HOME = "attr_dest_fx_rate_home"        // "1 USD = 150 JPY"
const val ATTR_DEST_PLUG_TYPE = "attr_dest_plug_type"              // "Type A, B"
const val ATTR_DEST_EMERGENCY_NUMBER = "attr_dest_emergency_number"
const val ATTR_DEST_ADVISORY_LEVEL = "attr_dest_advisory_level"    // 1-4
const val ATTR_DEST_ADVISORY_URL = "attr_dest_advisory_url"
```

**External services:**

| Service | Free tier | Purpose |
|---|---|---|
| **Open-Meteo** | Unlimited, no key required for non-commercial | Weather forecast at destination arrival time |
| **REST Countries** | Free, no key | Currency, plug type, emergency numbers, calling code |
| **ExchangeRate.host** / existing `CurrencyApiService` | Already integrated | FX rate (already in app) |
| **US State Dept Travel Advisories** (RSS / scrape) | Free | Advisory level for international destinations |

**Card content — FlightWeatherCard:**

- Header: "At arrival in {city}", title = `"{temp}° — {condition}"`.
- Short forecast line: "Expect light rain landing. Pack an umbrella."

Early-return if arrival is > 10 days out (forecast unreliable) or if Open-Meteo
returned nothing.

**Card content — ArrivalCityCard:**

- Compact grid: Time-zone delta · Currency + FX · Plug type · Emergency #.
- Advisory banner if level ≥ 2, linking to State Dept URL.
- Only renders when the flight crosses an international boundary (`ATTR_ORIGIN_CITY`
  country ≠ `ATTR_DESTINATION_CITY` country). No card for a LAX→JFK domestic
  hop.

**Done when:**
- A LAX→NRT flight 3 days out shows Tokyo weather + "+16h · ¥ · Type A,B"
  grid.
- A LAX→SFO domestic flight: both cards absent.

---

## Phase 6 — BoardingPassCard (user-provided)

**Goal:** Let the user paste or scan a boarding pass — store it as a local
Wallet-style view.

**Files touched:**
- `ui/main/current/editor/CurrentPlanEditorDialog.kt` — add "Boarding pass"
  fields.
- `ui/main/current/overlays/cards/BoardingPassCard.kt` — NEW.
- `CurrentTripEventDetailsDialog.kt` — slot at the top of the flight stack,
  right after `EventSummaryCard`, when a pass exists.

**New ATTR constants:**

```kotlin
const val ATTR_PASS_URL = "attr_pass_url"                  // PKPASS URL
const val ATTR_PASS_CODE_PAYLOAD = "attr_pass_code_payload"  // raw QR / PDF417 text
const val ATTR_CONFIRMATION_NUMBER = "attr_confirmation_number"
const val ATTR_RESERVATION_NAME = "attr_reservation_name"
const val ATTR_CHECKIN_OPENS_AT = "attr_checkin_opens_at"
```

**External service — optional:**

| Service | Free? | Purpose |
|---|---|---|
| **ZXing library** (on-device) | Free, MIT | Render QR / PDF417 codes locally from `ATTR_PASS_CODE_PAYLOAD` |
| Google Wallet Passes API | Free for basic passes, paid at scale | "Add to Google Wallet" action — skip for phase 6a; revisit if users demand |

Ship phase 6 with ZXing only. No external API calls; everything is local.

**Card content:**

- Large rendered QR / PDF417 code.
- Confirmation # + reservation name (copyable).
- "Check-in opens in 4h 20m" countdown → tappable airline check-in link.

Early-return if no pass payload and no confirmation number.

**Done when:**
- Pasting a PNR like "ABC123" into the editor shows a confirmation row on
  the card with a copy button.
- Pasting a PDF417 payload renders a scannable barcode that airport
  scanners accept.

---

## Phase 7 — Shared primitive extraction + regression pass

Same shape as Restaurant Phase 6. After the flight cards land, promote
anything that now appears in 2+ event types into shared primitives.

**Tasks:**

1. Confirm the shared `ProviderOfferRow` extracted in Restaurant Phase 3 is
   used by both hotel + restaurant + flight offer cards.
2. Shared `greatCircleArc(from, to, modifier)` composable if `FlightRouteCard`
   ended up with anything worth reusing.
3. Shared `LoyaltyCard` — currently in flight's tree, but identical needs
   exist for future hotel loyalty. Promote to `overlays/cards/` root-level.
4. Shared `WeatherCard` — restaurant plan also has one. Consolidate into
   a single `WeatherCard(event, anchorTime)` that both types call with
   different anchors.
5. Snapshot-style manual QA: one fully-populated flight + one bare-minimum
   flight, confirm the card list matches BP.md §4.

**Done when:**
- No sub-card source file is copy-pasted between event types.
- BP.md §4 matrix stays accurate for the flight column.

---

## Phase 8 — Telemetry & pruning (optional, post-launch)

Per BP.md §8: once all flight cards ship, instrument impressions +
tap-through per card ID. Prune at the 2-week mark — if nobody taps the
loyalty card or the aircraft card, retire it and free up scroll real estate.

Not in scope for the initial implementation.

---

## Free-Tier Service Summary (all phases)

| Service | Free terms | Used in |
|---|---|---|
| Unsplash API | 50 req/hr → 5,000 req/hr after form approval | Section A hero |
| Wikimedia Commons | Unlimited, no key | Section A hero (fallback) |
| Skyscanner logo CDN | Unlimited, no key | Section A logo overlay |
| Airhex | Free tier for small logos | Section A logo overlay (optional) |
| AviationStack | 100 req/month | Phase 2 status |
| AeroDataBox (RapidAPI) | 500 req/month | Phase 2 status fallback, Phase 4 aircraft |
| OpenSky Network | Anonymous 100/24h, account 1000/24h | Phase 2 stretch (live map) |
| Amadeus Self-Service | 10,000 tx/month on most endpoints, no CC | Phase 4 seat/baggage/aircraft |
| Open-Meteo | Unlimited, no key | Phase 5 weather |
| REST Countries | Unlimited, no key | Phase 5 destination facts |
| US State Dept RSS | Unlimited, no key | Phase 5 advisory |
| ZXing | MIT, on-device | Phase 6 boarding pass rendering |

No paid-only services. No credit-card-required signups. Each service has a
named fallback so a rate-limit or outage doesn't break the card — it just
degrades to absent.

---

## Risk Log

| Risk | Mitigation |
|---|---|
| AviationStack 100/month is tight for heavy users | Hard gate: only query within 24h of departure. AeroDataBox fallback kicks in after cap hit. Cache aggressively. |
| Amadeus free tier limits vary per endpoint; surprise throttling possible | Per-endpoint quota tracking in `AmadeusRepository`. When throttled, cards degrade to absent, not error. |
| Unsplash requires photo-download tracking for production approval | Add the `/photos/{id}/download` ping in `FlightHeroImageRepository` as part of the resolve step — one extra HTTP call per hero fetch, async / fire-and-forget. |
| Hero URL persisted to Firestore could rot (photo takedown on Unsplash) | On 404, re-resolve the chain and update Firestore. Single retry per dialog open, not per recomposition. |
| ZXing PDF417 codes rendered from a malformed payload | Wrap render in `try/catch`; on failure show a "We couldn't render this pass" note + copy-text fallback. |
| State Dept advisory feed format changes | Isolate parsing in a single small adapter; fail quiet (no advisory banner) on parse failure. |
| SerpAPI doesn't always return the destination city or IATA-to-city mapping | Maintain a small IATA → city lookup JSON as a resource file for the top 500 airports. Fallback to showing IATA code if lookup misses. |

---

## Delivery Order Summary

1. **Phase 0** — sign-off + account creation, no code.
2. **Phase 1** — Section A hero fix + existing-card cleanup. Biggest visible
   win, ships on its own.
3. **Phase 2** — FlightStatusCard + FlightLegsCard. First live-data cards.
4. **Phase 3** — FlightBookingOffersCard + LoyaltyCard. Zero new API keys.
5. **Phase 4** — Seat / Baggage / Aircraft. Adds Amadeus.
6. **Phase 5** — FlightWeatherCard + ArrivalCityCard. Destination preview.
7. **Phase 6** — BoardingPassCard (user-provided, on-device rendering).
8. **Phase 7** — primitive extraction + regression pass.
9. **Phase 8** — telemetry (optional, post-launch).

Each phase is independently shippable. Shipping Phase 1 alone resolves the
low-resolution hero complaint and tightens the three existing cards — a
meaningful release on its own.