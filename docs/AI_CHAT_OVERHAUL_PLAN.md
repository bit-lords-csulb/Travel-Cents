# AI Chat Overhaul Plan

## Goal

Turn `ai_trip_chat` into a card-first TravelCents trip copilot that minimizes typing, keeps context across sessions, and can later be invoked from other app surfaces.

## Product Direction

The page should feel like a planning console, not a generic chatbot.

Key interaction rules:

- users should be able to make progress mostly through tapping
- AI follow-up questions should usually render as selectable cards, not only prose
- text input should remain available, but be optional for many steps
- users should be able to combine card picks and written input before sending
- the last active chat should reopen by default
- starting a fresh chat should be explicit

## Current Status

### Real Stage Right Now

TravelCents AI chat is currently between `Phase 1C` and `Phase 2`.

What is already live:

- card-first chat shell, session restore/history, mixed draft input, and starter landing state
- persisted follow-up card groups and session resume behavior
- structured intake analysis that can patch trip fields, ask one LLM-generated follow-up, and decide whether to ask more, recommend curated, or build from scratch
- curated trip starter rows backed by saved-trip matching plus a generated starter fallback

What is still blocking the intended experience:

- most follow-up questions still fall back to a static card catalog, so they repeat and feel rule-authored instead of model-authored
- assistant acknowledgement copy is still generic, so the conversation does not visibly drive toward a concrete plan
- response cards do not yet support `2-6` options, `allow_other`, or a typed handoff when `Other` is selected
- there is no dedicated destination-recommendation row or place-recommendation row yet
- curated starters exist, but the hotspot seed catalog and location-first recommendation flow are not finished

### Done So Far

- foundation: richer chat state, redesigned shell, session resume, fresh chat reset, header history, and Enter-to-send
- card-first UI: rotating starter cards, tappable follow-up card groups, mixed draft input, and centered landing state
- motion and polish: gentler post-send repositioning, preserved thread context, and starter-card icon cleanup
- persistence: saved active follow-up cards and searchable local chat history
- early AI plumbing: structured intake JSON analysis and curated starter surfacing

### Active Focus

- replace static fallback questions with LLM-authored planning turns as the primary path
- make answer cards short, varied, and schema-driven
- add destination recommendations first, then place recommendations once direction is set

## UX Direction

### Header

The top bar should be compact and action-oriented.

Required actions:

- back
- history
- new chat

Recommended behavior:

- `history` opens a list of previous AI chat sessions
- `new chat` clears active session context and starts fresh
- do not use extra descriptive subtext in the header unless it earns its space

### Main Chat Area

The center of the page should mix:

- welcome state
- rotating starter prompt cards
- user messages
- AI messages
- response card groups
- destination recommendation rows
- curated trip starter cards
- place recommendation rows

The important change is that AI questions should not rely on the user typing long answers. If the AI asks about food preferences, pace, budget, party shape, or trip type, the response should usually appear as a dedicated card group the user can tap. Those questions should feel specific to what is already known, move the conversation toward the next concrete planning decision, and not read like the same template every time.

Motion behavior to add:

- when the user sends a message, that submitted message should visually float or pin upward toward the top of the active conversation area
- this should create immediate room below for incoming AI response cards
- the transition should feel intentional and fast, not like a full-page jump
- this behavior matters most when the next AI turn includes card groups, curated trip starters, or recommendation rows

Current implementation notes:

- the sent message now animates upward with a delayed settle instead of snapping immediately
- the thread preserves some visible context above the latest turn so older messages remain easier to reach
- intake-generated follow-up cards already exist, but the static fallback path is still too dominant and needs to become last-resort only
- this still needs a second pass for bubble/card entrance animation, keyboard-dismiss timing polish, and `Other`-option composer handoff

### Composer

The composer should support mixed input:

- select one or more cards
- select `Other` when the preset cards are close but not enough
- optionally type additional context
- press send once

That means card selections should be treated as draft input until submission, not always auto-send on tap.

## Interaction Model

## 1. Starter Prompt Cards

Replace the simple quick-prompt row with a richer card grid near the top of the chat.

Examples:

- `Plan a trip`
- `Warm places`
- `City break`
- `Foodie spots`
- `Beach escape`
- `Weekend getaway`
- `Romantic trip`
- `Family trip`
- `Nature and hiking`
- `Nightlife`
- `Budget-friendly`
- `Luxury stay`

Requirements:

- show them in changing order so the page does not feel stale
- rotate from a larger curated pool, not the same fixed 4 every time
- allow these cards to be reused later as AI follow-up choices

## 2. LLM-Authored Follow-Up Cards

When the AI asks a question, it should usually return:

- short acknowledgement text
- one concrete next-step question tied to the current planning gap
- exactly one primary answer-card group with `2-6` options
- explicit selection-mode metadata that says whether the group is single-select or multi-select
- explicit `Other` handling metadata when the answer space is open-ended

Question requirements:

- the wording should feel naturally authored by the model, not like a reused template
- the same underlying field can be phrased differently between sessions as long as the meaning stays clear
- the question should move the user toward a concrete plan, not just collect trivia
- the AI should not ask for information it already has

Option requirements:

- each visible option label should be no more than `2` words
- each group should return between `2` and `6` options
- the model should decide whether the group is single-choice or multi-choice
- `Other` should be available whenever the likely answer set is open-ended or the presets are only examples

Examples:

- traveler question, single-choice:
  - `Couple`
  - `Family`
  - `Friends`
  - `Solo`
  - `Other`
- food question, multi-choice:
  - `Seafood`
  - `Vegan`
  - `Street food`
  - `Cafes`
  - `Fine dining`
  - `Other`

These cards should be first-class response objects, not just quick-reply chips.

## 3. Multi-Select Draft Input

The screen should support:

- selecting multiple response cards
- adding typed context
- sending all of it together

Example:

- user taps `Street food`, `Temples`, and `Relaxed`
- then types `Prefer somewhere less crowded`
- then presses send

This means the chat needs a draft-input state separate from final submitted messages.

## 3A. `Other` Handoff

When a card group includes `Other`:

- tapping `Other` should keep the draft open instead of forcing an immediate fake preset answer
- the composer should clearly direct the user to type what they mean
- the submitted turn should preserve both the structured selection and the typed explanation
- `Other` should be especially available for party shape, food preferences, trip constraints, hotel area preferences, and destination ideas

## 3B. Sent Message Repositioning

After submission, the UI should bias toward preserving space for the next interactive response.

Desired behavior:

- user sends selected cards and/or text
- the submitted user message animates upward in the thread
- the lower visible region is reserved for AI follow-up cards and suggestions

Recommended implementation direction:

- keep the conversation in a lazy list
- animate scroll position after submit so the latest user turn settles higher in the viewport instead of near the bottom edge
- when the next AI response is card-heavy, bias the scroll so the first response cards are visible without extra manual scrolling
- avoid abrupt snapping if the keyboard is still dismissing

## 4. Destination Recommendations

The AI should be able to recommend locations before the user has locked in a destination.

These are not full itineraries. They are:

- destination or neighborhood suggestion rows
- grounded in known signals like trip type, pace, budget, food interests, season, and origin
- presented with short match reasons so the user sees why the AI suggested them
- editable stepping stones into either curated trip starters or place recommendations

Example behavior:

- user says they want a warm food-forward anniversary trip with a relaxed pace
- AI suggests `Barcelona`, `Mexico City`, and `Honolulu`
- user taps one location
- AI follows with either a refinement question or place recommendations for that location

## 5. Curated Trip Starters

The AI should be able to immediately suggest editable starter itineraries for popular destinations.

These are not final locked trips. They are:

- destination-focused trip shells
- editable by duration
- editable by vibe, budget, and interests
- usable as fast paths into deeper trip planning

Example behavior:

- AI suggests `4-day Bali temple and beach starter`
- user accepts it
- AI continues refining duration, budget, dining, hotel area, and activity mix

## 6. Place Recommendations

Once the destination is known or narrow enough, the AI should be able to recommend places inside the trip.

These can include:

- restaurants
- hotels or hotel areas
- activities
- neighborhoods
- day-trip ideas

Each recommendation row should explain the fit in plain language and support later actions such as swap, save, or add to trip.

## Curated Destination Seed List

This is the first list of hotspots worth supporting with curated editable trip starters and destination recommendation logic.

### Tropical / Beach

- Bali, Indonesia
- Phuket, Thailand
- Honolulu, Hawaii
- Cancun, Mexico
- Maldives
- Maui, Hawaii

### City Break / Culture

- Paris, France
- Tokyo, Japan
- Rome, Italy
- Barcelona, Spain
- London, UK
- Seoul, South Korea
- New York City, USA
- Singapore

### Food-Forward

- Tokyo, Japan
- Osaka, Japan
- Bangkok, Thailand
- Mexico City, Mexico
- Rome, Italy
- Barcelona, Spain
- Seoul, South Korea
- Istanbul, Turkey

### Nature / Scenic

- Banff, Canada
- Interlaken, Switzerland
- Reykjavik, Iceland
- Queenstown, New Zealand
- Sedona, Arizona
- Yosemite area, California

### Romantic / Honeymoon

- Paris, France
- Amalfi Coast, Italy
- Santorini, Greece
- Kyoto, Japan
- Bali, Indonesia
- Maldives

### Starter Set Recommendation

To keep the first curated-trip implementation manageable, start with:

- Bali
- Tokyo
- Paris
- Rome
- Barcelona
- Honolulu
- Cancun
- Bangkok

Those eight cover beach, food, culture, romance, and broad popularity.

## Architecture Updates

## 1. Conversation Model

The current model should evolve from simple text + system items into UI objects that can represent:

- `AiChatItem.TextMessage`
- `AiChatItem.ResponseCardGroup`
- `AiChatItem.DestinationRecommendationRow`
- `AiChatItem.CuratedTripRow`
- `AiChatItem.PlaceRecommendationRow`
- `AiChatItem.SystemStatus`
- `AiChatItem.ErrorState`

The previously visible preference summary should remain internal-only and not be rendered.

Likely affected files:

- `app/src/main/java/com/example/travelcents/data/ai/chat/AiChatItem.kt`
- `app/src/main/java/com/example/travelcents/ui/main/aichat/AiChatViewModel.kt`
- `app/src/main/java/com/example/travelcents/ui/main/aichat/AiTripChatPage.kt`

## 2. Session State

`AiChatViewModel` should manage:

- visible chat items
- background traveler profile
- raw LLM history
- current stage
- available starter cards
- active follow-up card groups
- selected draft cards
- draft free-text input
- pending `Other` handoff state
- current destination recommendation groups
- current place recommendation groups
- active session id

Likely new or changed files:

- `data/ai/chat/AiChatSessionState.kt`
- `data/ai/chat/AiChatSessionStore.kt`
- `ui/main/aichat/AiChatViewModel.kt`

## 3. Prompt Factory

The LLM should receive a structured prompt that asks it to return:

- acknowledgement copy
- next planning objective
- next follow-up question
- response card options
- selection mode metadata
- `allow_other` metadata and typed handoff hint
- optional destination recommendations
- optional curated trip starter suggestions
- optional place recommendation intents

This should move out of the view model and into a dedicated prompt/response layer.

Likely new files:

- `data/ai/chat/AiChatPromptFactory.kt`
- `data/ai/chat/AiChatResponseParser.kt`
- `data/ai/chat/AiChatCoordinator.kt`

## 4. Curated Trip Data Layer

Curated editable trip starters should come from a defined seed catalog rather than only freeform model output.

Recommended approach:

- maintain a curated destination catalog
- define starter trip templates per destination
- let the AI decide when to surface them
- let later steps mutate duration, pacing, dining style, and activities

Likely new files:

- `data/ai/chat/AiCuratedTripSeed.kt`
- `data/ai/chat/AiCuratedTripCatalog.kt`
- `data/ai/chat/AiCuratedTripMapper.kt`

## 5. Recommendation Layer

TravelCents needs a recommendation layer that can bridge intake signals into destination and place suggestions.

Recommended approach:

- use structured intake plus prompt output to decide whether to suggest locations, curated trip starters, or in-destination places
- support destination recommendations even when the user only knows the vibe, not the city
- support place recommendations once destination confidence is high enough
- combine curated internal seed data with remote providers where available

Likely new files:

- `data/ai/chat/AiDestinationRecommendationEngine.kt`
- `data/ai/chat/AiPlaceRecommendationCoordinator.kt`
- `data/ai/chat/AiRecommendationMapper.kt`

## UI Components To Add Or Update

### Header

- compact back button
- history button
- new chat button

### Welcome / Starter Area

- rotating starter prompt card grid
- centered hero-style starter state without a default assistant greeting
- iconography should map directly to the travel theme of each card

### Message Area

- assistant/user bubble support
- AI response card groups
- destination recommendation rows
- curated trip starter rows
- place recommendation rows

### Composer

- draft selected-card tray
- `Other` input prompt state
- optional text input
- single send action for text + selected cards

Likely files:

- `ui/main/aichat/AiTripChatPage.kt`
- `ui/main/aichat/components/AiPromptCardGrid.kt`
- `ui/main/aichat/components/AiResponseCardGroup.kt`
- `ui/main/aichat/components/AiRecommendationRow.kt`
- `ui/main/aichat/components/AiSelectedDraftBar.kt`
- `ui/main/aichat/components/AiCuratedTripCard.kt`
- `ui/main/aichat/components/AiCuratedTripRow.kt`
- `ui/main/aichat/components/AiChatHistorySheet.kt`
- `ui/main/aichat/components/AiChatComposer.kt`

## Delivery Plan

### Phase 1A: Foundation

Status:

- done

Completed work:

- structured AI chat state and model foundation
- redesigned chat shell with shared fonts and inputs
- session resume of last user-started chat
- fresh chat reset
- simplified header and composer behavior
- Enter-to-send support

### Phase 1B: Card-First Interaction

Status:

- done

Goal:

- reduce typing and make the flow tappable-first

Completed:

- add `history` action to header
- add rotating starter prompt card grid
- add AI follow-up response card groups
- support multi-select cards plus optional text before send
- remove the default opening assistant message in favor of a centered starter landing state
- restyle the middle UI to a more modern card-first layout while preserving the existing top and bottom chrome
- animate submitted user messages upward to create space for incoming cards without hard-snapping the list
- preserve access to prior messages by keeping scroll context above the latest turn
- improve starter card icon mapping so the visible icon better matches the trip theme
- remove dependence on chip-only replies for core flows
- persist the active follow-up card state when resuming a saved chat

Files most likely affected:

- `ui/main/aichat/AiTripChatPage.kt`
- `ui/main/aichat/AiChatViewModel.kt`
- `data/ai/chat/AiChatItem.kt`
- `data/ai/chat/AiChatSessionState.kt`
- new `ui/main/aichat/components/*`

### Phase 1C: Curated Editable Trip Starters

Status:

- in progress

Goal:

- let the AI quickly suggest editable trip skeletons for popular destinations

Completed:

- curated trip row UI and selection flow
- saved-trip matching against the user's existing itineraries
- generated starter fallback when no saved match is available

Remaining:

- hotspot destination seed catalog
- editable starter trip cards for the seed set, not only saved/generated fallbacks
- duration-change flows
- deeper refinement around budget, pace, food, hotel area, and activity mix

Files most likely affected:

- new `data/ai/chat/AiCuratedTrip*`
- `ui/main/aichat/AiChatViewModel.kt`
- `ui/main/aichat/components/*`

### Phase 2: Dynamic Structured Questioning

Status:

- in progress

Goal:

- make the model author the next best planning question instead of relying on repetitive fallback cards

Completed:

- intake prompt/schema extraction through the intake orchestrator
- JSON parsing for structured intake patches and follow-up question payloads
- decisioning for `ask_more`, `recommend_curated`, and `build_from_scratch`
- initial single-vs-multi-select follow-up support

Remaining:

- unify assistant text, planning goal, follow-up cards, and recommendation payloads into one response envelope
- expand follow-up groups to `2-6` options instead of the current tighter cap
- enforce max `2` words per visible option label
- add `allow_other` plus composer handoff for typed answers
- make static fallback questions last-resort only
- strengthen variation and anti-repetition rules so similar sessions do not feel cloned
- make each question visibly move the user toward a concrete plan

Files most likely affected:

- `data/ai/chat/AiChatPromptFactory.kt`
- `data/ai/chat/AiChatResponseParser.kt`
- `data/ai/chat/AiChatCoordinator.kt`

### Phase 3: Destination And Place Recommendations

Status:

- pending

Goal:

- recommend where to go first, then what to do, eat, and stay once the direction is clear

Changes:

- destination recommendation rows for cities, regions, and neighborhoods
- short match reasons grounded in the intake profile
- place recommendation rows for restaurants, activities, hotels, and day trips
- provider orchestration through Yelp/Serp plus curated seed data

Files most likely affected:

- `data/ai/chat/AiDestinationRecommendationEngine.kt`
- `data/ai/chat/AiPlaceRecommendationCoordinator.kt`
- `data/trip/remote/YelpRepository.kt`
- `data/trip/remote/SerpRepository.kt`
- `ui/main/aichat/components/*`
- shared current-trip card files

### Phase 4: Trip Mutations From Chat

Status:

- pending

Goal:

- make recommendation cards actionable against trip data

Changes:

- shared trip action service extraction
- add/replace/save flows
- trip refresh after accepted actions

Files most likely affected:

- `ui/main/current/CurrentTripViewModel.kt`
- `data/sync/TripSyncRemoteDataSource.kt`
- `data/trip/TripPlanActionService.kt`
- `ui/main/aichat/AiChatViewModel.kt`

### Phase 5: Existing Trip Copilot

Status:

- pending

Goal:

- let the chat operate on an existing itinerary with context

Changes:

- trip-context mapping
- current-trip launch entry points
- targeted replace/swap/suggest flows

### Phase 6: Social Chat Invocation

Status:

- pending

Goal:

- invoke TravelCents AI from direct/group chats via `@travelcents`

Changes:

- mention detection
- bounded thread-context capture
- deep-link or inline assistant response behavior

### Phase 7: Current Trip Entry Points

Status:

- pending

Goal:

- let current-trip surfaces launch AI with live trip context attached

Changes:

- current-trip entry point
- trip metadata injection
- context-aware alternatives and suggestions

## Existing Systems To Reuse

- `LlmClient`
- `YelpRepository`
- `SerpRepository`
- `TripSyncRemoteDataSource`
- `TravelEvent`
- `EventOption`
- `Itinerary`

## Existing Systems To Refactor Around

- `CurrentTripViewModel`
  - do not directly couple AI chat to this screen-level VM
- `TripPlannerRepository`
  - useful JSON-only prompt precedent, but not sufficient for full conversational flow

## Immediate Next Slice

The next concrete implementation slice should be `Phase 2`, because that is the main blocker behind the current "hardcoded" feel:

1. replace the static follow-up catalog as the normal path with a single structured LLM response envelope
2. require every AI-authored answer-card group to return `2-6` options, max `2` words per label, and explicit single vs multi-select metadata
3. add `Other` handling that pushes the user into the composer for typed clarification instead of forcing a weak preset answer
4. make the assistant acknowledgement and follow-up question vary naturally while still driving toward the next concrete planning decision
5. allow that same envelope to optionally return destination recommendations when the user has a vibe but not a locked location

## Next Recommended Steps

1. Move static fallback questions to a guarded recovery path only, not the normal experience.
2. Add parser validation and tests for option count, label length, `allow_other`, and repeated-question suppression.
3. Add destination recommendation rows before broader place recommendations so the chat can narrow the map before suggesting venues.
4. Once the response envelope is stable, finish the hotspot seed catalog and editable starter-trip flows for the initial destination set.
5. Keep the motion pass on the list, but treat it as secondary to fixing the repetitive question/answer generation path.
