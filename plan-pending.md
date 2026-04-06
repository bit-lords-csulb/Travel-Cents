# Travel Cents — Pending Tasks

> All incomplete `[ ]` items as of 2026-04-05. See `plan-done.md` for completed work.
>
> **Difficulty key:**
> - `[Easy]` — single file, < 30 lines, no logic → Claude Haiku / Codex
> - `[Medium]` — 1–3 files, some logic → Claude Sonnet / Codex
> - `[Hard]` — multi-file, architecture change → Claude Sonnet / Gemini
> - `[Complex]` — cross-cutting, deep context required → Claude Opus / paired agents

---

## Phase 2 — Event Management Pipeline

### Task 2.3 — SerpAPI Hotels (remaining items)
**Files:** `SerpRepository.kt`

- [ ] `[Medium]` Fetch page 2 via `next_page_token` for trips >7 days
- [ ] `[Medium]` Room type details (king/queen/suite): requires extra `property_token` call (1 credit each). Implement in Phase 5 if needed.

### Task 2.4 — Flight Fallback (remaining)
**Files:** `TripGeneratingPage.kt`

- [ ] `[Easy]` Update `TripGeneratingPage.kt` to show "Searching alternate airports…" during fallback — currently silently retried in `SerpRepository`, no mid-step UI update

### Task 2.5 — Yelp Integration (remaining)
**Files:** `NewTripViewModel.kt`, `GroqRepository.kt`

- [ ] `[Medium]` For activities Yelp can't cover (national parks, beaches): Groq-generated fallback with typed placeholder icon

### Task 2.6 — Local Image Storage (remaining)
**Files:** `ImageCacheManager.kt`, Coil configuration

- [ ] `[Medium]` Coil: configure a custom `ImageLoader` that checks `localImagePath` first, then falls back to URL, then to a typed vector placeholder
- [ ] `[Easy]` When an option is swapped: download new option image if not already cached

---

## Phase 3 — Event Selection UX (original)

### Task 3.1 — Option Selection UI (remaining)
**Files:** `FinalPlan.kt`, `EventOptionsPanel.kt`

- [ ] `[Hard]` Drag-to-change-day: deferred to Phase 5

### Task 3.2 — Trip Sharing via Chat (remaining)
**Files:** `FinalPlan.kt`, `ChatPage.kt`, `DirectChatPage.kt`, `TravelCentsNavigation.kt`

- [ ] `[Medium]` Tapping the trip card in chat → opens `FinalPlanPage` in read-only mode (navigation wiring)
- [ ] `[Medium]` Read-only view: disable edit fields, hide destructive menu items

---

## Phase 3 — Post-Completion Refinements

### Task 3.5 — Card Content & Swap Quality Fixes (remaining)
**Files:** `FinalPlan.kt`, `EventOptionsPanel.kt`, `ItineraryViewModel.kt`

- [x] **Exclude already-selected**: change menu must not list the currently active option; also exclude options already selected on another day in the same pool
- [ ] `[Hard]` **Two activities per day**: pipeline should aim for 2 activity slots per day and 1 restaurant slot (pipeline restructure in `NewTripViewModel.kt`)

### Task 3.6 — No-Duplicate Daily Recommendations (remaining)
**Files:** `NewTripViewModel.kt`, `GroqRepository.kt`

- [ ] `[Medium]` Same deduplication applies to change-menu alternatives: if a business is already the primary on another day, move it to alternatives only
- [ ] `[Easy]` Groq itinerary prompt: explicitly instruct the model not to suggest the same restaurant or activity name more than once across the plan skeleton

### Task 3.7 — Explicit Reorder Mode (Jiggle Mode) ✅
**Files:** `FinalPlan.kt`

- [x] Reorder mode off by default — activate via "Reorder Events" in triple-dot menu
- [x] Jiggle mode: subtle continuous rotation/wobble animation + "Done" banner
- [x] Confirmed `sh.calvin.reorderable:reorderable:2.4.0` in `build.gradle.kts`
- [x] Exit jiggle mode: tap "Done" (bottom bar) or re-tap "Reorder Events" in triple-dot; persists to Firestore on drag end
- [x] Drag handles visible only in jiggle mode

### Task 3.8 — API Response Debugging (remaining)
**Files:** `FinalPlan.kt`, `ExpandedEventCard.kt`

- [ ] `[Easy]` **Groq vs real data labeling in debug UI**: add debug-only source label on cards/expanded cards (matches external `test.py` report)
- [ ] `[Complex]` Once root causes are confirmed, implement the targeted fixes updating Tasks 2.4, 3.5, and 3.6

#### Task 3.8.2 — Order Of Operations (remaining)
**Files:** `test.py`, `NewTripViewModel.kt`, `GroqRepository.kt`

- [ ] `[Easy]` **Step 1 validation**: validate Day 1 grouping after parser fix using `test.py`
- [ ] `[Hard]` **Step 5**: Move deterministic itinerary fields out of `GroqRepository.kt`; generate locally: `itinerary_id`, `created_at`, `duration_days`, `status`, trip title, day slot structure; replace Groq IATA inference with deterministic lookup
- [ ] `[Easy]` **Step 6**: Update `test.py` whenever pipeline order, request shape, or image strategy changes; add summary metrics for missing days, provider errors, duplicate primaries, image-stage time

#### Task 3.8.3 — Concrete File Change Map (remaining)
- [ ] `[Easy]` `SerpRepository.kt`: improve hotel ranking and selected-option choice beyond "first result wins"
- [ ] `[Hard]` `NewTripViewModel.kt`: replace per-day Yelp fan-out with pooled fetch + local distribution; deduplicate restaurant/activity primaries; reduce image download on critical path
- [ ] `[Medium]` `YelpRepository.kt`: add pooled search entry points; bounded concurrency / backoff; preserve richer failure info instead of silently returning `null`
- [ ] `[Medium]` `ImageCacheManager.kt`: move from eager full-gallery prefetch to selected-image prefetch + lazy gallery fetch; add bounded concurrency
- [ ] `[Hard]` `GroqRepository.kt`: reduce Groq responsibility to only non-deterministic fields; remove locally derivable metadata from model call over time
- [ ] `[Easy]` `EventOption.kt`: keep `source` as canonical provider marker for debug tooling and UI labels
- [ ] `[Easy]` `FinalPlan.kt`: add debug-only provider source labels in debug builds
- [ ] `[Easy]` `ExpandedEventCard.kt`: add debug-only provider source labels; ensure lazy image/gallery strategy reflected in UI
- [ ] `[Easy]` `test.py`: keep as external regression harness for request ordering, missing-day detection, provider attribution, image-stage timing

### Task 3.10 — Multi-Photo Gallery for Events
**Files:** `ExpandedEventCard.kt`, `SerpModels.kt`, `SerpRepository.kt`, new `ImageGalleryScreen.kt`

- [ ] `[Easy]` Verify all SerpAPI hotel `images[]` are actually stored in Firestore after pipeline runs (see Task 3.8 debugging)
- [ ] `[Medium]` In expanded card: show a `HorizontalPager` of all stored images with a dot indicator
- [ ] `[Medium]` **Gallery icon trigger** (per Task 3.9): tapping the 2×2 grid icon opens `ImageGalleryScreen` — full-screen composable with swipe-to-navigate and image counter ("3 / 8")
- [ ] `[Medium]` `ImageGalleryScreen` is a Dialog overlay (not a nav destination) — back-press returns to expanded card

### Task 3.11 — Trip Sharing: Navigation Wiring + Read-Only View + Bug Fixes
**Files:** `FinalPlan.kt`, `ChatPage.kt`, `DirectChatPage.kt`, `ItineraryViewModel.kt`, `TravelCentsNavigation.kt`, `MainScaffold.kt`, `Message.kt`, `FirestoreRepository.kt`

- [ ] `[Medium]` **Fix wrong-trip bug**: shared message must store `ownerUid` + `tripId` explicitly shared. Render name/destination/dates from message document (denormalized) — never from receiver's trip list
- [ ] `[Hard]` **Navigation wiring**: add route `finalPlan/{ownerUid}/{tripId}?readOnly=true`; `ItineraryViewModel` loads trip by `ownerUid/tripId` (confirm Firestore cross-user read rules or add `sharedWith` field)
- [ ] `[Medium]` **Read-only mode**: disable inline edit fields, hide Delete/Archive/Reorder options when `readOnly=true`
- [ ] `[Hard]` **Save to My Trips**: in read-only mode, show "Save to My Trips" button; deep-copy trip + all events/options into receiver's own `users/{uid}/trips/` with a new `tripId`
- [ ] `[Easy]` **Re-share from read-only view**: "Share Trip" remains active in read-only mode for forwarding

### Task 3.12 — UI Density & Spacing Fixes (remaining)
**Files:** `FinalPlan.kt`

- [ ] `[Easy]` After layout changes: run on a device and confirm full 7-day itinerary scrolls without feeling bloated

---

## Phase 4 — UI Standardization

### Task 4.1 — Unify Bottom Button Layout
**Files:** All step pages, new `WizardBottomBar.kt`

- [ ] `[Easy]` Define a single `WizardBottomBar` composable used by all steps
- [ ] `[Easy]` Standard layout: full-width primary "Continue to X" button
- [ ] `[Easy]` Either remove Save Draft from Step 4 or add consistently to all steps
- [ ] `[Easy]` Consistent height (52dp), corner radius (16dp), horizontal padding (16dp)

### Task 4.2 — Unify Text Input Styling
**Files:** All step pages, new `TravelCentsTextField.kt`

- [ ] `[Easy]` Create `TravelCentsTextField` composable: `SurfaceBright` background, 16dp radius, blue cursor, label/placeholder, optional leading icon, transparent indicators
- [ ] `[Easy]` Step 4 budget field: keep `BasicTextField` but align colors/radius
- [ ] `[Easy]` Step 5 search bar: switch from transparent to `SurfaceBright` background

---

## Phase 5 — Polish & Edge Cases

### Task 5.1 — Placeholder & Error States
**Files:** All event card composables, `FinalPlanPage.kt`

- [ ] `[Medium]` No-flights-found card with actionable fallback (manual airport entry or link to Google Flights)
- [ ] `[Medium]` No-hotels-found card with fallback
- [ ] `[Easy]` Image load failure: show typed placeholder icon (plane, bed, fork)
- [ ] `[Medium]` Network error during generation: retry button per failed step (not full restart)

### Task 5.2 — Offline Support for Saved Trips
**Files:** `ImageCacheManager.kt`, `ItineraryViewModel.kt`

- [ ] `[Medium]` Saved trips with locally cached images viewable offline
- [ ] `[Easy]` Verify Firestore offline persistence works for trip data
- [ ] `[Easy]` Show "offline" indicator when viewing cached data

### Task 5.3 — Image Cleanup & Storage Management
**Files:** `ImageCacheManager.kt`

- [ ] `[Easy]` Delete locally stored images when a trip is deleted or archived
- [ ] `[Easy]` Track total image cache size
- [ ] `[Easy]` Optional: settings toggle to clear image cache

---

## Phase 6 — Chat & Messaging Improvements

### Task 6.1 — Unified Chat List Screen
**Files:** `ChatsPage.kt`, `ChatsViewModel.kt`

- [ ] `[Medium]` Replace split/separate list views with unified list (DMs + groups)
- [ ] `[Easy]` Visual distinction: DMs show circular avatar, groups show multi-person icon
- [ ] `[Easy]` Filter chip row: All / Direct / Groups
- [ ] `[Easy]` Sort by most recent message across both types
- [ ] `[Easy]` Show unread count badges on individual chat rows

### Task 6.2 — Nav Bar Message Notifications
**Files:** `MainScaffold.kt`, `ChatsViewModel.kt`

- [ ] `[Medium]` Track total unread count across all chats in `ChatsViewModel`
- [ ] `[Easy]` Render badge on Chat nav icon (≤9 shows count, >9 shows "9+")
- [ ] `[Easy]` Badge clears when user opens chat list and all are marked read

### Task 6.3 — Chat Management (Delete & Archive)
**Files:** `ChatsPage.kt`, `ChatsViewModel.kt`, `FirestoreRepository.kt`

- [ ] `[Medium]` Long-press / swipe on chat row reveals Delete option
- [ ] `[Hard]` **DM delete**: hide from current user's list; delete Firestore doc only if other user also deleted + no unread messages remain
- [ ] `[Hard]` **Group delete**: user leaves group; if last member, delete group chat doc + all messages
- [ ] `[Easy]` Confirmation dialog before deletion
- [ ] `[Medium]` Long-press / swipe also exposes Archive option
- [ ] `[Medium]` Archived chats disappear from main list; accessible via collapsed "Archived" section
- [ ] `[Easy]` Archive state stored in `users/{uid}/archivedChats/{chatId}`
- [ ] `[Easy]` Un-archive via long-press → Unarchive
- [ ] `[Easy]` New messages in archived chat auto-un-archive it

### Task 6.4 — New Group Chat Creation Screen UI Overhaul
**Files:** `CreateGroupChatPage.kt`, related ViewModel

- [ ] `[Easy]` Audit and remove all non-functional buttons from creation screen
- [ ] `[Easy]` Match input field and button styling to rest of app
- [ ] `[Easy]` Step 1: group name input (single field, hint text)
- [ ] `[Medium]` Step 2: member search/selection — searchable friends list with checkboxes; selected members as chips
- [ ] `[Easy]` Step 3: optional group icon/avatar selection
- [ ] `[Easy]` Replace broken confirmation flow with clear "Create Group" primary button
- [ ] `[Easy]` On success, navigate directly into new group chat

### Task 6.5 — User Profile Cards in Chat
**Files:** `ChatMessageBubble.kt`, new `UserProfileSheet.kt`, `ProfileViewModel.kt`

- [ ] `[Easy]` In DM chat header: tapping other user's name opens their profile card
- [ ] `[Easy]` In group chat: tapping message sender opens their profile card
- [ ] `[Medium]` Profile card shows: picture, display name, username, mutual trips, "Send DM" button
- [ ] `[Easy]` Implemented as `ModalBottomSheet` — no full navigation
- [ ] `[Easy]` Profile data lazily fetched from Firestore on first open; cached in ViewModel for session

### Task 6.6 — Shared Chat Input Box
**Files:** All chat screen files with input box implementations

- [ ] `[Easy]` Audit how many different chat input implementations exist
- [ ] `[Easy]` Create shared `ChatInputBox` composable: text field, send button, optional attachment/emoji
- [ ] `[Easy]` Replace all per-screen input implementations with shared composable
- [ ] `[Easy]` Match visual style to existing AI chat input box (most polished version)

---

## Phase 7 — Profile & Identity

### Task 7.1 — Profile Picture Upload & Display
**Files:** `ProfilePage.kt`, `ProfileViewModel.kt`, `FirestoreRepository.kt`, Firebase Storage

- [ ] `[Hard]` Add profile picture upload flow: tap avatar → image picker (gallery or camera) → upload to `profile_pictures/{uid}.jpg` in Firebase Storage; store download URL in Firestore
- [ ] `[Hard]` Display profile pictures in: chat avatars, group member lists, chat list rows, profile cards (Task 6.5), profile page
- [ ] `[Easy]` Show placeholder initial-letter avatar when no picture is set
- [ ] `[Medium]` Crop to circle on upload; compress before uploading

### Task 7.2 — Extended Profile Identifiers
**Files:** `ProfilePage.kt`, `ProfileViewModel.kt`, Firestore `users/{uid}` document

- [ ] `[Medium]` Add editable fields: username (unique handle), display name, phone, current city, bio, website
- [ ] `[Medium]` Username uniqueness: validate against Firestore on input; show inline error if taken
- [ ] `[Easy]` Phone: store as-is, no verification
- [ ] `[Easy]` Location: free-text city field, user types manually
- [ ] `[Easy]` Show relevant fields on public-facing profile card (Task 6.5)

---

## Phase 8 — Settings Page Overhaul

### Task 8.1 — Fix Account Deletion & Logout
**Files:** `SettingsPage.kt`, `SettingsViewModel.kt`, `AuthModel.kt`, `FirestoreRepository.kt`

- [ ] `[Hard]` **Account deletion**: delete `users/{uid}` doc + all subcollections, then `FirebaseAuth.currentUser?.delete()`. Handle re-auth requirement (prompt password re-entry or Google re-auth).
- [ ] `[Hard]` **Cascade deletion**: audit all Firestore collections for user-owned documents; delete them all (trips, expenses, etc.)
- [ ] `[Easy]` **Deletion error feedback**: show clear error when `task.isSuccessful == false` instead of silently leaving dialog open
- [ ] `[Easy]` Move delete button to a **Danger Zone** section: red/destructive color, confirmation dialog requiring user to type "DELETE"
- [ ] `[Hard]` **Logout bug**: diagnose why login fails after logout (likely stale Firestore listener or ViewModel state not cleared). Clear all ViewModel state and cancel listeners on sign-out before navigating.
- [ ] `[Easy]` Ensure `FirebaseAuth.signOut()` is called before navigation

### Task 8.2 — Fix Preferences Persistence
**Files:** `SettingsPage.kt`, `SettingsViewModel.kt`, Firestore or DataStore

- [ ] `[Easy]` Identify all toggle switches and their intended behavior
- [ ] `[Medium]` Persist each preference: use `DataStore<Preferences>` (preferred) or `preferences` sub-document in Firestore for cross-device sync
- [ ] `[Easy]` Load saved preference values on ViewModel init
- [ ] `[Easy]` Remove any switches with no backing functionality

### Task 8.3 — Settings Page Reorganization & UI Polish
**Files:** `SettingsPage.kt`, `SettingsViewModel.kt`

- [ ] `[Easy]` Default tab: start on left tab (or reorder so primary tab is first)
- [ ] `[Easy]` Unify styling: match card styles, typography, spacing, colors to rest of app
- [ ] `[Easy]` Reorganize sections: Account, Notifications, Privacy & Security, Preferences, About, Danger Zone
- [ ] `[Easy]` Privacy & Security section: add "Do not share my data" toggle + static "Data & Privacy" info screen
- [ ] `[Easy]` Remove or implement placeholder settings items
- [ ] `[Easy]` Profile section in settings: show picture, display name, username; tapping navigates to full profile edit screen
