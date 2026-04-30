# AI Chat Placeholder Trip Itinerary Plan

## Objective
Enable the AI Chat copilot to incrementally build a full trip itinerary (flights, hotels, restaurants, activities) in a local, temporary "placeholder" state. This trip will evolve over the conversation, intelligently scheduling events based on strict logical constraints (like flight landing times + buffers), and will only be committed to Firebase when the user explicitly clicks a "Save" or "Done" button.

## Motivation
Currently, AI chat either recommends fully-baked curated trips or standalone places. Users need a collaborative, turn-by-turn workflow where they can add an event (e.g., "Add a dinner on Friday"), have the LLM understand the current state of the trip, and slot it in seamlessly while respecting real-world constraints.

## Proposed Solution

### 1. Local Temporary State Management
- [ ] Introduce a `placeholderTrip: Itinerary` and `placeholderEvents: List<TravelEvent>` object into the `PersistedAiChatSnapshot` and `AiChatViewModel` state.
- [ ] This state acts as the "working draft" of the trip.
- [ ] Add a "Save Trip" floating action button (FAB) or dedicated card to the chat UI that appears once the draft has minimum viable data (dates, destination, flights/hotels).

### 2. LLM Context Injection
- [ ] Update `AiTripIntakeOrchestrator.kt` to serialize and inject the current `placeholderTrip` and its events into the base system prompt.
- [ ] Instruct the LLM to analyze the existing itinerary and only suggest additions or edits that make sense sequentially.
- [ ] The LLM will use structured output (or a new schema capability) to output `AddEvent`, `UpdateEvent`, or `RemoveEvent` actions alongside conversational text.

### 3. Strict Event Ordering Rules (The "Current Trip" Rules)
We will extract the scheduling logic currently buried in `NewTripViewModel` (e.g., `minimumActivityStartTime`, `maximumActivityEndTime`) into a shared `TripSchedulingEngine`.
- [ ] **The Inbound Buffer:** No non-travel event can be scheduled before the outbound flight's arrival time + a **3-hour buffer** (accounting for transit from airport, hotel check-in, and rest).
- [ ] **The Outbound Buffer:** No event on the final day can be scheduled within **3 hours** of the return flight's departure time.
- [ ] **Geographic Grouping:** The LLM will be instructed to group activities in the same neighborhood on the same day to minimize transit.
- [ ] **Pacing Constraints:** The LLM will respect the user's `AiTripPacePreference` (e.g., spacing events 2 hours apart for "Relaxed", or back-to-back for "Packed").

### 4. Chat Mutation Actions
- [ ] When the LLM outputs a modification intent, a new `AiChatItem.TripMutationRequest` card will appear.
- [ ] The UI will show a mini-preview: "Add Gyu-Kaku Japanese BBQ on Friday at 7:00 PM."
- [ ] If the user agrees, the `TripPlanActionService` will apply the change to the local placeholder state, and the UI will reflect the updated draft.

## Phased Implementation Plan

- [ ] **Phase 1: Local State & Context:** Add the placeholder trip to `AiChatSessionStore` and inject it into the LLM context envelope.
- [ ] **Phase 2: LLM Mutation Schema:** Expand `AiTripIntakeSchema` to allow the LLM to return specific scheduling intents (Add/Move/Remove event).
- [ ] **Phase 3: Scheduling Rules Engine:** Extract the "Current Trip" constraints (flight buffers) into a standalone evaluator that validates the LLM's proposed schedule before displaying it.
- [ ] **Phase 4: UI & Persistence:** Build the mutation preview cards in the chat stream and wire up the final "Save Trip" action to push the completed draft to Firebase via `TripRepository`.

## Verification & Testing
- [ ] Unit test the `TripSchedulingEngine` to guarantee the 3-hour inbound/outbound buffers are strictly enforced.
- [ ] Verify the LLM correctly interprets the serialized itinerary context and doesn't double-book timeslots.
- [ ] Ensure the placeholder trip survives process death (persisted locally) but is never leaked to Firestore prematurely.