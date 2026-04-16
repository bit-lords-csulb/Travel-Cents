# Trip Performance Baseline

Updated: 2026-04-16

This baseline captures the trip read paths that existed before the phase 1 cleanup started, plus the instrumentation added in this branch to measure the same flows going forward.

## Instrumentation

- Log tag: `TripPerformance`
- Metrics emitted:
  - `trip_query`
  - `event_query`
  - `option_query`
  - `yelp_enrichment_attempt`
  - `first_render`

## Pre-Phase-1 Query Paths

### App startup

- `HomeViewModel.init` immediately fetched `/users/{uid}/trips`.
- `MainScaffold` immediately called `CurrentTripViewModel.loadTrip()`.
- `CurrentTripViewModel.fetchLatestItinerary()` read the trips collection ordered by `createdAt`, then scanned client-side for the first non-archived trip.
- Once a trip doc was chosen, `CurrentTripViewModel.listenToEvents()` attached an `events` snapshot listener before the Current Trip surface was visible.

### Home load

- `HomeViewModel.loadAllTrips()` fetched the full owner trip list ordered by `dateFrom`.
- Home image enrichment then persisted missing `homeImageUrl` values back to the same trip docs.

### Current Trip open

- `CurrentTripViewModel.fetchTrip()` read the trip doc directly when a trip id was known.
- `listenToEvents()` attached a real-time listener on `/users/{uid}/trips/{tripId}/events`.
- Every event snapshot triggered `loadOptionsForEvents()`, which queried each event's `options` subcollection separately.
- Every snapshot also called Yelp enrichment prefetch across the full event list.

### Trip switch

- Header switcher called `CurrentTripViewModel.loadTrip(tripId)`.
- The previous events listener was removed only when the next trip load started.
- The replacement trip repeated the same trip doc read, event listener attach, and per-event option fanout.

### Delete trip

- `deleteTrip()` loaded all event docs.
- For each event doc it loaded the `options` subcollection.
- It then deleted options, then the event, then finally the trip doc.

### Share trip

- `fetchShareTargets()` loaded groups, direct chats, and user name documents for direct-message labels.
- `shareTripToChat()` performed an extra Firestore user lookup for the sender's own name before writing the chat message.

## Listener Lifecycle Notes

- Before phase 1, the current-trip listener could be attached during app startup, not just while the trip screen was visible.
- The phase 2 target is to tie listener attach/detach to the Current Trip surface itself instead of app mount.
