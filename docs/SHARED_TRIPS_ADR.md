# Shared Trips ADR

Updated: 2026-04-16
Status: Accepted for near-term implementation

## Decision

- Keep trip storage owner-scoped at `/users/{ownerUid}/trips/{tripId}` for the current performance work.
- Introduce `TripKey(ownerUid, tripId)` in app code now so read paths stop assuming the current user owns the trip.
- Treat `/trips/{tripId}` as the long-term canonical shared-trip destination once the repository layer and Firestore rules are ready.

## Why

- The app already stores production trip data under owner-scoped paths.
- Rewriting storage and permissions at the same time as the performance cleanup would mix two risky migrations.
- `TripKey` is the cheapest abstraction that lets Home, Current Trip, and chat trip cards converge on one read model before the schema migration.

## Roles

- `owner`: full read/write/delete access.
- `editor`: read access plus event and option mutations allowed by rules.
- `viewer`: read-only access.

## Near-Term Shape

- Trips remain at `/users/{ownerUid}/trips/{tripId}`.
- Chat trip cards continue to carry `ownerUid` and `sharedTripId`.
- Future repository APIs accept `TripKey`, even when `ownerUid == currentUser.uid`.
- Option documents should gain stable scoping fields:
  - `ownerUid`
  - `tripId`
  - `eventId`

## Long-Term Shape

- Canonical trips move to `/trips/{tripId}`.
- Trip docs store:
  - `ownerUid`
  - `memberUids`
  - `roleByUid`
  - `status`
  - summary fields needed by Home and chat cards
- Events move to `/trips/{tripId}/events/{eventId}`.
- Optional per-user index docs live under `/users/{uid}/tripRefs/{tripId}` for fast Home queries.

## Migration Path

1. Land phase 1 performance cleanup on owner-scoped storage.
2. Add the repository layer and route everything through `TripKey`.
3. Add shared-trip rules and read access against owner-scoped documents first.
4. Backfill canonical top-level trip docs only after the repository boundary is stable.
