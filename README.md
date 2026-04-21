# Travel Cents

An AI-powered travel planning Android app built with Jetpack Compose and Kotlin. Plan trips, generate itineraries with AI, collaborate with friends, and vote on group activities.

---

## What It Does

- **Generate a full trip itinerary** — fill out a form and the app uses an OpenAI-compatible LLM for itinerary metadata, then enriches the trip with live flights, hotels, restaurants, and activities.
- **Chat with an AI travel assistant** — conversational planning powered by a configurable AI provider.
- **View and edit your itinerary** — type-specific event cards grouped by day, with inline edit/delete and a sleek timeline view.
- **Group chats** — real-time messaging with friends in a trip group.
- **Direct messages** — 1-on-1 chat with any friend.
- **Friends system** — search users, send/accept friend requests.
- **Group event voting** — propose activities for a group trip, upvote/downvote, and comment on each suggestion.

---

## How to Run

### Prerequisites

- Android Studio Hedgehog or newer.
- Android device or emulator (API 24+).
- An AI provider API key for any OpenAI-compatible endpoint.
- A [SerpAPI key](https://serpapi.com/) for live flight and hotel prices.
- A Yelp Fusion API key for activity suggestions.
- A Firebase project with **Authentication** (email/password) and **Firestore** enabled.

### Setup

1. Clone the repo:
   ```bash
   git clone https://github.com/bit-lords-csulb/Travel-Cents.git
   ```

2. Add your API keys to `local.properties` (git-ignored, create if it doesn't exist):
   ```
   LLM_API_KEY=your-llm-key
   LLM_BASE_URL=https://api.groq.com/openai/v1/
   LLM_MODEL=llama-3.3-70b-versatile
   SERP_API_KEY=your-serpapi-key
   YELP_API_KEY=your-yelp-key
   ```

   `GROQ_API_KEY` is still accepted as a fallback for older local setups.

3. Add your Firebase config:
   - Download `google-services.json` from your Firebase console.
   - Place it in `app/`.

4. Open in Android Studio, let Gradle sync, then **Run**.

---

## Features

### Authentication
- Email/password sign-up and login.
- Username lookup via Firestore on login.
- Input validation, loading states, and error messages.

### Trip Planning
- Form: origin, destination, dates, adults/children, travel style, budget + currency, dietary preferences, interests, special requests.
- AI itinerary pipeline:
  1. Generates trip metadata (name, dates, duration, IATA codes).
  2. Uses SerpAPI for flights and hotels.
  3. Uses Yelp for restaurants, activities, and local events.
- SerpAPI enriches results with real flight and hotel pricing.
- Saves itinerary + all events to Firestore.

### Itinerary Viewer & Editor
- Events grouped by date with day headers.
- Four event card types: **Flight**, **Hotel**, **Restaurant**, and **Activity**.
- Real-time Firestore listener.
- Inline edit, add, and delete events.
- **Expanded event detail sheet** — Yelp ratings, hours, photos, booking/menu links, and an embedded static map for hotels, restaurants, and activities.
- **Final Plan** view for a polished, read-only or shareable summary of the trip.

### AI Chat
- Conversational travel assistant powered by the configured AI provider.
- Full multi-turn chat history.
- Typing indicator for a more responsive feel.

### Social & Chats
- **Group chat** — create trip groups, real-time messaging, last message preview.
- **Direct messages** — 1-on-1 chat with friends.
- **Friends** — search users, send/accept/reject friend requests, view pending requests.

### Group Event Voting
- Propose activities for a group trip with a title, description, location, time, and photo.
- Yelp API autocomplete for place search; Wikipedia fallback for descriptions.
- Upvote / downvote each proposal; vote count displayed live.
- Comment threads on each proposed event.
- Creator can delete their own proposals.

---

## Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material3 |
| Language | Kotlin |
| State | ViewModel + StateFlow |
| Navigation | Jetpack Navigation Compose |
| Auth | Firebase Authentication |
| Database | Firebase Firestore |
| AI | Configurable OpenAI-compatible provider |
| Flight & Hotel Data | SerpAPI |
| Activity Search | Yelp Fusion API |
| Activity Descriptions | Wikipedia REST API |
| Static Maps | OpenStreetMap (no API key) |
| HTTP (AI/Serp) | Retrofit 2 + OkHttp 4 |
| HTTP (Yelp/Wikipedia) | Ktor Client |
| Serialization | Gson |
| Images | Coil |
| Min SDK | 24 (Android 7.0) |

---

## Project Structure

```
Travel-Cents/
├── app/
│   ├── google-services.json             # Firebase configuration file
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml      # App manifest
│       │   ├── java/com/example/travelcents/
│       │   │   ├── MainActivity.kt      # Entry point activity
│       │   │   ├── data/                # Data layer: Repositories, APIs, and Core Models
│       │   │   │   ├── model/           # DTOs and internal data structures (Itinerary, TravelEvent, etc.)
│       │   │   │   ├── remote/          # API services (AI provider, Serp, Yelp) and their repositories
│       │   │   │   ├── AuthModel.kt     # Data structures for user authentication
│       │   │   │   ├── ChatMessage.kt   # Model for chat messages
│       │   │   │   ├── FirestoreRepository.kt # Central hub for Firebase Firestore operations
│       │   │   │   ├── model/LlmModels.kt       # Shared request/response models for the AI provider
│       │   │   │   ├── remote/LlmClient.kt      # Shared OpenAI-compatible AI client
│       │   │   │   ├── remote/TripPlannerRepository.kt # Itinerary metadata generation prompts/parsing
│       │   │   │   ├── Trip.kt          # Main Trip data class
│       │   │   │   └── TripEvent.kt     # Model for individual trip events
│       │   │   └── ui/                  # UI layer: Composables and ViewModels
│       │   │       ├── auth/            # Authentication flow (Login, SignUp, ForgotPassword)
│       │   │       ├── theme/           # App styling (Colors, Typography, Shapes)
│       │   │       ├── main/            # Core application features post-login
│       │   │       │   ├── aichat/      # AI assistant chat interface and logic
│       │   │       │   ├── chats/       # Social hub (Direct messages, Group chats, Friends, Voting)
│       │   │       │   │   ├── chat/    # Individual and list-view chat screens
│       │   │       │   │   ├── friends/ # Friend management (Add, Requests, List)
│       │   │       │   │   ├── groups/  # Group-specific trip chat views
│       │   │       │   │   └── voting/  # Group event proposal and voting system
│       │   │       │   ├── itinerary/   # Itinerary management (View, Edit, Final Plan)
│       │   │       │   ├── newtrip/     # Multi-step trip creation wizard
│       │   │       │   ├── CurrentPage.kt  # Root for main content navigation
│       │   │       │   ├── HomePage.kt     # User dashboard with trip summaries
│       │   │       │   ├── MainScaffold.kt # Main layout shell with bottom navigation
│       │   │       │   └── SettingsPage.kt # User settings and profile options
│       │   │       └── TravelCentsNavigation.kt # Top-level app navigation graph
│       │   └── res/                     # Resources (drawables, layouts, values)
│       └── test/                        # Unit tests
└── build.gradle.kts                     # Project-level build configuration
```
