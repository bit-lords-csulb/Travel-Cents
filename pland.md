# Unified Trip Screen — Integration Plan

## Goal

Merge `FinalPlan.kt` and `CurrentPage.kt` into a single unified screen that lives on the **Current** bottom-nav tab. The unified screen combines:

- FinalPlan's richer timeline card layout, trip rename, trip switcher, share/archive/delete trip, event options panel, expanded event details, and drag-to-reorder
- CurrentPage's trash-can-per-card delete, Week/Day view tabs, and `PlanEditorDialog` (used as the edit UI for all card clicks)
- Enhanced `PlanEditorDialog` that surfaces type-specific detail fields (airline, hotel name, cuisine, etc.) from FinalPlan's event model

The `final_plan` / `final_plan/{tripId}` routes will navigate to `current` instead of rendering the separate FinalPlan screen.

---

## Feature Comparison (Before → After)

| Feature | FinalPlan | CurrentPage | Unified (target) |
|---|---|---|---|
| Itinerary (list) view | ✅ TimelineEventCard | ✅ ListPlanCard | ✅ TimelineEventCard |
| Week view | ❌ | ✅ | ✅ |
| Day view | ❌ | ✅ | ✅ |
| Trash can per card | ❌ (delete via expanded card) | ✅ | ✅ on all cards/modes |
| Edit via PlanEditorDialog | ❌ (inline patch only) | ✅ | ✅ with type-specific fields |
| Type-specific detail fields in editor | ❌ | ❌ | ✅ (new) |
| Trip rename | ✅ tap title | ❌ | ✅ |
| Trip switcher | ✅ dropdown | ❌ | ✅ |
| Archive / Delete trip | ✅ | ❌ | ✅ |
| Share trip to chats | ✅ | ❌ | ✅ |
| Event options / alternatives | ✅ | ❌ | ✅ |
| Yelp reviews in expanded card | ✅ | ❌ | ✅ (via editor) |
| Drag-to-reorder (jiggle mode) | ✅ | ❌ | ✅ |
| Add new plan button | ❌ | ✅ | ✅ |
| Image thumbnails on cards | ✅ | ❌ | ✅ |

---

## Files to Change

### 1. `CurrentTripViewModel.kt` — Add ItineraryViewModel capabilities

`ItineraryViewModel` already imports `CurrentTripUiState` from this file, so they share the state model. The cleanest path is to **move all ItineraryViewModel methods into CurrentTripViewModel** and then delete `ItineraryViewModel`.

**Add StateFlows:**
```kotlin
// Already in ItineraryViewModel, missing from CurrentTripViewModel:
private val _allTrips = MutableStateFlow<List<Itinerary>>(emptyList())
val allTrips: StateFlow<List<Itinerary>> = _allTrips.asStateFlow()

private val _eventOptions = MutableStateFlow<Map<String, List<EventOption>>>(emptyMap())
val eventOptions: StateFlow<Map<String, List<EventOption>>> = _eventOptions.asStateFlow()

private val _rejectedOptions = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
val rejectedOptions: StateFlow<Map<String, Set<String>>> = _rejectedOptions.asStateFlow()

private val _yelpReviews = MutableStateFlow<Map<String, List<YelpReview>>>(emptyMap())
val yelpReviews: StateFlow<Map<String, List<YelpReview>>> = _yelpReviews.asStateFlow()

private val _reviewsLoading = MutableStateFlow<Set<String>>(emptySet())
val reviewsLoading: StateFlow<Set<String>> = _reviewsLoading.asStateFlow()

private val _shareTargets = MutableStateFlow<List<ShareTarget>>(emptyList())
val shareTargets: StateFlow<List<ShareTarget>> = _shareTargets.asStateFlow()
```

**Add / migrate methods from `ItineraryViewModel`:**
- `fun loadTrip(tripId: String? = null)` — already exists in `CurrentTripViewModel` with different signature; reconcile so it accepts an optional tripId (load latest when null, load specific when provided)
- `fun loadAllTrips()` — fetch all trips ordered by `createdAt DESC` for the switcher dropdown
- `fun renameTrip(newName: String)` — update `tripName` field in Firestore + update `uiState.tripTitle`
- `fun archiveTrip(tripId: String)` — set `status = "archived"` in Firestore + reload latest non-archived trip
- `fun deleteTrip(tripId: String)` — delete trip doc + all events subcollection + reload
- `fun moveEventLocally(eventId, fromDate, toDate, toIndex)` — reorder in memory only (no Firestore write)
- `fun persistEventPlacements(affectedDates: Set<String>)` — batch-write `sortOrder` + `date` for affected events to Firestore
- `fun fetchYelpReviews(yelpId: String)` — call `YelpRepository`, store in `_yelpReviews`
- `fun fetchShareTargets()` — load user's friends + groups from Firestore into `_shareTargets`
- `fun shareTripToChat(target: ShareTarget)` — post trip summary message to the target chat/group
- `fun selectOption(eventId, optionId)` — mark option as selected in Firestore
- `fun rejectOption(eventId, optionId)` — mark option as rejected locally
- `fun patchEventFields(eventId, title, time, notes)` — lightweight field update (keep this for the editor's save path)

**Update `listenToEvents()`:**
- Copy the ItineraryViewModel version that also reads `imageUrl` and `photoUrls` from Firestore (CurrentTripViewModel's version skips these, causing missing thumbnails on FinalPlan-generated events)
- After building `sortedEvents`, trigger `loadOptionsForEvents()` to populate `_eventOptions` (this pipeline already exists in ItineraryViewModel)

**Update `resetTripState()`:**
- Also clear `_allTrips`, `_eventOptions`, `_rejectedOptions`, `_yelpReviews`, `_reviewsLoading`, `_shareTargets`

**Move `ShareTarget` data class here** (currently declared in ItineraryViewModel's file).

---

### 2. `CurrentPage.kt` — Unified screen (largest change)

#### 2a. New state variables to add at the top of `CurrentPage()`
```kotlin
val allTrips by viewModel.allTrips.collectAsState()
val eventOptions by viewModel.eventOptions.collectAsState()
val rejectedOptions by viewModel.rejectedOptions.collectAsState()
val yelpReviews by viewModel.yelpReviews.collectAsState()
val reviewsLoading by viewModel.reviewsLoading.collectAsState()
val shareTargets by viewModel.shareTargets.collectAsState()

var jiggleMode by remember { mutableStateOf(false) }
var optionsPanelEventId by remember { mutableStateOf<String?>(null) }
var showShareSheet by remember { mutableStateOf(false) }
var shareConfirmation by remember { mutableStateOf<String?>(null) }
```

Add `LaunchedEffect(Unit)` to call `viewModel.loadAllTrips()` on mount.

#### 2b. Replace `SharedTripHeader` usage with new `UnifiedTripHeader` (new private composable inside CurrentPage.kt)

`SharedTripHeader` is too simple — it only shows a static title. The unified header needs:
- Inline-editable title (tap → `BasicTextField`, same pattern as `FinalPlanTopBar`)
- Trip switcher arrow button → dropdown of all trips (same as `FinalPlanTopBar`'s `switcherExpanded`)
- `+` plan button (keep from current `SharedTripHeader`)
- `⋮` overflow menu: Share Trip, Reorder Events (toggle jiggle mode), Archive Trip, Delete Trip
- `LaunchedEffect(uiState.tripTitle)` to sync editable title when VM switches trips

Extract this as `private fun UnifiedTripHeader(...)` inside `CurrentPage.kt`, replacing the `SharedTripHeader` call in the main `CurrentPage` composable. `SharedTripHeader.kt` itself does not need to change (it is still used by `ItineraryScreen.kt` which is the old Current tab screen — though that screen will be retired too, see §5).

#### 2c. Replace `ItineraryContent` / `ListPlanCard` with FinalPlan's `TimelineEventCard`

The `ItineraryContent` composable currently renders `ListPlanCard`. Replace with the `TimelineEventCard`-based timeline:

- Copy `buildPlanItems()` / `FinalPlanItem` sealed interface into `CurrentPage.kt` (or into a new shared utility file if preferred)
- Copy `TimelineEventCard` from `FinalPlan.kt` into `CurrentPage.kt` (as a private composable)
- **Add a trash-can `IconButton`** inside `TimelineEventCard` (top-right corner, same `DeleteOutline` icon and `Color(0xFFE77D90)` tint already used in week/day event rows). On click → set `deleteCandidate`.
- Copy `primaryEventTitle()` and `secondaryEventText()` helper functions (already richer than CurrentPage's `eventTitle()` and `eventSubtitle()`)
- Copy jiggle/reorder wiring (`rememberReorderableLazyListState`, wobble animation, `ReorderableItem`) into `ItineraryContent` / the main `LazyColumn`
- The mode tab that previously triggered `onViewItineraryRequested` (navigating to FinalPlan) is removed — Itinerary mode is now handled inline

#### 2d. Enhance `PlanEditorDialog` with type-specific detail fields

The existing `PlanEditorDialog` edits: type, title, date, startTime, endTime, timezone, location, notes, colorKey. Extend it to show type-specific fields that are read from `plan.existingDetails` and written back into `existingDetails` on save:

- **flight**: airline, flight_number, origin_airport, destination_airport, total_price, trip_segment (outbound/return)
- **hotel**: hotel_name, address, rating, check_in, check_out
- **restaurant / dining**: restaurant_name, cuisine
- **activity**: activity_name (maps to the title field already)

These fields are shown conditionally based on `plan.type`. They pre-populate from `plan.existingDetails` and are merged back in on save via the existing `existingDetails` map merge in `upsertPlan()`. No new data model changes needed.

Add an **"Alternatives" button** in the editor footer (only visible when `eventOptions[plan.eventId].orEmpty().size > 1`):
```
[ Alternatives ]  [ Delete ]  [ Save ]
```
Clicking Alternatives: dismiss the editor, set `optionsPanelEventId = plan.eventId`.

#### 2e. Wire card clicks in all three modes

All three modes (Itinerary, Week, Day) already have an `onEventClick` callback that sets `editorPlan`. No change needed there. The `PlanEditorDialog` is the unified edit UI for all modes.

For Yelp reviews: add a `LaunchedEffect(editorPlan)` that fetches reviews when the editor opens for an event with a `yelp_id` in its details:
```kotlin
LaunchedEffect(editorPlan) {
    editorPlan?.existingDetails?.get("yelp_id")?.let { yelpId ->
        viewModel.fetchYelpReviews(yelpId)
    }
}
```
Show reviews inside the `PlanEditorDialog` as a read-only collapsible section at the bottom.

#### 2f. Add overlay composables (at the bottom of `CurrentPage()`, same pattern as FinalPlan)

```kotlin
// Event options / alternatives panel
optionsPanelEventId?.let { eid -> EventOptionsPanel(...) }

// Share bottom sheet
if (showShareSheet) { ShareTripSheet(...) }

// Share confirmation snackbar
shareConfirmation?.let { msg -> ... }
```

Copy `ShareTripSheet` from `FinalPlan.kt` into `CurrentPage.kt` (private composable).

---

### 3. `MainScaffold.kt` — Consolidate ViewModels and routes

#### 3a. Remove separate `finalPlanViewModel`

```kotlin
// Remove:
val finalPlanViewModel: ItineraryViewModel = viewModel()

// currentTripViewModel now handles everything
```

Update `selectedBottomRoute` mapping to remove `FINAL_PLAN` / `FINAL_PLAN_BY_ID` from the `NEW_TRIP` group — these routes will be removed.

#### 3b. Update `final_plan` composable routes

Both `MainRoutes.FINAL_PLAN` and `MainRoutes.FINAL_PLAN_BY_ID` should navigate to `current` instead of rendering `FinalPlanPage`:

```kotlin
composable(MainRoutes.FINAL_PLAN) {
    LaunchedEffect(Unit) {
        currentTripViewModel.loadTrip()
        navController.navigate(MainRoutes.CURRENT) {
            popUpTo(MainRoutes.HOME) { inclusive = false }
        }
    }
}

composable(
    route = MainRoutes.FINAL_PLAN_BY_ID,
    arguments = listOf(navArgument("tripId") { type = NavType.StringType })
) { backStackEntry ->
    val tripId = backStackEntry.arguments?.getString("tripId")
    LaunchedEffect(tripId) {
        currentTripViewModel.loadTrip(tripId)
        navController.navigate(MainRoutes.CURRENT) {
            popUpTo(MainRoutes.HOME) { inclusive = false }
        }
    }
}
```

Alternatively, remove `FINAL_PLAN` and `FINAL_PLAN_BY_ID` from `MainRoutes` and update all `navController.navigate(MainRoutes.FINAL_PLAN)` call sites to navigate to `CURRENT` directly (cleaner, fewer routes).

#### 3c. Update `CURRENT` composable

```kotlin
composable(MainRoutes.CURRENT) {
    LaunchedEffect(Unit) { currentTripViewModel.loadTrip() }
    CurrentPage(
        viewModel = currentTripViewModel,
        onEditEventClick = { clickedEventId ->
            itineraryUiState.currentTripId?.let { tripId ->
                navController.navigate("edit_plan/$tripId/$clickedEventId")
            }
        },
        onAddEventClick = {
            itineraryUiState.currentTripId?.let { tripId ->
                navController.navigate("edit_plan/$tripId/new")
            }
        }
    )
}
```

Note: `onEditEventClick` / `onAddEventClick` are currently used by `ItineraryScreen`. The unified `CurrentPage` uses the inline `PlanEditorDialog` instead of navigating to `EditPlanScreen`, so these callbacks can be removed from `CurrentPage`'s signature. The `EDIT_PLAN` route and `EditPlanScreen` remain available for other use cases.

#### 3d. Remove `ItineraryScreen` composable from CURRENT route

`ItineraryScreen.kt` was the previous UI for the Current tab. After this change it is no longer wired to the `current` route. It can be left in place for now and deleted in a follow-up cleanup.

---

### 4. `ItineraryViewModel.kt` — Delete or deprecate

After all methods are migrated into `CurrentTripViewModel`, this file is no longer needed. The `FinalPlanPage` composable will also be retired (see §5), removing the only consumer.

**Action**: Delete `ItineraryViewModel.kt` after verifying no remaining imports.

---

### 5. `FinalPlan.kt` — Delete

`FinalPlanPage` is replaced by the unified `CurrentPage`. Once `MainScaffold.kt` no longer references `FinalPlanPage`, this file can be deleted.

**Private helpers to migrate before deleting:**
- `primaryEventTitle()` → move into `CurrentPage.kt` or a new `EventDisplayUtils.kt`
- `secondaryEventText()` → same
- `eventTypeColor()` → same (CurrentPage has `eventPalette()` which is similar but different; reconcile)
- `TimelineEventCard` → move into `CurrentPage.kt`
- `buildPlanItems()` / `FinalPlanItem` → move into `CurrentPage.kt`
- `ShareTripSheet` → move into `CurrentPage.kt`
- `formatDateHeader()` → move into `CurrentPage.kt` or `CalendarDateTimeUtils.kt`

---

### 6. `SharedTripHeader.kt` — No change required

`SharedTripHeader` is used only for the old `ItineraryScreen` (and previously CurrentPage). After the unified screen is in place, it can be deleted in a follow-up cleanup along with `ItineraryScreen.kt`. Do not modify it as part of this change.

---

### 7. `ItineraryScreen.kt` — No change required (cleanup later)

This was the previous "Current" tab UI (a simpler version). It is no longer wired into navigation after this change. Leave it alone for now; delete in a separate cleanup PR after verifying nothing references it.

---

## Navigation Call Sites to Update

| Location | Current target | New target |
|---|---|---|
| `TripGeneratingPage` → `onTripReady` | `FINAL_PLAN` | `CURRENT` |
| `NewTripLandingPage` → `onViewLastTripClick` | `FINAL_PLAN` | `CURRENT` |
| `HomePage` → `onTripClick` | `final_plan/{tripId}` | `CURRENT` (with pre-selected trip via VM) |
| `MainScaffold` `selectedBottomRoute` mapping | `FINAL_PLAN`, `FINAL_PLAN_BY_ID` → NEW_TRIP | Remove from mapping |

---

## Order of Implementation

1. **`CurrentTripViewModel.kt`** — migrate all ItineraryViewModel methods and StateFlows. Verify build compiles.
2. **`CurrentPage.kt`** — implement `UnifiedTripHeader`, replace `ItineraryContent` with timeline cards + trash cans, enhance `PlanEditorDialog`, add overlay composables (options panel, share sheet).
3. **`MainScaffold.kt`** — remove `finalPlanViewModel`, update routes, update call sites.
4. **Delete `ItineraryViewModel.kt`** — verify no remaining imports.
5. **Delete `FinalPlan.kt`** — verify no remaining imports.
6. **(Later)** Delete `ItineraryScreen.kt` and `SharedTripHeader.kt` once confirmed unused.

---

## Risk Notes

- **`EditablePlan.existingDetails`**: The enhanced editor writes type-specific fields back into `existingDetails`. Ensure `upsertPlan()` merges these correctly so AI-generated fields (airline, hotel_name, etc.) are not overwritten with blanks. The current `upsertPlan()` already does `plan.existingDetails.toMutableMap()` and applies overrides on top — this is correct, but verify that fields not shown in the editor (e.g., `yelp_id`, `imageUrl`) are preserved.
- **`listenToEvents()` divergence**: CurrentTripViewModel's version omits `imageUrl`/`photoUrls` and the options-loading pipeline. The ItineraryViewModel version includes both. Use the ItineraryViewModel version verbatim to avoid missing thumbnails.
- **Two ViewModels in MainScaffold**: Currently `currentTripViewModel` and `finalPlanViewModel` are separate instances. After the merge, a single `currentTripViewModel` handles both tabs. Verify that navigating from the Current tab to the New Trip wizard and back does not reset the loaded trip (it should not since the VM is scoped to `MainScaffold`).
- **jiggle mode + PlanEditor conflict**: When jiggle mode is active, card clicks should be suppressed (handled by `enabled = !jiggleMode` on the card's `clickable`). The bottom bar "Done" button exits jiggle mode. These behaviors carry over from FinalPlan unchanged.

---

## Additional Requirements

---

### A. Home Page Modularization

**Goal**: Split `HomePage.kt` into smaller, self-contained composable modules — mirroring how Current Trip was decomposed into route-based pages.

Each card section on the home page should be extracted into its own composable module that:
- Can be imported and reused independently in other screens (e.g., a `UpcomingTripsCard` appearing both on Home and in a future dashboard)
- Exposes a clean parameter surface (data + callbacks only, no ViewModel dependency)
- Lives in its own file under `ui/main/home/` (or `ui/main/components/home/`)

**Candidate modules to extract:**

| Section | Suggested file |
|---|---|
| Active / upcoming trip card | `HomeActiveTrip.kt` |
| Recent trips list row | `HomeRecentTripRow.kt` |
| Quick-action buttons row | `HomeQuickActions.kt` |
| Budget summary card | `HomeBudgetSummary.kt` |
| Currency converter card | `HomeCurrencyConverter.kt` |

`HomePage.kt` becomes a thin coordinator that assembles these modules and passes data down from `HomeViewModel`.

**Order of implementation:**
1. Create `ui/main/home/` package.
2. Extract each section composable into its own file.
3. Replace inline content in `HomePage.kt` with the new imports.
4. Verify each module compiles and renders identically.

---

### B. Currency Converter — Full Currency Search and Scroll

**Problem**: The converter currently hard-codes or defaults to USD → EUR and only displays 3–4 currency pairs with no way to browse or search the full list.

**Required changes:**

1. **Expose the full currency list** — load all available currency codes (and their display names/flags if available) from the exchange rate API response into a `StateFlow<List<CurrencyEntry>>` in the ViewModel.

2. **Searchable currency picker** — replace the current static dropdown with a modal bottom sheet (or full-screen dialog) that contains:
   - A `TextField` for filtering by code or country name
   - A `LazyColumn` showing the filtered list with scroll support
   - Each row: currency code (bold) + full name (secondary text)
   - Tapping a row selects it as the From or To currency and dismisses the sheet

3. **Two separate picker triggers** — the From and To currency fields each open the same picker sheet but write to their respective StateFlow slot.

4. **Persist last-used pair** — save the selected From/To pair to `SharedPreferences` (or `DataStore`) so it survives app restarts without defaulting back to USD/EUR.

5. **Display all conversion results** — the output area should show the converted amount for the selected pair prominently, with an optional expandable section listing conversions to a user-configurable set of "pinned" currencies.

**Files affected:**
- `HomeCurrencyConverter.kt` (new module from §A above, or the existing inline section)
- `HomeViewModel.kt` (or a new `CurrencyViewModel.kt` if the logic warrants isolation)
- The exchange rate data source / repository layer

---

### C. Current Trip — Event Card Detail View (Two-Step Edit Flow)

**Problem**: Tapping an event card in the Current Trip screen immediately opens `PlanEditorDialog` (the edit form). This is too aggressive — the user may just want to view the event details, not edit them.

**Required changes:**

**Step 1 — Default tap action: Enlarged Event Detail View**

Replace the direct-to-editor navigation with a new `EventDetailSheet` (modal bottom sheet or full-screen overlay) that shows:

- Large hero image (if `imageUrl` present)
- Event type chip + color indicator
- Full event title (large text)
- Date, time range, timezone
- Location / address
- Notes / description
- Type-specific details (airline + flight number, hotel name + rating, cuisine, etc.)
- Yelp star rating + review count (if `yelp_id` present) — lazy-loaded the same way reviews are
- Two primary action buttons at the bottom:
  - **"Switch Event"** — opens the existing `EventOptionsPanel` (alternatives picker) to swap the entire event for a different provider option
  - **"Edit Details"** — opens `PlanEditorDialog` (the existing edit form) pre-populated with this event

**Step 2 — Edit Details: existing `PlanEditorDialog`**

No structural change to `PlanEditorDialog` itself — it is simply no longer the first thing the user sees on a card tap.

**Step 3 — Switch Event: existing `EventOptionsPanel`**

No structural change to `EventOptionsPanel` — it is surfaced from the detail view instead of from inside the editor footer.

**Implementation notes:**
- Add `var detailSheetEvent by remember { mutableStateOf<TravelEvent?>(null) }` to `CurrentPage`.
- Card `onClick` sets `detailSheetEvent = event` (instead of `editorPlan = ...`).
- `EventDetailSheet` receives the event, `eventOptions`, `yelpReviews`, `reviewsLoading`, and two callbacks: `onEditClick` and `onSwitchClick`.
- `onEditClick` → set `editorPlan` from the event (existing path), dismiss the sheet.
- `onSwitchClick` → set `optionsPanelEventId`, dismiss the sheet.
- The sheet can be a `ModalBottomSheet` or a slide-up `AnimatedVisibility` overlay depending on preference.
- Apply to all three view modes (Itinerary, Week, Day) — all three currently route card taps through `onEventClick`, so changing the handler in one place covers all modes.

**Files affected:**
- `CurrentPage.kt` — add `EventDetailSheet` composable, update card click handlers
- `CurrentTripViewModel.kt` — no new methods needed; `fetchYelpReviews` already exists