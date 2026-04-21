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

### Done

- done: replaced the old flat message flow with a richer AI chat model and state layer
- done: rebuilt the screen around TravelCents visual standards and shared components
- done: removed the visible traveler-profile summary from the UI while keeping profile context in the background
- done: added session resume so the last user-started chat reopens instead of creating a new one every time
- done: added a fresh-session reset action
- done: added Enter-to-send support
- done: simplified the header and composer spacing
- done: added chat history access from the header with searchable local session history
- done: replaced the quick-prompt strip with rotating starter cards
- done: added tappable AI follow-up response card groups
- done: added mixed draft input so users can combine card picks with written text before sending
- done: added post-send scroll behavior that biases the last user turn upward to make room for incoming cards
- done: removed the default assistant welcome message and replaced it with a centered starter-question landing state
- done: restyled the middle UI to a more modern card-first layout while preserving the existing top and bottom chrome
- done: removed descriptive placeholder copy from the composer so it stays visually quiet until the user types
- done: changed post-send motion from a hard snap to a gentler upward scroll that settles the latest turn higher in the viewport
- done: preserved visible thread context above the latest turn so older messages remain reachable after sending
- done: corrected mismatched starter-card icons so the visible symbols better match the trip themes

### Still To Do In The Redesign

- pending: add curated editable trip starters for popular destinations
- pending: add recommendation cards and editable trip suggestions deeper in the flow
- pending: refine send/receive motion further so bubbles and follow-up cards animate in, not only the list scroll
- pending: validate icon semantics across the full starter-card pool and follow-up card system, not only the first visible set

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
- curated trip starter cards
- later recommendation cards

The important change is that AI questions should not rely on the user typing long answers. If the AI asks about food preferences, pace, budget, or trip type, the response should usually appear as a dedicated card group the user can tap.

Motion behavior to add:

- when the user sends a message, that submitted message should visually float or pin upward toward the top of the active conversation area
- this should create immediate room below for incoming AI response cards
- the transition should feel intentional and fast, not like a full-page jump
- this behavior matters most when the next AI turn includes card groups, curated trip starters, or recommendation rows

Current implementation notes:

- the sent message now animates upward with a delayed settle instead of snapping immediately
- the thread preserves some visible context above the latest turn so older messages remain easier to reach
- this still needs a second pass for bubble/card entrance animation and keyboard-dismiss timing polish

### Composer

The composer should support mixed input:

- select one or more cards
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

## 2. AI Follow-Up Cards

When the AI asks a question, it should usually return:

- short acknowledgement text
- one or more related answer-card groups

Examples:

- food preference question:
  - `Street food`
  - `Seafood`
  - `Fine dining`
  - `Vegan`
  - `Coffee and bakeries`
- pace question:
  - `Relaxed`
  - `Balanced`
  - `Packed`
- budget question:
  - `Budget`
  - `Comfort`
  - `Luxury`
  - `A few splurges`

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

## 4. Curated Trip Starters

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

## Curated Destination Seed List

This is the first list of hotspots worth supporting with curated editable trip starters.

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
- `AiChatItem.CuratedTripRow`
- `AiChatItem.RecommendationRow`
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
- current recommendation groups
- active session id

Likely new or changed files:

- `data/ai/chat/AiChatSessionState.kt`
- `data/ai/chat/AiChatSessionStore.kt`
- `ui/main/aichat/AiChatViewModel.kt`

## 3. Prompt Factory

The LLM should receive a structured prompt that asks it to return:

- acknowledgement copy
- next follow-up question
- response card options
- optional curated trip starter suggestions
- optional recommendation intents

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
- curated trip starter rows
- later recommendation rows

### Composer

- draft selected-card tray
- optional text input
- single send action for text + selected cards

Likely files:

- `ui/main/aichat/AiTripChatPage.kt`
- `ui/main/aichat/components/AiPromptCardGrid.kt`
- `ui/main/aichat/components/AiResponseCardGroup.kt`
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

- pending

Goal:

- let the AI quickly suggest editable trip skeletons for popular destinations

Changes:

- curated destination catalog
- editable starter trip cards
- duration-change flows
- follow-up refinement around budget, pace, food, hotel area, and activity mix

Files most likely affected:

- new `data/ai/chat/AiCuratedTrip*`
- `ui/main/aichat/AiChatViewModel.kt`
- `ui/main/aichat/components/*`

### Phase 2: Structured LLM Responses

Status:

- pending

Goal:

- make AI outputs deterministic enough to drive card-first UI

Changes:

- prompt factory extraction
- JSON response parsing
- explicit follow-up card payloads
- curated trip surfacing decisions

Files most likely affected:

- `data/ai/chat/AiChatPromptFactory.kt`
- `data/ai/chat/AiChatResponseParser.kt`
- `data/ai/chat/AiChatCoordinator.kt`

### Phase 3: Recommendation Cards

Status:

- pending

Goal:

- show real restaurant/activity/hotel/flight recommendations in-card

Changes:

- provider orchestration through Yelp/Serp
- recommendation rows
- editable option actions

Files most likely affected:

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

The next concrete implementation slice should be Phase 1C:

1. scaffold curated editable trip cards for the initial hotspot set
2. add destination-specific starter trip shells for Bali, Tokyo, Paris, Rome, Barcelona, Honolulu, Cancun, and Bangkok
3. let the AI surface those trip cards as editable starting points deeper in the flow
4. add duration adjustment and trip-detail refinement once a starter is chosen

## Next Recommended Steps

1. Add a dedicated motion pass so user bubbles, assistant bubbles, and follow-up card groups animate into place instead of relying only on list scrolling.
2. Tune the scroll behavior against real device keyboard dismissal so the thread settles consistently without overshooting or feeling locked near the bottom.
3. Finish a full icon audit for all starter and response-card themes so every visible symbol is literal, travel-relevant, and consistent.
4. Start Phase 1C with curated trip starter cards, since the onboarding shell is now stable enough to support destination-specific entry points.
