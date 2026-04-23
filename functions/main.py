from firebase_functions import https_fn
from firebase_admin import initialize_app
import os
import requests
from groq import Groq
from pinecone import Pinecone
from dotenv import load_dotenv

# Initialize Firebase and load environment variables
initialize_app()
load_dotenv()


@https_fn.on_call()
def generate_itinerary(req: https_fn.CallableRequest):
    # 1. Grab the city from the Android App request
    target_city = req.data.get("city", "London")

    # 2. Get API Keys from .env
    groq_key = os.getenv("GROQ_API_KEY")
    hf_token = os.getenv("HF_TOKEN")
    pinecone_key = os.getenv("PINECONE_API_KEY")

    # 3. Setup Groq (The Brain)
    client = Groq(api_key=groq_key)

    prompt = f"""
    Create a 3-day travel itinerary for {target_city}. 
    Return ONLY a JSON object with this exact structure:
    {{
      "itinerary": [
        {{ "title": "Activity Name", "description": "Short description" }}
      ]
    }}
    Include 3 activities total.
    """

    chat_completion = client.chat.completions.create(
        messages=[{"role": "user", "content": prompt}],
        model="llama-3.1-8b-instant",
        response_format={"type": "json_object"}
    )

    import json
    raw_itinerary = json.loads(chat_completion.choices[0].message.content)

    # 4. Setup Pinecone & Matchmaking
    pc = Pinecone(api_key=pinecone_key)
    index = pc.Index("travel-cents-inventory")

    final_itinerary = []

    for activity in raw_itinerary["itinerary"]:
        # Hugging Face Setup (The Vectorizer)
        hf_api_url = "https://router.huggingface.co/hf-inference/models/BAAI/bge-base-en-v1.5/pipeline/feature-extraction"
        headers = {"Authorization": f"Bearer {hf_token}"}

        # Wrap description in "inputs" as required by HF Inference API
        hf_payload = {"inputs": activity["description"]}

        try:
            hf_response = requests.post(hf_api_url, headers=headers, json=hf_payload)

            if hf_response.status_code == 200:
                vector = hf_response.json()

                # Query Pinecone for the closest matching REAL activity in our DB
                search_result = index.query(
                    vector=vector,
                    top_k=1,
                    include_metadata=True,
                    filter={"city": {"$eq": target_city}}
                )

                if search_result['matches']:
                    best_match = search_result['matches'][0]['metadata']
                    activity['booking_url'] = best_match.get('booking_url', "")
                    activity['real_title'] = best_match.get('title', "")
                    activity['isNativeBookable'] = True
                else:
                    activity['isNativeBookable'] = False
            else:
                print(f"HF Error: {hf_response.status_code} - {hf_response.text}")
                activity['isNativeBookable'] = False

        except Exception as e:
            print(f"Matchmaking Exception: {e}")
            activity['isNativeBookable'] = False

        final_itinerary.append(activity)

    return {"itinerary": final_itinerary}