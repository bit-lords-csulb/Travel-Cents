# AI Chat Overhaul Plan

## Goals

- Replace the current basic message screen with a modern assistant workflow that feels native to the rest of the app.
- Let the assistant learn user preferences through guided back-and-forth, not just one-shot prompts.
- Present recommended restaurants, events, and activities as real event cards instead of plain text.
- Keep the AI layer vendor-agnostic by continuing to route all completions through the shared `LlmClient`.

## Product Direction

The new chat experience should act like a trip copilot, not a generic chatbot. It should:

- ask follow-up questions when the user is vague
- offer quick-reply suggestions when the user does not know what to type
- summarize what it has learned about the traveler
- recommend itinerary-ready options in card format
- let the user add, replace, or save options into an existing or new itinerary

## UI Direction

Use the app's existing visual language instead of introducing a separate chat style.

- Reuse `TripWizardColors` and shared field styling from `ui/components/TcTextField.kt`.
- Keep the DeepSea surfaces and card density consistent with itinerary and settings screens.
- Add richer interaction patterns: quick-reply chips, collapsible preference summaries, loading states, and recommendation carousels.
- Support long multi-line input, but keep one-tap suggestion actions visible near the composer.

## Architecture Changes

### 1. Conversation Model

Replace the current plain `ChatMessage` list with a richer sealed model, for example:

- `AiChatItem.TextMessage`
- `AiChatItem.PreferencePrompt`
- `AiChatItem.PreferenceSummary`
- `AiChatItem.RecommendationRow`
- `AiChatItem.SystemStatus`

This is the key change that allows the chat to render both text and itinerary-style cards.

### 2. Session State

Create a dedicated chat session state in `AiChatViewModel`:

- user profile/preferences collected so far
- current conversation stage
- visible quick replies
- pending recommendation cards
- target itinerary id when the chat is attached to an existing trip

### 3. Preference Memory

Add a structured profile object the assistant can keep updating:

- budgets
- cuisine preferences
- activity interests
- travel pace
- mobility/accessibility needs
- party composition
- disliked categories

The LLM should receive both the raw chat history and the structured profile summary.

### 4. Recommendation Pipeline

Split recommendation generation into two layers:

- LLM layer: decides what categories and constraints to search for
- data layer: uses Yelp/Serp/current itinerary data to build candidate `TravelEvent` options

This keeps the LLM responsible for reasoning while existing providers remain responsible for actual places and prices.

### 5. Card Reuse

Extract reusable event card composables from the itinerary stack instead of rebuilding card UI inside chat.

Primary reuse candidates:

- `ui/main/itinerary/ExpandedEventCard.kt`
- the itinerary event card styling used by current trip/final plan screens

The chat should render recommendation cards with actions like:

- `Add to trip`
- `Replace current option`
- `Save for later`
- `Show similar`

## Delivery Phases

### Phase 1: Interaction Foundation

- Replace the existing chat list with richer AI chat item rendering.
- Add quick-reply chips and guided onboarding questions.
- Add a structured preference summary card that updates live.

### Phase 2: Recommendation Cards

- Connect the chat to itinerary-aware recommendation generation.
- Render restaurant/activity/event suggestions as event cards.
- Support add/replace/save actions from chat into Firestore-backed trips.

### Phase 3: Multi-Itinerary Copilot

- Let the chat target either a new trip draft or an existing itinerary.
- Add context-aware follow-ups like "swap Friday dinner" or "find a cheaper hotel area".
- Add recommendation refresh flows based on accepted/rejected cards.

## Implementation Notes

- Keep the route `ai_trip_chat` so navigation does not need to change.
- Keep all AI completions behind `LlmClient`.
- Do not fork a second card design system for chat. Reuse itinerary components wherever possible.
- Avoid hard-coding provider names in the UI. The assistant should be branded as TravelCents AI, not as the underlying vendor.
