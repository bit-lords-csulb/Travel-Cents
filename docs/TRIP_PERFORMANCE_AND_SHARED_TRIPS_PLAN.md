# Trip Performance and Shared Trips Plan

Updated: 2026-04-16

## Goals

- Reduce Firestore reads, duplicate listeners, and redundant Yelp/media work in current-trip flows.
- Make trip loading predictable on cold start, tab switches, and trip switches.
- Add a single trip data access path that can later support owner trips and shared trips without a second rewrite.
- Stage high-risk schema and migration work behind low-risk wins.

## Findings And Corrections

- `ItineraryViewModel` is duplicate source, but current navigation already passes one shared `CurrentTripViewModel` through the current-trip routes. Removing the duplicate is still required to stop drift and future accidental double-loading.
- The proposed `collectionGroup("options").whereEqualTo("itineraryId", tripId)` query is not immediately possible because option documents do not currently persist `itineraryId` or `ownerUid`.
- Android Firestore already provides offline disk persistence today, but the app does not make cache behavior explicit and still reissues queries on startup. We should treat offline behavior as a first-class feature and verify it with tests and instrumentation.
- Current trip data is owner-scoped under `/users/{uid}/trips/{tripId}`. Shared trip access will require both client-side path abstraction and Firestore rules. Long term, a top-level canonical trip model is cleaner.

## Target End State

### Read Path

- All UI reads trip data through one `TripRepository`.
- The repository loads trips through a `TripKey(ownerUid, tripId)` in the short term, then through a canonical `tripId` once shared-trip migration is complete.
- Home, Current Trip, chat trip cards, and future shared-trip screens reuse the same trip summary and event streams.
- Normal itinerary load reads only:
  - trip summary/doc
  - event list
  - selected-option state already merged onto event docs
  - member list when needed
- Full option lists are loaded only when the user opens alternatives, or by one bulk query if product requires eager loading.

### Local Caching Strategy

- Keep Firestore offline persistence enabled and explicitly configured at app startup.
- Treat Firestore cache as the raw offline layer for canonical remote data.
- Add an app-owned local read model only for startup-critical views if metrics show Firestore cache alone is not enough:
  - trip summaries
  - last opened trip
  - event list
  - selected option summary per event
- Attach real-time listeners only while the relevant screen is visible.

### Shared Trip Direction

Short term:

- Keep storage under `/users/{ownerUid}/trips/{tripId}`.
- Introduce `TripKey(ownerUid, tripId)` in app code now.
- Add `ownerUid`, `tripId`, and `eventId` to option docs so queries and future sharing logic have stable keys.

Long term:

- Move canonical shared trips to `/trips/{tripId}`.
- Trip doc contains:
  - `ownerUid`
  - `memberUids`
  - `roleByUid`
  - `status`
  - summary fields needed by Home and chat cards
- Events live under `/trips/{tripId}/events/{eventId}`.
- Optional user index docs live under `/users/{uid}/tripRefs/{tripId}` for fast Home queries.

## Phase 0: Baseline And Guardrails

### Scope

- Document current query paths and listener lifecycles for:
  - app startup
  - Home load
  - Current Trip open
  - trip switch
  - delete trip
  - share trip
- Add lightweight instrumentation for:
  - trip query count
  - event query count
  - option query count
  - Yelp enrichment attempts
  - cold start time to first current-trip render
- Write an ADR for shared-trip direction:
  - keep owner-scoped paths temporarily
  - define migration path to top-level trips
  - define roles: owner, editor, viewer

### Deliverables

- Metrics dashboard or log tags for current-trip load path.
- Shared-trip ADR.
- Repository interface sketch for `TripRepository` and `TripKey`.

### Exit Criteria

- We can compare before/after Firestore query counts for each later phase.
- The team agrees on short-term versus long-term shared-trip storage direction.

## Phase 1: Quick Wins And Dead Code Removal

### Primary Issues Addressed

- Issue 1: Duplicate ViewModels
- Issue 3: `fetchLatestItinerary` reads all trips
- Issue 5: Yelp prefetch runs on every snapshot
- Issue 6: offline behavior is not explicitly configured or verified
- Issue 7: `shareTripToChat` reads sender name from Firestore

### Work

- Delete `ItineraryViewModel`.
- Remove duplicate `ShareTarget` and any duplicated trip-loading code paths left behind.
- Keep `CurrentTripViewModel` as the single source of truth until the repository layer lands.
- Fix latest-trip loading:
  - filter out archived trips in query
  - apply `limit(1)`
  - stop downloading the full trips collection just to pick the first active trip
- Pre-filter Yelp prefetch to only Yelp-backed event types before calling enrichment.
- Replace `fetchUserNames(listOf(uid))` in trip sharing with local auth/profile data.
- Add an `Application` class and make Firestore cache behavior explicit and tested.
- Remove adjacent duplicate startup reads that are not required for the visible screen:
  - avoid eager current-trip loading before the user enters the trip surface unless product requires it
  - stop duplicate "all trips" fetches where one shared summary source is enough

### Deliverables

- One current-trip ViewModel path in the codebase.
- Faster latest-trip load.
- Lower Yelp churn on snapshot updates.
- Explicit Firestore startup configuration.

### Exit Criteria

- No live code path references `ItineraryViewModel`.
- Latest active trip load uses one bounded query.
- Yelp enrichment attempts drop materially on non-restaurant and non-activity snapshots.
- Share-to-chat no longer performs a Firestore read for the sender's own name.

## Phase 2: Single Trip Data Layer And Cache-Aware Loading

### Primary Outcome

- Stop spreading trip reads across multiple screens and ViewModels.
- Prepare the app to load trips owned by another user or by a shared trip container.

### Work

- Introduce `TripRepository`.
- Introduce `TripKey(ownerUid, tripId)`.
- Move direct Firestore access out of `CurrentTripViewModel` for:
  - trip doc load
  - event listener
  - trip summaries
  - trip members
  - option loading
- Introduce one shared trip summary stream used by Home and Current Trip.
- Add cache-aware loading rules:
  - load cached/local summary first
  - attach listener only when the trip screen is visible
  - detach listeners when leaving the trip surface
- If startup metrics still lag after Firestore cache cleanup, add a small Room read model for:
  - trip summaries
  - current trip snapshot
  - event list
  - selected option snapshot

### Shared-Trip Preparation

- All trip navigation and deep links must pass `TripKey`, not just `tripId`.
- Chat trip cards already carry `ownerUid` and `sharedTripId`; the repository layer should make those values usable.

### Exit Criteria

- Home and Current Trip do not each invent their own trip-loading logic.
- Any trip surface can open a trip by `(ownerUid, tripId)` without assuming the current user is the owner.
- Listener lifecycle is tied to screen visibility, not app startup.

## Phase 3: Option Schema And N+1 Read Elimination

### Primary Issue Addressed

- Issue 2: N+1 Firestore reads for options

### Work

- Extend option documents to persist:
  - `ownerUid`
  - `tripId`
  - `eventId`
- Update every writer that creates or updates option docs:
  - trip generation
  - option selection
  - Yelp enrichment persistence
  - any future trip-share copy or migration path
- Backfill legacy option docs:
  - preferred: admin migration script
  - fallback: lazy backfill when a trip is loaded and missing fields are detected
- Replace per-event option fanout with one bulk load path.

### Recommended Read Strategy

Default:

- Keep selected option data denormalized onto the event doc.
- Normal itinerary load should not need the full options list.

When alternatives are opened:

- Either run one bulk `collectionGroup("options")` query filtered by `ownerUid` and `tripId`, then group by `eventId`.
- Or run a scoped event-options query only for the selected event if the product only needs one panel at a time.

### Why This Phase Matters For Shared Trips

- Shared trips need stable keys on options regardless of who owns the trip.
- Bulk option queries become much easier once options are no longer anonymous subcollection records.

### Exit Criteria

- Snapshot updates for a 20-event trip no longer issue 20 option queries.
- Normal itinerary rendering does not depend on eager full-option reads.
- Legacy trips continue to work during migration.

## Phase 4: Mutation Efficiency And Delete Path Cleanup

### Primary Issue Addressed

- Issue 4: `deleteTrip` performs sequential network operations

### Work

- Replace serial delete loops with batched deletes.
- Chunk large batches to stay within Firestore write limits.
- If trip deletion remains complex, move recursive delete into a callable backend path.
- Clean up local media cache separately from remote document deletion so remote deletes are not blocked by file work.
- Define delete and archive rules for shared trips:
  - owner can delete canonical trip
  - editors can mutate events only if rules allow
  - viewers cannot mutate or delete

### Exit Criteria

- Trip deletion scales by batch count, not event-by-event round trips.
- Shared-trip ownership semantics are explicit before collaborative deletes ship.

## Phase 5: Shared Trip Read Access

### Primary Outcome

- Users can open another user's shared trip safely and efficiently.

### Work

- Add shared membership fields on trip docs.
- Add Firestore rules:
  - owners can read and write
  - editors can read and write allowed fields
  - viewers can read only
- Wire chat trip-card clicks to open the trip using repository-backed trip access.
- Rework trip-member loading to use authoritative membership on the trip or trip group, not ad hoc queries.
- Ensure Home and Current Trip can distinguish:
  - my trips
  - shared with me
  - archived

### Short-Term Version

- Support shared reads against `/users/{ownerUid}/trips/{tripId}` through `TripKey`.

### Long-Term Version

- Migrate canonical shared trips to `/trips/{tripId}` and keep per-user refs only for indexing and personalization.

### Exit Criteria

- A user can open a shared trip from chat without copying the trip into their own subtree.
- Shared-trip read rules are enforced by Firestore, not by client trust.

## Phase 6: Shared Trip Collaboration And Final Cleanup

### Primary Outcome

- Shared trips are collaborative without reintroducing the same performance problems.

### Work

- Add role-based collaboration rules for event edits, option selection, and trip rename/archive.
- Decide conflict model:
  - last write wins for simple fields
  - transactions or merge rules where needed
- Keep real-time listeners scoped to the active trip only.
- Remove compatibility code left over from owner-scoped-only assumptions.
- Complete migration to canonical top-level trip storage if approved.

### Exit Criteria

- Shared trips support the required collaboration level.
- Performance remains within target after shared access is enabled.
- Owner-only path assumptions are removed from the app's main trip surfaces.

## Issue Mapping

- Issue 1: Phase 1
- Issue 2: Phase 3
- Issue 3: Phase 1
- Issue 4: Phase 4
- Issue 5: Phase 1
- Issue 6: Phase 1 and Phase 2
- Issue 7: Phase 1

## Recommended Order

1. Phase 0
2. Phase 1
3. Phase 2
4. Phase 3
5. Phase 4
6. Phase 5
7. Phase 6

## Notes For Implementation

- Do not couple shared-trip support to the old duplicate ViewModel structure. Remove the duplicate first.
- Do not ship the collection-group option query until option docs have stable trip-scoping fields.
- Do not rely on Firestore cache alone as a substitute for listener lifecycle control. Fewer active listeners still matters.
- Prefer one repository-backed trip model over screen-specific fixes. It is the cheapest way to solve performance now and sharing later.
