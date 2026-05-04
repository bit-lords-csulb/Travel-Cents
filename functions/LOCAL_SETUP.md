# Local Setup and Testing Guide

Use this guide to run the Firebase emulator and Android client locally for testing the AI scheduling and Pinecone matchmaking logic.

1. Set up environment variables
   - Go into `functions/` and create a file named `.env`.
   - Add:

     ```env
     GROQ_API_KEY=your_groq_api_key_here
     HF_TOKEN=your_huggingface_token_here
     PINECONE_API_KEY=your_pinecone_api_key_here
     ```

2. Create the virtual environment
   - Windows:

     ```powershell
     python -m venv venv
     ```

   - Mac / Linux:

     ```bash
     python3 -m venv venv
     ```

3. Activate the virtual environment
   - Windows (PowerShell):

     ```powershell
     .\venv\Scripts\Activate.ps1
     ```

   - Mac / Linux:

     ```bash
     source venv/bin/activate
     ```

   - Your terminal prompt should show `(venv)` when the environment is active.

4. Install dependencies
   - With the virtual environment active, run:

     ```bash
     pip install -r requirements.txt
     ```

5. Start the Firebase emulator
   - Go to the project root, where `firebase.json` lives.
   - Firebase CLI must be installed and authenticated first.
   - Windows or Mac:

     ```bash
     npm install -g firebase-tools
     ```

   - Then start the emulator:

     ```bash
     firebase emulators:start
     ```

   - Leave this terminal open. The Python backend listens here for requests from the Android app.

6. Run the Android client
   - Open the project in Android Studio.
   - Let Gradle sync if prompted.
   - Select your emulator, such as `Pixel 7 API 34`, and click `Run`.
   - Navigate to the `New Trip` screen.
   - Enter your constraints, such as dates, budget, and interests, and watch the terminal logs as the AI builds your itinerary.
