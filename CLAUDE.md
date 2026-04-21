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

All values are injected as `BuildConfig` fields at build time via `app/build.gradle.kts`.

## Package Root

`app/src/main/java/com/example/travelcents/`

## Key Files by Area

### Shared UI Components
- `ui/components/TcTextField.kt` — **standard text field for the whole app**. Rounded filled style matching the NewTrip wizard. Use this everywhere instead of raw `TextField`. Params: `value`, `onValueChange`, `label`, `placeholder`, `enabled`, `leadingIcon`, `trailingIcon`, `visualTransformation`, `keyboardOptions`, `keyboardActions`.
- `ui/components/Button.kt` — shared button component
- `ui/components/ProfileAvatar.kt` — avatar component
- `ui/modules/TripPhotoGallery.kt` — photo gallery dialog + helpers
- `ui/modules/CalendarDateTimeUtils.kt` — shared date/time formatting utilities
- `ui/modules/TripStaticMap.kt` — `rememberStaticMapModel()` + `prefetchStaticMaps()`: loads and caches OpenStreetMap static map tiles per event

### Auth
- `ui/auth/AuthViewModel.kt` — signUp / logIn / signOut, StateFlow state
- `ui/auth/LoginPage.kt` / `SignUpPage.kt` — login + registration forms (both use `TcTextField`)
- `ui/auth/GoogleAuthButton.kt` — Google sign-in button
- `data/auth/AuthRepository.kt` — Firebase Auth + Firestore user creation

### Trip Planning (AI Pipeline)
- `ui/main/newTrip/NewTripLandingPage.kt` — landing with options (plan, AI chat, last trip)
- `ui/main/newTrip/TripStep1DestinationPage.kt` through `TripStep5InterestsPage.kt` — 5-step wizard pages
- `ui/main/newTrip/TripGeneratingPage.kt` — loading screen shown during generation
- `ui/main/newTrip/NewTripViewModel.kt` — wizard state + AI itinerary pipeline + Firestore save
- `ui/main/newTrip/TripWizardColors.kt` — color palette used by the wizard and `TcTextField`
- `data/ai/repository/TripPlannerRepository.kt` — LLM-backed itinerary metadata generation
- `data/ai/remote/LlmClient.kt` — shared OpenAI-compatible AI client
- `data/ai/remote/LlmApiService.kt` — Retrofit interface for LLM completions
- `data/ai/remote/LlmConfig.kt` — reads `BuildConfig.LLM_*` values
- `data/ai/model/LlmModels.kt` — shared request/response DTOs for AI completions
- `data/trip/model/TravelRequest.kt` / `Itinerary.kt` / `TravelEvent.kt` / `EventOption.kt` — core trip data models
- `data/trip/model/TripPreview.kt` — lightweight trip summary for list views
- `data/trip/remote/SerpRepository.kt` — Google Flights + Hotels search via SerpAPI
- `data/trip/remote/SerpApiService.kt` / `SerpCache.kt` — Retrofit interface + in-memory cache
- `data/trip/model/SerpModels.kt` — SerpAPI response DTOs
- `data/trip/remote/YelpRepository.kt` — restaurant + activity search via Yelp Fusion
- `data/trip/remote/YelpApiService.kt` — Retrofit interface for Yelp
- `data/trip/model/YelpModels.kt` — Yelp response DTOs
- `data/trip/remote/DestinationImageRepository.kt` — hero image fetching for destinations
- `data/trip/remote/CurrencyApiService.kt` — exchange rate API
- `data/trip/local/CurrencyRateCache.kt` — persisted currency rate cache
- `data/media/ImageCacheManager.kt` — downloads and caches trip images (hero photos + static maps) locally
- `data/media/StaticMapUrlFactory.kt` — builds OpenStreetMap static map tile URLs (no API key required); provider constant: `PROVIDER = "osm_staticmap"`
- `data/trip/model/EventDetailContract.kt` — canonical `ATTR_*` string constants for all `details` map keys; extension functions `detailValue()`, `firstNonBlank()`, `displayName()` for `TravelEvent` and `EventOption`

### Current Trip / Itinerary
- `ui/main/current/screen/CurrentTripScreen.kt` — top-level screen; composes all current-trip views
- `ui/main/current/CurrentTripViewModel.kt` — loads active trip, exposes events + display mode
- `ui/main/current/navigation/CurrentTripRoutes.kt` — route constants for current trip sub-nav
- `ui/main/current/itinerary/CurrentTripItineraryContent.kt` — itinerary list view (event cards)
- `ui/main/current/calendar/view/CurrentTripDayView.kt` — single-day calendar view
- `ui/main/current/calendar/view/CurrentTripWeekView.kt` — week calendar view
- `ui/main/current/calendar/CurrentTripCalendarLayoutUtils.kt` — layout math for calendar grid
- `ui/main/current/header/CurrentTripHeader.kt` / `CurrentTripHeroLayout.kt` / `DayDateHero.kt` / `WeekDateHero.kt` — header components per mode
- `ui/main/current/components/CurrentTripChrome.kt` / `CurrentTripModeSwitcher.kt` / `CurrentTripStatusViews.kt` — shared chrome, mode toggle, empty/error states
- `ui/main/current/overlays/CurrentTripEventDetailsDialog.kt` / `CurrentTripOverlayHost.kt` / `EventOptionsPanel.kt` — event detail overlays
- `ui/main/current/editor/CurrentPlanEditorDialog.kt` — inline event editor
- `ui/main/current/helpers/CurrentTripPlanUtils.kt` — utility functions for plan manipulation
- `ui/main/current/sharing/CurrentTripShareSheet.kt` — share sheet for exporting a trip
- `ui/main/itinerary/ItineraryViewModel.kt` — real-time Firestore listener; exposes `events`, `tripTitle`, `currentTripId`; lazily enriches events with Yelp business details and Yelp reviews on demand
- `ui/main/itinerary/ExpandedEventCard.kt` — full-detail bottom sheet for a single event; renders static map tile, Yelp details, and photos
- `ui/main/itinerary/SharedTripHeader.kt` — shared header used by itinerary views

### Home
- `ui/main/home/HomePage.kt` — home screen: saved trips, quick actions
- `ui/main/home/HomeViewModel.kt` — loads trip previews from Firestore
- `ui/main/home/CurrencyConverterCard.kt` — inline currency converter widget
- `ui/main/home/CurrencyViewModel.kt` — fetches live exchange rates

### AI Chat
- `ui/main/aichat/AiTripChatPage.kt` — conversational travel assistant UI
- `ui/main/aichat/AiChatViewModel.kt` — AI multi-turn chat **(NOT the group chat ViewModel)**
- `data/ai/ChatMessage.kt` — `ChatMessage(text, isFromUser)` data class

### Group Chats
- `ui/main/chats/chat/ChatsPage.kt` / `ChatPage.kt` — group list + message thread
- `ui/main/chats/chat/ChatsViewModel.kt` / `ChatViewModel.kt` — Firestore listeners **(different from AI ChatViewModel)**
- `ui/main/chats/chat/DirectChatPage.kt` / `DirectChatViewModel.kt` — 1:1 direct messages
- `ui/main/chats/friends/FriendsPage.kt` / `AddFriendPage.kt` / `FriendRequestsPage.kt` — friends management
- `ui/main/chats/friends/FriendsViewModel.kt` / `AddFriendViewModel.kt` / `FriendRequestsViewModel.kt`
- `ui/main/chats/voting/EventsPage.kt` / `CreateEventPage.kt` / `EventCommentsPage.kt` — group trip voting
- `ui/main/chats/voting/EventsViewModel.kt` / `CreateEventViewModel.kt` / `EventCommentsViewModel.kt`
- `data/social/repository/FriendsRepository.kt` — friend requests + accepted friends
- `data/social/repository/GroupsRepository.kt` — group CRUD + membership
- `data/social/repository/DirectMessagesRepository.kt` — 1:1 DM threads
- `data/social/repository/SocialUserRepository.kt` — user search and profile lookups
- `data/social/repository/SocialRepositoryMappers.kt` — Firestore ↔ model conversions
- `data/social/model/Friend.kt` / `Group.kt` / `Message.kt` / `DirectChatPreview.kt`

### Settings
- `ui/main/settings/SettingsPage.kt` — entry point: profile header (tappable → Account tab) + tab selector. Default tab: Account.
- `ui/main/settings/SettingsViewModel.kt` — `SettingsUserState` (firstName, lastName, username, email). Actions: `loadUser`, `updateProfile`, `signOut`, `deleteAccount`.
- `ui/main/settings/AccountTab.kt` — profile info card, inline edit form, Danger Zone (logout + delete)
- `ui/main/settings/SecurityTab.kt` — change password (3 independent visibility toggles) + Privacy & Security
- `ui/main/settings/PreferencesTab.kt` — notifications, display, About section
- `ui/main/settings/SettingsComponents.kt` — shared: `SettingCard`, `SettingHeader`, `SwitchSettingItem`, `SettingRow`

### User Profile
- `data/user/UserProfileRepository.kt` — read/write user profile in Firestore
- `data/user/model/CurrentUserProfile.kt` — in-memory user profile model

### Navigation
- `TravelCentsNavigation.kt` — top-level: login → signup → home (MainScaffold)
- `ui/main/MainScaffold.kt` — bottom nav (5 tabs) + inner NavHost; route constants in `MainRoutes`

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
current / current_itinerary / current_day / current_week → CurrentTripScreen (mode-switched)
new_trip        → NewTripLandingPage
new_trip_step1  → TripStep1DestinationPage
new_trip_step2  → TripStep2DatesPage
new_trip_step3  → TripStep3TravelersPage
new_trip_step4  → TripStep4BudgetPage
new_trip_step5  → TripStep5InterestsPage
trip_generating → TripGeneratingPage
home            → HomePage
chats           → ChatsScreen
settings        → SettingsPage
ai_trip_chat    → AiTripChatPage
```

## Firestore Structure

```
users/{uid}
    firstName, lastName, username, email, uid, createdAt

users/{uid}/trips/{itineraryId}
    itineraryId, userId, tripName, destination, origin
    originIata, destinationIata
    dateFrom, dateTo, durationDays, currency, travelStyle, adults, children
    createdAt, status, eventIds[]

users/{uid}/trips/{itineraryId}/events/{eventId}
    eventId, type, itineraryId, tz, date, startTime, endTime
    imageUrl, localImagePath, photoUrls[]
    + all details fields flattened (airline, flight_number, hotel_name, cuisine, etc.)

users/{uid}/trips/{itineraryId}/events/{eventId}/options/{optionId}
    optionId, eventId, source, selected, imageUrl, photoUrls[], details{}

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
      data class Loading(val statusMessage: String = "Getting things ready...") : TripUiState()
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
| Shared transport | `data/ai/remote/LlmClient.kt` + `LlmApiService.kt` | OpenAI-compatible chat completions |
| Config | `data/ai/remote/LlmConfig.kt` | Reads `BuildConfig.LLM_*` at runtime |
| Itinerary generation | `data/ai/repository/TripPlannerRepository.kt` | Prompts + JSON parsing for itinerary metadata |
| AI chat | `ui/main/aichat/AiChatViewModel.kt` | Multi-turn chat completions via the shared AI client |

The default provider is Groq, but the app is wired through `LLM_BASE_URL` and `LLM_API_KEY` so compatible providers can be swapped without changing Kotlin code.

The itinerary system prompt: `"You are a travel planner. Always respond with valid JSON only. No markdown, no extra text."`

## Naming Collision Warning

| File | Package | Purpose |
|---|---|---|
| `ui/main/aichat/AiChatViewModel.kt` | `ui.main.aichat` | AI chat (LLM, multi-turn) |
| `ui/main/chats/chat/ChatViewModel.kt` | `ui.main.chats.chat` | Group chat (Firestore) |
| `ui/main/newTrip/NewTripViewModel.kt` | `ui.main.newTrip` | AI itinerary pipeline + Firestore save |
| `ui/main/chats/groups/NewTripViewModel.kt` | `ui.main.chats.groups` | Group creation ViewModel |

Always check the package when importing.

## Event Cards

Events are rendered by type. Color scheme in `FinalPlan.kt` / `ExpandedEventCard.kt`:

| Type | Accent | Key `details` Fields |
|---|---|---|
| `flight` | Blue `#64B5F6` | airline, flight_number, origin_airport, destination_airport, departure_time, arrival_time, total_price, flight_duration_min |
 | `hotel` | Purple `#B5A0FF` | `ATTR_HOTEL_NAME`, check_in_date, check_out_date, `ATTR_RATE_PER_NIGHT`, `ATTR_AMENITIES`, `ATTR_BOOKING_URL` |
| `restaurant` / `dining` / `food` | Red `#FF716C` | `ATTR_BUSINESS_NAME`, `ATTR_CATEGORIES`, `ATTR_AVERAGE_RATING`, `ATTR_HOURS_SUMMARY`, `ATTR_YELP_URL`, `ATTR_MENU_URL` |
| `activity` (default) | Light blue `#D5E3FB` | `ATTR_BUSINESS_NAME`, activity_name, title, location, `ATTR_YELP_URL` |

All typed `ATTR_*` constants live in `data/trip/model/EventDetailContract.kt`. Use `detailValue(ATTR_FOO, "legacy_key")` (not raw string literals) for new code — it falls back through the list and returns the first non-blank value.

Title resolution: use `TravelEvent.displayName()` / `EventOption.displayName(eventType)` from `EventDetailContract.kt` rather than manual key lookups.

Static map fields: `ATTR_STATIC_MAP_URL`, `ATTR_STATIC_MAP_PROVIDER` (value: `"osm_staticmap"`), `ATTR_LATITUDE`, `ATTR_LONGITUDE`. Use `rememberStaticMapModel(event)` from `TripStaticMap.kt` in Composables.

`TravelEvent.details` is a flat `Map<String, String>` — all type-specific fields live here. `TravelEvent.options` is a `List<EventOption>` stored in a Firestore subcollection (not in the main document).

## Theme

`DeepSea` palette defined in `ui/theme/Color.kt`:
```
DeepSea1 = 0xFF0D1B2A  // background
DeepSea2 = 0xFF1B263B  // cards / bottom nav
DeepSea3 = 0xFF415A77  // interactive / dividers
DeepSea4 = 0xFF778DA9  // secondary text / icons
DeepSea5 = 0xFFE0E1DD  // primary text
```

`TripWizardColors` (in `ui/main/newTrip/TripWizardColors.kt`) extends the palette with blues and surface variants used by the wizard and `TcTextField`.

## Known Gaps

- `ForgotPassword.kt` — not implemented