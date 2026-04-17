# Local-First Trip Sync Plan

Updated: 2026-04-16

## Status Snapshot

Most of phases 1 through 5 are now represented in code. Home and Current Trip both read from Room-backed local state first, manifest and trip-ref sync metadata exist remotely, and option hydration is lazy with a trip-level bulk query path.

### Remaining Tasks

- Add the phase 6 background hydration layer:
  - WorkManager-based follow-up sync
  - `TripHydrationWorker`
  - media metadata persistence via a `media_asset` table
- Move launch-time repair work out of the hot path:
  - remove or gate unconditional `backfillOwnedTripAccess()` from normal Home startup
- Finish local profile/member hydration:
  - populate and read `user_stub` rows instead of relying on direct remote profile lookups during trip member refresh
- Expand sync-state usage beyond Home manifest tracking:
  - track per-trip summary, events, members, and options freshness explicitly in local sync state
- Tighten Current Trip refresh behavior:
  - avoid unconditional canonical trip summary fetches when local version metadata is already sufficient
- Add targeted test coverage for the local-first sync path:
  - Room DAO coverage
  - manifest freshness / stale-path coordinator tests
  - Current Trip selective refresh tests
  - lazy options bulk-query and fallback tests

## Goals

- Make Home and Current Trip render from app-owned local storage first, not from live Firestore reads.
- On first launch after login, download only the minimum data needed for Home, then hydrate deeper data in the background.
- On later launches, perform one lightweight remote freshness check before deciding whether anything else needs to be downloaded.
- Cache trip summaries, IDs, ownership, membership, user display data, events, options, and media references locally.
- Load deeper trip data on demand:
  - Home needs summaries only.
  - Current Trip needs summary, members, events, and selected option state.
  - Full option lists should load only when the user opens alternatives, or during background hydration if product requires it.
- Keep Firestore offline persistence enabled, but stop treating it as the app's primary read model.

## Non-Goals

- Replace Firestore entirely.
- Rewrite the entire shared-trip storage model in the same pass.
- Build a full offline-first mutation queue in phase 1. Initial rollout can stay write-through with local updates and remote reconciliation.

## Current State

- Firestore disk persistence is already enabled globally in `TravelCentsApplication` through `FirestoreStartupConfig`.
- Startup is still Firestore-first:
  - `HomeViewModel` reads trips on init.
  - `CurrentTripViewModel` fetches the latest trip from Firestore.
  - `CurrentTripScreen` triggers additional trip list reloads.
- There is no app-owned relational cache for trips:
  - no `Room`
  - no local sync state table
  - no WorkManager-based hydration layer
- Trip docs have `createdAt` but do not have a reliable `updatedAt` or version field that can drive cheap local-vs-remote comparisons.
- Option docs now carry enough scope to support bulk queries:
  - `ownerUid`
  - `tripId`
  - `eventId`
- `getEventOptions()` still fans out with one query per event.

## Key Design Decision

Use a two-layer local strategy:

1. Firestore offline cache remains enabled as the raw remote cache and write queue.
2. Add an app-owned local database, backed by Room, as the UI source of truth.

That separation matters because Firestore cache alone does not give the app:

- stable query shapes for fast Home rendering
- explicit sync metadata
- cheap "no-op" startup checks
- partial hydration control
- local joins for users, trips, members, and cached media

## Target End State

### First Launch After Login

1. App gets the logged-in UID.
2. App fetches a small remote manifest for that user.
3. App fetches only the trip summaries needed for Home.
4. App writes those summaries, memberships, and user stubs into Room.
5. Home renders from Room immediately after those rows exist.
6. Background hydration starts:
   - missing user names and avatars
   - trip hero images
   - latest/current trip events
   - options for only the latest/current trip if needed

### Later Launches

1. App opens Room immediately and renders cached Home.
2. App performs one cheap remote freshness check:
   - if versions match local sync state, do nothing
   - if versions differ, fetch only changed summaries or changed sections
3. Current Trip opens from cached local rows first.
4. Events, members, and options are refreshed only if their versions changed or the user opens a screen that needs them.

### Screen-Level Read Rules

- Home:
  - local trip summaries
  - local user stubs
  - local cached hero image paths
- Current Trip:
  - local trip summary
  - local events
  - local selected option snapshot per event
  - local members and user stubs
- Event options panel:
  - local cached option list if available
  - otherwise one trip-level bulk option read, then cache locally

## Remote Schema Changes

The current schema does not support cheap local-vs-remote comparisons cleanly. Add these fields.

### User-Level Manifest

Add a manifest doc under the logged-in user:

- path: `/users/{uid}/sync/trips`
- fields:
  - `manifestVersion: Long`
  - `updatedAt: serverTimestamp`
  - `tripCount: Int`
  - `latestActiveOwnerUid: String`
  - `latestActiveTripId: String`

Purpose:

- one small document read at startup tells the app whether the locally cached Home index may be stale
- if `manifestVersion` matches local state, the app can skip reloading Home summaries

### Per-User Trip Index

Add lightweight trip reference docs for all visible trips:

- path: `/users/{uid}/tripRefs/{ownerUid_tripId}`
- fields:
  - `ownerUid`
  - `tripId`
  - `tripName`
  - `destination`
  - `dateFrom`
  - `dateTo`
  - `status`
  - `homeImageUrl`
  - `role`
  - `memberUids`
  - `summaryVersion`
  - `eventsVersion`
  - `optionsVersion`
  - `membersVersion`
  - `updatedAt`

Purpose:

- fast Home query path
- fast latest-trip lookup path
- cheap per-trip version comparison without opening full trip docs

### Canonical Trip Doc Additions

For `/users/{ownerUid}/trips/{tripId}`, add:

- `summaryVersion`
- `eventsVersion`
- `optionsVersion`
- `membersVersion`
- `updatedAt`

For `/events/{eventId}` docs, add:

- `updatedAt`

For `/options/{optionId}` docs, add:

- `updatedAt`

### Version Rules

- Any change to trip summary fields bumps `summaryVersion`.
- Any event create/edit/delete/reorder bumps `eventsVersion`.
- Any option create/select/delete/update bumps `optionsVersion`.
- Any membership or role change bumps `membersVersion`.
- All of the above also update `updatedAt`.
- The user manifest version bumps whenever any `tripRefs` document visible to that user changes.

## Local Database Design

Add Room with the following tables.

### `trip_summary`

- primary key: `ownerUid + tripId`
- fields:
  - summary fields used by Home and trip switchers
  - `summaryVersion`
  - `eventsVersion`
  - `optionsVersion`
  - `membersVersion`
  - `lastHydratedAt`
  - `isCurrentCandidate`

### `trip_member`

- primary key: `ownerUid + tripId + memberUid`
- fields:
  - `role`
  - `memberVersion`

### `user_stub`

- primary key: `uid`
- fields:
  - `displayName`
  - `avatarUrl`
  - `profileVersion`
  - `lastResolvedAt`

### `trip_event`

- primary key: `ownerUid + tripId + eventId`
- fields:
  - event body needed for Current Trip
  - selected-option summary fields denormalized onto the event row
  - `updatedAt`
  - `eventVersionGroup = eventsVersion at last sync`

### `event_option`

- primary key: `ownerUid + tripId + eventId + optionId`
- fields:
  - option body
  - `updatedAt`
  - `optionsVersionGroup = optionsVersion at last sync`

### `media_asset`

- primary key: `ownerUid + tripId + remoteUrl`
- fields:
  - `localPath`
  - `contentHash`
  - `downloadedAt`
  - `lastAccessedAt`

### `sync_state`

- primary key examples:
  - `user:{uid}:manifest`
  - `trip:{ownerUid}:{tripId}:summary`
  - `trip:{ownerUid}:{tripId}:events`
  - `trip:{ownerUid}:{tripId}:options`
- fields:
  - `remoteVersion`
  - `localVersion`
  - `lastCheckedAt`
  - `lastSuccessfulSyncAt`
  - `syncStatus`
  - `error`

### `app_state`

- primary key examples:
  - `lastLoggedInUid`
  - `lastOpenedTripOwnerUid`
  - `lastOpenedTripId`

## App Architecture Changes

### New Components

- `TripDatabase`
- `TripSummaryDao`
- `TripMemberDao`
- `UserStubDao`
- `TripEventDao`
- `EventOptionDao`
- `MediaAssetDao`
- `SyncStateDao`
- `TripLocalDataSource`
- `TripRemoteDataSource`
- `TripSyncCoordinator`
- `TripHydrationWorker`

### Repository Direction

Refactor `TripRepository` into a local-first facade:

- UI reads from Room flows
- repository decides when to sync
- Firestore reads move behind `TripRemoteDataSource`

The repository contract should expose:

- `observeHomeTrips(uid): Flow<List<TripSummary>>`
- `refreshHomeTripsIfStale(uid)`
- `observeTripSummary(key)`
- `observeTripEvents(key)`
- `observeTripMembers(key)`
- `loadOptionsIfNeeded(key, eventId)`
- `refreshTripIfVersionsChanged(key)`

## Sync Strategy

### Startup Sync

1. Read `sync_state` and cached `trip_summary` rows from Room.
2. Render Home immediately from local rows if available.
3. Fetch `/users/{uid}/sync/trips`.
4. Compare remote `manifestVersion` to local manifest version.
5. If versions match:
   - skip summary downloads
   - keep UI on local data
6. If versions differ:
   - fetch `/users/{uid}/tripRefs`
   - upsert changed summaries into Room
   - delete missing local summaries
   - update manifest sync state
7. Start low-priority hydration for missing user stubs and media.

### Current Trip Sync

1. Resolve current trip from local `app_state` first.
2. Open cached summary/events from Room immediately.
3. Compare local summary and event versions against local `trip_summary`.
4. If data for the trip is incomplete or stale:
   - fetch the canonical trip doc
   - fetch events
   - fetch members only if `membersVersion` changed
5. Write results into Room.
6. UI updates from Room flow automatically.

### Options Sync

Do not eagerly load options for every event on initial trip render.

Rules:

- If the UI only needs the selected option outcome, store that on the event row locally.
- When the user opens the options panel:
  - read local `event_option` rows first
  - if local `optionsVersion` matches the trip summary row, use local only
  - otherwise perform one bulk query:
    - `collectionGroup("options")`
    - filter by `ownerUid`
    - filter by `tripId`
  - group by `eventId`
  - store all returned options locally

Fallback:

- if bulk option query fails because of legacy docs missing scope or missing index, fall back to the existing per-event path temporarily

### Background Hydration

Use WorkManager for non-blocking follow-up work:

- resolve missing `user_stub` rows for trip members
- download missing hero images and option media
- pre-hydrate the last opened trip's events after Home is visible
- optionally pre-hydrate options for only the last opened trip

## Write Path Changes

All writes that currently go directly to Firestore must also keep local rows coherent.

Short-term rollout:

- keep write-through behavior
- update Room optimistically or immediately after successful Firestore write
- bump remote version fields in the same Firestore batch
- let next sync reconcile any drift

Long-term option:

- add an explicit mutation queue for fully offline-authored edits

The following operations must bump versions and update indexes:

- create trip
- rename trip
- archive trip
- delete trip
- share trip
- membership changes
- add/edit/delete event
- reorder event placements
- create/select/delete option

## Codebase Touch Points

Primary files likely to change:

- `app/src/main/java/com/example/travelcents/TravelCentsApplication.kt`
- `app/src/main/java/com/example/travelcents/data/trip/TripRepository.kt`
- `app/src/main/java/com/example/travelcents/data/trip/FirestoreTripRepository.kt`
- `app/src/main/java/com/example/travelcents/ui/main/home/HomeViewModel.kt`
- `app/src/main/java/com/example/travelcents/ui/main/current/CurrentTripViewModel.kt`
- `app/src/main/java/com/example/travelcents/ui/main/current/screen/CurrentTripScreen.kt`
- `app/src/main/java/com/example/travelcents/ui/main/MainScaffold.kt`
- `app/src/main/java/com/example/travelcents/data/user/UserProfileRepository.kt`
- new `data/local/...` Room package
- new `data/sync/...` package

## Implementation Phases

## Phase 0: Baseline And Guardrails

- Add instrumentation around:
  - app cold start to Home render
  - app cold start to Current Trip render
  - Home Firestore reads
  - Current Trip Firestore reads
  - option query count
- Document current startup flows and duplicate loads.
- Freeze current behavior with tests where practical.

Exit criteria:

- baseline metrics exist
- startup and current-trip read paths are documented

## Phase 1: Introduce Room For Home Summaries

- Add Room schema for:
  - `trip_summary`
  - `user_stub`
  - `sync_state`
  - `app_state`
- Add `TripLocalDataSource`.
- Refactor `HomeViewModel` to observe local summaries.
- Keep current Firestore summary fetch temporarily, but write fetched results into Room first and render from Room.

Exit criteria:

- Home reads from Room flow
- Home can render previously cached summaries offline

## Phase 2: Add Remote Manifest And Version Metadata

- Add `/users/{uid}/sync/trips`.
- Add `/users/{uid}/tripRefs`.
- Add version fields to trip docs.
- Update all relevant Firestore write paths to bump versions and refresh tripRefs.
- Add backfill logic for existing trips lacking version fields.

Exit criteria:

- each trip has version metadata
- manifest can tell the app whether Home is stale

## Phase 3: Make Startup Compare Versions Before Downloading

- Implement `TripSyncCoordinator.refreshHomeIfNeeded(uid)`.
- On app start:
  - read Room first
  - fetch manifest
  - skip trip summary reload when manifest matches local sync state
- Store `lastOpenedTrip` in local app state.

Exit criteria:

- second launch with no server changes performs one lightweight manifest check and no bulk trip summary reload

## Phase 4: Move Current Trip To Local-First

- Add Room schema for:
  - `trip_event`
  - `trip_member`
- Cache selected option outcome onto local event rows.
- Refactor `CurrentTripViewModel` to observe local trip summary, events, and members first.
- Remove startup-triggered full Firestore fetches from Current Trip where possible.

Exit criteria:

- opening Current Trip renders from Room first
- Firestore only refreshes changed sections

## Phase 5: Lazy Option Loading And Bulk Reads

- Add Room schema for `event_option`.
- Replace eager `getEventOptions()` fanout with:
  - lazy panel-triggered loads
  - trip-level bulk option query
  - local cache reuse
- Keep legacy fallback path during migration window.

Exit criteria:

- large trips do not issue one option query per event during initial render
- options panel opens from local cache when data is already hydrated

## Phase 6: Background Hydration And Media

- Add WorkManager-based background hydration.
- Store media metadata in Room and local file paths in `media_asset`.
- Hydrate:
  - member user stubs
  - trip hero images
  - last opened trip events
  - optional trip media

Exit criteria:

- Home is fast without blocking on media or user-profile fetches
- missing pieces populate progressively after first paint

## Phase 7: Cleanup And Remove Startup Hotspots

- Remove or gate hot startup work that does not belong on every launch:
  - repeated `getTripSummaries()` calls
  - eager latest-trip resolution from Firestore when local state is sufficient
  - unconditional `backfillOwnedTripAccess()` on startup
- Keep explicit repair or admin migration paths separate from normal launch.

Exit criteria:

- launch path is local-first and incremental
- migration and repair work no longer runs in the hot path

## Testing Plan

### Unit Tests

- DAO read/write coverage
- sync coordinator version-compare logic
- stale vs fresh manifest behavior
- selective section refresh behavior
- option bulk query grouping and fallback logic

### Integration Tests

- first login hydrates Home summaries into Room
- second launch with unchanged manifest performs no summary refetch
- changed manifest refreshes only changed summaries
- Current Trip renders cached events before remote refresh completes
- options panel uses cached options when versions match

### Manual Verification

- launch app twice with no remote changes and confirm no trip summary redownload
- add a remote trip and confirm manifest mismatch triggers summary refresh only
- rename a trip and confirm Home updates from sync without deep trip reload
- open a large trip and confirm initial render does not fan out option reads
- go offline and confirm Home and Current Trip still render from Room

## Migration Notes

- Existing trips need a one-time backfill of version fields and tripRefs docs.
- Existing option docs that lack scope fields may require temporary fallback queries until migrated.
- Shared-trip discovery index creation is still required for the best remote path, even after local-first caching is in place.

## Risks And Mitigations

- Risk: stale local data if version bumps are missed on writes.
  - Mitigation: centralize write helpers and add tests for every mutation path.
- Risk: rollout complexity from introducing Room and remote version fields together.
  - Mitigation: phase Home first, then Current Trip, then options.
- Risk: duplicate state drift between Firestore cache and Room.
  - Mitigation: Room is the only UI source of truth; Firestore cache stays internal to the remote layer.
- Risk: startup still too chatty if trip manifest is not small and stable.
  - Mitigation: keep manifest to one document and shift trip refs to the second step only when needed.

## Acceptance Criteria

- First launch downloads only Home-critical trip summary data before rendering Home.
- Missing names, images, and deep trip data appear progressively after initial render.
- Second launch with no remote changes renders from local cache and performs only the manifest freshness check.
- Current Trip opens from local data first and refreshes only stale sections.
- Initial trip render does not eagerly fan out option reads for every event.
- Trip summaries, users, members, events, options, and media references are all cached locally in app-owned storage.
