# Travel Cents

An AI-powered travel planning Android app built with Jetpack Compose and Kotlin. Plan trips, generate itineraries with AI, collaborate with friends, and vote on group activities.

---

## What It Does

- **Generate a full trip itinerary** — Fill out a form and the app uses an LLM (Groq/OpenAI) for itinerary metadata, then enriches the trip with live flights, hotels, restaurants, and activities.
- **Smart Itinerary Enrichment** — Automatically fetches weather forecasts, popular times (BestTime), walkability scores (WalkScore), and transit directions for your destinations.
- **Multiple Itinerary Views** — Toggle between a sleek timeline view, a detailed single-day view, and a high-level week overview.
- **Chat with an AI travel assistant** — Conversational planning powered by a configurable AI provider (Groq default).
- **Group chats & Social** — Real-time messaging with friends, search users, and manage friend requests.
- **Group event voting** — Propose activities for a group trip, upvote/downvote, and comment on each suggestion.
- **Offline Support & Sync** — Local-first architecture using Room and background synchronization with WorkManager.

---

## How to Run

### Prerequisites

- Android Studio Koala or newer.
- Android device or emulator (API 26+).
- An AI provider API key for any OpenAI-compatible endpoint (Groq recommended).
- A [SerpAPI key](https://serpapi.com/) for live flight and hotel prices.
- A Yelp Fusion API key for activity and restaurant suggestions.
- **Optional:** Mapbox Token (for static maps), BestTime API key (popular times), WalkScore API key, Google Directions API key.
- A Firebase project with **Authentication** and **Firestore** enabled.

### Setup

1. Clone the repo:
   ```bash
   git clone https://github.com/bit-lords-csulb/Travel-Cents.git
   ```

2. Add your API keys to `local.properties`:
   ```properties
   LLM_API_KEY=your-llm-key
   LLM_BASE_URL=https://api.groq.com/openai/v1/
   LLM_MODEL=llama-3.3-70b-versatile
   SERP_API_KEY=your-serpapi-key
   YELP_API_KEY=your-yelp-key
   MAPBOX_TOKEN=pk.your-mapbox-token
   BESTTIME_API_KEY=your-besttime-key
   WALKSCORE_API_KEY=your-walkscore-key
   GOOGLE_DIRECTIONS_KEY=your-google-directions-key
   ```

   TICKETMASTER_API_KEY=your-ticketmaster-consumer-key
   ```

   `GROQ_API_KEY` is still accepted as a fallback for older local setups.
   Ticketmaster Discovery uses the consumer key as the API key; the consumer secret is not needed for read-only event search.

3. Add your Firebase config:
   - Download `google-services.json` from your Firebase console.
   - Place it in `app/`.

4. Open in Android Studio, let Gradle sync, then **Run**.

---

## Features

### AI Trip Generation Pipeline
The generation process follows a structured pipeline:
1. **Metadata Generation**: Groq LLM generates trip name, dates, and IATA codes.
2. **Flight & Hotel Search**: SerpAPI fetches real-time pricing and options.
3. **Local Enrichment**: Yelp Fusion identifies restaurants and activities.
4. **Data Hydration**: Background workers fetch weather, popular times, and walk scores.

### Itinerary & Event Cards
Events are richly detailed with type-specific data:
- **Flight**: Airline details, route info, and pricing.
- **Hotel**: Amenities, check-in/out, and Mapbox/OSM static maps.
- **Restaurant**: Yelp ratings, hours, menus, and popular times.
- **Activity**: Description, reviews, and neighborhood walkability.

### Social & Collaboration
- **Groups**: Create trip groups to collaborate on plans.
- **Voting**: Propose new events and vote on them in real-time.
- **Messaging**: Built-in 1:1 and group chat.

---

## Tech Stack

| Layer | Technology |
|---|---|
| **UI** | Jetpack Compose (Material 3) |
| **Language** | Kotlin 2.1+ |
| **Local DB** | Room |
| **Networking** | Retrofit 2 + OkHttp 4 (AI/Serp), Ktor (Yelp/Wikipedia) |
| **Background** | WorkManager |
| **Maps** | Mapbox Static Images / OpenStreetMap |
| **Auth & Sync** | Firebase (Auth, Firestore, BOM 33+) |
| **Images** | Coil 2.7.0 |
| **Serialization** | Gson + Kotlinx Serialization |

---

## Project Structure

```
Travel-Cents/
├── app/src/main/java/com/example/travelcents/
│   ├── data/                # Data Layer
│   │   ├── ai/              # LLM clients, prompts, and models
│   │   ├── auth/            # Firebase Auth repositories
│   │   ├── firebase/        # Firestore configuration
│   │   ├── local/           # Room Database and DAO
│   │   ├── media/           # Image caching and Static Map factories
│   │   ├── social/          # Friends, Groups, and Messaging
│   │   ├── sync/            # WorkManager sync and hydration logic
│   │   ├── trip/            # Trip/Itinerary repositories (Remote & Local)
│   │   └── user/            # User profile management
│   └── ui/                  # UI Layer (Composables & ViewModels)
│       ├── auth/            # Login, Signup, Google Auth
│       ├── components/      # Shared UI (TcTextField, Buttons, etc.)
│       ├── main/            # Core App Screens
│       │   ├── aichat/      # AI Assistant
│       │   ├── chats/       # Social Hub (DMs, Groups, Voting)
│       │   ├── current/     # Active Trip views (Timeline, Day, Week)
│       │   ├── home/        # Dashboard & Currency Converter
│       │   ├── newTrip/     # Trip Creation Wizard
│       │   └── settings/    # Account & Preferences
│       └── theme/           # DeepSea palette & Typography
├── docs/                    # Feature plans and architecture notes
└── test.py                  # Standalone pipeline debugger (Python)
```

---

## Development Tools

### Pipeline Debugger (`test.py`)
A standalone Python script that mirrors the Android app's trip generation pipeline. Useful for testing prompts and API integrations without rebuilding the app.
- **Usage**: `python test.py --origin "LAX" --destination "CDG" --date-from "2026-06-01" --date-to "2026-06-07"`
- **Output**: Generates a detailed Markdown report and JSON timeline in `debug_pipeline_runs/`.
