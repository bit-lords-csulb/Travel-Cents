# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run all unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Clean build
./gradlew clean assembleDebug
```

Run these from the repo root. On Windows use `gradlew.bat` or `.\gradlew` in PowerShell.

## API Key Setup

In `local.properties` (never commit):
```
LLM_API_KEY=your-key-here
LLM_BASE_URL=https://api.groq.com/openai/v1/
LLM_MODEL=llama-3.3-70b-versatile
SERP_API_KEY=your-key-here
YELP_API_KEY=your-key-here
```

`GROQ_API_KEY` is still accepted as a fallback for older local setups.

All values are injected as `BuildConfig` fields at build time via `app/build.gradle.kts`.

## Package Root

`app/src/main/java/com/example/travelcents/`

## Key Files by Area

### Shared UI Components
- `ui/components/TcTextField.kt` — **standard text field for the whole app**. Rounded filled style matching the NewTrip wizard. Use this everywhere instead of raw `TextField`. Params: `value`, `onValueChange`, `label`, `placeholder`, `enabled`, `leadingIcon`, `trailingIcon`, `visualTransformation`, `keyboardOptions`, `keyboardActions`.

### Auth
- `ui/auth/AuthViewModel.kt` — signUp / logIn / signOut, StateFlow state
- `ui/auth/LoginPage.kt` / `SignUpPage.kt` — login + registration forms (both use `TcTextField`)
- `data/AuthModel.kt` — Firebase Auth + Firestore user creation

### Trip Planning (AI Pipeline)
- `ui/main/newTrip/NewTripLandingPage.kt` — landing with options (plan, AI chat, last trip)
- `ui/main/newTrip/TripStep1–5*.kt` — 5-step wizard pages
- `ui/main/newTrip/NewTripViewModel.kt` — wizard state + AI itinerary pipeline + Firestore save
- `ui/main/newTrip/TripWizardColors.kt` — color palette used by the wizard and `TcTextField`
- `data/remote/TripPlannerRepository.kt` — LLM-backed itinerary metadata generation
- `data/remote/LlmClient.kt` — shared OpenAI-compatible AI client
- `data/model/LlmModels.kt` — shared request/response models for AI completions
- `data/model/TravelRequest.kt` / `Itinerary.kt` / `TravelEvent.kt` — data models

### Current Trip / Itinerary
- `ui/main/current/screen/CurrentTripScreen.kt` — main itinerary/day/week views
- `ui/main/current/CurrentTripViewModel.kt` — loads trip, exposes events + display mode
- `ui/main/itinerary/FinalPlan.kt` — full itinerary card view
- `ui/main/itinerary/ItineraryViewModel.kt` — real-time Firestore listener; exposes `events`, `tripTitle`, `currentTripId`

### AI Chat
- `ui/main/aichat/AiTripChatPage.kt` — conversational travel assistant UI
- `ui/main/aichat/AiChatViewModel.kt` — AI multi-turn chat **(NOT the group chat ViewModel)**
- `data/remote/LlmClient.kt` — shared OpenAI-compatible client used by both itinerary generation and AI chat

### Group Chats
- `ui/main/chats/chat/ChatsPage.kt` / `ChatPage.kt` — group list + message thread
- `ui/main/chats/chat/ChatsViewModel.kt` / `ChatViewModel.kt` — Firestore listeners **(different from AI ChatViewModel)**
- `data/FirestoreRepository.kt` — all Firestore ops: users, friends, groups, messages

### Settings
- `ui/main/settings/SettingsPage.kt` — entry point: profile header (tappable → Account tab) + tab selector. Default tab: Account.
- `ui/main/settings/SettingsViewModel.kt` — `SettingsUserState` (firstName, lastName, username, email). Actions: `loadUser`, `updateProfile`, `signOut`, `deleteAccount`.
- `ui/main/settings/AccountTab.kt` — profile info card, inline edit form, Danger Zone (logout + delete)
- `ui/main/settings/SecurityTab.kt` — change password (3 independent visibility toggles) + Privacy & Security (data toggle + info)
- `ui/main/settings/PreferencesTab.kt` — notifications, display, About section
- `ui/main/settings/SettingsComponents.kt` — shared: `SettingCard`, `SettingHeader`, `SwitchSettingItem`, `SettingRow`

### Navigation
- `TravelCentsNavigation.kt` — top-level: login → signup → home (MainScaffold)
- `ui/main/MainScaffold.kt` — bottom nav (5 tabs) + inner NavHost

## Navigation Structure

### Top-Level
```
login           → LoginPage
signup          → SignUpPage
home            → MainScaffold
forgot_password → ForgotPassword (not implemented)
```

### MainScaffold Inner NavHost
```
current      → CurrentTripScreen (itinerary / day / week modes)
new_trip     → NewTripLandingPage → TripStep1–5 → TripGeneratingPage
home         → HomePage
chats        → ChatsScreen
settings     → SettingsPage
ai_trip_chat → AiTripChatPage (bottom nav hidden on this route)
```

## Firestore Structure

```
users/{uid}
    firstName, lastName, username, email, uid, createdAt

users/{uid}/trips/{itineraryId}
    itineraryId, userId, tripName, destination, origin
    dateFrom, dateTo, durationDays, currency, travelStyle, adults, children

users/{uid}/trips/{itineraryId}/events/{eventId}
    eventId, type, itineraryId, date, startTime, endTime, title, location, notes
    + type-specific fields: airline, flight_number, hotel_name, cuisine, etc.

users/{uid}/friends/{friendUid}
    status: "pending" | "accepted" | "rejected"
    direction: "sent" | "received"

groups/{groupId}
    name, members[], lastMessage, lastMessageTime

groups/{groupId}/messages/{msgId}
    text, senderId, senderName, timestamp (serverTimestamp)
```

## Architecture Patterns

- ViewModels use `StateFlow` + sealed `UiState`:
  ```kotlin
  sealed class TripUiState {
      data object Idle : TripUiState()
      data object Loading : TripUiState()
      data class Success(val itinerary: Itinerary, val events: List<TravelEvent>) : TripUiState()
      data class Error(val message: String) : TripUiState()
  }
  ```
- Firebase imports: `com.google.firebase.Firebase` — **not** `.ktx.Firebase` (removed in SDK 32+)
- AI provider config: `BuildConfig.LLM_API_KEY`, `BuildConfig.LLM_BASE_URL`, `BuildConfig.LLM_MODEL`
- Firestore listeners are deferred until auth is confirmed

## AI Provider Architecture

| Layer | File | Purpose |
|---|---|---|
| Shared transport | `LlmClient.kt` + `LlmApiService.kt` | OpenAI-compatible chat completions |
| Itinerary generation | `TripPlannerRepository.kt` | Prompting + JSON parsing for itinerary metadata |
| AI chat | `AiChatViewModel.kt` | Multi-turn chat completions via the shared AI client |

The default provider is Groq, but the app is wired through `LLM_BASE_URL` and `LLM_API_KEY` so compatible providers can be swapped without changing Kotlin code.

The itinerary system prompt remains: `"You are a travel planner. Always respond with valid JSON only. No markdown, no extra text."`

## Naming Collision Warning

There is still a naming collision around `NewTripViewModel.kt`, but the AI chat ViewModel has been renamed to avoid confusion:

| File | Package | Purpose |
|---|---|---|
| `ui/main/aichat/AiChatViewModel.kt` | `ui.main.aichat` | AI chat (LLM, multi-turn) |
| `ui/main/chats/chat/ChatViewModel.kt` | `ui.main.chats.chat` | Group chat (Firestore) |
| `ui/main/newTrip/NewTripViewModel.kt` | `ui.main.newTrip` | AI itinerary pipeline + Firestore save |
| `ui/main/chats/groups/NewTripViewModel.kt` | `ui.main.chats.groups` | Group creation ViewModel |

Always check the package when importing.

## ItineraryScreen — Event Cards

Events are dispatched by type in `EventCardDispatcher`:

| Type | Accent | Key Fields |
|---|---|---|
| `flight` | Pink `#EC4899` | airline, flight_number |
| `hotel` | Cyan `#06B6D4` | hotel_name, check-in time |
| `restaurant` | Yellow `#EAB308` | cuisine |
| `activity` | Purple `#8B5CF6` | location |

Title resolution (all cards): `details["title"]` → `details["activity_name"]` → type-specific fallback.

## Theme

`DeepSea` palette defined in `ui/theme/`:
```
DeepSea1 = 0xFF0D1B2A  // background
DeepSea2 = 0xFF1B263B  // cards / bottom nav
DeepSea3 = 0xFF415A77  // interactive / dividers
DeepSea4 = 0xFF778DA9  // secondary text / icons
DeepSea5 = 0xFFE0E1DD  // primary text
```

`TripWizardColors` (in `ui/main/newTrip/TripWizardColors.kt`) extends the palette with blues and surface variants used by the wizard and `TcTextField`.

## Known Gaps

- `HomePage.kt` — placeholder, not fully implemented
- `ForgotPassword.kt` — not implemented
