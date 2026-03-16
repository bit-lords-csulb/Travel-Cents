# Travel Cents

An AI-powered travel planning Android app built with Jetpack Compose and Kotlin. Plan trips, generate itineraries with AI, and collaborate with friends.

---

## What It Does

Travel Cents lets you:

- **Generate a full trip itinerary** by filling out a form (destination, dates, travelers, style, budget, interests) — the app sends your preferences to the Groq AI API and builds a day-by-day schedule with flights, hotels, restaurants, and activities
- **Chat with an AI travel assistant** conversationally to plan or refine your trip
- **View your itinerary** with type-specific event cards grouped by day
- **Collaborate via group chat** — create a trip group with friends and message in real time
- **Authenticate** with email or username via Firebase

---

## How to Run

### Prerequisites

- Android Studio Hedgehog or newer
- Android device or emulator (API 24+)
- A [Groq API key](https://console.groq.com/keys) (free)
- A [SerpAPI key](https://serpapi.com/) for live flight and hotel data
- A Firebase project with **Authentication** (email/password) and **Firestore** enabled

### Setup

1. Clone the repo:
   ```bash
   git clone https://github.com/bit-lords-csulb/Travel-Cents.git
   ```

2. Add your API keys to `local.properties` (create the file if it doesn't exist — it is git-ignored):
   ```
   GROQ_API_KEY=your-groq-api-key-here
   SERP_API_KEY=your-serpapi-key-here
   ```

3. Add your Firebase config:
   - Download `google-services.json` from your Firebase project console
   - Place it in `app/`

4. Open the project in Android Studio, let Gradle sync, then **Run**.

---

## Current Features

### Authentication
a- Email or username login, email and password sign-up
- Username resolved to email via Firestore lookup on login
- Input validation (password length, required fields)
- Loading states and error messages

### Trip Planning (New Trip tab)
- Form with: origin, destination, dates, adults/children count, travel style (Budget / Comfort / Luxury), budget + currency, dietary preferences, interests, and special requests
- Submits to **Groq `llama-3.3-70b-versatile`** via a two-call pipeline:
  1. Generates trip metadata (name, dates, duration)
  2. Generates the full events list (flights, hotels, restaurants, activities)
- Saves the generated itinerary and all events to **Firestore** under the logged-in user

### Itinerary Viewer (Current tab)
- Displays the most recent trip or a specific trip passed via navigation
- Events grouped by date with day headers
- Four styled card types:
  - **Flight** — pink accent, airline + flight number
  - **Hotel** — cyan accent, hotel name + check-in time
  - **Restaurant** — yellow accent, name + cuisine
  - **Activity** — purple accent, name + location
- Real-time Firestore listener (updates live if data changes)

### AI Chat (from New Trip tab)
- Conversational travel assistant powered by Groq
- Maintains full multi-turn chat history
- Typing indicator while waiting for a response
- Back button returns to New Trip form

### Group Chat (Chats tab)
- Create a trip group by searching friends and picking a destination
- Real-time group messaging via Firestore
- Group list with search, last message preview, and timestamps
- Message bubbles styled by sender (mine on the right, others on the left)

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
| AI | Groq API (llama-3.3-70b-versatile) |
| Live Data | SerpAPI (flights + hotels) |
| HTTP | Retrofit 2 + OkHttp 4 |
| Serialization | Gson |
| Images | Coil |
| Min SDK | 24 (Android 7.0) |

---

## Project Structure

```
app/src/main/java/com/example/travelcents/
├── MainActivity.kt
├── TravelCentsNavigation.kt       # top-level nav graph
├── data/
│   ├── model/                     # Itinerary, TravelEvent, TravelRequest, GroqModels
│   ├── remote/                    # GroqRepository, GroqApiService
│   ├── AuthModel.kt
│   ├── FirestoreRepository.kt
│   ├── ChatMessage.kt
│   └── GroqApi.kt
└── ui/
    ├── theme/                     # DeepSea color palette, Typography
    ├── auth/                      # LoginPage, SignUpPage, AuthViewModel
    └── main/
        ├── MainScaffold.kt        # bottom nav + NavHost
        ├── ItineraryScreen.kt     # trip itinerary display
        ├── ItineraryViewModel.kt
        ├── NewTripPage.kt         # trip planning form
        ├── NewTripViewModel.kt
        ├── AiTripChatPage.kt      # AI chat screen
        ├── ChatViewModel.kt       # AI chat ViewModel
        └── chats/                 # group chat screens + ViewModels
```

---

## Open PRs

| PR | Branch | Description |
|---|---|---|
| #15 | `EditItinerary` | Edit, add, and delete itinerary events |