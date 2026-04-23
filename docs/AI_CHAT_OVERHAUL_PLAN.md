# AI Chat Overhaul Plan

## Goal
Turn `ai_trip_chat` into a card-first TravelCents trip copilot that minimizes typing and preserves context across sessions.

## Product Direction
The page should feel like a planning console:
- [x] Tappable progress over typing
- [x] Selectable card-based AI follow-ups
- [x] Mixed draft input (cards + text)
- [x] Session persistence by default

---

## Phase 1A & 1B: Foundation & Card-First UI
**Status: [x] Complete**
- [x] Redesigned chat shell with shared TravelCents visual standards.
- [x] Implemented session resume, fresh-start reset, and local searchable history.
- [x] Added rotating starter prompt cards and multi-select draft input tray.
- [x] Implemented sophisticated scroll logic that settles user messages upward to make room for cards.
- [x] Removed opening assistant greeting in favor of a centered landing state.

## Phase 2: Structured LLM Responses
**Status: [x] Mostly Complete**
- [x] Created `AiTripIntakeOrchestrator` to enforce deterministic JSON responses via schema.
- [x] Implemented `AiTripIntakeSchema` for structured profile patching (destination, budget, pace, etc.).
- [x] Added automated follow-up question generation with card options driven by LLM analysis.
- [x] Linked orchestrator decisions (ask_more vs. recommend) to UI state transitions.

## Phase 1C: Curated Editable Trip Starters
**Status: [x] Complete**
- [x] Created `AiCuratedTripCatalog` to score and recommend trips from Firestore.
- [x] Implemented `AiCuratedTripRow` and `AiCuratedTripCard` components.
- [x] Added support for "Generated Starters" when no saved match is found.
- [x] Added hardcoded hotspot templates for Bali, Tokyo, Paris, Rome, Barcelona, Honolulu, Cancun, and Bangkok in `AiCuratedTripSeedCatalog`.
- [x] Implemented duration adjustment (pill picker on card → `AiCuratedTripCatalog.adjustStarterDuration` → `AiChatViewModel.updateCuratedStarterDuration`).
- [x] Implemented per-starter refinement flow: selecting a SEEDED starter now surfaces a tag- and neighborhood-aware follow-up group (`buildStarterRefinementGroup`) instead of the generic profile-gap fallback.

## Phase 3: Recommendation Cards
**Status: [ ] Pending**
- [ ] Integrate `YelpRepository` for real-time restaurant/activity suggestions.
- [ ] Integrate `SerpRepository` for flight and hotel options.
- [ ] Add `AiChatItem.RecommendationRow` to the conversation model.
- [ ] Create interactive UI cards for external provider results.

## Phase 4: Trip Mutations from Chat
**Status: [ ] Pending**
- [ ] Extract shared trip action service for non-UI-coupled mutations.
- [ ] Implement "Add to Trip" and "Replace Event" flows directly from chat cards.
- [ ] Trigger background sync/refresh after accepted chat actions.

## Phase 5: Context-Aware Copilot
**Status: [ ] Pending**
- [ ] Inject live trip metadata into the intake orchestrator.
- [ ] Enable targeted "Find an alternative" or "Fill this gap" intents.
- [ ] Add entry points from the Current Trip screen that pass specific event context.

## Phase 6: Social & External Invocation
**Status: [ ] Pending**
- [ ] Support @mention detection in group chats.
- [ ] Implement bounded context capture for thread-specific assistance.

---

## Next Steps
1. [ ] **Hotspot Data:** Populate the `AiCuratedTripCatalog` with the recommended destination seed list.
2. [ ] **UX Motion:** Add item-level entrance animations (slide/fade) for bubbles and card groups.
3. [ ] **Phase 3 Start:** Begin mapping Yelp/Serp results into `AiChatItem` models.
