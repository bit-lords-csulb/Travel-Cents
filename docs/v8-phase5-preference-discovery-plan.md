# V8 Phase 5 Revision — Preference Discovery + Curated Suggestions

**Builds on:** [`docs/v8.md`](C:/Users/Zaher503/AndroidStudioProjects/Travel-Cents/docs/v8.md) Phase 1-4 foundation, current `final-push` branch state

---

## Goal

Replace the current thin V8 Phase 5 with a richer guided discovery flow that:

- collects structured food, activity, and event preferences,
- shows live curated suggestions in chat,
- lets the user add, bookmark, skip, and paginate through options,
- adds accepted items to the preview trip in real time,
- stays deterministic and debuggable in Kotlin instead of relying on the LLM to control state transitions.

This plan preserves the product direction of the original expanded Phase 5, but changes the architecture:

- **The app owns flow control.**
- **The LLM assists with wording, light inference, and ranking hints.**
- **Tool calls, slot scheduling, paging, idempotency, and completion are deterministic.**

---

## Why This Revision Is Needed

The current chat architecture is not a clean fit for an LLM-driven multi-track discovery loop.

### Current constraints

- `AiTripIntakeOrchestrator` only supports `ask_more`, `suggest_destinations`, and `build_trip`.
- `AiToolRouterOrchestrator` is optimized for opportunistic live search from the latest user turn, not for a queued multi-slot discovery workflow.
- `AiChatSessionState` still centers on one active question row, one active place row, and one active event card at a time.
- The current `buildVisibleItems()` flow is designed for transient rows, not for a persistent chat history of frozen question cards and partially consumed suggestion carousels.

### Architectural principle for revised Phase 5

The user experience should feel concierge-like, but the implementation should behave like a state machine:

- Kotlin decides what track is next.
- Kotlin decides what slot is next.
- Kotlin decides when a search should fire.
- Kotlin decides when discovery is complete.
- The LLM does not decide control flow through arbitrary `next_action` values.

This keeps the system testable, replayable, and resistant to prompt drift.

---

## Product Overview

Phase 5 becomes a **guided discovery system** that runs after dates are confirmed.

### Entry condition

Phase 4 must be complete.

Required precondition:

- `sessionState.intakeProfile.dateFrom != null`
- `sessionState.intakeProfile.dateTo != null`

### Discovery tracks

Tracks run in this order:

1. Food
2. Activities
3. Events

### User experience

The assistant asks a short chip-based preference question for the current track, then shows a suggestion card stack for a deterministic trip slot:

- meals for food,
- day plans for activities,
- trip-window event results for events.

The user can:

- add an item to the preview trip,
- bookmark it for later,
- skip it,
- load more options,
- revise preferences for the current track.

At the end, bookmarked items are summarized for optional bulk-add or discard.

---

## High-Level Architecture

Phase 5 should be app-driven, not prompt-driven.

### Responsibilities split

**Kotlin owns:**

- track sequencing,
- slot generation,
- slot completion,
- page offsets and provider cursors,
- skip/add/bookmark state,
- deduplication,
- preview-trip insertion,
- completion criteria,
- stale/regenerate behavior after preference changes.

**LLM assists with:**

- friendly question phrasing,
- short assistant bridge messages,
- optional query hints or search term refinement,
- contextual tips between result sets,
- concise completion copy.

### Rule of thumb

If a decision affects persistence, ordering, paging, or idempotency, Kotlin should own it.

---

## Data Model Changes

### 1. Preference profile

Add a new file:

`data/ai/chat/PreferenceProfile.kt`

```kotlin
data class PreferenceProfile(
    val cuisineTypes: List<String> = emptyList(),
    val diningStyle: String? = null,
    val dietaryRestrictions: List<String> = emptyList(),
    val activityStyles: List<String> = emptyList(),
    val activityPace: String? = null,
    val wantsMusicEvents: Boolean? = null,
    val musicGenres: List<String> = emptyList(),
    val eventTypes: List<String> = emptyList()
)
```

This profile is not enough by itself. It must be paired with deterministic discovery state.

### 2. Discovery track state

Add:

```kotlin
enum class DiscoveryTrack {
    NOT_STARTED,
    FOOD,
    ACTIVITIES,
    EVENTS,
    COMPLETE
}
```

### 3. Slot model

Add a deterministic slot model:

```kotlin
enum class DiscoverySlotStatus {
    PENDING_QUESTION,
    READY_TO_SEARCH,
    SEARCHING,
    SHOWING_RESULTS,
    COMPLETED,
    STALE
}

enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER
}

data class DiscoverySlot(
    val id: String,
    val track: DiscoveryTrack,
    val dayIndex: Int? = null,
    val date: String? = null,
    val mealType: MealType? = null,
    val title: String,
    val status: DiscoverySlotStatus = DiscoverySlotStatus.PENDING_QUESTION,
    val page: Int = 0,
    val pageSize: Int = 6,
    val exhausted: Boolean = false,
    val providerCursor: String? = null,
    val shownSuggestionIds: Set<String> = emptySet(),
    val skippedSuggestionIds: Set<String> = emptySet(),
    val addedSuggestionIds: Set<String> = emptySet(),
    val bookmarkedSuggestionIds: Set<String> = emptySet()
)
```

This is the key difference from the original proposal. Discovery is driven by slots, not by free-form `next_action`.

### 4. Session state additions

Extend `AiChatSessionState.kt` with:

```kotlin
val preferenceProfile: PreferenceProfile = PreferenceProfile()
val discoveryTrack: DiscoveryTrack = DiscoveryTrack.NOT_STARTED
val discoverySlots: List<DiscoverySlot> = emptyList()
val activeDiscoverySlotId: String? = null
val bookmarkedSuggestions: List<SuggestionItem> = emptyList()
val suggestionPagesBySlot: Map<String, List<SuggestionItem>> = emptyMap()
val staleTrackSet: Set<DiscoveryTrack> = emptySet()
```

### 5. Suggestion model

Add a richer suggestion type:

```kotlin
data class SuggestionItem(
    val id: String,
    val name: String,
    val subtitle: String,
    val imageUrl: String?,
    val address: String,
    val detailUrl: String?,
    val source: String,
    val providerId: String,
    val slotId: String,
    val rawEvent: TravelEvent
)
```

`providerId` should be distinct from chat-card ID so dedupe remains stable across rerenders.

---

## Chat Item Model Changes

The current chat model is too transient for this flow. Phase 5 should add persistent item types.

### New chat items

In `AiChatItem.kt`, add:

```kotlin
data class PreferenceQuestionCard(
    override val id: String,
    val track: DiscoveryTrack,
    val question: String,
    val options: List<String>,
    val multiSelect: Boolean,
    val answered: Boolean = false,
    val answerSummary: String = ""
) : AiChatItem

data class SuggestionCarouselCard(
    override val id: String,
    val slotId: String,
    val track: DiscoveryTrack,
    val label: String,
    val suggestions: List<SuggestionItem>,
    val hasMore: Boolean,
    val isStale: Boolean = false,
    val exhausted: Boolean = false
) : AiChatItem

data class BookmarkSummaryCard(
    override val id: String,
    val bookmarks: List<SuggestionItem>
) : AiChatItem
```

### Important behavior change

Do not treat these as `active*` transient rows only. They should be appended to the chat item history and updated by stable ID.

That means `AiChatViewModel` needs a stateful item update path rather than only rebuilding visible items from:

- `activeResponseCardGroup`,
- `activePlaceRecommendationRow`,
- `activeSingleEventCard`.

---

## Search Orchestration Strategy

### Core rule

The app fires searches because a slot becomes `READY_TO_SEARCH`, not because the LLM says so.

### Search sources

- Food: Yelp restaurant search
- Activities: Yelp activity/attraction search
- Events: Ticketmaster event search

### Search inputs

Search parameters come from:

- locked destination,
- confirmed trip dates,
- deterministic slot context,
- `PreferenceProfile`,
- optional LLM-generated query hint text

### Search output handling

Each search returns:

- a visible first page,
- a persisted page/cache entry for the slot,
- a normalized list of `SuggestionItem`s,
- an exhaustion flag or next-page metadata.

### Paging

Paging must be owned by Kotlin:

- `page` increments on `loadMoreSuggestions(slotId)`,
- repeated provider IDs are filtered,
- skipped IDs are never reshown within that slot,
- loaded pages are mapped per slot.

Do not hardcode `offset = 6` as global behavior. Keep page math per slot.

---

## Preference Collection Design

Preference collection should still feel light and fast.

### Food questions

1. Cuisine preferences
2. Dining style
3. Optional dietary restrictions only if useful

### Activity questions

1. Activity styles
2. Pace

### Event questions

1. Interest yes/no/maybe
2. Genres if interested
3. Event types if interested

### Recommended rule

Ask the minimum number of questions needed to unlock good search parameters. Do not force extra turns when the user is clearly flexible.

### LLM role here

The LLM can provide:

- final phrasing,
- concise option labels,
- a bridge sentence after the user answers.

But question IDs and track progression should still be deterministic.

---

## Deterministic Slot Generation

After dates are confirmed, Kotlin should generate slots up front.

### Food slot generation

Use deterministic heuristics based on trip duration:

- arrival day: dinner only,
- full days: lunch and dinner,
- breakfast only when trip length and departure timing justify it,
- departure day: breakfast only by default.

Keep this simple at first. Over-modeling meal slots will increase churn.

### Activity slot generation

Generate one or two activity slots per full day instead of trying to overfit exact morning/afternoon timing in v1.

Recommended first version:

- one activity slot per non-departure day,
- optional second slot only for longer trips or packed pace.

This is more stable than promising “2–3 activities per day” immediately.

### Event slot generation

Events are one trip-window slot, not one slot per day.

That keeps Ticketmaster usage bounded and reduces repeated empty-result loops.

---

## Preview Trip Insertion Rules

Preview-trip insertion should happen only from explicit user actions.

### Add

When the user taps add:

- convert `SuggestionItem.rawEvent` into preview event,
- insert into preview trip,
- mark the suggestion as added for that slot,
- prevent duplicate insertion by provider dedupe key.

### Bookmark

Bookmarking should:

- remove or collapse the visible row,
- add to `bookmarkedSuggestions`,
- not insert into the preview trip.

### Skip

Skipping should:

- mark the provider ID as skipped for that slot,
- collapse the visible row,
- never show that exact provider ID again for that slot.

### Idempotency requirement

All add/bookmark/skip actions must be idempotent.

Recommended dedupe key:

```kotlin
data class PreviewInsertionKey(
    val source: String,
    val providerId: String,
    val slotId: String
)
```

If the same provider item reappears because of refresh or page load, insertion must still be blocked.

---

## Preference Changes and Stale State

The original proposal froze question cards permanently and blocked re-answering. That is safe, but too rigid.

### Revised behavior

Allow “Change preferences” for the current track.

When preferences change:

- mark existing carousels for that track as `STALE`,
- leave old cards visible in history,
- visually label them as outdated,
- regenerate future slot results for that track from the updated profile,
- keep already added preview events intact unless the user removes them manually.

This gives the user a recoverable flow without forcing destructive rollback.

---

## UI Components

### 1. `AiPreferenceQuestionCard.kt`

Responsibilities:

- render question header,
- render chips,
- support single and multi-select,
- show a submit button when needed,
- freeze after submit,
- show a compact answer summary,
- optionally show `Change preferences` when the track is still active.

### 2. `AiSuggestionCarouselCard.kt`

Despite the name, this should remain a vertical stack within chat.

Responsibilities:

- render 4-6 visible results,
- support add/bookmark/skip,
- show load more when more results exist,
- show empty/exhausted state,
- show stale badge when invalidated by preference change,
- show loading state during page fetch.

### 3. `AiBookmarkSummaryCard.kt`

Responsibilities:

- render bookmark list,
- allow add individually,
- allow remove individually,
- allow add all,
- allow clear all.

### 4. Locked banner enhancement

Extend `AiChatLockedDestinationBanner` so it can show:

- `Building your trip...`
- `Searching restaurants...`
- `Searching activities...`
- `Searching events...`
- `7 events added`
- `Ready to save`

This should be driven from deterministic discovery state, not inferred from model text.

---

## ViewModel Responsibilities

`AiChatViewModel` becomes the Phase 5 coordinator.

### New responsibilities

- detect Phase 4 completion,
- initialize Phase 5 slots,
- start track preference questions,
- apply preference answers,
- transition slots from `PENDING_QUESTION` to `READY_TO_SEARCH`,
- trigger searches,
- map results into `SuggestionItem`,
- append/update chat cards,
- route user actions,
- add accepted events to preview,
- emit bookmark summary,
- mark discovery complete.

### Suggested public functions

```kotlin
fun startDiscoveryIfEligible()
fun answerPreferenceQuestion(cardId: String, selected: List<String>)
fun addSuggestion(slotId: String, itemId: String)
fun bookmarkSuggestion(slotId: String, itemId: String)
fun skipSuggestion(slotId: String, itemId: String)
fun loadMoreSuggestions(slotId: String)
fun changeTrackPreferences(track: DiscoveryTrack)
fun addBookmarkedSuggestion(itemId: String)
fun addAllBookmarkedSuggestions()
fun clearBookmarks()
```

### Important implementation note

Do not keep this flow inside the current `dispatchToolCalls()` path alone. That code path is designed for opportunistic live search from a single free-form user message, not a long-lived discovery state machine.

---

## Tooling Layer Changes

### Recommendation

Do not overload the current intake `next_action` schema with Phase 5 control flow.

Instead:

- keep the intake orchestrator focused on trip understanding and date clarification,
- keep the existing tool router for free-form “find me X” chat,
- add a dedicated Phase 5 discovery coordinator in Kotlin for structured preference-driven search.

### Optional LLM support endpoint

If needed, introduce a lightweight helper prompt for:

- wording a question,
- refining a Yelp term,
- generating a short tip line,
- generating a completion sentence.

This helper should not return state transitions.

---

## Provider-Specific Guidance

### Yelp restaurants and activities

Use the existing Yelp repository path, but add deterministic query builders for:

- cuisine/category mapping,
- price hints,
- day/meal labels,
- duplicate filtering across pages,
- local fallback when results are sparse.

### Ticketmaster events

Search once for the full trip window.

If 0 results:

- show a deterministic no-results message,
- offer fallback venue search if supported,
- allow skipping the event track cleanly.

Do not repeatedly search per day until there is evidence that the product needs that granularity.

---

## Phased Implementation Plan

### Phase 5.1 — Discovery State Foundation

Goal:

- add deterministic track and slot state,
- add `PreferenceProfile`,
- add persistent chat item types for questions and suggestion stacks.

Files:

- `data/ai/chat/PreferenceProfile.kt` new
- `data/ai/chat/AiChatSessionState.kt`
- `data/ai/chat/AiChatItem.kt`
- `data/ai/chat/AiChatUiState.kt`
- `ui/main/aichat/AiChatViewModel.kt`

Acceptance:

- discovery state can be initialized after dates are confirmed,
- slots are generated deterministically,
- question cards and placeholder suggestion cards can persist in chat history.

### Phase 5.2 — Food Track

Goal:

- collect food preferences,
- generate meal slots,
- run Yelp restaurant search per slot,
- support add/bookmark/skip/load-more.

Files:

- `ui/main/aichat/components/AiPreferenceQuestionCard.kt` new
- `ui/main/aichat/components/AiSuggestionCarouselCard.kt` new
- `ui/main/aichat/AiTripChatPage.kt`
- `ui/main/aichat/AiChatViewModel.kt`
- Yelp query builder integration files

Acceptance:

- food preferences produce deterministic restaurant searches,
- skipped results do not reappear in that slot,
- added items appear in preview trip,
- bookmark behavior is stable.

### Phase 5.3 — Activity Track

Goal:

- collect activity preferences,
- generate daily activity slots,
- run Yelp activity search per slot,
- support the same action set as food.

Acceptance:

- activity slots are day-based and deterministic,
- preview-trip additions are deduped,
- load-more remains slot-specific.

### Phase 5.4 — Event Track

Goal:

- collect event interest and style,
- run one Ticketmaster search for the trip window,
- map results to actual dates in preview trip.

Acceptance:

- event track skips cleanly when the user is not interested,
- no-results handling is explicit,
- accepted events land on their actual trip date.

### Phase 5.5 — Bookmarks + Completion

Goal:

- summarize bookmarked items,
- allow bulk add or clear,
- mark discovery complete,
- update banner status.

Files:

- `ui/main/aichat/components/AiBookmarkSummaryCard.kt` new
- `ui/main/aichat/components/AiChatLockedDestinationBanner.kt`
- `ui/main/aichat/AiChatViewModel.kt`

Acceptance:

- bookmark summary appears only when needed,
- add-all is idempotent,
- discovery completion state is explicit.

### Phase 5.6 — Preference Revision and Stale Regeneration

Goal:

- allow users to revise preferences for an active track,
- mark old cards stale,
- regenerate future results safely.

Acceptance:

- changed preferences do not silently mutate old cards,
- stale cards remain visible,
- regenerated results use the new preference state.

---

## Suggested File Impact

### New files

- `data/ai/chat/PreferenceProfile.kt`
- `ui/main/aichat/components/AiPreferenceQuestionCard.kt`
- `ui/main/aichat/components/AiSuggestionCarouselCard.kt`
- `ui/main/aichat/components/AiBookmarkSummaryCard.kt`

### Updated files

- `data/ai/chat/AiChatSessionState.kt`
- `data/ai/chat/AiChatItem.kt`
- `data/ai/chat/AiChatUiState.kt`
- `ui/main/aichat/AiChatViewModel.kt`
- `ui/main/aichat/AiTripChatPage.kt`
- `ui/main/aichat/components/AiChatLockedDestinationBanner.kt`
- `ui/main/current/CurrentTripViewModel.kt`
- `ui/main/MainScaffold.kt`

### Files that should probably stay mostly untouched

- `AiTripIntakeOrchestrator.kt`
- `AiToolRouterOrchestrator.kt`

Those systems can remain focused on general intake and free-form routing. Phase 5 deserves its own deterministic coordinator.

---

## Acceptance Criteria

- Discovery starts only after `dateFrom` and `dateTo` are confirmed.
- Tracks run in deterministic order: food, then activities, then events.
- Slot generation is deterministic and testable.
- Searches are triggered by slot state, not arbitrary LLM control flow.
- Each slot supports add, bookmark, skip, and load more.
- Skipped items do not reappear in the same slot.
- Added items are inserted into the preview trip exactly once.
- Bookmarks never insert into the trip until explicitly added.
- Event results map to their actual event dates.
- Discovery can complete even if one provider returns sparse or empty results.
- Preference changes mark prior track outputs stale rather than silently mutating history.
- Banner status reflects actual discovery state from Kotlin.

---

## Risks and Mitigations

### Risk: Scope blow-up

This is materially larger than the current V8 Phase 5.

Mitigation:

- ship Food first,
- then Activities,
- then Events,
- then bookmark summary,
- then preference revision.

### Risk: Existing chat architecture is too transient

Current `active*` patterns are not designed for persistent multi-step cards.

Mitigation:

- introduce stable item models early in Phase 5.1,
- treat discovery cards as append-only chat history entries.

### Risk: Duplicate preview events

Multi-page results and refreshes can produce repeated provider entries.

Mitigation:

- add insertion keys and slot-level dedupe,
- test double taps and repeated page loads explicitly.

### Risk: Provider sparsity

Some locations will have weak Yelp or Ticketmaster coverage.

Mitigation:

- make empty-state handling deterministic,
- provide “change preferences” and “skip this track” exits,
- avoid repeated blind retries.

---

## Recommended Build Order

1. Phase 5.1 state foundation
2. Phase 5.2 food track
3. Phase 5.3 activity track
4. Phase 5.4 event track
5. Phase 5.5 bookmarks and completion
6. Phase 5.6 preference revision and stale regeneration

This ordering reduces architectural risk and gets a useful slice in front of users quickly.

---

## Final Recommendation

Implement the richer Phase 5, but do it as a **deterministic discovery engine with LLM-assisted copy**, not as an **LLM-owned state machine**.

That gives you:

- the same concierge feel,
- far better reliability,
- simpler debugging,
- safer pagination,
- stable idempotency,
- clearer test coverage,
- less prompt fragility over time.
