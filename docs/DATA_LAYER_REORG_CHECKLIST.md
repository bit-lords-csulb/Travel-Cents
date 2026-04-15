# Data Layer Reorganization Checklist

## Goal

Separate UI code from non-UI code more clearly and make the backend/data layer easier to approach by organizing it by feature instead of leaving unrelated files at `data/` root.

This checklist is the source of truth for the refactor so the work can continue safely even if chat context is lost later.

## Current Problems

- `data/` root mixes auth, AI chat state, Firestore social logic, image caching, and legacy trip models.
- `data/model/` mixes app/domain models with provider DTOs.
- `data/remote/` mixes real remote clients with caches and feature repositories.
- Some files appear legacy or duplicate the newer trip model layer:
  - `data/Trip.kt`
  - `data/TripEvent.kt`
  - `data/MockItineraryData.kt`

## Target Phase Structure

Phase 1 reorganizes only non-UI code. UI packages stay where they are for now.

```text
data/
├── ai/
│   ├── model/
│   ├── remote/
│   └── repository/
├── auth/
├── media/
├── social/
│   └── model/
├── trip/
│   ├── local/
│   ├── model/
│   ├── remote/
│   └── legacy/
└── user/
    └── model/
```

## Phase 1 File Map

### AI

- `data/ChatMessage.kt` -> `data/ai/ChatMessage.kt`
- `data/model/LlmModels.kt` -> `data/ai/model/LlmModels.kt`
- `data/remote/LlmApiService.kt` -> `data/ai/remote/LlmApiService.kt`
- `data/remote/LlmClient.kt` -> `data/ai/remote/LlmClient.kt`
- `data/remote/LlmConfig.kt` -> `data/ai/remote/LlmConfig.kt`
- `data/remote/TripPlannerRepository.kt` -> `data/ai/repository/TripPlannerRepository.kt`

### Auth

- `data/AuthModel.kt` -> `data/auth/AuthModel.kt`

### Media

- `data/ImageCacheManager.kt` -> `data/media/ImageCacheManager.kt`

### Social

- `data/FirestoreRepository.kt` -> `data/social/FirestoreRepository.kt`
- `data/model/DirectChatPreview.kt` -> `data/social/model/DirectChatPreview.kt`
- `data/model/Group.kt` -> `data/social/model/Group.kt`
- `data/model/Message.kt` -> `data/social/model/Message.kt`

### Trip

- `data/local/CurrencyRateCache.kt` -> `data/trip/local/CurrencyRateCache.kt`
- `data/model/Event.kt` -> `data/trip/model/Event.kt`
- `data/model/EventOption.kt` -> `data/trip/model/EventOption.kt`
- `data/model/Itinerary.kt` -> `data/trip/model/Itinerary.kt`
- `data/model/SerpModels.kt` -> `data/trip/model/SerpModels.kt`
- `data/model/TravelEvent.kt` -> `data/trip/model/TravelEvent.kt`
- `data/model/TravelRequest.kt` -> `data/trip/model/TravelRequest.kt`
- `data/model/TripPreview.kt` -> `data/trip/model/TripPreview.kt`
- `data/model/YelpModels.kt` -> `data/trip/model/YelpModels.kt`
- `data/remote/CurrencyApiService.kt` -> `data/trip/remote/CurrencyApiService.kt`
- `data/remote/DestinationImageRepository.kt` -> `data/trip/remote/DestinationImageRepository.kt`
- `data/remote/SerpApiService.kt` -> `data/trip/remote/SerpApiService.kt`
- `data/remote/SerpCache.kt` -> `data/trip/remote/SerpCache.kt`
- `data/remote/SerpRepository.kt` -> `data/trip/remote/SerpRepository.kt`
- `data/remote/WikipediaApiService.kt` -> `data/trip/remote/WikipediaApiService.kt`
- `data/remote/YelpApiService.kt` -> `data/trip/remote/YelpApiService.kt`
- `data/remote/YelpRepository.kt` -> `data/trip/remote/YelpRepository.kt`

### User

- `data/UserProfileRepository.kt` -> `data/user/UserProfileRepository.kt`
- `data/model/CurrentUserProfile.kt` -> `data/user/model/CurrentUserProfile.kt`

### Legacy Holding Area

Move these out of the way but do not delete them during Phase 1:

- `data/Trip.kt` -> `data/trip/legacy/Trip.kt`
- `data/TripEvent.kt` -> `data/trip/legacy/TripEvent.kt`
- `data/MockItineraryData.kt` -> `data/trip/legacy/MockItineraryData.kt`

## Rules For This Refactor

- Do not mix UI moves with data-layer moves in the same pass.
- After each move batch, fix imports immediately.
- Compile after the batch using:
  - `.\gradlew.bat :app:compileDebugKotlin`
- Do not delete legacy files until imports/usages are confirmed dead.
- Avoid behavior changes while reorganizing packages.

## Expected Import Changes

Examples:

- `com.example.travelcents.data.ChatMessage` -> `com.example.travelcents.data.ai.ChatMessage`
- `com.example.travelcents.data.AuthModel` -> `com.example.travelcents.data.auth.AuthModel`
- `com.example.travelcents.data.FirestoreRepository` -> `com.example.travelcents.data.social.FirestoreRepository`
- `com.example.travelcents.data.UserProfileRepository` -> `com.example.travelcents.data.user.UserProfileRepository`
- `com.example.travelcents.data.ImageCacheManager` -> `com.example.travelcents.data.media.ImageCacheManager`
- `com.example.travelcents.data.model.Itinerary` -> `com.example.travelcents.data.trip.model.Itinerary`
- `com.example.travelcents.data.remote.SerpRepository` -> `com.example.travelcents.data.trip.remote.SerpRepository`
- `com.example.travelcents.data.remote.LlmClient` -> `com.example.travelcents.data.ai.remote.LlmClient`

## Phase 2 Candidates

Do this only after Phase 1 compiles cleanly:

- Rename `AuthModel` to `AuthRepository`
- Split `FirestoreRepository` by domain:
  - friends
  - groups
  - direct messages
- Move UI packages from `ui/main/...` to `ui/feature/...`
- Extract `Friend` from `FriendsPage.kt` into a proper social model package
- Delete or replace legacy trip files after confirmed unused

## Done Criteria For Phase 1

- No remaining feature-specific files at `data/` root except intentionally deferred ones
- Imports updated across the app
- Kotlin compile succeeds
- This checklist remains committed and up to date
