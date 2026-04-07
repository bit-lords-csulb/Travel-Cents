# Travel Cents — Codebase Map

## Package Root
`app/src/main/java/com/example/travelcents/`

## Key Files by Area

### Auth
- `ui/auth/AuthViewModel.kt` — signUp / logIn / signOut, StateFlow state
- `ui/auth/LoginPage.kt` / `SignUpPage.kt` — login + registration forms
- `data/AuthModel.kt` — Firebase Auth + Firestore user creation

### Trip Planning (AI Pipeline)
- `ui/main/NewTripPage.kt` — form UI (origin, dest, dates, budget, interests)
- `ui/main/NewTripViewModel.kt` — form state, calls GroqRepository, saves to Firestore
- `data/remote/GroqRepository.kt` — two-call Groq pipeline: generateItinerary() + generateEvents()
- `data/remote/GroqApiService.kt` — Retrofit interface for trip pipeline
- `data/model/TravelRequest.kt` / `Itinerary.kt` / `TravelEvent.kt` — data models

### Itinerary Screen
- `ui/main/ItineraryScreen.kt` — events grouped by date, type-specific cards (flight/hotel/restaurant/activity)
- `ui/main/ItineraryViewModel.kt` — real-time Firestore listener; exposes events, tripTitle, currentTripId

### AI Chat
- `ui/main/AiTripChatPage.kt` — conversational travel assistant UI
- `ui/main/ChatViewModel.kt` — Groq multi-turn chat (NOT the group chat ViewModel)
- `data/GroqApi.kt` — separate Retrofit instance for chat (base URL differs from pipeline)

### Group Chats
- `ui/main/chats/ChatsPage.kt` / `ChatPage.kt` — group list + message thread
- `ui/main/chats/ChatsViewModel.kt` / `ChatViewModel.kt` — Firestore listeners (different from AI ChatViewModel)
- `data/FirestoreRepository.kt` — all Firestore ops: users, friends, groups, messages

### Navigation
- `TravelCentsNavigation.kt` — top-level: login → signup → home (MainScaffold)
- `ui/main/MainScaffold.kt` — bottom nav (5 tabs) + inner NavHost

## Firestore Structure
```
users/{uid}/trips/{itineraryId}          ← Itinerary doc
users/{uid}/trips/{itineraryId}/events/  ← TravelEvent subcollection
groups/{groupId}/messages/               ← group chat
```

## Patterns
- ViewModels use `StateFlow` + sealed `UiState` (Idle/Loading/Success/Error)
- Firebase imports: `com.google.firebase.Firebase` (NOT `.ktx.Firebase` — removed in SDK 32+)
- Groq API key: `BuildConfig.GROQ_API_KEY` sourced from `local.properties`
- Firestore listeners are deferred until auth is confirmed (see Phase 1.2 commit)

## Theme
`DeepSea` palette: `0xFF0D1B2A` (bg) → `0xFFE0E1DD` (text). Defined in `ui/theme/`.

## Known Gaps
- `HomePage.kt`, `SettingsPage.kt`, `ForgotPassword.kt` — placeholders, not implemented