# Weather And Transport Trip Update Plan

## Objective

Add a proactive trip-update feature that reviews the current itinerary against weather and transportation context, then suggests practical changes when a planned stop becomes inconvenient or risky.

Initial implementation uses deterministic dummy weather and transportation data. The feature should be structured so real providers can replace the dummy sources later without rewriting the scoring, UI, or persistence flow.

For the demo version, advisories should only activate when a demo-mode switch is enabled and the user navigates to the itinerary/current-trip screen. This keeps normal app behavior unchanged and makes demos predictable.

Example target behavior:

- User has an outdoor safari scheduled at 2:00 PM.
- Dummy weather says heavy rain starts around that time.
- App flags the safari as weather-sensitive and suggests an indoor museum, indoor market, aquarium, show, or similar activity for the same time window.
- User can accept the replacement, save it as another option, dismiss it, or leave the plan unchanged.

## Product Principles

- Suggestions are advisory. Do not automatically mutate the trip.
- Keep the existing itinerary and Firestore repository shape intact. This feature should integrate with the existing trip/event/action services, not redesign the database.
- Dummy data must be deterministic so tests and demos are repeatable.
- Demo mode must be opt-in. If the switch is off, no dummy advisories should appear.
- Demo checks should run when the itinerary surface becomes active, not continuously in the background.
- The first version should be explainable with simple rules before adding LLM summarization or live APIs.
- Preserve the user's selected plan unless they explicitly accept a replacement.

## Current Repo Fit

The app already has the right surface area for this feature:

- `TravelEvent.details` stores extensible event metadata.
- `EventOption` already represents alternate choices for an itinerary slot.
- `TripPlanActionService` already supports updating an event, replacing a selected option, and saving an option.
- `CurrentTripViewModel` already merges live detail overrides into visible events.
- `WeatherRepository` and `TransportRepository` already model weather and transport snapshots, even though this feature will begin with dummy providers.
- Current-trip detail cards already render weather and transport data when present.

This plan should use those paths rather than introducing a separate trip storage model.

## Core Data Model

### New Domain Models

Add a small model package, likely under `data/trip/advisory/`.

```kotlin
data class TripAdvisory(
    val advisoryId: String,
    val eventId: String,
    val severity: AdvisorySeverity,
    val reason: AdvisoryReason,
    val title: String,
    val message: String,
    val affectedDate: String,
    val affectedStartTime: String,
    val suggestedOptions: List<EventOption>,
    val generatedAtEpochMs: Long
)

enum class AdvisorySeverity {
    LOW,
    MEDIUM,
    HIGH
}

enum class AdvisoryReason {
    RAIN_OUTDOOR_ACTIVITY,
    EXTREME_HEAT,
    HIGH_WIND,
    TRANSIT_DELAY,
    WALKING_TIME_TOO_LONG,
    RIDESHARE_COST_SPIKE
}
```

For storage, keep Phase 1 advisory state in ViewModel/session memory unless product needs cross-device advisory persistence. Accepted changes should persist through the existing event/option path.

### New Detail Attributes

Add constants in `EventDetailContract.kt` only as needed:

- `ATTR_ACTIVITY_ENVIRONMENT`: `indoor`, `outdoor`, `mixed`, `unknown`
- `ATTR_WEATHER_SENSITIVITY`: `rain`, `heat`, `wind`, `none`
- `ATTR_ENVIRONMENT_CONFIDENCE`: `high`, `medium`, `low`
- `ATTR_ADVISORY_REASON`
- `ATTR_ADVISORY_SEVERITY`
- `ATTR_TRANSPORT_DELAY_MIN`
- `ATTR_TRANSPORT_RELIABILITY`

Do not overload existing weather card fields for advisory state. Weather and transport facts are inputs; advisory fields describe recommendations.

These `ATTR_*` fields are optional metadata inside `TravelEvent.details` or `EventOption.details`. Do not add top-level fields to `TravelEvent` for the demo version.

When Groq or the itinerary-generation pipeline knows the activity environment, it should set the optional metadata:

```kotlin
details = mapOf(
    "title" to "Safari Drive",
    ATTR_ACTIVITY_ENVIRONMENT to "outdoor",
    ATTR_WEATHER_SENSITIVITY to "rain",
    ATTR_ENVIRONMENT_CONFIDENCE to "high"
)
```

Fallback behavior:

- If `ATTR_ACTIVITY_ENVIRONMENT` is present, the advisory engine should trust it.
- If it is missing, demo mode can infer lightly from event type/title keywords such as `safari`, `hike`, `park`, `beach`, `museum`, or `aquarium`.
- If the environment is still unknown, do not generate a weather-based advisory for that event.
- If confidence is `low`, prefer a `LOW` or `MEDIUM` advisory unless the weather/transport issue is severe.

## Dummy Providers

### Dummy Weather Provider

Create an interface first, then a dummy implementation.

```kotlin
interface TripWeatherContextProvider {
    suspend fun weatherFor(event: TravelEvent, trip: Itinerary): WeatherContext?
}

data class WeatherContext(
    val condition: String,
    val precipitationPct: Int,
    val temperatureC: Int,
    val windKph: Int,
    val startsAtLocalTime: String?
)
```

Dummy behavior should be scenario-based:

- Prefer `ATTR_ACTIVITY_ENVIRONMENT` and `ATTR_WEATHER_SENSITIVITY` when present.
- Safari/outdoor activity keywords -> rain at the scheduled time.
- Beach/park/hike keywords -> high wind or high heat.
- Indoor restaurant/hotel/flight -> normal weather.
- Unknown activity -> mild weather unless a test scenario is selected.

This keeps demos reliable without hardcoding the feature to one exact event name.

### Dummy Transport Provider

```kotlin
interface TripTransportContextProvider {
    suspend fun transportFor(event: TravelEvent, previousEvent: TravelEvent?, trip: Itinerary): TransportContext?
}

data class TransportContext(
    val walkMin: Int?,
    val transitMin: Int?,
    val rideshareMin: Int?,
    val delayMin: Int,
    val reliability: String,
    val summary: String
)
```

Dummy behavior:

- If two events are far apart by mock location/category, add a delay.
- If an event starts less than 30 minutes after the previous event and transport takes more than 25 minutes, flag it.
- If rain is active and walk time is high, suggest rideshare or a closer indoor replacement.

## Advisory Engine

Add a deterministic engine that consumes itinerary events plus provider snapshots.

```kotlin
class TripAdvisoryEngine(
    private val weatherProvider: TripWeatherContextProvider,
    private val transportProvider: TripTransportContextProvider,
    private val alternativeProvider: TripAlternativeProvider
) {
    suspend fun evaluate(
        trip: Itinerary,
        events: List<TravelEvent>,
        optionsByEvent: Map<String, List<EventOption>>
    ): List<TripAdvisory>
}
```

### Weather Rules

- If event is outdoor or rain-sensitive and precipitation is at least 60%, create a `RAIN_OUTDOOR_ACTIVITY` advisory.
- If event is outdoor and temperature is at least 34C, create an `EXTREME_HEAT` advisory.
- If event is outdoor and wind is at least 35 kph, create a `HIGH_WIND` advisory.
- Restaurant patio weather should remain a detail-card concern unless the restaurant is explicitly outdoor-only.

### Transport Rules

- If delay is at least 20 minutes and would make arrival late, create a `TRANSIT_DELAY` advisory.
- If walking time is more than 30 minutes during rain or heat, create a `WALKING_TIME_TOO_LONG` advisory.
- If rideshare cost spike data exists later, only mark `HIGH` when there is no reasonable transit/walking fallback.

### Severity

- `HIGH`: event likely cannot work as planned.
- `MEDIUM`: event works but needs a mode/time/activity change.
- `LOW`: informational warning; no immediate replacement needed.

## Alternative Generation

Create `TripAlternativeProvider` with a dummy implementation first.

```kotlin
interface TripAlternativeProvider {
    suspend fun alternativesFor(
        trip: Itinerary,
        event: TravelEvent,
        reason: AdvisoryReason
    ): List<EventOption>
}
```

Dummy alternative examples:

- Outdoor safari rain -> indoor museum, aquarium, covered market, cooking class.
- Beach heat -> shaded garden, indoor food hall, museum, spa.
- Long wet walk -> closer indoor cafe/activity near previous stop.
- Transit delay -> later time slot for same event plus one nearby fallback.

Each dummy option should include realistic `details`:

- `title`
- `description`
- `address`
- `attr_activity_environment`
- `attr_latitude` / `attr_longitude` when useful
- `price_tier` or rough cost
- `source = "dummy_advisory"`

This allows the existing option panel and selected-option merge behavior to do most of the work.

## UI Plan

### Current Trip Surface

Add a compact advisory strip above the itinerary content or inside the affected event card:

- Severity icon/color.
- Short reason: `Rain expected during Safari`.
- Primary action: `Review alternatives`.
- Secondary action: `Dismiss`.

Avoid hiding the original plan. The card should make it clear this is a suggestion, not an applied change.

### Advisory Review Sheet

Use a bottom sheet similar to `EventOptionsPanel`, but advisory-specific:

- Header: reason and weather/transport facts.
- Original plan summary.
- Suggested replacement options.
- Actions:
  - `Replace plan`
  - `Save as option`
  - `Dismiss`

For Phase 1, reuse the existing `EventOption` rendering where possible. If a dedicated sheet is faster, keep it visually aligned with the current option panel.

### Detail Cards

Current `WeatherCard` and `TransportCard` can remain factual cards. Add a new `AdvisoryCard` only when the event has an active advisory or accepted advisory metadata.

## ViewModel Integration

Add advisory state to `CurrentTripViewModel`:

```kotlin
private val _advisories = MutableStateFlow<List<TripAdvisory>>(emptyList())
val advisories: StateFlow<List<TripAdvisory>> = _advisories.asStateFlow()
```

### Demo Mode Triggering

For the current demo scope, add a simple demo-mode gate:

```kotlin
private val _isAdvisoryDemoModeEnabled = MutableStateFlow(false)
val isAdvisoryDemoModeEnabled: StateFlow<Boolean> = _isAdvisoryDemoModeEnabled.asStateFlow()

fun setAdvisoryDemoModeEnabled(enabled: Boolean) {
    _isAdvisoryDemoModeEnabled.value = enabled
    if (!enabled) {
        _advisories.value = emptyList()
    }
}
```

The itinerary screen should call a ViewModel entry point when it becomes visible:

```kotlin
fun onItineraryVisible() {
    if (!_isAdvisoryDemoModeEnabled.value) return
    evaluateDemoAdvisories()
}
```

This keeps the dummy behavior tied to the demo flow:

- Demo switch off: itinerary behaves normally.
- Demo switch on: switching to the itinerary triggers one advisory evaluation.
- Leaving and returning to the itinerary can re-run the deterministic demo evaluation.

Evaluation triggers for demo mode:

- When the user switches to the itinerary/current-trip screen.
- When the demo switch is turned on while already viewing the itinerary.
- Optionally after event drag/reorder or time edit, but only if demo mode is enabled.

Do not run the dummy advisory engine from background workers for the demo version.

Implementation notes:

- Do not persist dummy advisory output.
- Keep the switch local/debug-only unless product wants it visible in normal builds.
- The switch can live in Settings, a debug panel, or a temporary current-trip overflow/menu action.
- When user accepts a replacement, call `TripPlanActionService.replaceSelectedOption(...)` if the replacement is an option for that slot.
- When user chooses `Save as option`, call `TripPlanActionService.saveOption(...)`.
- When user dismisses, store dismissal in ViewModel state keyed by `eventId + reason`, not in Firestore for Phase 1.

## Phased Implementation

### [ ] Phase 1: Advisory Contracts And Dummy Inputs

- Add `TripAdvisory`, `WeatherContext`, `TransportContext`, provider interfaces, and dummy implementations.
- Add activity environment/advisory constants to `EventDetailContract.kt`.
- Add unit tests for dummy weather and transport scenarios.

Exit criteria:

- A safari/outdoor event at the configured dummy rain time produces a high-severity weather context.
- Indoor events do not produce rain advisories.

### [ ] Phase 2: Rule Engine

- Implement `TripAdvisoryEngine`.
- Add deterministic weather and transport rules.
- Add dummy alternatives through `TripAlternativeProvider`.
- Unit test rain, heat, wind, late-arrival, and no-advisory cases.

Exit criteria:

- Outdoor safari + rain returns a `RAIN_OUTDOOR_ACTIVITY` advisory with at least two indoor `EventOption` alternatives.
- Normal indoor dinner returns no advisory.

### [ ] Phase 3: Current Trip State Wiring

- Add advisory state to `CurrentTripViewModel`.
- Add a demo-mode switch/state gate.
- Evaluate advisories when the itinerary/current-trip screen becomes visible and demo mode is enabled.
- Add dismiss handling.
- Keep all advisory state local for Phase 1.

Exit criteria:

- With demo mode off, opening the itinerary shows no dummy advisories.
- With demo mode on, switching to the itinerary shows advisory state in ViewModel without persisting new Firestore documents.
- Reordering or editing the affected event can re-run advisory evaluation only while demo mode is enabled.

### [ ] Phase 4: UI

- Add an advisory strip/card to the current trip timeline/day view.
- Add advisory review sheet with original event, reason, and replacement options.
- Wire `Replace plan`, `Save as option`, and `Dismiss`.

Exit criteria:

- User can review rain-driven safari alternatives.
- Accepting a replacement updates the itinerary through the existing action service.
- Dismissing hides that advisory for the session.

### [ ] Phase 5: Demo Scenario And QA

- Add a small hardcoded/demo itinerary or test fixture that includes:
  - Outdoor safari during rain.
  - Indoor museum unaffected by rain.
  - Two events with an impossible transport gap.
- Add a demo-mode switch and confirm advisories are only generated when the itinerary screen is activated.
- Add focused unit tests and, if feasible, a Compose preview for the advisory sheet.

Exit criteria:

- Demo mode off produces no dummy advisories.
- Demo mode on plus itinerary navigation reliably shows the safari-to-indoor-activity suggestion.
- Demo reliably shows the safari-to-indoor-activity suggestion.
- Demo reliably shows one transport-delay suggestion.
- No advisory appears for unaffected events.

### [ ] Phase 6: Live Provider Swap Later

After dummy behavior is accepted:

- Replace dummy weather with `WeatherRepository` or a provider wrapper around it.
- Replace dummy transport with `TransportRepository` or a provider wrapper around it.
- Keep `TripAdvisoryEngine` unchanged except for thresholds if product tuning requires it.
- Consider using Yelp/LLM/vector retrieval for richer alternatives after the deterministic baseline works.

## Test Plan

Unit tests:

- `DummyTripWeatherContextProviderTest`
- `DummyTripTransportContextProviderTest`
- `TripAdvisoryEngineTest`
- `TripAlternativeProviderTest`
- `CurrentTripViewModel` advisory state tests if existing ViewModel test setup supports it.

Manual demo:

1. Load a trip with an outdoor safari activity.
2. Set dummy scenario to rain during that time.
3. Confirm advisory appears.
4. Open advisory review sheet.
5. Replace safari with an indoor option.
6. Confirm event updates through existing itinerary UI.
7. Dismiss another advisory and confirm it stays hidden until reload.

Regression checks:

- Existing weather and transport detail cards still render factual data.
- Existing option replacement still works.
- Existing Firestore sync does not receive dummy advisory documents.
- Existing current-trip load remains stable offline.

## Open Decisions

- Whether advisories should be session-only or persisted cross-device after Phase 1.
- Whether accepted advisory metadata should be kept on the event details for audit/history.
- Whether the demo-mode switch should live in Settings, a debug panel, or the current-trip screen overflow.
- Whether the first UI should be event-level only or include a trip-wide "Needs attention" summary.
