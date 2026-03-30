# Travel Cents

An AI-powered travel planning Android app built with Jetpack Compose and Kotlin. Plan trips, generate itineraries with AI, collaborate with friends, and vote on group activities.

---

## What It Does

- **Generate a full trip itinerary** — fill out a form and the app calls Groq AI to produce a day-by-day schedule with flights, hotels, restaurants, and activities
- **Chat with an AI travel assistant** — conversational planning powered by Groq
- **View and edit your itinerary** — type-specific event cards grouped by day, with inline edit/delete
- **Group chats** — real-time messaging with friends in a trip group
- **Direct messages** — 1-on-1 chat with any friend
- **Friends system** — search users, send/accept friend requests
- **Group event voting** — propose activities for a group trip, upvote/downvote, and comment on each suggestion

---

## How to Run

### Prerequisites

- Android Studio Hedgehog or newer
- Android device or emulator (API 24+)
- A [Groq API key](https://console.groq.com/keys) (free)
- A [SerpAPI key](https://serpapi.com/) for live flight and hotel prices
- A [Yelp Fusion API key](https://docs.developer.yelp.com/docs/fusion-intro) for activity suggestions
- A Firebase project with **Authentication** (email/password) and **Firestore** enabled

### Setup

1. Clone the repo:
   ```bash
   git clone https://github.com/bit-lords-csulb/Travel-Cents.git
   ```

2. Add your API keys to `local.properties` (git-ignored, create if it doesn't exist):
   ```
   GROQ_API_KEY=your-groq-key
   SERP_API_KEY=your-serpapi-key
   YELP_API_KEY=your-yelp-key
   ```

3. Add your Firebase config:
   - Download `google-services.json` from your Firebase console
   - Place it in `app/`

4. Open in Android Studio, let Gradle sync, then **Run**.

---

## Features

### Authentication
- Email/password sign-up and login
- Username lookup via Firestore on login
- Input validation, loading states, error messages

### Trip Planning
- Form: origin, destination, dates, adults/children, travel style, budget + currency, dietary preferences, interests, special requests
- Two-call Groq pipeline:
  1. Generates trip metadata (name, dates, duration)
  2. Generates full event list (flights, hotels, restaurants, activities)
- SerpAPI enriches results with real flight and hotel pricing
- Saves itinerary + all events to Firestore

### Itinerary Viewer & Editor
- Events grouped by date with day headers
- Four event card types: **Flight** (pink), **Hotel** (cyan), **Restaurant** (yellow), **Activity** (purple)
- Real-time Firestore listener
- Inline edit, add, and delete events

### AI Chat
- Conversational travel assistant (Groq)
- Full multi-turn chat history
- Typing indicator

### Social & Chats
- **Group chat** — create trip groups, real-time messaging, last message preview
- **Direct messages** — 1-on-1 chat with friends
- **Friends** — search users, send/accept/reject friend requests, view pending requests

### Group Event Voting
- Propose activities for a group trip with a title, description, location, time, and photo
- Yelp API autocomplete for place search; Wikipedia fallback for descriptions
- Upvote / downvote each proposal; vote count displayed live
- Comment threads on each proposed event
- Creator can delete their own proposals

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
| AI | Groq API (`llama-3.3-70b-versatile`) |
| Flight & Hotel Data | SerpAPI |
| Activity Search | Yelp Fusion API |
| Activity Descriptions | Wikipedia REST API |
| HTTP (Groq/Serp) | Retrofit 2 + OkHttp 4 |
| HTTP (Yelp/Wikipedia) | Ktor Client |
| Serialization | Gson |
| Images | Coil |
| Min SDK | 24 (Android 7.0) |

---

## Project Structure

```
Travel-Cents/
├── app/
│   ├── build.gradle.kts
│   ├── google-services.json
│   └── src/
│       ├── androidTest/java/com/example/travelcents/    # ExampleInstrumentedTest
│       ├── test/java/com/example/travelcents/           # ExampleUnitTest
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/example/travelcents/
│           │   ├── MainActivity.kt
│           │   ├── data/
│           │   │   ├── AuthModel.kt
│           │   │   ├── ChatMessage.kt
│           │   │   ├── FirestoreRepository.kt
│           │   │   ├── GroqApi.kt
│           │   │   ├── MockItineraryData.kt
│           │   │   ├── Trip.kt / TripEvent.kt
│           │   │   ├── model/                          # itinerary, chat, group, request, and API DTO models
│           │   │   └── remote/                         # Groq + Serp services, repositories, and cache
│           │   └── ui/
│           │       ├── TravelCentsNavigation.kt        # login/signup/home navigation entry
│           │       ├── auth/                           # LoginPage, SignUpPage, ForgotPassword, AuthViewModel
│           │       ├── theme/                          # app colors, typography, Material theme
│           │       └── main/
│           │           ├── MainScaffold.kt             # bottom nav shell + nested NavHost
│           │           ├── CurrentPage.kt
│           │           ├── HomePage.kt
│           │           ├── SettingsPage.kt
│           │           ├── aichat/                     # AiTripChatPage, ChatViewModel
│           │           ├── itinerary/                  # ItineraryScreen, EditPlanScreen, SharedTripHeader, ViewModels
│           │           ├── newtrip/                    # landing page, 5-step flow, form page, NewTripViewModel
│           │           └── chats/
│           │               ├── chat/                   # chats list, group/direct chat pages, related ViewModels
│           │               ├── friends/                # add friends, requests, friends list, related ViewModels
│           │               ├── groups/                 # NewTripChatPage, group trip chat ViewModel
│           │               └── voting/                 # event proposals, comments, creation flow, related ViewModels
│           └── res/                                    # themes, strings, launcher assets, backup/data rules
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
└── README.md
```
