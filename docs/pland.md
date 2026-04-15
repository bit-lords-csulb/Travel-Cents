# Plan Audit

Updated: 2026-04-13

## Done

- Current Trip is already unified under `ui/main/current/` and split into modular files for `screen`, `header`, `itinerary`, `editor`, `overlays`, and helpers.
- `CurrentTripViewModel` already owns the migrated itinerary logic: trip loading, trip switching, rename/archive/delete, share targets, Yelp reviews, event options, and reorder persistence.
- The Current Trip tap flow is already two-step: open event details first, then edit details or switch to alternatives.
- `MainScaffold` already routes trip generation and home-trip selection into the Current Trip flow.
- `ui/main/home/` already exists, and the currency converter has already been split into `CurrencyConverterCard.kt` and `CurrencyViewModel.kt`.

## Pending

### 1. Legacy Current-Trip Cleanup

- Remove `FINAL_PLAN` and `FINAL_PLAN_BY_ID` from `MainScaffold.kt` once legacy alias routes are no longer needed.
- Delete unused legacy files:
  - `ui/main/itinerary/ItineraryViewModel.kt`
  - `ui/main/itinerary/FinalPlan.kt`
  - `ui/main/itinerary/SharedTripHeader.kt` if nothing else will reuse it
- If any of those files need to stay temporarily, mark them as deprecated and point to the replacement implementation under `ui/main/current/`.

### 2. Home Screen Modularization

- Keep `ui/main/home/HomePage.kt` as a thin coordinator only.
- Extract the remaining inline sections into dedicated files:
  - `HomeHeader.kt`
  - `HomeTripCarousel.kt` or `TripsCarousel.kt`
  - `SavedPlacesWidget.kt`
  - `TripStatusWidget.kt`
  - `DocumentsWidget.kt`
- Remove the old empty stubs in `ui/main/HomePage.kt` and `ui/main/HomeViewModel.kt` once imports are stable.

### 3. Currency Converter Completion

- Keep the current search + scroll picker; that part is already done.
- Replace the hard-coded currency list in `ui/main/home/CurrencyViewModel.kt` with API-driven currency metadata.
- Decide whether the picker should remain a compact dropdown or move to a bottom sheet only after checking the current mobile UX.
- Add pinned currencies / multi-result output only if that feature is still wanted; it is not implemented yet.
- Keep `SharedPreferences` persistence unless there is already a broader `DataStore` migration planned.

## Suggested Edits To The Old Plan

- Do not describe the Current Trip work as a `CurrentPage.kt` rewrite anymore; the real implementation is already modular under `ui/main/current/`.
- Remove tasks about migrating methods from `ItineraryViewModel` into `CurrentTripViewModel`; that migration is already done.
- Remove tasks about adding the event-detail-first flow before edit; that is already done.
- Narrow the currency section so it tracks only the unfinished work: API-driven full list, optional pinned currencies, and UI polish if still needed.
- Treat legacy-file deletion as cleanup, not as core feature work.

## Recommended Order

1. Delete or deprecate the legacy itinerary/final-plan files and alias routes.
2. Finish the remaining Home page extraction.
3. Finish the remaining currency-data work.
4. Do one final dead-code pass on `ui/main/`.
